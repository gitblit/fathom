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

package fathom.realm;

import com.typesafe.config.Config;
import fathom.authc.AuthenticationToken;

/**
 * Interface the defines a Realm.
 *
 * @author James Moger
 */
public interface Realm {

    void setup(Config config);

    void start();

    void stop();

    String getRealmName();

    boolean canAuthenticate(AuthenticationToken authenticationToken);

    Account authenticate(AuthenticationToken authenticationToken);

    boolean hasAccount(String username);

    Account getAccount(String username);

}
