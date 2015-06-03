## About

**Fathom-Security** provides rich authentication and authorization options.

## Installation

Add the **Fathom-Security** artifact.

```XML
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

**Fathom-Security** is configured by the [HOCON] config file `conf/realms.conf`.

## Memory Realm

### Configuration

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

## LDAP Realm

### Installation

Add the **Fathom-Security-LDAP** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-ldap</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

### Installation

Add the **Fathom-Security-JDBC** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-jdbc</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

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

    # fathom-auth-jdbc supports HikariCP
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

### Installation

Add the **Fathom-Security-Redis** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-redis</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
realms: [

]
```

----

## PAM Realm

### Installation

Add the **Fathom-Security-PAM** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-pam</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
realms: [

]
```

----

## Windows Realm

### Installation

Add the **Fathom-Security-Windows** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-windows</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
realms: [

]
```

----

## Htpasswd Realm

### Installation

Add the **Fathom-Security-Htpasswd** artifact.

```XML
<dependency>
    <groupId>com.gitblit.fathom</groupId>
    <artifactId>fathom-security-htpasswd</artifactId>
    <version>${fathom.version}</version>
</dependency>
```

### Configuration

```hocon
realms: [

]
```

[HOCON]: https://github.com/typesafehub/config/blob/master/README.md
