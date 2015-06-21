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
import fathom.conf.Settings;
import fathom.rest.RestServlet;
import fathom.rest.controller.Auth;
import fathom.rest.controller.Body;
import fathom.rest.controller.Controller;
import fathom.rest.controller.ControllerHandler;
import fathom.rest.controller.ControllerUtil;
import fathom.rest.controller.Header;
import fathom.rest.controller.Local;
import fathom.rest.controller.Max;
import fathom.rest.controller.Min;
import fathom.rest.controller.Param;
import fathom.rest.controller.Produces;
import fathom.rest.controller.Range;
import fathom.rest.controller.Required;
import fathom.rest.controller.Return;
import fathom.rest.controller.Session;
import fathom.rest.security.aop.RequireToken;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import io.swagger.models.ArrayModel;
import io.swagger.models.Contact;
import io.swagger.models.ExternalDocs;
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
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FileProperty;
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
import ro.pippo.core.Error;
import ro.pippo.core.FileItem;
import ro.pippo.core.HttpConstants;
import ro.pippo.core.route.Route;
import ro.pippo.core.route.Router;
import ro.pippo.core.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SwaggerBuilder builds a Swagger specification from your registered Controller Routes.
 *
 * @author James Moger
 */
public class SwaggerBuilder {

    private final static Logger log = LoggerFactory.getLogger(SwaggerBuilder.class);

    private static final Pattern PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE = Pattern.compile("\\{(.*?)(:\\s(.*?))?\\}");

    private static final List<String> METHODS = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private final Settings settings;

    private final Router router;

    private final String relativeSwaggerBasePath;

