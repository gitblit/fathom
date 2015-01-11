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

package fathom.utils;

import com.google.common.base.Preconditions;
import fathom.Constants;
import fathom.conf.Mode;
import fathom.conf.RequireSetting;
import fathom.conf.RequireSettings;
import fathom.conf.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * RequireUtil helps enforce runtime requirements for loading classes,
 * using instances, or executing methods.
 *
 * @author James Moger
 */
public class RequireUtil {

    private static final Logger log = LoggerFactory.getLogger(RequireUtil.class);

    /**
     * Determines if this object may be used in the current runtime environment.
     * Fathom settings are considered as well as runtime modes.
     *
     * @param settings
     * @param object
     * @return true if the object may be used
     */
    public static boolean allowInstance(Settings settings, Object object) {

        Preconditions.checkNotNull(object, "Can not check runtime permissions on a null instance!");

        if (object instanceof Method) {
            return allowMethod(settings, (Method) object);
        }

        return allowClass(settings, object.getClass());
    }

    /**
     * Determines if this class may be used in the current runtime environment.
     * Fathom settings are considered as well as runtime modes.
     *
     * @param settings
     * @param aClass
     * @return true if the class may be used
     */
    public static boolean allowClass(Settings settings, Class<?> aClass) {
        // Settings-based method exclusions/inclusions
        if (aClass.isAnnotationPresent(RequireSettings.class)) {
            // multiple keys required
            RequireSetting[] requireSettings = aClass.getAnnotation(RequireSettings.class).value();
            StringJoiner joiner = new StringJoiner(", ");
            Arrays.asList(requireSettings).forEach((require) -> {
                if (!settings.hasSetting(require.value())) {
                    joiner.add("\"" + require.value() + "\"");
                }
            });
            String requiredSettings = joiner.toString();

            log.warn("skipping '{}', it requires the following {} mode settings: {}",
                    aClass.getName(), settings.getMode(), requiredSettings);

            return false;

        } else if (aClass.isAnnotationPresent(RequireSetting.class)) {
            // single key required
            RequireSetting requireSetting = aClass.getAnnotation(RequireSetting.class);
            String requiredKey = requireSetting.value();
            if (!settings.hasSetting(requiredKey)) {
                log.warn("skipping '{}', it requires the following {} mode setting: \"{}\"",
                        aClass.getName(), settings.getMode(), requiredKey);

                return false;
            }
        }

        // Mode-based class exclusions/inclusions
        Set<Constants.Mode> modes = new HashSet<>();
        for (Annotation annotation : aClass.getAnnotations()) {
            Class<? extends Annotation> annotationClass = annotation.annotationType();
            if (annotationClass.isAnnotationPresent(Mode.class)) {
                Mode mode = annotationClass.getAnnotation(Mode.class);
                modes.add(mode.value());
            }
        }
        boolean allowInMode = modes.isEmpty() || modes.contains(settings.getMode());

        if (!allowInMode) {
            StringJoiner joiner = new StringJoiner(", ");
            modes.forEach((mode) -> joiner.add(mode.name()));
            String requiredModes = joiner.toString();

            log.warn("skipping '{}', it may only be used in the following modes: {}",
                    aClass.getName(), requiredModes);
        }

        return allowInMode;
    }

    /**
     * Determines if this method may be used in the current runtime environment.
     * Fathom settings are considered as well as runtime modes.
     *
     * @param settings
     * @param method
     * @return true if the method may be used
     */
    public static boolean allowMethod(Settings settings, Method method) {

        // Settings-based method exclusions/inclusions
        if (method.isAnnotationPresent(RequireSettings.class)) {
            // multiple keys required
            RequireSetting[] requireSettings = method.getAnnotation(RequireSettings.class).value();
            StringJoiner joiner = new StringJoiner(", ");
            Arrays.asList(requireSettings).forEach((require) -> {
                if (!settings.hasSetting(require.value())) {
                    joiner.add("\"" + require.value() + "\"");
                }
            });
            String requiredSettings = joiner.toString();

            log.warn("skipping '{}', it requires the following {} mode settings: {}",
                    Util.toString(method), settings.getMode(), requiredSettings);

            return false;

        } else if (method.isAnnotationPresent(RequireSetting.class)) {
            // single key required
            RequireSetting requireSetting = method.getAnnotation(RequireSetting.class);
            String requiredKey = requireSetting.value();
            if (!settings.hasSetting(requiredKey)) {
                log.debug("skipping '', it requires the following {} mode setting: \"{}\"",
                        Util.toString(method), settings.getMode(), requiredKey);

                return false;
            }
        }

        // Mode-based method exclusions/inclusions
        Set<Constants.Mode> modes = new HashSet<>();
        for (Annotation annotation : method.getAnnotations()) {
            Class<? extends Annotation> annotationClass = annotation.annotationType();
            if (annotationClass.isAnnotationPresent(Mode.class)) {
                Mode mode = annotationClass.getAnnotation(Mode.class);
                modes.add(mode.value());
            }
        }

        if (!modes.isEmpty() && !modes.contains(settings.getMode())) {
            StringJoiner joiner = new StringJoiner(", ");
            modes.forEach((mode) -> joiner.add(mode.name()));
            String requiredModes = joiner.toString();

            log.warn("skipping '{}', it may only be used in the following modes: {}",
                    Util.toString(method), requiredModes);

            return false;
        }

        // method is allowed, check declaring class
        return allowClass(settings, method.getDeclaringClass());
    }
}
