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

import com.codahale.metrics.MetricRegistry;
import fathom.Module;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

/**
 * Fathom Module for Metrics
 *
 * @author James Moger
 */
public final class MetricsModule extends Module {

    @Override
    protected void setup() {

        MetricRegistry appMetrics = new MetricRegistry();
        bind(MetricRegistry.class).toInstance(appMetrics);

        bind(Metrics.class);

        TimedInterceptor timedInterceptor = new TimedInterceptor(getProvider(Metrics.class));
        bindInterceptor(any(), annotatedWith(Timed.class), timedInterceptor);

        MeteredInterceptor meteredInterceptor = new MeteredInterceptor(getProvider(Metrics.class));
        bindInterceptor(any(), annotatedWith(Metered.class), meteredInterceptor);

        CountedInterceptor countedInterceptor = new CountedInterceptor(getProvider(Metrics.class));
        bindInterceptor(any(), annotatedWith(Counted.class), countedInterceptor);

    }

}
