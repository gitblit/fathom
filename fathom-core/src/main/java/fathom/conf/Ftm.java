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
package fathom.conf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ftm is the default implementation of the Fathom interface.
 *
 * @author James Moger
 * @see <a href="https://en.wikipedia.org/wiki/Fathom">Fathom</a>
 */
@Singleton
public class Ftm implements Fathom {

    private static final Logger log = LoggerFactory.getLogger(Ftm.class);

    @Inject
    protected Settings settings;

    protected AtomicReference<Date> bootDate = new AtomicReference<>();

    protected void showLogo() {
        log.info(Constants.getLogo());
    }

    @Override
    public void onStartup() {

        // display a boot logo
        showLogo();
        bootDate.set(new Date());

    }

    @Override
    public Date getBootDate() {
        return bootDate.get();
    }

    @Override
    public void onShutdown() {
    }

}
