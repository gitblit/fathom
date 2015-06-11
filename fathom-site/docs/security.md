## About

**Fathom-Security** provides support for multiple authentication realms and an authorization infrastructure.

The core authorization design of [Apache Shiro] was harvested & married to the core authentication design of [Gitblit] to form a similar but significantly lighterweight security infrastructure.

A complete authentication and authorization model will include declarations of:

Realms
:   *Realms* are sources of *Accounts* and potentially *Roles* and *Permissions*.
    *Realms* are interrogated during the authentication process.

Accounts
:   *Accounts* represent a *username-password* pair.
    They may also include additional metadata such as display name, email addresses, *Roles*, and *Permissions*.

Roles
:   *Roles* are a named grouping of specific *Permissions*.

Permissions
:   *Permissions* are allowed application actions.

## Installation

Add the **Fathom-Security** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

## Layout

```
YourApp
└── src
    └── main
        └── java
            └── conf
                └── realms.conf
```

## Configuration

**Fathom-Security** is configured by the [HOCON] resource config file `conf/realms.conf`.

```hocon
# Configured Realms.
# Realms will be tried in the order specified until an authentication is successful.
realms: []

# If you have multiple Realms and are creating aggregate Accounts you
# may cache the aggregate/assembled accounts in the SecurityManager.
#
# Configure the aggregated Account time-to-live (TTL) in minutes and the maximum
# number of aggregated accounts to keep cached.
# A TTL of 0 disables this cache.
cacheTtl: 0
cacheMax: 100
```

## Usage

**Fathom-Security** provides a singleton instance of a *SecurityManager* [service](services.md).  This service contains all loaded *Realms*, *Accounts*, *Roles*, and *Permissions*.

```java
@Inject
SecurityManager securityManager;

public Account login(String username, String password) {
  StandardCredentials credentials = new StandardCredentials(username, password);
  Account account = securityManager.authenticate(credentials);
  return account;
}
```

## Accounts

*Account* usernames are specified to be **global** across all *realms*.  **Fathom-Security** will collect and merge *account* definitions across all defined *realms* to create an aggregate *account*.  This is necessary because not all *realms* are able to provide full *account* metadata, *roles*, or *permissions*.

For example, the *account* named `james` is assumed to represent the same person across all defined *realms* so that if `james` authenticates against a **PAM Realm**, his *account* metadata, *roles*, and *permissions* can be collected from the `james` *account* defined in a **File Realm**, **JDBC Realm**, etc.

## Permissions

*Permissions* are allowed application actions.  *Permissions* can be specified as a simple action (e.g. *view*) or as a granular, colon-delimited action (e.g. *employees:view:5*).  Granular permissions may be assigned with up to three components: `domain:action:instance`.

<pre>
# Permit viewing employees 5 and 10
employees:view:5,10

# Permit viewing all employees (these are equivalent)
employees:view:*
employees:view

# Permit updating employees 5 and 10
employees:update:5,10

# Permit all actions on employee 5
employees:*:5

# Permit adding and deleting any employee (these are equivalent)
employees:add,delete:*
employees:add,delete

# Permit all actions on all employees and contractors
employees,contractors:*</pre>

## Roles

*Roles* may be specified on an *account*.  *Roles* may also be defined to have explicit *permissions*.  The primary value of a *role* is that it allows you to forgo maintaining the same set of *permissions* on multiple *accounts*.  Instead you can maintain a single set of *permissions* in the *role* definition and assign this *role* to multiple *accounts*.

In the example below, the `admin` *account* has two assigned *roles*.  The `administrator` *role* is explicitly defined with the `*` *permission*, while the `tester` *role* has no definition and therefore has no explicit *permissions*.  The `frank` *account* has been assigned the `normal` *role* and is only granted the `secure:view` *permission*.

