/*
 * Copyright (C) 2015 the original author or authors.
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

package fathom.rest.swagger;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import fathom.rest.RestServlet;
import fathom.rest.controller.Body;
import fathom.rest.controller.ControllerHandler;
import fathom.rest.controller.Header;
import fathom.rest.controller.Param;
import fathom.rest.controller.Produces;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import io.swagger.models.ArrayModel;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UUIDProperty;
import io.swagger.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.Router;
import ro.pippo.core.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author James Moger
 */
public class SwaggerBuilder {

    private final static Logger log = LoggerFactory.getLogger(SwaggerBuilder.class);

    private static final Pattern PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE = Pattern.compile("\\{(.*?)(:\\s(.*?))?\\}");

    private static final List<String> METHODS = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private Settings settings;

    private Router router;

    public SwaggerBuilder(Settings settings, Router router) {
        this.settings = settings;
        this.router = router;
    }

    /**
     * Generates a Swagger 2.0 JSON specification from the collection of routes.
     *
     * @param routes
     * @return a Swagger 2.0 JSON specification
     */
    public String generateJSON(Collection<Route> routes) {
        Swagger swagger = build(routes);
        String json = Json.pretty(swagger);
        return json;
    }

    /**
     * Builds a Swagger object from a collection of routes.
     *
     * @param routes
     * @return the Swagger object
     */
    public Swagger build(Collection<Route> routes) {

        Swagger swagger = new Swagger();

        // application metadata
        Info info = new Info();
        info.setTitle(settings.getString("swagger.api.title", settings.getApplicationName()));
        info.setVersion(settings.getString("swagger.api.version", settings.getApplicationVersion()));
        info.setDescription(loadStringResource(settings.getFileUrl("swagger.api.description", "classpath:swagger/api.md")));

        // api support contact
        Contact contact = new Contact();
        contact.setName(settings.getString("swagger.api.contact.name", null));
        contact.setUrl(settings.getString("swagger.api.contact.url", null));
        contact.setEmail(settings.getString("swagger.api.contact.email", null));
        if (Strings.isNullOrEmpty(contact.getName())
                && Strings.isNullOrEmpty(contact.getUrl())
                && Strings.isNullOrEmpty(contact.getEmail())) {
            // no contact info
        } else {
            info.setContact(contact);
        }

        // License
        License license = new License();
        license.setName(settings.getString("swagger.api.license.name", null));
        license.setUrl(settings.getString("swagger.api.license.url", null));
        if (Strings.isNullOrEmpty(license.getName())
                && Strings.isNullOrEmpty(license.getUrl())) {
            // no license
        } else {
            info.setLicense(license);
        }

        swagger.setInfo(info);

        // transport and base url details
        List<Scheme> schemes = new ArrayList<>();
        if (settings.getInteger(Settings.Setting.undertow_httpPort, 0) > 0) {
            schemes.add(Scheme.HTTP);
        }
        if (settings.getInteger(Settings.Setting.undertow_httpsPort, 0) > 0) {
            schemes.add(Scheme.HTTPS);
        }
        swagger.setSchemes(schemes);

        String contextPath = StringUtils.removeStart(Strings.emptyToNull(settings.getString(Settings.Setting.undertow_contextPath, null)), "/");
        String servletPath = StringUtils.removeStart(Strings.emptyToNull(settings.getString(RestServlet.SETTING_URL, null)), "/");
        String apiPath = StringUtils.removeStart(Strings.emptyToNull(settings.getString("swagger.api.path", null)), "/");
        String applicationApiPath = Joiner.on("/").skipNulls().join(contextPath, servletPath, apiPath);
        swagger.setBasePath(applicationApiPath);

        // register each valid RESTful route
        for (Route route : routes) {
            if (route.getRouteHandler() instanceof ControllerHandler) {
                ControllerHandler handler = (ControllerHandler) route.getRouteHandler();
                if (canRegister(route, handler)) {
                    registerHandler(swagger, route, handler);
                }
            }
        }

        return swagger;
    }