    public SwaggerBuilder(Settings settings, Router router) {
        this.settings = settings;
        this.router = router;
        this.relativeSwaggerBasePath = Optional.fromNullable(Strings.emptyToNull(settings.getString("swagger.basePath", null))).or("/");
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
        info.setTitle(settings.getString("swagger.info.title", settings.getApplicationName()));
        info.setVersion(settings.getString("swagger.info.version", settings.getApplicationVersion()));
        info.setDescription(ClassUtil.loadStringResource(settings.getFileUrl("swagger.info.description", "classpath:swagger/info.md")));

        // api support contact
        Contact contact = new Contact();
        contact.setName(settings.getString("swagger.info.contact.name", null));
        contact.setUrl(settings.getString("swagger.info.contact.url", null));
        contact.setEmail(settings.getString("swagger.info.contact.email", null));
        if (Strings.isNullOrEmpty(contact.getName())
                && Strings.isNullOrEmpty(contact.getUrl())
                && Strings.isNullOrEmpty(contact.getEmail())) {
            // no contact info
        } else {
            info.setContact(contact);
        }

        // License
        License license = new License();
        license.setName(settings.getString("swagger.info.license.name", null));
        license.setUrl(settings.getString("swagger.info.license.url", null));
        if (Strings.isNullOrEmpty(license.getName())
                && Strings.isNullOrEmpty(license.getUrl())) {
            // no license
        } else {
            info.setLicense(license);
        }

        swagger.setInfo(info);

        // External docs
        ExternalDocs externalDocs = new ExternalDocs();
        externalDocs.setUrl(settings.getString("swagger.externalDocs.url", null));
        externalDocs.setDescription(settings.getString("swagger.externalDocs.description", null));
        if (Strings.isNullOrEmpty(externalDocs.getUrl())) {
            // no external docs
        } else {
            swagger.setExternalDocs(externalDocs);
        }

        // host (name or ip) serving the API
        String host = Strings.emptyToNull(settings.getString("swagger.host", null));
        if (host != null) {
            swagger.setHost(host);
        }

        // transport and base url details
        List<String> configuredSchemes = settings.getStrings("swagger.schemes");
        List<Scheme> schemes = new ArrayList<>();
        if (configuredSchemes.isEmpty()) {
            // add schemes based on undertow settings
            if (settings.getInteger(Settings.Setting.undertow_httpPort, 0) > 0) {
                schemes.add(Scheme.HTTP);
            }
            if (settings.getInteger(Settings.Setting.undertow_httpsPort, 0) > 0) {
                schemes.add(Scheme.HTTPS);
            }
        } else {
            // set configured schemes
            for (String scheme : configuredSchemes) {
                Scheme s = Scheme.forValue(scheme.trim());
                if (s != null) {
                    schemes.add(s);
                }
            }
        }
        swagger.setSchemes(schemes);

        String contextPath = StringUtils.removeStart(Strings.emptyToNull(settings.getString(Settings.Setting.undertow_contextPath, null)), "/");
        String servletPath = StringUtils.removeStart(Strings.emptyToNull(settings.getString(RestServlet.SETTING_URL, null)), "/");
        String apiPath = StringUtils.removeStart(Strings.emptyToNull(settings.getString("swagger.basePath", null)), "/");
        String applicationApiPath = Joiner.on("/").skipNulls().join(contextPath, servletPath, apiPath);
        swagger.setBasePath(applicationApiPath);

        // register each valid RESTful route
        for (Route route : routes) {
            if (route.getRouteHandler() instanceof ControllerHandler) {
                ControllerHandler handler = (ControllerHandler) route.getRouteHandler();
                if (canRegister(route, handler)) {
                    registerOperation(swagger, route, handler);
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

        List<String> produces = handler.getDeclaredProduces();
        if (produces.isEmpty()) {
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

        if (!route.getUriPattern().startsWith(relativeSwaggerBasePath)) {
            log.debug("Skip {} {}, {} route is not within Swagger basePath '{}'",
                    route.getRequestMethod(), route.getUriPattern(), Util.toString(handler.getControllerMethod()),
                    relativeSwaggerBasePath);
            return false;
        }

        return true;
    }


    /**
     * Registers a ControllerHandler as a Swagger Operation.
     * If the path for the operation is unrecognized, a new Swagger Path is created.
     *
     * @param swagger
     * @param route
     * @param handler
     */
    protected void registerOperation(Swagger swagger, Route route, ControllerHandler handler) {

        Class<? extends Controller> controller = handler.getControllerClass();
        Method method = handler.getControllerMethod();

        List<String> produces = handler.getDeclaredProduces();

        Operation operation = new Operation();
        if (Strings.isNullOrEmpty(route.getName())) {
            operation.setSummary(Util.toString(method));
        } else {
            operation.setSummary(route.getName());
        }

        operation.setDescription(getNotes(method));
        operation.setOperationId(Util.toString(method));
        operation.setConsumes(produces);
        operation.setProduces(produces);
        operation.setDeprecated(method.isAnnotationPresent(Deprecated.class)
                || controller.isAnnotationPresent(Deprecated.class));

        registerResponses(swagger, operation, method);
        registerSecurity(swagger, operation, method);

        Tag tag = SwaggerUtil.getTag(controller);
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

    /**
     * Registers the declared responses for the operation.
     *
     * @param swagger
     * @param operation
     * @param method
     */
    protected void registerResponses(Swagger swagger, Operation operation, Method method) {
        for (Return aReturn : ControllerUtil.collectReturns(method)) {
            registerResponse(swagger, operation, aReturn);
        }
    }

    /**
     * Registers a declared response for the operation.
     *
     * @param swagger
     * @param operation
     * @param aReturn
     */
    protected void registerResponse(Swagger swagger, Operation operation, Return aReturn) {
        Response response = new Response();
        response.setDescription(aReturn.description());

        Class<?> resultType = aReturn.onResult();
        if (Exception.class.isAssignableFrom(resultType)) {
            resultType = Error.class;
        }

        if (Void.class != resultType) {
            // Return type
            if (resultType.isArray()) {
                // ARRAY[]
                Class<?> componentClass = resultType.getComponentType();
                ArrayProperty arrayProperty = new ArrayProperty();
                Property componentProperty = getSwaggerProperty(swagger, componentClass);
                arrayProperty.setItems(componentProperty);
                response.setSchema(arrayProperty);
            } else {
                // Object
                Property returnProperty = getSwaggerProperty(swagger, resultType);
                response.setSchema(returnProperty);
            }
        }

        operation.response(aReturn.code(), response);
    }

    /**
     * Registers a custom object model with Swagger. A model is only registered once.
     *
     * @param swagger
     * @param modelClass
     * @return the Swagger ref of the model
     */
    protected String registerModel(Swagger swagger, Class<?> modelClass) {
        final Tag modelTag = SwaggerUtil.getModelTag(modelClass);
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

            if (!field.isAnnotationPresent(ApiProperty.class) && !field.isAnnotationPresent(Param.class)) {
                // not a documented model property
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
            property.setRequired(field.isAnnotationPresent(Required.class) || field.isAnnotationPresent(NotNull.class));

            if (field.isAnnotationPresent(ApiProperty.class)) {
                ApiProperty apiProperty = field.getAnnotation(ApiProperty.class);
                if (!Strings.isNullOrEmpty(apiProperty.name())) {
                    property.setName(apiProperty.name());
                }

                if (!Strings.isNullOrEmpty(apiProperty.description())) {
                    property.setDescription(apiProperty.description());
                }

                if (!Strings.isNullOrEmpty(apiProperty.example())) {
                    property.setExample(apiProperty.example());
                }

                if (!Strings.isNullOrEmpty(apiProperty.defaultValue())) {
                    property.setDefault(apiProperty.defaultValue());
                }

                if (apiProperty.readOnly()) {
                    property.setReadOnly(true);
                }
            }

            model.addProperty(field.getName(), property);

        }

        return ref;
    }

    protected void registerSecurity(Swagger swagger, Operation operation, Method method) {
        RequireToken requireToken = ClassUtil.getAnnotation(method, RequireToken.class);
        if (requireToken != null) {
            String apiKeyName = requireToken.value();
            if (swagger.getSecurityDefinitions() == null || !swagger.getSecurityDefinitions().containsKey(apiKeyName)) {
                ApiKeyAuthDefinition security = new ApiKeyAuthDefinition();
                security.setName(apiKeyName);
                security.setIn(In.HEADER);
                security.setType("apiKey");
                swagger.addSecurityDefinition(apiKeyName, security);
            }

            operation.addSecurity(apiKeyName, Collections.emptyList());
        }
    }

    /**
     * Register an Operation's parameters.
     *
     * @param swagger
     * @param operation
     * @param route
     * @param method
     * @return the registered Swagger URI for the operation
     */
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

        // identify body, header, query, & form parameters
        for (Parameter methodParameter : method.getParameters()) {

            String methodParameterName = ControllerUtil.getParameterName(methodParameter);

            if (pathParameterPlaceholders.containsKey(methodParameterName)) {
                // path parameter already accounted for
                continue;
            }

            if (methodParameter.isAnnotationPresent(Local.class)) {
                // ignore parameter
                continue;
            }

            if (methodParameter.isAnnotationPresent(Session.class)) {
                // ignore parameter
                continue;
            }

            if (methodParameter.isAnnotationPresent(Auth.class)) {
                // ignore parameter
                continue;
            }

            if (methodParameter.isAnnotationPresent(Body.class)) {

                // BODY
                BodyParameter bodyParameter = new BodyParameter();
                bodyParameter.setName(methodParameterName);
                bodyParameter.setDescription(SwaggerUtil.getDescription(methodParameter));
                bodyParameter.setRequired(true);

                if (methodParameter.getType().isArray()) {
                    // ARRAY []
                    Property property = getSwaggerProperty(swagger, methodParameter.getType().getComponentType());
                    ArrayModel arrayModel = new ArrayModel();
                    arrayModel.setItems(property);
                    bodyParameter.setSchema(arrayModel);
                } else if (Collection.class.isAssignableFrom(methodParameter.getType())) {
                    // COLLECTION
                    Class<?> componentClass = ClassUtil.getParameterGenericType(method, methodParameter);
                    Property property = getSwaggerProperty(swagger, componentClass);
                    ArrayModel arrayModel = new ArrayModel();
                    arrayModel.setItems(property);
                    bodyParameter.setSchema(arrayModel);
                } else {
                    // OBJECT
                    Property property = getSwaggerProperty(swagger, methodParameter.getType());
                    if (property instanceof RefProperty) {
                        RefProperty ref = (RefProperty) property;
                        bodyParameter.setSchema(new RefModel(ref.getSimpleRef()));
                    }
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

                headerParameter.setDescription(SwaggerUtil.getDescription(methodParameter));
                setPropertyType(swagger, headerParameter, method);

                operation.addParameter(headerParameter);

            } else if (methodParameter.isAnnotationPresent(Form.class) || FileItem.class == methodParameter.getType()) {

                // FORM
                FormParameter formParameter = new FormParameter();
                formParameter.setName(methodParameterName);
                formParameter.setDescription(SwaggerUtil.getDescription(methodParameter));
                setPropertyType(swagger, formParameter, method);

                operation.addParameter(formParameter);

                if (FileItem.class == methodParameter.getType()) {
                    // if we see a FileItem, then this MUST be a multipart POST
                    operation.setConsumes(Arrays.asList(HttpConstants.ContentType.MULTIPART_FORM_DATA));
                } else if (!operation.getConsumes().contains(HttpConstants.ContentType.MULTIPART_FORM_DATA)) {
                    // only override consumes if this is NOT a multipart POST
                    operation.setConsumes(Arrays.asList(HttpConstants.ContentType.APPLICATION_FORM_URLENCODED));
                }

            } else {

                // QUERY
                QueryParameter queryParameter = new QueryParameter();
                queryParameter.setName(methodParameterName);
                queryParameter.setDescription(SwaggerUtil.getDescription(methodParameter));
                setPropertyType(swagger, queryParameter, method);

                operation.addParameter(queryParameter);
            }

        }

        // we need to rewrite the uripattern without regex for Swagger
        // e.g. /employee/{id: 0-9+} must be rewritten as /employee/{id}
        String swaggerUri = router.uriFor(route.getUriPattern(), pathParameterPlaceholders);
        return swaggerUri;
    }

    protected void setPropertyType(Swagger swagger, AbstractSerializableParameter swaggerParameter, Method method) {

        for (Parameter methodParameter : method.getParameters()) {

            String methodParameterName = ControllerUtil.getParameterName(methodParameter);

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
                    Class<?> componentClass = ClassUtil.getParameterGenericType(method, methodParameter);
                    Property componentProperty = getSwaggerProperty(swagger, componentClass);
                    ArrayProperty arrayProperty = new ArrayProperty(componentProperty);
                    arrayProperty.setUniqueItems(Set.class.isAssignableFrom(parameterClass));
                    swaggerProperty = arrayProperty;
                } else {
                    // TYPES
                    swaggerProperty = getSwaggerProperty(swagger, parameterClass);
                }

                if (swaggerProperty != null) {
                    swaggerParameter.setDescription(SwaggerUtil.getDescription(methodParameter));
                    swaggerParameter.setRequired(SwaggerUtil.isRequired(methodParameter));
                    swaggerParameter.setProperty(swaggerProperty);

                    if (swaggerProperty instanceof StringProperty) {
                        StringProperty property = (StringProperty) swaggerProperty;
                        if (methodParameter.isAnnotationPresent(Password.class)) {
                            property.setFormat("password");
                        }
                    }

                    if (swaggerProperty instanceof AbstractNumericProperty) {

                        AbstractNumericProperty numericProperty = (AbstractNumericProperty) swaggerProperty;
                        if (methodParameter.isAnnotationPresent(Min.class)) {
                            Min min = methodParameter.getAnnotation(Min.class);
                            numericProperty.setMinimum((double) min.value());
                        }

                        if (methodParameter.isAnnotationPresent(Max.class)) {
                            Max max = methodParameter.getAnnotation(Max.class);
                            numericProperty.setMaximum((double) max.value());
                        }

                        if (methodParameter.isAnnotationPresent(Range.class)) {
                            Range range = methodParameter.getAnnotation(Range.class);
                            numericProperty.setMinimum((double) range.min());
                            numericProperty.setMaximum((double) range.max());
                        }
                    }

                    break;
                }
            }
        }
    }

    /**
     * Returns the appropriate Swagger Property instance for a given object class.
     *
     * @param swagger
     * @param objectClass
     * @return a SwaggerProperty instance
     */
    protected Property getSwaggerProperty(Swagger swagger, Class<?> objectClass) {
        Property swaggerProperty = null;
        if (byte.class == objectClass || Byte.class == objectClass) {
            // STRING
            swaggerProperty = new StringProperty("byte");
        } else if (char.class == objectClass || Character.class == objectClass) {
            // CHAR is STRING LEN 1
            StringProperty property = new StringProperty();
            property.setMaxLength(1);
            swaggerProperty = property;
        } else if (short.class == objectClass || Short.class == objectClass) {
            // SHORT is INTEGER with 16-bit max & min
            IntegerProperty property = new IntegerProperty();
            property.setMinimum((double) Short.MIN_VALUE);
            property.setMaximum((double) Short.MAX_VALUE);
            swaggerProperty = property;
        } else if (int.class == objectClass || Integer.class == objectClass) {
            // INTEGER
            swaggerProperty = new IntegerProperty();
        } else if (long.class == objectClass || Long.class == objectClass) {
            // LONG
            swaggerProperty = new LongProperty();
        } else if (float.class == objectClass || Float.class == objectClass) {
            // FLOAT
            swaggerProperty = new FloatProperty();
        } else if (double.class == objectClass || Double.class == objectClass) {
            // DOUBLE
            swaggerProperty = new DoubleProperty();
        } else if (BigDecimal.class == objectClass) {
            // DECIMAL
            swaggerProperty = new DecimalProperty();
        } else if (boolean.class == objectClass || Boolean.class == objectClass) {
            // BOOLEAN
            swaggerProperty = new BooleanProperty();
        } else if (String.class == objectClass) {
            // STRING
            swaggerProperty = new StringProperty();
        } else if (Date.class == objectClass) {
            // DATETIME
            DateTimeProperty property = new DateTimeProperty();
            swaggerProperty = property;
        } else if (java.sql.Date.class == objectClass) {
            // DATE
            DateProperty property = new DateProperty();
            swaggerProperty = property;
        } else if (UUID.class == objectClass) {
            // UUID
            swaggerProperty = new UUIDProperty();
        } else if (objectClass.isEnum()) {
            // ENUM
            StringProperty property = new StringProperty();
            List<String> enumValues = new ArrayList<>();
            for (Object enumValue : objectClass.getEnumConstants()) {
                enumValues.add(((Enum) enumValue).name());
            }
            property.setEnum(enumValues);
            swaggerProperty = property;
        } else if (FileItem.class == objectClass) {
            // FILE UPLOAD
            FileProperty property = new FileProperty();
            swaggerProperty = property;
        } else {
            // Register a Model class
            String modelRef = registerModel(swagger, objectClass);
            swaggerProperty = new RefProperty(modelRef);
        }
        return swaggerProperty;
    }

    protected String getNotes(Method method) {
        if (method.isAnnotationPresent(Notes.class)) {
            Notes notes = method.getAnnotation(Notes.class);
            String resource = "classpath:swagger/" + method.getDeclaringClass().getName().replace('.', '/')
                    + "/" + method.getName() + ".md";
            if (!Strings.isNullOrEmpty(notes.value())) {
                resource = notes.value();
            }

            if (resource.startsWith("classpath:")) {
                String content = ClassUtil.loadStringResource(resource);
                if (Strings.isNullOrEmpty(content)) {
                    log.error("'{}' specifies @{} but '{}' was not found!",
                            Util.toString(method), Notes.class.getSimpleName(), resource);
                }
                return content;
            } else {
                return resource;
            }
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

}