```hocon
accounts: [
  {
    username: "admin"
    roles: ["administrator", "tester"]
    permissions: ["powers:speed,strength,agility"]
  }
  {
    username: "frank"
    roles: ["normal"]
  }
  {
    username: "joe"
    roles: ["normal"]
    disabled: true
  }
]

roles: {
  administrator: ["*"]
  normal: ["secure:view"]
}
```

## Authorization

*Account* instances have many methods to enforce authorization for a particular action.

```java
public void update(Employee employee) {
  Account account = getAccount();
  if (account.hasRole("administrator") || account.isPermitted("employee:update")) {
    employeeDao.update(employee);
  }
}
```

You can also enforce authorization with methods that throw an *AuthorizationException* rather than returning a boolean status.

```java
public void update(Employee employee) {
  Account account = getAccount();
  account.checkPermission("employee:update");
  employeeDao.update(employee);
}
```

## Disabling Accounts

In the above example the `joe` *account* is disabled.

```hocon
accounts: [
  {
    username: "joe"
    disabled: true
  }
]
```

In this case the *SecurityManager* will not allow the `joe` *account* to authenticate.

## Realms

There are many realm integrations available for Fathom.

| Realm       | Module                                                         |
|-------------|----------------------------------------------------------------|
| Memory      | [com.gitblit.fathom:fathom-security](#memory-realm)            |
| File        | [com.gitblit.fathom:fathom-security](#file-realm)              |
| Htpasswd    | [com.gitblit.fathom:fathom-security-htpasswd](#htpasswd-realm) |
| LDAP        | [com.gitblit.fathom:fathom-security-ldap](#ldap-realm)         |
| JDBC        | [com.gitblit.fathom:fathom-security-jdbc](#jdbc-realm)         |
| Redis       | [com.gitblit.fathom:fathom-security-redis](#redis-realm)       |
| PAM         | [com.gitblit.fathom:fathom-security-pam](#pam-realm)           |
| Windows     | [com.gitblit.fathom:fathom-security-windows](#windows-realm)   |

### Memory Realm

The **Memory Realm** defines *Accounts* & *Roles* within the `conf/realms.conf` resource file.

*Accounts* and *Roles* are loaded only once on startup.

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # MEMORY REALM
    # All Accounts and Roles are loaded from this definition and cached in a ConcurrentHashMap.
    name: "Memory Realm"
    type: "fathom.realm.MemoryRealm"

    accounts: [
      {
        name: "Administrator"
        username: "admin"
        password: "admin"
        emailAddresses: ["fathom@gitblit.com"]
        roles: ["administrator"]
        permissions: ["powers:speed,strength,agility"]
      }

      {name: "User", username: "user", password: "user", roles: ["normal"], disabled: true}
      {name: "Guest", username: "guest", password: "guest"}

      # assign metadata and a role to an htpasswd account
      {name: "Luke Skywalker", username: "red5", roles: ["normal"]}

      # assign a role to an ldap account
      {username: "UserOne", roles: ["normal"]}
    ]

    #
    # Defined Roles are named and have an array of Permissions.
    #
    roles: {
      administrator: ["*"]
      normal: ["secure:view"]
    }
  }
]
```

----

### File Realm

The **File Realm** defines *Accounts* & *Roles* in an external [HOCON] file.

This realm will hot-reload on modification to the [HOCON] file.

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # FILE REALM
    # All Accounts and Roles are loaded from this definition and cached in a ConcurrentHashMap.
    name: "File Realm"
    type: "fathom.realm.FileRealm"
    file: "classpath:conf/users.conf"
  }
]
```

**conf/users.conf**
```hocon
accounts: [
  {
    name: "Administrator"
    username: "admin"
    password: "admin"
    emailAddresses: ["fathom@gitblit.com"]
    roles: ["administrator"]
    permissions: ["powers:speed,strength,agility"]
  }

  {name: "User", username: "user", password: "user", roles: ["normal"], disabled: true}
  {name: "Guest", username: "guest", password: "guest"}

  # assign metadata and a role to an htpasswd account
  {name: "Luke Skywalker", username: "red5", roles: ["normal"]}

  # assign a role to an ldap account
  {username: "UserOne", roles: ["normal"]}
]

#
# Defined Roles are named and have an array of Permissions.
#
roles: {
  administrator: ["*"]
  normal: ["secure:view"]
}
```

----

### Htpasswd Realm

The **Htpasswd Realm** defines partial *Accounts* (username & password) in an [htpasswd] file.

This realm will hot-reload on modification to the [htpasswd] file.

!!! Note
    You may only *authenticate* against an **Htpasswd Realm**.
    This *realm* does not support persistence of authorization data.

#### Installation

Add the **Fathom-Security-Htpasswd** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-htpasswd</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # HTPASSWD REALM
    name: "Htpasswd Realm"
    type: "fathom.realm.htpasswd.HtpasswdRealm"
    file: "classpath:conf/users.htpasswd"
    allowClearPasswords: false
  }
]
```

----

### LDAP Realm

The **LDAP Realm** allows you to integrate authentication and authorization with your LDAP server.

!!! Note
    You may authenticate and authorize using LDAP-sourced data but *Role definitions* are not currently supported by the **LDAP Realm**.

#### Installation

Add the **Fathom-Security-LDAP** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-ldap</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # LDAP REALM
    # Authenticates credentials from an LDAP server.
    # This is a CachingRealm which may optionally configure an expiring cache.
    name: "UnboundID LDAP"
    type: "fathom.realm.ldap.LdapRealm"
    url: "ldap://localhost:1389"
    username: "cn=Directory Manager"
    password: "password"

    # LDAP search syntax for looking up accounts.
    accountBase: "OU=Users,OU=UserControl,OU=MyOrganization,DC=MyDomain"
    accountPattern: "(&(objectClass=person)(sAMAccountName=${username}))"

    # LDAP search syntax for looking up groups.
    # LDAP group names are mapped to Roles.
    # Roles can be optionally mapped to permissions.
    groupBase: "OU=Groups,OU=UserControl,OU=MyOrganization,DC=MyDomain"
    groupMemberPattern: "(&(objectClass=group)(member=${dn}))"

    # Members of these LDAP Groups are given "*" administrator permissions.
    # Invidual accounts can be specified with the "@" prefix
    adminGroups: ["@UserThree", "Git_Admins", "Git Admins"]

    # Mapping controls for account name and email address extraction.
    # These may be an attribute name or can be a complex expression.
    nameMapping: "displayName"
    emailMapping: "email"

    # Configure the cached Account time-to-live (TTL) in minutes and the maximum
    # number of accounts to keep cached.
    # A TTL of 0 disables this cache.
    cacheTtl: 0
    cacheMax: 100
  }
]
```

----

### JDBC Realm

The **JDBC Realm** allows you to integrate authentication and authorization with an SQL database.

#### Installation

Add the **Fathom-Security-JDBC** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-jdbc</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # JDBC/SQL REALM
    # Authenticates credentials from an SQL datasource.
    # This is a CachingRealm which may optionally configure an expiring cache.
    name: "H2 Realm"
    type: "fathom.realm.jdbc.JdbcRealm"
    url: "jdbc:h2:mem:fathom"
    username: ""
    password: ""

    # Specify a script to run on startup of the Realm.
    # This script creates our tables and populates some data.
    startScript: "classpath:conf/realm.sql"

    # Specify an account query and column mappings to populate Account metadata.
    #
    # This optional mapping only works for the table (or view) referenced in the
    # accountQuery.
    accountQuery: "select * from accounts where username=?"
    nameMapping: "name"
    passwordMapping: "password"

    # Email address column mapping if your addresses are in the same table as your accounts.
    # This value may be delimited by a comma or semi-colon to support multiple addresses.
    emailMapping: "email"

    # A Role column mapping if your roles are in the same table as your accounts.
    # This value may be delimited by a comma or semi-colon to support multiple roles.
    roleMapping: ""

    # A Permission column mapping if your permissions are in the same table as your accounts.
    # This value may be delimited by a semi-colon to support multiple permissions.
    permissionMapping: ""

    # Specify an account roles query.
    # This is useful if your roles are defined in a separate table from your accounts.
    #
    # The first column of the ResultSet must be a String role name.
    # The String role name may be delimited by a comma or semi-colon to support multiple roles.
    accountRolesQuery: "select role from account_roles where username=?"

    # Specify an account permissions query.
    # This is useful if your permissions are defined in a separate table from your accounts.
    #
    # The first column of the ResultSet must be a String permission value.
    # The String permission value may be delimited by a semi-colon to support multiple permissions.
    accountPermissionsQuery: "select permission from account_permissions where username=?"

    # Specify a defined roles query.
    # Defined roles specify permissions for a role name.
    #
    # The first column of the ResultSet must be a String role name.
    # The second column of the ResultSet must be a String permission value.
    # The String definition value may be delimited by a semi-colon to support multiple permissions.
    definedRolesQuery: "select role, definition from defined_roles"

    # Configure the cached Account time-to-live (TTL) in minutes and the maximum
    # number of accounts to keep cached.
    # A TTL of 0 disables this cache.
    cacheTtl: 0
    cacheMax: 100

    # fathom-security-jdbc supports HikariCP
    # see http://brettwooldridge.github.io/HikariCP/
    hikariCP {
      connectionTimeout: 5000
      registerMbeans: true
    }
  }
]
```

----

### Redis Realm

The **Redis Realm** allows you to integrate authentication and authorization with a Redis server.

!!! Note
    You may authenticate and authorize using Redis-sourced data but *Role definitions* are not currently supported by the **Redis Realm**.

#### Installation

Add the **Fathom-Security-Redis** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-redis</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # REDIS REALM
    name: "Redis Realm"
    type: "fathom.realm.redis.RedisRealm"

    # Specify the url to the Redis database
    url: "redis://localhost:6379/8"
    # The password for the Redis server, if needed
    password: ""

    # Specify the key mappings for account and role lookups
    passwordMapping: "fathom:${username}:password"
    nameMapping: "fathom:${username}:name"
    emailMapping: "fathom:${username}:email"
    roleMapping: "fathom:${username}:roles"
    permissionMapping: "fathom:${username}:permissions"
  }
]
```

----

### PAM Realm

The **PAM Realm** allows you to authenticate against the local accounts on a Linux/Unix/OSX machine.

!!! Note
    You may only *authenticate* against a **PAM Realm**.
    This *realm* does not support persistence of authorization data.

#### Installation

Add the **Fathom-Security-PAM** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-pam</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # PAM REALM
    name: "PAM Realm"
    type: "fathom.realm.pam.PamRealm"

    serviceName: "system-auth"
  }
]
```

----

### Windows Realm

The **Windows Realm** allows you to authenticate against the local accounts on a Windows machine.

!!! Note
    You may only *authenticate* against a **Windows Realm**.
    This *realm* does not support persistence of authorization data.

#### Installation

Add the **Fathom-Security-Windows** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-windows</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

#### Configuration

**conf/realms.conf**
```hocon
realms: [
  {
    # WINDOWS REALM
    name: "Windows Realm"
    type: "fathom.realm.windows.WindowsRealm"

    defaultDomain: ""
    allowGuests: false
    adminGroups: [ "BUILTIN\Administrators" ]
  }
]
```

[Apache Shiro]: https://shiro.apache.org/
[Gitblit]: http://gitblit.com
[HOCON]: https://github.com/typesafehub/config/blob/master/README.md
[htpasswd]: https://httpd.apache.org/docs/current/programs/htpasswd.html