    /**
     * Determines if this controller handler can be registered in the Swagger specification.
     *
     * @param route
     * @param handler
     * @return true if the controller handler can be registered in the Swagger specification
     */
    protected boolean canRegister(Route route, ControllerHandler handler) {
        if (!METHODS.contains(route.getRequestMethod().toUpperCase())) {
            log.debug("Skip {} {}, {} Swagger does not support specified HTTP method",
                    route.getRequestMethod(), route.getUriPattern(), Util.toString(handler.getControllerMethod()));
            return false;
        }

        List<String> produces = getProduces(handler);
        if (produces == null || produces.isEmpty()) {
            log.debug("Skip {} {}, {} does not generate RESTful API content",
                    route.getRequestMethod(), route.getUriPattern(), Util.toString(handler.getControllerMethod()));
            return false;
        }

        produces.remove(Produces.HTML);
        produces.remove(Produces.TEXT);

        if (produces.isEmpty()) {
            log.debug("Skip {} {}, {} does not generate RESTful API content",
                    route.getRequestMethod(), route.getUriPattern(), Util.toString(handler.getControllerMethod()));
            return false;
        }

        if (handler.getControllerMethod().isAnnotationPresent(Undocumented.class)
                || handler.getControllerMethod().getDeclaringClass().isAnnotationPresent(Undocumented.class)) {
            log.debug("Skip {} {}, {} is annotated as Undocumented",
                    route.getRequestMethod(), route.getUriPattern(), Util.toString(handler.getControllerMethod()));
            return false;
        }

        return true;
    }

    /**
     * Extracts the declared Produced content-types from the method and/or controller class.
     *
     * @param handler
     * @return the list of produced content-types for the method
     */
    protected List<String> getProduces(ControllerHandler handler) {
        if (handler.getControllerMethod().isAnnotationPresent(Produces.class)) {
            Produces produces = handler.getControllerMethod().getAnnotation(Produces.class);
            return new ArrayList<>(Arrays.asList(produces.value()));
        } else if (handler.getControllerMethod().getDeclaringClass().isAnnotationPresent(Produces.class)) {
            Produces produces = handler.getControllerMethod().getDeclaringClass().getAnnotation(Produces.class);
            return new ArrayList<>(Arrays.asList(produces.value()));
        }
        return null;
    }

    /**
     * Registers a ControllerHandler as a Swagger Path.
     *
     * @param swagger
     * @param route
     * @param handler
     */
    protected void registerHandler(Swagger swagger, Route route, ControllerHandler handler) {

        Method method = handler.getControllerMethod();
        Class<?> controller = method.getDeclaringClass();

        List<String> produces = getProduces(handler);

        Operation operation = new Operation();
        operation.setSummary(getSummary(method));
        operation.setDescription(getNotes(method));
        operation.setOperationId(Util.toString(method));
        operation.setConsumes(produces);
        operation.setProduces(produces);
        operation.setDeprecated(method.isAnnotationPresent(Deprecated.class)
                || controller.isAnnotationPresent(Deprecated.class));

        registerResponses(swagger, operation, method);

        Tag tag = getTag(controller);
        if (tag == null) {
            operation.addTag(controller.getSimpleName());
        } else {
            swagger.addTag(tag);
            operation.addTag(tag.getName());
        }

        String operationPath = StringUtils.removeStart(registerParameters(swagger, operation, route, method), swagger.getBasePath());
        if (swagger.getPath(operationPath) == null) {
            swagger.path(operationPath, new Path());
        }

        Path path = swagger.getPath(operationPath);
        path.set(route.getRequestMethod().toLowerCase(), operation);
        log.debug("Add {} {} => {}",
                route.getRequestMethod(), operationPath, Util.toString(method));
    }

