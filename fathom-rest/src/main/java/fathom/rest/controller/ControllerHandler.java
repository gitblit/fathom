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
package fathom.rest.controller;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Injector;
import com.google.inject.Provider;
import fathom.exception.FathomException;
import fathom.rest.controller.extractors.ArgumentExtractor;
import fathom.rest.controller.extractors.ConfigurableExtractor;
import fathom.rest.controller.extractors.ContextExtractor;
import fathom.rest.controller.extractors.ExtractWith;
import fathom.rest.controller.extractors.NamedExtractor;
import fathom.rest.controller.extractors.ParamExtractor;
import fathom.rest.Context;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * ControllerHandler executes controller methods.
 *
 * @author James Moger
 */
public class ControllerHandler implements RouteHandler<Context> {

    private static final Logger log = LoggerFactory.getLogger(ControllerHandler.class);

    protected final Provider<?> controllerProvider;
    protected final Method method;
    protected ArgumentExtractor[] extractors;
    protected String[] patterns;

    public ControllerHandler(Injector injector, Class<?> controllerClass, String methodName) {
        this.controllerProvider = injector.getProvider(controllerClass);
        this.method = findMethod(injector, controllerClass, methodName);

        Preconditions.checkNotNull(method, "Failed to find controller method '%s::%s'", controllerClass.getSimpleName(),
                methodName);
        log.trace("Obtained method for '{}'", Util.toString(method));
    }

    public Method getControllerMethod() {
        return method;
    }

    @Override
    public void handle(Context context) {

        try {

            log.trace("Preparing '{}' arguments from request", Util.toString(method));
            Object[] args = prepareMethodArgs(context);

            log.trace("Invoking '{}'", Util.toString(method));
            Object controller = controllerProvider.get();

            ControllerResult result = (ControllerResult) method.invoke(controller, args);

            Preconditions.checkNotNull(result, "Controller method '{}' returned null!", Util.toString(method));
            log.trace("Processing '{}' result from '{}'", result.getClass().getSimpleName(), Util.toString(method));
            result.process(context, method);

            context.next();

        } catch (Exception e) {
            throw new FathomException(e);
        }
    }

    protected Method findMethod(Injector injector, Class<?> controllerClass, String name) {
        // identify first method which matches the name
        Method controllerMethod = null;
        for (Method method : controllerClass.getMethods()) {
            if (method.getName().equals(name)) {
                if (controllerMethod == null) {
                    controllerMethod = method;

                    // validate method return type
                    if (!ControllerResult.class.isAssignableFrom(method.getReturnType())) {
                        throw new FathomException("The return type of '{}' must be an implementation of '{}'!",
                                Util.toString(controllerMethod),
                                ControllerResult.class.getName());
                    }

                    // mapped parameters
                    Class<?>[] types = method.getParameterTypes();
                    extractors = new ArgumentExtractor[types.length];
                    patterns = new String[types.length];
                    for (int i = 0; i < types.length; i++) {
                        Class<?> type = types[i];

                        // determine the appropriate extractor
                        Class<? extends ArgumentExtractor> extractorType;
                        if (Context.class == type) {
                            extractorType = ContextExtractor.class;
                        } else {
                            extractorType = getArgumentExtractor(controllerMethod, i);
                        }

                        // instantiate the extractor
                        extractors[i] = injector.getInstance(extractorType);

                        // configure the extractor
                        if (extractors[i] instanceof ConfigurableExtractor<?>) {
                            ConfigurableExtractor extractor = (ConfigurableExtractor) extractors[i];
                            Annotation annotation = getAnnotation(controllerMethod, i, extractor.getAnnotationClass());
                            if (annotation != null) {
                                extractor.configure(annotation);
                            }
                        }

                        // test the target type
                        extractors[i].checkTargetType(type);

                        if (extractors[i] instanceof NamedExtractor) {
                            // ensure that the extractor has a proper name
                            NamedExtractor namedExtractor = (NamedExtractor) extractors[i];
                            if (Strings.isNullOrEmpty(namedExtractor.getName())) {
                                // parameter is not named via annotation
                                // try looking for the parameter name in the compiled .class file
                                Parameter parameter = method.getParameters()[i];
                                if (parameter.isNamePresent()) {
                                    namedExtractor.setName(parameter.getName());
                                } else {
                                    log.error("Properly annotate your controller methods OR specify the '-parameters' flag for your Java compiler!");
                                    throw new FathomException(
                                            "Controller method '{}.{}' parameter {} of type '{}' does not specify a name!",
                                            controllerClass.getSimpleName(), method.getName(), i + 1, type.getSimpleName());
                                }
                            }
                        }
                    }
                } else {
                    throw new FathomException("Found overloaded controller method '{}.{}'. Method names must be unique!",
                            controllerClass.getSimpleName(), method.getName());
                }
            }
        }

        return controllerMethod;
    }

    protected Object[] prepareMethodArgs(Context context) {
        Class<?>[] types = method.getParameterTypes();

        if (types.length == 0) {
            return new Object[]{};
        }

        Object[] args = new Object[types.length];
        for (int i = 0; i < args.length; i++) {
            Class<?> type = types[i];
            ArgumentExtractor extractor = extractors[i];
            Object value = extractor.extract(context, type);
            if (value == null || ClassUtil.isAssignable(value, type)) {
                args[i] = value;
            } else {
                String parameter = getParameterName(method, i);
                throw new FathomException("Type for '{}' is actually '{}' but was specified as '{}'!",
                        parameter, value.getClass().getName(), type.getName());
            }
        }

        return args;
    }

    protected String getParameterName(Method method, int i) {
        Annotation annotation = getAnnotation(method, i, Param.class);
        if (annotation != null) {
            Param parameter = (Param) annotation;
            return parameter.value();
        } else {
            // parameter is not named via annotation
            // try looking for the parameter name in the compiled .class file
            Parameter parameter = method.getParameters()[i];
            if (parameter.isNamePresent()) {
                return parameter.getName();
            }
        }
        return null;
    }

    protected Class<? extends ArgumentExtractor> getArgumentExtractor(Method method, int i) {
        Annotation[] annotations = method.getParameterAnnotations()[i];
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAnnotationPresent(ExtractWith.class)) {
                ExtractWith with = annotation.annotationType().getAnnotation(ExtractWith.class);
                Class<? extends ArgumentExtractor> extractorClass = with.value();
                return extractorClass;
            }
        }
        // if unspecified we use the ParamExtractor
        return ParamExtractor.class;
    }

    protected Annotation getAnnotation(Method method, int i, Class<?> annotationClass) {
        Annotation[] annotations = method.getParameterAnnotations()[i];
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                return annotation;
            }
        }
        return null;
    }
}
