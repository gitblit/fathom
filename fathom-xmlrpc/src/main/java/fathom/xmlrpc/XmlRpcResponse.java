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

import com.google.common.base.Throwables;
import fathom.exception.FathomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author James Moger
 */
class XmlRpcResponse {

    private final Logger log = LoggerFactory.getLogger(XmlRpcResponse.class);

    public static Charset charset = StandardCharsets.UTF_8;

    private final DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    private final XmlRpcMethodRegistrar methodRegistrar;

    XmlRpcResponse(XmlRpcMethodRegistrar methodRegistrar) {
        this.methodRegistrar = methodRegistrar;
    }

    byte[] process(XmlRpcRequest request) {
        try {
            log.debug("Processing '{}' {}", request.getMethodName(), request.getMethodArguments());

            // check for errors from the XML parser
            if (request.isParsingError()) {
                throw new FathomException(request.getErrorMessage());
            }

            Object result = methodRegistrar.invoke(request.getMethodName(), request.getMethodArguments());

            XmlWriter writer = new XmlWriter();
            writeResponse(result, writer);
            return writer.getBytes();
        } catch (Exception x) {
            Throwable t = Throwables.getRootCause(x);
            log.error(t.getMessage(), t);

            XmlWriter writer = new XmlWriter();
            String message = t.toString();
            writeError(999, message, writer);
            return writer.getBytes();
        }
    }

    /**
     * Writes an XML-RPC response to the XML writer.
     */
    void writeResponse(Object param, XmlWriter writer) {
        writer.startElement("methodResponse");
        writer.startElement("params");
        writer.startElement("param");
        writeObject(param, writer);
        writer.endElement("param");
        writer.endElement("params");
        writer.endElement("methodResponse");
    }

    /**
     * Writes an XML-RPC error response to the XML writer.
     */
    void writeError(int code, String message, XmlWriter writer) {
        Map<String, Object> map = new HashMap<>();
        map.put("faultCode", code);
        map.put("faultString", message);
        writer.startElement("methodResponse");
        writer.startElement("fault");
        writeObject(map, writer);
        writer.endElement("fault");
        writer.endElement("methodResponse");
    }

    /**
     * Writes the XML representation of a supported Java object to the XML writer.
     */
    void writeObject(Object what, XmlWriter writer) {
        if (what == null) {
            return;
        }

        writer.startElement("value");

        if (what instanceof String) {
            writer.chardata(what.toString());
        } else if (what instanceof Integer) {
            writer.startElement("int");
            writer.write(what.toString());
            writer.endElement("int");
        } else if (what instanceof Boolean) {
            writer.startElement("boolean");
            writer.write(((Boolean) what) ? "1" : "0");
            writer.endElement("boolean");
        } else if (what instanceof Double || what instanceof Float) {
            writer.startElement("double");
            writer.write(what.toString());
            writer.endElement("double");
        } else if (what instanceof Date) {
            writer.startElement("dateTime.iso8601");
            Date d = (Date) what;
            writer.write(df.format(d));
            writer.endElement("dateTime.iso8601");
        } else if (what instanceof byte[]) {
            writer.startElement("base64");
            writer.write(Base64.getEncoder().encodeToString((byte[]) what));
            writer.endElement("base64");
        } else if (what instanceof Collection) {
            writer.startElement("array");
            writer.startElement("data");
            Collection v = (Collection) what;
            for (Object o : v) {
                writeObject(o, writer);
            }
            writer.endElement("data");
            writer.endElement("array");
        } else if (what instanceof Object[]) {
            writer.startElement("array");
            writer.startElement("data");
            Object[] v = (Object[]) what;
            for (Object o : v) {
                writeObject(o, writer);
            }
            writer.endElement("data");
            writer.endElement("array");
        } else if (what instanceof Map) {
            writer.startElement("struct");
            Map<String, Object> map = (Map<String, Object>) what;
            for (String nextkey : map.keySet()) {
                Object nextval = map.get(nextkey);
                writer.startElement("member");
                writer.startElement("name");
                writer.write(nextkey);
                writer.endElement("name");
                writeObject(nextval, writer);
                writer.endElement("member");
            }
            writer.endElement("struct");
        } else {
            throw new RuntimeException("unsupported Java type: " + what.getClass());
        }
        writer.endElement("value");
    }

    static class XmlWriter {

        StringBuilder sb;
        Charset charset;

        XmlWriter() {
            this(new StringBuilder(), XmlRpcResponse.charset);
        }

        XmlWriter(StringBuilder sb, Charset charset) {
            this.sb = sb;
            this.charset = charset;
            sb.append("<?xml version=\"1.0\" encoding=\"").append(charset.name()).append("\"?>");
        }

        void startElement(String elem) {
            sb.append("<");
            sb.append(elem);
            sb.append(">");
        }

        void endElement(String elem) {
            sb.append("</");
            sb.append(elem);
            sb.append(">");
        }

        void chardata(String text) {
            int l = text.length();
            for (int i = 0; i < l; i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '&':
                        sb.append("&amp;");
                        break;
                    default:
                        sb.append(c);
                }
            }
        }

        void write(String text) {
            sb.append(text);
        }

        byte[] getBytes() {
            return sb.toString().getBytes(charset);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

}


