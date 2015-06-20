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

package fathom.authc;

import com.google.common.base.Preconditions;

/**
 * Represents token credentials.
 *
 * @author James Moger
 */
public class TokenCredentials implements AuthenticationToken {

    private final String token;

    public TokenCredentials(String token) {
        Preconditions.checkNotNull(token, "Token may not be null!");
        this.token = token.trim();
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "TokenCredentials{" +
                "token='" + token + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenCredentials that = (TokenCredentials) o;

        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31* (token != null ? token.hashCode() : 0);
        return result;
    }

}
