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

package fathom.shiro;

import com.google.common.base.Strings;
import fathom.ServletsModule;
import fathom.rest.RestServlet;
import fathom.shiro.aop.AopModule;
import org.apache.shiro.config.Ini;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.env.IniWebEnvironment;
import org.apache.shiro.web.env.WebEnvironment;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.kohsuke.MetaInfServices;


/**
 * Simple auth module based on Apache Shiro Core.
 *
 * @author James Moger
 */
@MetaInfServices
public class ShiroModule extends ServletsModule {

    @Override
    protected void setup() {

        String configFile = getSettings().getString("shiro.configurationFile", "classpath:conf/shiro.ini");
        Ini ini = Ini.fromResourcePath(configFile);

        IniWebEnvironment webEnvironment = new IniWebEnvironment();
        webEnvironment.setIni(ini);
        webEnvironment.setServletContext(getServletContext());
        webEnvironment.init();

        bind(WebEnvironment.class).toInstance(webEnvironment);
        bind(SecurityManager.class).toInstance(webEnvironment.getSecurityManager());
        bind(WebSecurityManager.class).toInstance(webEnvironment.getWebSecurityManager());

        String basePath = Strings.nullToEmpty(getSettings().getString(RestServlet.SETTING_URL, null)).trim();
        filter(basePath + "/*").through(ShiroFilter.class);

        install(new AopModule());
    }

}