    protected void registerResponses(Swagger swagger, Operation operation, Method method) {
        if (method.isAnnotationPresent(ResponseCodes.class)) {
            ResponseCodes responseCodes = method.getAnnotation(ResponseCodes.class);
            for (ResponseCode responseCode : responseCodes.value()) {
                registerResponse(swagger, operation, responseCode);
            }
        } else if (method.isAnnotationPresent(ResponseCode.class)) {
            ResponseCode responseCode = method.getAnnotation(ResponseCode.class);
            registerResponse(swagger, operation, responseCode);
        }
    }

    protected void registerResponse(Swagger swagger, Operation operation, ResponseCode responseCode) {
        Response response = new Response();
        response.setDescription(responseCode.message());

        if (responseCode.returns() != Void.class) {
            // Return type
            if (responseCode.returns().isArray()) {
                // ARRAY[]
                Class<?> componentClass = responseCode.returns().getComponentType();
                ArrayProperty arrayProperty = new ArrayProperty();
                Property componentProperty = getSwaggerProperty(swagger, componentClass);
                arrayProperty.setItems(componentProperty);
                response.setSchema(arrayProperty);
            } else {
                // Object
                Property returnProperty = getSwaggerProperty(swagger, responseCode.returns());
                response.setSchema(returnProperty);
            }
        }

        operation.response(responseCode.code(), response);
    }

    protected String registerModel(Swagger swagger, Class<?> modelClass) {
        final Tag modelTag = getModelRef(modelClass);
        final String ref = modelTag.getName();

        if (swagger.getDefinitions() != null && swagger.getDefinitions().containsKey(ref)) {
            // model already registered
            return ref;
        }

        ModelImpl model = new ModelImpl();
        swagger.addDefinition(modelTag.getName(), model);

        if (!Strings.isNullOrEmpty(modelTag.getDescription())) {
            model.setDescription(modelTag.getDescription());
        }

        // document any exposed model properties
        for (Field field : modelClass.getDeclaredFields()) {

            if (field.isAnnotationPresent(Undocumented.class)) {
                // undocumented field, skip
                continue;
            }

            Property property;
            Class<?> fieldType = field.getType();
            if (fieldType.isArray()) {
                Class<?> componentType = fieldType.getComponentType();
                Property componentProperty = getSwaggerProperty(swagger, componentType);
                ArrayProperty arrayProperty = new ArrayProperty();
                arrayProperty.setItems(componentProperty);
                property = arrayProperty;
            } else {
                property = getSwaggerProperty(swagger, fieldType);
            }
            property.setRequired(field.isAnnotationPresent(NotNull.class));

            if (field.isAnnotationPresent(Desc.class)) {
                Desc desc = field.getAnnotation(Desc.class);
                property.setDescription(desc.value());
            }

            if (field.isAnnotationPresent(Example.class)) {
                Example example = field.getAnnotation(Example.class);
                property.setExample(example.value());
            }

            model.addProperty(field.getName(), property);

        }

        return ref;
    }

    protected Tag getModelRef(Class<?> modelClass) {
        if (modelClass.isAnnotationPresent(fathom.rest.swagger.Tag.class)) {
            fathom.rest.swagger.Tag annotation = modelClass.getAnnotation(fathom.rest.swagger.Tag.class);
            Tag tag = new Tag();
            tag.setName(Optional.fromNullable(Strings.emptyToNull(annotation.name())).or(modelClass.getSimpleName()));
            tag.setDescription(annotation.description());
            return tag;
        }
        Tag tag = new Tag();
        tag.setName(modelClass.getName());
        return tag;
    }

