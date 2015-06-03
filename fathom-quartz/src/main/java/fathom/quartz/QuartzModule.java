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
package fathom.quartz;

import com.google.common.base.Optional;
import fathom.Module;
import fathom.utils.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fathom module for Quartz.
 * This module auto-loads the JobsModule and registers the Quartz service.
 */
public class QuartzModule extends Module {

    private static final Logger log = LoggerFactory.getLogger(QuartzModule.class);

    private static final String JOBS_CLASS = "conf.Jobs";

    @Override
    protected void setup() {
        Optional<String> applicationPackage = Optional.fromNullable(getSettings().getApplicationPackage());
        String fullClassName = ClassUtil.buildClassName(applicationPackage, JOBS_CLASS);
        if (ClassUtil.doesClassExist(fullClassName)) {
            Class<?> moduleClass = ClassUtil.getClass(fullClassName);
            if (JobsModule.class.isAssignableFrom(moduleClass)) {
                JobsModule jobsModule = (JobsModule) ClassUtil.newInstance(moduleClass);
                if (jobsModule != null) {

                    log.info("Scheduling Quartz jobs in '{}'", jobsModule.getClass().getName());
                    install(jobsModule);

                    bind(Quartz.class);
                }
            }
        }
    }

}