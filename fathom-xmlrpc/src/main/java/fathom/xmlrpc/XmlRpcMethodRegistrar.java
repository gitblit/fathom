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
import com.google.inject.Inject;
import com.google.inject.Injector;
import fathom.exception.FathomException;
import fathom.utils.ClassUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author James Moger
 */
public final class XmlRpcMethodRegistrar {

    private final String defaultMethods = "";
    private final Injector injector;
    private final Map<String, XmlRpcMethodInvoker> methodGroups;

    @Inject
    public XmlRpcMethodRegistrar(Injector injector) {
        this.injector = injector;
        this.methodGroups = new ConcurrentHashMap<>();
    }

    /**
     * Add @XmlRpc methods from this class to the default method handler.
     *
     * @param methodGroupClass
     */
    public void addDefaultMethodGroup(Class<?> methodGroupClass) {
        Object methodsObject = injector.getInstance(methodGroupClass);
        methodGroups.put(defaultMethods, new XmlRpcMethodInvoker("", methodGroupClass, methodsObject));
    }

    public void addMethodGroup(Class<?> methodGroupClass) {
        String methodGroup = methodGroupClass.getName();
        if (methodGroupClass.isAnnotationPresent(XmlRpc.class)) {
            XmlRpc xmlrpc = ClassUtil.getAnnotation(methodGroupClass, XmlRpc.class);
            methodGroup = Optional.fromNullable(Strings.emptyToNull(xmlrpc.value())).or(methodGroup);
        }
        addMethodGroup(methodGroup, methodGroupClass);
    }

    public void addMethodGroup(String methodGroup, Class<?> methodGroupClass) {
        Object methodsObject = injector.getInstance(methodGroupClass);
        methodGroups.put(Strings.nullToEmpty(methodGroup), new XmlRpcMethodInvoker(methodGroup, methodGroupClass, methodsObject));
    }

    Object invoke(String fullMethodName, List<Object> methodArgs) throws Exception {
        String methodGroup;
        String methodName;
        int dot = fullMethodName.lastIndexOf('.');
        if (dot == -1) {
            methodGroup = defaultMethods;
            methodName = fullMethodName;
        } else {
            methodGroup = fullMethodName.substring(0, dot);
            methodName = fullMethodName.substring(dot + 1);
        }

        XmlRpcMethodInvoker methodInvoker = methodGroups.get(methodGroup);
        if (methodInvoker == null) {
            throw new FathomException("Failed to find method group '{}'", methodGroup);
        }

        Object result = methodInvoker.invokeMethod(methodName, methodArgs);
        return result;
    }

}
