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

import com.google.common.base.Optional;
import fathom.authc.AuthenticationException;
import fathom.authc.Credentials;
import fathom.authc.StandardCredentials;
import fathom.authz.AuthorizationException;
import fathom.authz.Authorizations;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Account represents Credentials and Authorizations.
 * This class is partially based on Apache Shiro Subject.
 *
 * @author James Moger
 */
public class Account implements Serializable {

    public transient final static Account GUEST = new Account("Guest", new StandardCredentials("guest", null),
            new Authorizations(Collections.emptySet(), Collections.emptySet()));

    private static final long serialVersionUID = 2533708087596778061L;

    protected final Credentials credentials;

    protected final Authorizations authorizations;

    protected final Set<String> emailAddresses;

    protected final Set<String> tokens;

    protected String name;

    protected boolean isDisabled;

    public Account(String name, Credentials credentials) {
        this(name, credentials, new Authorizations());
    }

    public Account(String name, Credentials credentials, Authorizations authorizations) {
        this.name = name;
        this.credentials = credentials;
        this.authorizations = authorizations;
        this.emailAddresses = new LinkedHashSet<>();
        this.tokens = new HashSet<>();
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return credentials.getUsername();
    }

    public void addEmailAddress(String address) {
        this.emailAddresses.add(address);
    }

    public void addEmailAddresses(String... addresses) {
        this.emailAddresses.addAll(Arrays.asList(addresses));
    }

    public void addEmailAddresses(Collection<String> addresses) {
        this.emailAddresses.addAll(addresses);
    }

    public String getEmailAddress() {
        if (emailAddresses.isEmpty()) {
            return null;
        }
        return emailAddresses.toArray()[0].toString();
    }

    public Collection<String> getEmailAddresses() {
        return Collections.unmodifiableCollection(emailAddresses);
    }

    public void addToken(String token) {
        this.tokens.add(token);
    }

    public void addTokens(String... tokens) {
        this.tokens.addAll(Arrays.asList(tokens));
    }

    public void addTokens(Collection<String> tokens) {
        this.tokens.addAll(tokens);
    }

    public String getToken() {
        if (tokens.isEmpty()) {
            return null;
        }
        return tokens.toArray()[0].toString();
    }

    public Collection<String> getTokens() {
        return Collections.unmodifiableCollection(tokens);
    }

    /**
     * Disables this account.
     */
    public void setDisabled() {
        this.isDisabled = true;
    }

    /**
     * Enables this account.
     */
    public void setEnabled() {
        this.isDisabled = false;
    }

    @Override
    public String toString() {
        return Optional.fromNullable(name).or(credentials.getUsername());
    }

    /**
     * Returns {@code true} if this Account is the GUEST account.
     *
     * @return true if this Account is the GUEST account.
     */
    public boolean isGuest() {
        return this == Account.GUEST;
    }

    /**
     * Returns {@code true} if this Account is authenticated.
     *
     * @return true if this Account is authenticated.
     */
    public boolean isAuthenticated() {
        return !isGuest();
    }

    /**
     * Returns {@code true} if this Account is an administrator.
     *
     * @return true if this Account is an administrator.
     */
    public boolean isAdministrator() {
        return isPermitted("*");
    }

    /**
     * Returns {@code true} if this Account is disabled.
     *
     * @return true if this Account is disabled.
     */
    public boolean isDisabled() {
        return isDisabled;
    }

    /**
     * Returns {@code true} if this Account is enabled.
     *
     * @return true if this Account is enabled.
     */
    public boolean isEnabled() {
        return !isDisabled;
    }

    /**
     * Ensures this Account is the Guest account.
     * <p>
     *
     * @throws fathom.authc.AuthenticationException if the Account is not the Guest account.
     */
    public void checkGuest() throws AuthenticationException {
        if (!isGuest()) {
            throw new AuthorizationException("'{}' is not the Guest account", toString());
        }
    }