    protected String registerParameters(Swagger swagger, Operation operation, Route route, Method method) {
        Map<String, Object> pathParameterPlaceholders = new HashMap<>();
        for (String uriParameterName : getUriParameterNames(route.getUriPattern())) {
            // path parameters are required
            PathParameter pathParameter = new PathParameter();
            pathParameter.setName(uriParameterName);
            setPropertyType(swagger, pathParameter, method);
            pathParameter.setRequired(true);

            operation.addParameter(pathParameter);

            pathParameterPlaceholders.put(uriParameterName, "{" + uriParameterName + "}");
        }

        // identify query/form parameters
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter methodParameter = method.getParameters()[i];

            // identify parameter name and pattern from method signature
            String methodParameterName = methodParameter.getName();
            if (methodParameter.isAnnotationPresent(Param.class)) {
                Param param = methodParameter.getAnnotation(Param.class);
                if (!Strings.isNullOrEmpty(param.value())) {
                    methodParameterName = param.value();
                }
            }

            if (pathParameterPlaceholders.containsKey(methodParameterName)) {
                // path parameter already accounted for
                continue;
            }

            if (methodParameter.isAnnotationPresent(Body.class)) {

                // BODY
                BodyParameter bodyParameter = new BodyParameter();
                bodyParameter.setName(methodParameterName);
                bodyParameter.setDescription(getDescription(methodParameter));
                bodyParameter.setRequired(true);

                if (methodParameter.getType().isArray()) {
                    // ARRAY []
                    Property property = getSwaggerProperty(swagger, methodParameter.getType().getComponentType());
                    ArrayModel arrayModel = new ArrayModel();
                    arrayModel.setItems(property);
                    bodyParameter.setSchema(arrayModel);
                } else if (Collection.class.isAssignableFrom(methodParameter.getType())) {
                    // COLLECTION
                    Property property = getSwaggerProperty(swagger, getParameterGenericType(method, i));
                    ArrayModel arrayModel = new ArrayModel();
                    arrayModel.setItems(property);
                    bodyParameter.setSchema(arrayModel);
                } else {
                    // OBJECT
                    bodyParameter.setSchema(new RefModel(methodParameter.getType().getName()));
                }

                operation.addParameter(bodyParameter);

            } else if (methodParameter.isAnnotationPresent(Header.class)) {

                // HEADER
                Header header = methodParameter.getAnnotation(Header.class);
                HeaderParameter headerParameter = new HeaderParameter();
                if (Strings.isNullOrEmpty(header.value())) {
                    headerParameter.setName(methodParameterName);
                } else {
                    headerParameter.setName(header.value());
                }

                headerParameter.setDescription(getDescription(methodParameter));
                setPropertyType(swagger, headerParameter, method);

                operation.addParameter(headerParameter);

            } else if (methodParameter.isAnnotationPresent(Form.class)) {

                // FORM
                FormParameter formParameter = new FormParameter();
                formParameter.setName(methodParameterName);
                formParameter.setDescription(getDescription(methodParameter));
                setPropertyType(swagger, formParameter, method);

                operation.addParameter(formParameter);

                operation.setConsumes(Arrays.asList(HttpConstants.ContentType.APPLICATION_FORM_URLENCODED));

            } else {

                // QUERY
                QueryParameter queryParameter = new QueryParameter();
                queryParameter.setName(methodParameterName);
                queryParameter.setDescription(getDescription(methodParameter));
                setPropertyType(swagger, queryParameter, method);

                operation.addParameter(queryParameter);
            }

        }

