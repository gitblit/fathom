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
package fathom.metrics;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.Service;
import fathom.conf.Settings;
import fathom.utils.RequireUtil;
import fathom.utils.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Metrics service.
 *
 * @author James Moger
 */
@Singleton
public class Metrics implements Service {

    private static final Logger log = LoggerFactory.getLogger(Metrics.class);

    private final Settings settings;
    private final MetricRegistry metricRegistry;
    private final List<Closeable> reporters;

    @Inject
    public Metrics(MetricRegistry appMetrics, Settings settings) {

        this.settings = settings;
        this.metricRegistry = appMetrics;
        this.reporters = new ArrayList<>();

    }

    @Override
    public int getPreferredStartOrder() {
        return 50;
    }

    @Override
    public void start() {

        String applicationName = settings.getApplicationName();

        // Register optional metrics
        if (settings.getBoolean(Settings.Setting.metrics_jvm_enabled, false)) {
            registerAll("jvm.gc", new GarbageCollectorMetricSet());
            registerAll("jvm.memory", new MemoryUsageGaugeSet());
            registerAll("jvm.threads", new ThreadStatesGaugeSet());
            registerAll("jvm.classes", new ClassLoadingGaugeSet());

            log.debug("Registered JVM-Metrics integration");
        }

        // MBeans for VisualVM, JConsole, or JMX
        if (settings.getBoolean(Settings.Setting.metrics_mbeans_enabled, false)) {
            JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).inDomain(applicationName).build();
            reporter.start();
            reporters.add(reporter);

            log.debug("Started metrics MBeans reporter");
        }

        // Add classpath reporters
        ServiceLocator.locateAll(MetricsReporter.class).forEach((reporter) -> {
            if (RequireUtil.allowInstance(settings, reporter)) {
                reporter.start(settings, metricRegistry);
                reporters.add(reporter);
            }
        });
    }

    @Override
    public void stop() {

        for (Closeable reporter : reporters) {
            log.debug("Stopping {}", reporter.getClass().getName());
            try {
                reporter.close();
            } catch (IOException e) {
                log.error("Failed to stop Metrics reporter", e);
            }
        }

    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    private void registerAll(String prefix, MetricSet metrics) throws IllegalArgumentException {
        for (Map.Entry<String, Metric> entry : metrics.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(MetricRegistry.name(prefix, entry.getKey()), (MetricSet) entry.getValue());
            } else {
                metricRegistry.register(MetricRegistry.name(prefix, entry.getKey()), entry.getValue());
            }
        }
    }
}