    /**
     * Ensures this Account has actively authenticated.
     * <p>
     *
     * @throws fathom.authc.AuthenticationException if the Account is not authenticated.
     */
    public void checkAuthenticated() throws AuthenticationException {
        if (!isAuthenticated()) {
            throw new AuthorizationException("'{}' has not authenticated", toString());
        }
    }

    /**
     * Ensures this Account is an administrator.
     * <p>
     *
     * @throws fathom.authc.AuthenticationException if the Account is not an administrator.
     */
    public void checkAdministrator() throws AuthenticationException {
        if (!isAdministrator()) {
            throw new AuthorizationException("'{}' is not an administrator", toString());
        }
    }

    /**
     * Returns {@code true} if this Account is permitted to perform an action or access a resource summarized by the
     * specified permission string.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permission the String representation of a Permission that is being checked.
     * @return true if this Account is permitted, false otherwise.
     */
    public boolean isPermitted(String permission) {
        return authorizations.isPermitted(permission);
    }

    /**
     * Checks if this Account implies the given permission strings and returns a boolean array indicating which
     * permissions are implied.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the String representations of the Permissions that are being checked.
     * @return a boolean array where indices correspond to the index of the
     * permissions in the given list.  A true value at an index indicates this Account is permitted for
     * for the associated {@code Permission} string in the list.  A false value at an index
     * indicates otherwise.
     */
    public boolean[] isPermitted(String... permissions) {
        return authorizations.isPermitted(permissions);
    }

    /**
     * Returns {@code true} if this Account implies all of the specified permission strings, {@code false} otherwise.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission}
     * variant.  Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the String representations of the Permissions that are being checked.
     * @return true if this Account has all of the specified permissions, false otherwise.
     * @since 0.9
     */
    public boolean isPermittedAll(String... permissions) {
        return authorizations.isPermittedAll(permissions);
    }

    /**
     * Returns {@code true} if this Account implies all of the specified permission strings, {@code false} otherwise.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission}
     * variant.  Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the String representations of the Permissions that are being checked.
     * @return true if this Account has all of the specified permissions, false otherwise.
     * @since 0.9
     */
    public boolean isPermittedAll(Collection<String> permissions) {
        return authorizations.isPermittedAll(permissions);
    }

    /**
     * Ensures this Account implies the specified permission String.
     * <p>
     * If this Account's existing associated permissions do not {@link fathom.authz.Permission#implies(fathom.authz.Permission)} imply}
     * the given permission, an {@link fathom.authz.AuthorizationException} will be thrown.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permission the String representation of the Permission to check.
     * @throws fathom.authz.AuthorizationException if the user does not have the permission.
     */
    public void checkPermission(String permission) throws AuthorizationException {
        if (!isPermitted(permission)) {
            throw new AuthorizationException("'{}' does not have the permission '{}'", toString(), permission);
        }
    }

    /**
     * Ensures this Account
     * {@link fathom.authz.Permission#implies(fathom.authz.Permission) implies} all of the
     * specified permission strings.
     * <p>
     * If this Account's existing associated permissions do not
     * {@link fathom.authz.Permission#implies(fathom.authz.Permission) imply} all of the given permissions,
     * an {@link fathom.authz.AuthorizationException} will be thrown.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the string representations of Permissions to check.
     * @throws AuthorizationException if this Account does not have all of the given permissions.
     */
    public void checkPermissions(String... permissions) throws AuthorizationException {
        if (!isPermittedAll(permissions)) {
            throw new AuthorizationException("'{}' does not have the permissions {}", toString(), Arrays.toString(permissions));
        }
    }

    /**
     * Ensures this Account
     * {@link fathom.authz.Permission#implies(fathom.authz.Permission) implies} all of the
     * specified permission strings.
     * <p>
     * If this Account's existing associated permissions do not
     * {@link fathom.authz.Permission#implies(fathom.authz.Permission) imply} all of the given permissions,
     * an {@link fathom.authz.AuthorizationException} will be thrown.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the string representations of Permissions to check.
     * @throws AuthorizationException if this Account does not have all of the given permissions.
     */
    public void checkPermissions(Collection<String> permissions) throws AuthorizationException {
        if (!isPermittedAll(permissions)) {
            throw new AuthorizationException("'{}' does not have the permissions {}", toString(), permissions);
        }
    }

