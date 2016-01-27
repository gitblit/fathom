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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import fathom.exception.FathomException;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author James Moger
 */
class XmlRpcMethodInvoker {

    private final Logger log = LoggerFactory.getLogger(XmlRpcMethodInvoker.class);

    private final String methodGroup;
    private final Class<?> targetClass;
    private final Object invokeTarget;

    private final Map<String, Method> methodCache;

    XmlRpcMethodInvoker(String methodGroup, Class<?> targetClass, Object target) {
        this.methodGroup = methodGroup;
        this.targetClass = targetClass;
        this.invokeTarget = target;
        this.methodCache = new ConcurrentHashMap<>();
    }

    String getMethodGroup() {
        return methodGroup;
    }

    Method findMethod(String methodName, List<Object> methodParameters) throws NoSuchMethodException {
        Class[] argClasses = null;
        if (methodParameters != null) {
            argClasses = new Class[methodParameters.size()];
            for (int i = 0; i < methodParameters.size(); i++) {
                Object value = methodParameters.get(i);
                if (value instanceof Integer) {
                    argClasses[i] = Integer.TYPE;
                } else if (value instanceof Double) {
                    argClasses[i] = Double.TYPE;
                } else if (value instanceof Boolean) {
                    argClasses[i] = Boolean.TYPE;
                } else {
                    argClasses[i] = value.getClass();
                }
            }
        }

        final String argsTypeList = Arrays.toString(argClasses);
        final String methodKey = methodName + argsTypeList;
        if (methodCache.containsKey(methodKey)) {
            return methodCache.get(methodKey);
        }

        final String xmlrpcMethod;
        if (Strings.isNullOrEmpty(methodGroup)) {
            xmlrpcMethod = methodName;
        } else {
            xmlrpcMethod = methodGroup + '.' + methodName;
        }

        log.debug("Locating @XmlRpc '{}' {}", xmlrpcMethod, argsTypeList);
        for (Method method : targetClass.getMethods()) {
            if (method.isAnnotationPresent(XmlRpc.class)) {
                XmlRpc xmlRpc = ClassUtil.getAnnotation(method, XmlRpc.class);
                String name = Optional.fromNullable(Strings.emptyToNull(xmlRpc.value())).or(method.getName());
                if (methodName.equals(name) && Arrays.equals(argClasses, method.getParameterTypes())) {
                    methodCache.put(methodKey, method);
                    return method;
                }
            }
        }

        log.warn("Failed to find @XmlRpc '{}' {}", xmlrpcMethod, argsTypeList);
        return null;
    }

    Object invokeMethod(String methodName, List<Object> methodParameters) throws Exception {
        Method method = findMethod(methodName, methodParameters);
        if (method == null) {
            return null;
        }
        return invokeMethod(method, methodParameters);
    }

    Object invokeMethod(Method method, List<Object> methodParameters) throws Exception {
        Object[] argValues = null;
        if (methodParameters != null) {
            argValues = new Object[methodParameters.size()];
            for (int i = 0; i < methodParameters.size(); i++) {
                argValues[i] = methodParameters.get(i);
            }
        }

        try {
            method.setAccessible(true);
            return method.invoke(invokeTarget, argValues);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            log.error("Failed to execute {}", Util.toString(method), t);
            throw new FathomException(t.getMessage());
        }
    }

}
