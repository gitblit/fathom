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

package fathom.rest.controller.extractors;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import fathom.realm.Account;
import fathom.rest.controller.Auth;
import fathom.rest.Context;
import fathom.rest.security.AuthConstants;

/**
 * @author James Moger
 */
public class AuthExtractor implements TypedExtractor, NamedExtractor, ConfigurableExtractor<Auth> {

    private String name;

    @Override
    public Class<Auth> getAnnotationClass() {
        return Auth.class;
    }

    @Override
    public void configure(Auth param) {
        setName(param.value());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setObjectType(Class<?> objectType) {
        Preconditions.checkState(Account.class.isAssignableFrom(objectType));
    }

    @Override
    public Account extract(Context context) {
        Account session = context.getSession(AuthConstants.ACCOUNT_ATTRIBUTE);
        Account local = context.getLocal(AuthConstants.ACCOUNT_ATTRIBUTE);
        Account account = Optional.fromNullable(session).or(Optional.fromNullable(local).or(Account.GUEST));
        return account;
    }
}
