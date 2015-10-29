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
package fathom;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Injector;
import fathom.conf.Settings;
import fathom.exception.FatalException;
import fathom.utils.RequireUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Listens for Service registrations from fathom.Module & manages the service lifecycle.
 *
 * @author James Moger
 */
public final class Services {

    private static final Logger log = LoggerFactory.getLogger(Services.class);

    private final Settings settings;
    private final List<Class<? extends Service>> classes;
    private final List<Service> instances;
    private boolean started;

    public Services(Settings settings) {
        this.settings = settings;
        this.classes = new ArrayList<>();
        this.instances = new ArrayList<>();
    }

    public void register(Class<? extends Service> serviceClass) {
        Preconditions.checkState(started == false, "All services have already been registered & started!");
        classes.add(serviceClass);
    }

    public void register(Service service) {
        Preconditions.checkState(started == false, "All services have already been registered & started!");
        instances.add(service);
    }

    public List<Class<? extends Service>> getServiceClasses() {
        return classes;
    }

    public List<Service> getServices() {
        return Collections.unmodifiableList(instances);
    }

    public synchronized void start(Injector injector) {
        started = true;

        getServiceClasses().forEach((serviceClass) -> {

            if (RequireUtil.allowClass(settings, serviceClass)) {
                Service service = injector.getInstance(serviceClass);
                instances.add(service);
            }

        });

        // sort the services into the preferred start order
        Collections.sort(instances, new Comparator<Service>() {
            @Override
            public int compare(Service o1, Service o2) {
                if (o2.getPreferredStartOrder() < 0) {
                    return -1;
                } else if (o1.getPreferredStartOrder() < 0) {
                    return 1;
                } else if (o1.getPreferredStartOrder() < o2.getPreferredStartOrder()) {
                    return -1;
                } else if (o1.getPreferredStartOrder() > o2.getPreferredStartOrder()) {
                    return 1;
                }
                return 0;
            }
        });

        for (Service service : instances) {
            log.debug("{} '{}'", Strings.padStart("" + service.getPreferredStartOrder(), 3, '0'),
                    service.getClass().getName());
        }

        for (Service service : instances) {
            log.info("Starting service '{}'", service.getClass().getName());
            try {
                service.start();
            } catch (Exception e) {
                log.error("Failed to start '{}'", service.getClass().getName(), e);

                if (e instanceof FatalException) {
                    stop();
                    System.exit(1);
                }
            }
        }
    }

    public synchronized void stop() {
        // stop the services in the reverse order
        Collections.reverse(instances);
        for (Service service : instances) {
            if (service.isRunning()) {
                log.info("Stopping service '{}'", service.getClass().getName());
                try {
                    service.stop();
                } catch (Exception e) {
                    log.error("Failed to stop '{}'", service.getClass().getName(), e);
                }
            }
        }
    }
}
