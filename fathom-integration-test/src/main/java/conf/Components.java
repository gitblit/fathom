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
package conf;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import controllers.HelloStaticRoutes;
import dao.EmployeeDao;
import dao.ItemDao;
import fathom.Module;
import fathom.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Components extends Module {

    @Override
    protected void setup() {

        bind(ItemDao.class);
        bind(EmployeeDao.class);
        bind(LdapService.class);

        // we have to manually specify our static controllers
        // for injection, if we choose that design
        requestStaticInjection(HelloStaticRoutes.class);

    }

    private static class LdapService implements Service {

        private final static Logger log = LoggerFactory.getLogger(LdapService.class);

        InMemoryDirectoryServer ds;

        @Override
        public int getPreferredStartOrder() {
            return 50;
        }

        @Override
        public void start() {
            try {
                InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=MyDomain");
                config.addAdditionalBindCredentials("cn=Directory Manager", "password");
                config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", 1389));
                config.setSchema(null);

                ds = new InMemoryDirectoryServer(config);
                ds.startListening();
                ds.importFromLDIF(true, new LDIFReader(getClass().getResourceAsStream("/conf/realm.ldif")));
            } catch (LDAPException e) {
                ds = null;
                log.error("Failed to start UnboundID LDAP server!", e);
            }
        }

        @Override
        public boolean isRunning() {
            return ds != null;
        }

        @Override
        public void stop() {
            if (ds != null) {
                ds.shutDown(true);
            }
        }
    }

}
