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

package fathom.realm.ldap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import fathom.authc.StandardCredentials;
import fathom.realm.Account;
import fathom.realm.CachingRealm;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

/**
 * LdapRealm allows you to authenticate against an Ldap server.
 *
 * @author John Crygier
 * @author James Moger
 */
public class LdapRealm extends CachingRealm {

    private final static Logger log = LoggerFactory.getLogger(LdapRealm.class);

    protected String ldapUrl;

    protected String ldapUsername;

    protected String ldapPassword;

    protected String ldapBindPattern;

    protected String accountBase;

    protected String accountPattern;

    protected String groupBase;

    protected String groupMemberPattern;

    protected String nameMapping;

    protected String emailMapping;

    protected List<String> adminGroups;

    // From: https://www.owasp.org/index.php/Preventing_LDAP_Injection_in_Java
    public static final String escapeLDAPSearchFilter(String filter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filter.length(); i++) {
            char curChar = filter.charAt(i);
            switch (curChar) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\u0000':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(curChar);
            }
        }
        return sb.toString();
    }

    @Override
    public void setup(Config config) {
        super.setup(config);

        ldapUrl = Strings.emptyToNull(config.getString("url"));
        Preconditions.checkNotNull(ldapUrl, "The LDAP 'url' setting may not be null nor empty!");

        ldapUsername = "";
        if (config.hasPath("username")) {
            ldapUsername = config.getString("username");
        }

        ldapPassword = "";
        if (config.hasPath("password")) {
            ldapPassword = config.getString("password");
        }

        ldapBindPattern = "";
        if (config.hasPath("bindPattern")) {
            ldapBindPattern = config.getString("bindPattern");
        }

        accountBase = "";
        if (config.hasPath("accountBase")) {
            accountBase = config.getString("accountBase");
        }

        accountPattern = "(&(objectClass=person)(sAMAccountName=${username}))";
        if (config.hasPath("accountPattern")) {
            accountPattern = config.getString("accountPattern");
        }

        if (config.hasPath("nameMapping")) {
            nameMapping = config.getString("nameMapping");
        }

        if (config.hasPath("emailMapping")) {
            emailMapping = config.getString("emailMapping");
        }

        groupBase = "";
        if (config.hasPath("groupBase")) {
            config.getString("groupBase");
        }

        groupMemberPattern = "(&(objectClass=group)(member=${dn}))";
        if (config.hasPath("groupMemberPattern")) {
            groupMemberPattern = config.getString("groupMemberPattern");
        }

        if (config.hasPath("adminGroups")) {
            adminGroups = config.getStringList("adminGroups");
        }

    }

    @Override
    public void start() {
        log.debug("Realm '{}' configuration:", getRealmName());
        Util.logSetting(log, "url", ldapUrl);
        Util.logSetting(log, "username", ldapUsername);
        Util.logSetting(log, "password", ldapPassword);
        Util.logSetting(log, "bindPattern", ldapBindPattern);
        Util.logSetting(log, "accountBase", accountBase);
        Util.logSetting(log, "accountPattern", accountPattern);
        Util.logSetting(log, "nameMapping", nameMapping);
        Util.logSetting(log, "emailMapping", emailMapping);
        Util.logSetting(log, "groupBase", groupBase);
        Util.logSetting(log, "groupMemberPattern", groupMemberPattern);
        Util.logSetting(log, "adminGroups", adminGroups);
        super.logCacheSettings(log);
    }

    @Override
    public void stop() {
    }

    @Override
    public Account authenticate(StandardCredentials requestCredentials) {

        final String username = getSimpleUsername(requestCredentials.getUsername());
        final String password = requestCredentials.getPassword();

        if (hasAccount(username)) {
            // account is cached, authenticate against the cache
            return super.authenticate(new StandardCredentials(username, password));
        }

        return authenticate(username, password);
    }

    @Override
    public Account authenticate(final String username, final String password) {
        LDAPConnection ldapConnection = getLdapConnection();
        if (ldapConnection != null) {
            try {
                boolean alreadyAuthenticated = false;

                if (!Strings.isNullOrEmpty(ldapBindPattern)) {
                    try {
                        String bindUser = ldapBindPattern.replace("${username}", escapeLDAPSearchFilter(username));
                        ldapConnection.bind(bindUser, password);

                        alreadyAuthenticated = true;
                    } catch (LDAPException e) {
                        return null;
                    }
                }

                // Find the logging in user's DN
                String searchPattern = accountPattern.replace("${username}", escapeLDAPSearchFilter(username));

                SearchResult result = doSearch(ldapConnection, accountBase, searchPattern);
                if (result != null && result.getEntryCount() == 1) {
                    SearchResultEntry accountSearchResult = result.getSearchEntries().get(0);
                    String accountDN = accountSearchResult.getDN();

                    if (alreadyAuthenticated || isAuthenticated(ldapConnection, accountDN, password)) {
                        log.debug("Authentication succeeded for '{}' against '{}'", username, getRealmName());

                        Account account = null;
                        synchronized (this) {
                            account = new Account(username, new StandardCredentials(username, password));
                            setAccountRoles(ldapConnection, accountSearchResult, account);
                            setAccountAttributes(accountSearchResult, account);

                            cacheAccount(account);
                        }

                        return account;
                    } else {
                        log.debug("Authentication failed for '{}' against '{}'", username, getRealmName());
                    }
                } else if (result == null || result.getSearchEntries().size() == 0) {
                    log.debug("No account found for '{}' in '{}'", username, getRealmName());
                }
            } finally {
                ldapConnection.close();
            }
        }
        return null;
    }

    private LDAPConnection getLdapConnection() {
        try {

            URI ldapUrl = new URI(this.ldapUrl);
            String ldapHost = ldapUrl.getHost();
            int ldapPort = ldapUrl.getPort();

            LDAPConnection conn;
            if (ldapUrl.getScheme().equalsIgnoreCase("ldaps")) {
                // SSL
                SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
                conn = new LDAPConnection(sslUtil.createSSLSocketFactory());
                if (ldapPort == -1) {
                    ldapPort = 636;
                }
            } else if (ldapUrl.getScheme().equalsIgnoreCase("ldap") || ldapUrl.getScheme().equalsIgnoreCase("ldap+tls")) {
                // no encryption or StartTLS
                conn = new LDAPConnection();
                if (ldapPort == -1) {
                    ldapPort = 389;
                }
            } else {
                log.error("Unsupported LDAP URL scheme: " + ldapUrl.getScheme());
                return null;
            }

            conn.connect(ldapHost, ldapPort);

            if (ldapUrl.getScheme().equalsIgnoreCase("ldap+tls")) {
                SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
                ExtendedResult extendedResult = conn.processExtendedOperation(
                        new StartTLSExtendedRequest(sslUtil.createSSLContext()));
                if (extendedResult.getResultCode() != ResultCode.SUCCESS) {
                    throw new LDAPException(extendedResult.getResultCode());
                }
            }

            if (Strings.isNullOrEmpty(ldapUsername) && Strings.isNullOrEmpty(ldapPassword)) {
                // anonymous bind
                conn.bind(new SimpleBindRequest());
            } else {
                // authenticated bind
                conn.bind(new SimpleBindRequest(ldapUsername, ldapPassword));
            }

            return conn;

        } catch (URISyntaxException e) {
            log.error("Bad LDAP URL, should be in the form: ldap(s|+tls)://<server>:<port>", e);
        } catch (GeneralSecurityException e) {
            log.error("Unable to create SSL Connection", e);
        } catch (LDAPException e) {
            if (!Strings.isNullOrEmpty(e.getDiagnosticMessage())) {
                log.error(e.getDiagnosticMessage());
            } else {
                log.error("Error connecting to LDAP server", e);
            }
        }

        return null;
    }

    private boolean isAuthenticated(LDAPConnection ldapConnection, String userDn, String password) {
        try {
            // Binding will stop any LDAP-Injection Attacks since the searched-for user needs to bind to that DN
            ldapConnection.bind(userDn, password);
            return true;
        } catch (LDAPException e) {
            if (!Strings.isNullOrEmpty(e.getDiagnosticMessage())) {
                log.error(e.getDiagnosticMessage());
            } else {
                log.error("Error authenticating user", e);
            }
            return false;
        }
    }

    private SearchResult doSearch(LDAPConnection ldapConnection, String base, String filter) {
        try {
            return ldapConnection.search(base, SearchScope.SUB, filter);
        } catch (LDAPSearchException e) {
            if (!Strings.isNullOrEmpty(e.getDiagnosticMessage())) {
                log.error(e.getDiagnosticMessage());
            } else {
                log.error("Problem searching LDAP", e);
            }
            return null;
        }
    }

    private SearchResult doSearch(LDAPConnection ldapConnection, String base, boolean dereferenceAliases, String filter, List<String> attributes) {
        try {
            SearchRequest searchRequest = new SearchRequest(base, SearchScope.SUB, filter);
            if (dereferenceAliases) {
                searchRequest.setDerefPolicy(DereferencePolicy.SEARCHING);
            }
            if (attributes != null) {
                searchRequest.setAttributes(attributes);
            }
            return ldapConnection.search(searchRequest);

        } catch (LDAPException e) {
            if (!Strings.isNullOrEmpty(e.getDiagnosticMessage())) {
                log.error(e.getDiagnosticMessage());
            } else {
                log.error("Problem searching LDAP", e);
            }
            return null;
        }
    }

    private void setAccountRoles(LDAPConnection ldapConnection, SearchResultEntry accountSearchResult, Account account) {
        String accountDN = accountSearchResult.getDN();

        String groupMemberPattern = this.groupMemberPattern.replace("${dn}", escapeLDAPSearchFilter(accountDN));
        groupMemberPattern = groupMemberPattern.replace("${username}", escapeLDAPSearchFilter(account.getUsername()));

        // Fill in attributes into groupMemberPattern
        for (Attribute attribute : accountSearchResult.getAttributes()) {
            groupMemberPattern = groupMemberPattern.replace("${" + attribute.getName() + "}", escapeLDAPSearchFilter(attribute.getValue()));
        }

        SearchResult groupsSearchResult = doSearch(ldapConnection, groupBase, true, groupMemberPattern, Arrays.asList("cn"));
        if (groupsSearchResult != null && groupsSearchResult.getEntryCount() > 0) {
            for (int i = 0; i < groupsSearchResult.getEntryCount(); i++) {
                SearchResultEntry groupEntry = groupsSearchResult.getSearchEntries().get(i);
                String roleName = groupEntry.getAttribute("cn").getValue();

                account.getAuthorizations().addRole(roleName);
            }
        }
    }

    private void setAccountAttributes(SearchResultEntry userSearchResult, Account account) {
        // Is this user an admin?
        setAdminAttribute(account);

        // Get full name Attribute
        if (!Strings.isNullOrEmpty(nameMapping)) {
            // Replace embedded ${} with attributes
            if (nameMapping.contains("${")) {
                // build display name from attributes
                String pattern = nameMapping;
                for (Attribute userAttribute : userSearchResult.getAttributes()) {
                    pattern = pattern.replace("${" + userAttribute.getName() + "}", userAttribute.getValue());
                }
                account.setName(pattern);
            } else {
                // display name is an attribute
                Attribute attribute = userSearchResult.getAttribute(nameMapping);
                if (attribute != null && attribute.hasValue()) {
                    account.setName(attribute.getValue());
                }
            }
        }

        // Get email address Attribute
        if (!Strings.isNullOrEmpty(emailMapping)) {
            if (emailMapping.contains("${")) {
                // build email address from attributes
                String pattern = emailMapping;
                for (Attribute userAttribute : userSearchResult.getAttributes()) {
                    pattern = pattern.replace("${" + userAttribute.getName() + "}", userAttribute.getValue());
                }
                account.addEmailAddress(pattern);
            } else {
                // email address is an attribute
                Attribute attribute = userSearchResult.getAttribute(emailMapping);
                if (attribute != null && attribute.hasValue()) {
                    account.addEmailAddress(attribute.getValue());
                }
            }
        }
    }

    /**
     * Set the admin attribute from group memberships retrieved from LDAP.
     *
     * @param account
     */
    private void setAdminAttribute(Account account) {
        if (adminGroups != null) {
            for (String adminGroup : adminGroups) {
                if (adminGroup.startsWith("@") && account.getUsername().equalsIgnoreCase(adminGroup.substring(1))) {
                    // admin user
                    account.getAuthorizations().addPermission("*");
                } else if (account.hasRole(adminGroup)) {
                    // admin role
                    account.getAuthorizations().addPermission("*");
                }
            }
        }
    }

    /**
     * Returns a simple username without any domain prefixes.
     *
     * @param username
     * @return a simple username
     */
    private String getSimpleUsername(String username) {
        int lastSlash = username.lastIndexOf('\\');
        if (lastSlash > -1) {
            username = username.substring(lastSlash + 1);
        }

        return username;
    }
}
