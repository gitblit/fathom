## About

**Fathom-Security** provides support for multiple authentication realms and an authorization infrastructure.

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

A complete authentication and authorization model will include declarations of:

- **Realms**<br/>*Realms* are sources of *Accounts* and potentially *Roles* and *Permissions*.<br/>*Realms* are interrogated during the authentication process.
- **Accounts**<br/>*Accounts* represent a *username-password* pair.<br/>They may also include additional metadata such as *display name*, *email addresses*, *Roles*, and *Permissions*.
- **Roles**<br/>*Roles* are a named grouping of specific *Permissions*.
- **Permissions**<br/>*Permissions* are discrete authorization rules.

*Account* usernames are specified to be *global* across all *Realms*.  **Fathom-Security** will collect and merge *Account* definitions across all defined *Realms* to create an aggregate *Account*.  This is necessary because not all *Realms* are able to provide full *Account* metadata, *Roles*, or *Permissions*.

For example, the *Account* named *james* is assumed to represent the same person across all defined *Realms* so that if *james* authenticates against a **PAM Realm**, his *Account* metadata, *Roles*, and *Permissions* will be collected from the *james* *Account* in the defined **File Realm**.

**Fathom-Security** is configured by the [HOCON] config file `conf/realms.conf`.

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


----

## Memory Realm

The **Memory Realm** defines *Accounts* & *Roles* within the `conf/realms.conf` file.

*Accounts* and *Roles* are loaded only once on startup.

### Configuration

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

## File Realm

The **File Realm** defines *Accounts* & *Roles* in an external [HOCON] file.

This realm will hot-reload on modification to the [HOCON] file.

### Configuration

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

## Htpasswd Realm

The **Htpasswd Realm** defines partial *Accounts* (username & password) in an external [htpasswd] file.

This realm will hot-reload on modification to the [htpasswd] file.

**Note:**<br/>
You may only *authenticate* against an **Htpasswd Realm**.<br/>This *realm* does not support persistence of authorization data.

### Installation

Add the **Fathom-Security-Htpasswd** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-htpasswd</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

## LDAP Realm

The **LDAP Realm** allows you to integrate authentication and authorization with your LDAP server.

**Note:**<br/>
You may authenticate and authorize using LDAP-sourced data but *Role definitions* are not currently supported by the **LDAP Realm**.

### Installation

Add the **Fathom-Security-LDAP** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-ldap</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

## JDBC Realm

The **JDBC Realm** allows you to integrate authentication and authorization with an SQL database.

### Installation

Add the **Fathom-Security-JDBC** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-jdbc</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

## Redis Realm

The **Redis Realm** allows you to integrate authentication and authorization with a Redis server.

**Note:**<br/>
You may authenticate and authorize using Redis-sourced data but *Role definitions* are not currently supported by the **Redis Realm**.

### Installation

Add the **Fathom-Security-Redis** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-redis</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

## PAM Realm

The **PAM Realm** allows you to authenticate against the local accounts on a Linux/Unix/OSX machine.

**Note:**<br/>
You may only *authenticate* against a **PAM Realm**.<br/>This *realm* does not support persistence of authorization data.

### Installation

Add the **Fathom-Security-PAM** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-pam</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

## Windows Realm

The **Windows Realm** allows you to authenticate against the local accounts on a Windows machine.

**Note:**<br/>
You may only *authenticate* against a **Windows Realm**.<br/>This *realm* does not support persistence of authorization data.

### Installation

Add the **Fathom-Security-Windows** artifact.

```xml
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-windows</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

[HOCON]: https://github.com/typesafehub/config/blob/master/README.md
[htpasswd]: https://httpd.apache.org/docs/current/programs/htpasswd.html
