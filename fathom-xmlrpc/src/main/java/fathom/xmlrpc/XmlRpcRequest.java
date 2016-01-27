/*
 * Copyright (C) 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fathom.xmlrpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author James Moger
 */
class XmlRpcRequest extends HandlerBase {

    private final Logger log = LoggerFactory.getLogger(XmlRpcRequest.class);

    enum DataType {
        String, Integer, Boolean, Double, Date, Base64, Struct, Array
    }

    private final DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    private final List<Object> methodArguments;
    private final Stack<RequestValue> values;
    private final StringBuilder cdata;

    private String methodName;
    private RequestValue currentRequestValue;

    boolean readCdata;

    // Error level + message
    private boolean parsingError;
    private String errorMessage;

    XmlRpcRequest() {
        this.errorMessage = null;
        this.values = new Stack<>();
        this.cdata = new StringBuilder(128);
        this.readCdata = false;
        this.currentRequestValue = null;
        this.methodArguments = new ArrayList<>();
    }

    boolean isParsingError() {
        return parsingError;
    }

    void parse(InputStream is) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(is), this);
        } catch (Exception e) {
            log.error("Failed to parse XML-RPC request", e);
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if (!readCdata) {
            return;
        }
        cdata.append(ch, start, length);
    }

    @Override
    public void endElement(String name) throws SAXException {
        // finalize character data, if appropriate
        if (currentRequestValue != null && readCdata) {
            currentRequestValue.characterData(cdata.toString());
            cdata.setLength(0);
            readCdata = false;
        }

        if ("value".equals(name)) {
            int depth = values.size();
            // Only handle top level objects or objects contained in arrays here.
            // For objects contained in structs, wait for </member> (see code below).
            if (depth < 2 || values.elementAt(depth - 2).getType() != DataType.Struct) {
                RequestValue v = currentRequestValue;
                values.pop();
                if (depth < 2) {
                    methodArguments.add(v.value);
                    currentRequestValue = null;
                } else {
                    // add object to sub-array; if current container is a struct, add later (at </member>)
                    currentRequestValue = values.peek();
                    currentRequestValue.endElement(v);
                }
            }
        }

        // Handle objects contained in structs.
        if ("member".equals(name)) {
            RequestValue v = currentRequestValue;
            values.pop();
            currentRequestValue = values.peek();
            currentRequestValue.endElement(v);
        } else if ("methodName".equals(name)) {
            methodName = cdata.toString();
            cdata.setLength(0);
            readCdata = false;
        }
    }

    @Override
    public void startElement(String name, AttributeList atts) throws SAXException {

        if ("value".equals(name)) {
            RequestValue v = new RequestValue();
            values.push(v);
            currentRequestValue = v;
            cdata.setLength(0);
            readCdata = true;
        } else if ("methodName".equals(name)) {
            cdata.setLength(0);
            readCdata = true;
        } else if ("name".equals(name)) {
            cdata.setLength(0);
            readCdata = true;
        } else if ("string".equals(name)) {
            cdata.setLength(0);
            readCdata = true;
        } else if ("i4".equals(name) || "int".equals(name)) {
            currentRequestValue.setType(DataType.Integer);
            cdata.setLength(0);
            readCdata = true;
        } else if ("boolean".equals(name)) {
            currentRequestValue.setType(DataType.Boolean);
            cdata.setLength(0);
            readCdata = true;
        } else if ("double".equals(name)) {
            currentRequestValue.setType(DataType.Double);
            cdata.setLength(0);
            readCdata = true;
        } else if ("dateTime.iso8601".equals(name)) {
            currentRequestValue.setType(DataType.Date);
            cdata.setLength(0);
            readCdata = true;
        } else if ("base64".equals(name)) {
            currentRequestValue.setType(DataType.Base64);
            cdata.setLength(0);
            readCdata = true;
        } else if ("struct".equals(name)) {
            currentRequestValue.setType(DataType.Struct);
        } else if ("array".equals(name)) {
            currentRequestValue.setType(DataType.Array);
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        log.error("Error parsing XML-RPC", e);
        errorMessage = e.toString();
        parsingError = true;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        log.error("Error parsing XML-RPC", e);
        errorMessage = e.toString();
        parsingError = true;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<Object> getMethodArguments() {
        return methodArguments;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * This represents an XML-RPC RequestValue while the request is being parsed.
     */
    class RequestValue {

        DataType type;
        Object value;
        // the name to use for the next member of struct values
        String nextMemberName;

        Map<String, Object> struct;
        List<Object> array;

        /**
         * Constructor.
         */
        RequestValue() {
            this.type = DataType.String;
        }

        /**
         * Notification that a new child element has been parsed.
         */
        void endElement(RequestValue child) {
            if (type == DataType.Array) {
                array.add(child.value);
            } else if (type == DataType.Struct) {
                struct.put(nextMemberName, child.value);
            }
        }

        /**
         * Set the type of this value. If it's a container, create the corresponding java container.
         */
        void setType(DataType type) {
            this.type = type;
            if (type == DataType.Array) {
                value = array = new ArrayList<>();
            }
            if (type == DataType.Struct) {
                value = struct = new HashMap<>();
            }
        }

        /**
         * Set the character data for the element and interpret it according to the
         * element type
         */
        void characterData(String cdata) {
            switch (type) {
                case Integer:
                    value = new Integer(cdata.trim());
                    break;
                case Boolean:
                    value = "1".equals(cdata.trim());
                    break;
                case Double:
                    value = new Double(cdata.trim());
                    break;
                case Date:
                    try {
                        value = df.parse(cdata.trim().replace('.', ':'));
                    } catch (ParseException p) {
                        throw new RuntimeException(p.getMessage());
                    }
                    break;
                case Base64:
                    value = Base64.getDecoder().decode(cdata);
                    break;
                case String:
                    value = cdata;
                    break;
                case Struct:
                    // this is the name to use for the next member of this struct
                    nextMemberName = cdata;
                    break;
            }
        }

        DataType getType() {
            return type;
        }

        @Override
        public String toString() {
            return (type.name() + " element " + value);
        }
    }

}