    /**
     * Returns {@code true} if this Account has the specified role, {@code false} otherwise.
     *
     * @param roleIdentifier the application-specific role identifier (usually a role id or role name).
     * @return {@code true} if this Account has the specified role, {@code false} otherwise.
     */
    public boolean hasRole(String roleIdentifier) {
        return authorizations.hasRole(roleIdentifier);
    }

    /**
     * Returns {@code true} if this Account has the specified roles, {@code false} otherwise.
     *
     * @param roleIdentifiers the application-specific role identifiers to check (usually role ids or role names).
     * @return {@code true} if this Account has all the specified roles, {@code false} otherwise.
     */
    public boolean hasRoles(String... roleIdentifiers) {
        return authorizations.hasRoles(roleIdentifiers);
    }

    /**
     * Returns {@code true} if this Account has the specified roles, {@code false} otherwise.
     *
     * @param roleIdentifiers the application-specific role identifiers to check (usually role ids or role names).
     * @return {@code true} if this Account has all the specified roles, {@code false} otherwise.
     */
    public boolean hasRoles(Collection<String> roleIdentifiers) {
        return authorizations.hasRoles(roleIdentifiers);
    }

    /**
     * Asserts this Account has the specified role by returning quietly if they do or throwing an
     * {@link fathom.authz.AuthorizationException} if they do not.
     *
     * @param roleIdentifier the application-specific role identifier (usually a role id or role name ).
     * @throws fathom.authz.AuthorizationException if this Account does not have the role.
     */
    public void checkRole(String roleIdentifier) throws AuthorizationException {
        if (!hasRole(roleIdentifier)) {
            throw new AuthorizationException("'{}' does not have the role '{}'", toString(), roleIdentifier);
        }
    }

    /**
     * Asserts this Account has all of the specified roles by returning quietly if they do or throwing an
     * {@link fathom.authz.AuthorizationException} if they do not.
     *
     * @param roleIdentifiers roleIdentifiers the application-specific role identifiers to check (usually role ids or role names).
     * @throws AuthorizationException fathom.authz.AuthorizationException
     *                                if this Account does not have all of the specified roles.
     */
    public void checkRoles(String... roleIdentifiers) throws AuthorizationException {
        if (!hasRoles(roleIdentifiers)) {
            throw new AuthorizationException("'{}' does not have the roles {}", toString(), Arrays.toString(roleIdentifiers));
        }
    }

    /**
     * Asserts this Account has all of the specified roles by returning quietly if they do or throwing an
     * {@link fathom.authz.AuthorizationException} if they do not.
     *
     * @param roleIdentifiers roleIdentifiers the application-specific role identifiers to check (usually role ids or role names).
     * @throws AuthorizationException fathom.authz.AuthorizationException
     *                                if this Account does not have all of the specified roles.
     */
    public void checkRoles(Collection<String> roleIdentifiers) throws AuthorizationException {
        if (!hasRoles(roleIdentifiers)) {
            throw new AuthorizationException("'{}' does not have the roles {}", toString(), roleIdentifiers);
        }
    }

    /**
     * Returns {@code true} if this Account has the specified role, {@code false} otherwise.
     *
     * @param token the authentication token.
     * @return {@code true} if this Account has the specified token, {@code false} otherwise.
     */
    public boolean hasToken(String token) {
        return tokens.contains(token);
    }

    /**
     * Asserts this Account has the specified token by returning quietly if they do or throwing an
     * {@link fathom.authz.AuthorizationException} if they do not.
     *
     * @param token the authentication token.
     * @throws fathom.authz.AuthorizationException if this Account does not have the token.
     */
    public void checkToken(String token) throws AuthorizationException {
        if (!hasToken(token)) {
            throw new AuthorizationException("'{}' does not have the token '{}'", toString(), token);
        }
    }

}