        // we need to rewrite the uripattern without regex for Swagger
        // e.g. /employee/{id: 0-9+} must be rewritten as /employee/{id}
        String swaggerUri = router.uriFor(route.getUriPattern(), pathParameterPlaceholders);
        return swaggerUri;
    }

    protected Tag getTag(Class<?> controllerClass) {
        if (controllerClass.isAnnotationPresent(fathom.rest.swagger.Tag.class)) {
            fathom.rest.swagger.Tag annotation = controllerClass.getAnnotation(fathom.rest.swagger.Tag.class);
            Tag tag = new Tag();
            tag.setName(Optional.fromNullable(Strings.emptyToNull(annotation.name())).or(controllerClass.getSimpleName()));
            tag.setDescription(annotation.description());
            if (!Strings.isNullOrEmpty(tag.getDescription())) {
                return tag;
            }
        }
        return null;
    }

    protected String getSummary(Method method) {
        if (method.isAnnotationPresent(Summary.class)) {
            Summary annotation = method.getAnnotation(Summary.class);
            return annotation.value();
        }
        return Util.toString(method);
    }

    protected String getDescription(Parameter parameter) {
        if (parameter.isAnnotationPresent(Desc.class)) {
            Desc annotation = parameter.getAnnotation(Desc.class);
            return annotation.value();
        }
        return null;
    }

    protected String getNotes(Method method) {
        if (method.isAnnotationPresent(Notes.class)) {
            Notes notes = method.getAnnotation(Notes.class);
            String resource = "classpath:swagger/" + method.getDeclaringClass().getName().replace('.', '/')
                    + "/" + method.getName() + ".md";
            if (!Strings.isNullOrEmpty(notes.value())) {
                resource = notes.value();
            }

            String content = loadStringResource(resource);
            if (Strings.isNullOrEmpty(content)) {
                log.error("'{}' specifies @{} but '{}' was not found!",
                        Util.toString(method), Notes.class.getSimpleName(), resource);
            }
            return content;
        }
        return null;
    }

    protected List<String> getUriParameterNames(String uriPattern) {
        ArrayList list = new ArrayList();
        Matcher matcher = PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE.matcher(uriPattern);

        while (matcher.find()) {
            list.add(matcher.group(1));
        }

        return list;
    }

    protected void setPropertyType(Swagger swagger, AbstractSerializableParameter swaggerParameter, Method method) {

        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter methodParameter = method.getParameters()[i];

            // identify parameter name and pattern from method signature
            String methodParameterName = methodParameter.getName();
            if (methodParameter.isAnnotationPresent(Param.class)) {
                Param param = methodParameter.getAnnotation(Param.class);
                if (!Strings.isNullOrEmpty(param.value())) {
                    methodParameterName = param.value();
                }
            }

            if (methodParameterName.equals(swaggerParameter.getName())) {
                // determine Swagger property from type of method parameter
                Property swaggerProperty = null;
                Class parameterClass = methodParameter.getType();
                if (parameterClass.isArray()) {
                    // ARRAYS
                    Property componentProperty = getSwaggerProperty(swagger, parameterClass.getComponentType());
                    ArrayProperty arrayProperty = new ArrayProperty(componentProperty);
                    arrayProperty.setUniqueItems(false);
                    swaggerProperty = arrayProperty;
                } else if (Collection.class.isAssignableFrom(parameterClass)) {
                    // COLLECTIONS
                    Property componentProperty = getSwaggerProperty(swagger, getParameterGenericType(method, i));
                    ArrayProperty arrayProperty = new ArrayProperty(componentProperty);
                    arrayProperty.setUniqueItems(Set.class.isAssignableFrom(parameterClass));
                    swaggerProperty = arrayProperty;
                } else {
                    // TYPES
                    swaggerProperty = getSwaggerProperty(swagger, parameterClass);
                }

                if (swaggerProperty != null) {
                    swaggerParameter.setDescription(getDescription(methodParameter));
                    swaggerParameter.setRequired(isRequired(methodParameter));
                    swaggerParameter.setProperty(swaggerProperty);
                    break;
                }
            }
        }
    }

    protected Class<?> getParameterGenericType(Method method, int i) {
        Type parameterType = method.getGenericParameterTypes()[i];
        if (!ParameterizedType.class.isAssignableFrom(parameterType.getClass())) {
            throw new FathomException("Please specify a generic parameter type for '{}', parameter {} of '{}'",
                    method.getParameterTypes()[i].getName(), i, Util.toString(method));
        }

        ParameterizedType parameterizedType = (ParameterizedType) parameterType;
        try {
            Class<?> genericClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            return genericClass;
        } catch (ClassCastException e) {
            throw new FathomException("Please specify a generic parameter type for '{}', parameter {} of '{}'",
                    method.getParameterTypes()[i].getName(), i, Util.toString(method));
        }
    }

    protected boolean isRequired(Parameter parameter) {
        return parameter.isAnnotationPresent(Body.class)
                || parameter.isAnnotationPresent(NotNull.class);
    }

    protected Property getSwaggerProperty(Swagger swagger, Class<?> parameterClass) {
        Property swaggerProperty = null;
        if (byte.class == parameterClass || Byte.class == parameterClass) {
            // STRING
            swaggerProperty = new StringProperty("byte");
        } else if (char.class == parameterClass || Character.class == parameterClass) {
            // CHAR is STRING LEN 1
            StringProperty property = new StringProperty();
            property.setMaxLength(1);
            swaggerProperty = property;
        } else if (short.class == parameterClass || Short.class == parameterClass) {
            // SHORT is INTEGER with 16-bit max & min
            IntegerProperty property = new IntegerProperty();
            property.setMinimum((double) Short.MIN_VALUE);
            property.setMaximum((double) Short.MAX_VALUE);
            swaggerProperty = property;
        } else if (int.class == parameterClass || Integer.class == parameterClass) {
            // INTEGER
            swaggerProperty = new IntegerProperty();
        } else if (long.class == parameterClass || Long.class == parameterClass) {
            // LONG
            swaggerProperty = new LongProperty();
        } else if (float.class == parameterClass || Float.class == parameterClass) {
            // FLOAT
            swaggerProperty = new FloatProperty();
        } else if (double.class == parameterClass || Double.class == parameterClass) {
            // DOUBLE
            swaggerProperty = new DoubleProperty();
        } else if (BigDecimal.class == parameterClass) {
            // DECIMAL
            swaggerProperty = new DecimalProperty();
        } else if (boolean.class == parameterClass || Boolean.class == parameterClass) {
            // BOOLEAN
            swaggerProperty = new BooleanProperty();
        } else if (String.class == parameterClass) {
            // STRING
            swaggerProperty = new StringProperty();
        } else if (Date.class == parameterClass) {
            // DATETIME
            DateTimeProperty property = new DateTimeProperty();
            swaggerProperty = property;
        } else if (java.sql.Date.class == parameterClass) {
            // DATE
            DateProperty property = new DateProperty();
            swaggerProperty = property;
        } else if (UUID.class == parameterClass) {
            // UUID
            swaggerProperty = new UUIDProperty();
        } else if (parameterClass.isEnum()) {
            // ENUM
            StringProperty property = new StringProperty();
            List<String> enumValues = new ArrayList<>();
            for (Object enumValue : parameterClass.getEnumConstants()) {
                enumValues.add(((Enum) enumValue).name());
            }
            property.setEnum(enumValues);
            swaggerProperty = property;
        } else {
            // Register a Model class
            String modelRef = registerModel(swagger, parameterClass);
            swaggerProperty = new RefProperty(modelRef);
        }
        return swaggerProperty;
    }

    protected String loadStringResource(String resource) {
        try {
            URL url;
            if (resource.startsWith("classpath:")) {
                url = ClassUtil.getResource(resource.substring("classpath:".length()));
            } else if (resource.startsWith("url:")) {
                url = new URL(resource.substring("url:".length()));
            } else if (resource.startsWith("file:")) {
                url = new URL(resource.substring("file:".length()));
            } else {
                url = new URL(resource);
            }
            return loadStringResource(url);
        } catch (IOException e) {
            throw new FathomException(e, "Failed to read String resource from '{}'", resource);
        }
    }

    protected String loadStringResource(URL resourceUrl) {
        String content = null;
        if (resourceUrl != null) {
            try {
                content = CharStreams.toString(new InputStreamReader(resourceUrl.openStream()));
            } catch (IOException e) {
                log.error("Failed to read String resource from {}", resourceUrl, e);
            }
        }
        return content;
    }

}
