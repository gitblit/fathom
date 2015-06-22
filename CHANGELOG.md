## Changelog
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

### [Unreleased][unreleased]

#### Fixed

#### Changed

- [fathom-rest] Refined controllers to be more declarative
- [fathom-core] Added more system information to startup log
- [fathom-core] Update Undertow to 1.2.8

#### Added

- [fathom-rest-swagger] Added new module for automatic Swagger specification generation
- [fathom-rest] Added simple validation of controller arguments
- [fathom-rest] Added HeaderFilter and CORSFilter
- [fathom-core] Added Undertow performance tuning settings

#### Removed

### [0.5.1] - 2015-06-11

#### Fixed

- [fathom-rest] Fixed discovery of controllers when *application.package* is specified
- [fathom-archetype-standard] Honor specified package during project generation

#### Changed

- [fathom-archetype-standard] Updated to Infinispan 7.2.2

### [0.5.0] - 2015-06-09

Initial release.

#### Added

- Added [fathom-core]
- Added [fathom-eventbus]
- Added [fathom-jcache]
- Added [fathom-mailer]
- Added [fathom-metrics]
- Added [fathom-metrics-ganglia]
- Added [fathom-metrics-graphite]
- Added [fathom-metrics-influxdb]
- Added [fathom-metrics-librato]
- Added [fathom-quartz]
- Added [fathom-rest]
- Added [fathom-rest-security]
- Added [fathom-rest-shiro]
- Added [fathom-security]
- Added [fathom-security-htpasswd]
- Added [fathom-security-jdbc]
- Added [fathom-security-ldap]
- Added [fathom-security-pam]
- Added [fathom-security-redis]
- Added [fathom-security-windows]
- Added [fathom-test-tools]
- Added [fathom-archetype-standard]

[unreleased]: https://github.com/gitblit/fathom/compare/release-0.5.1...HEAD
[0.5.1]: https://github.com/gitblit/fathom/compare/release-0.5.0...release-0.5.1

[fathom-core]: https://github.com/gitblit/fathom/tree/master/fathom-core
[fathom-eventbus]: https://github.com/gitblit/fathom/tree/master/fathom-eventbus
[fathom-jcache]: https://github.com/gitblit/fathom/tree/master/fathom-jcache
[fathom-mailer]: https://github.com/gitblit/fathom/tree/master/fathom-mailer
[fathom-metrics]: https://github.com/gitblit/fathom/tree/master/fathom-metrics
[fathom-metrics-ganglia]: https://github.com/gitblit/fathom/tree/master/fathom-metrics-ganglia
[fathom-metrics-graphite]: https://github.com/gitblit/fathom/tree/master/fathom-metrics-graphite
[fathom-metrics-influxdb]: https://github.com/gitblit/fathom/tree/master/fathom-metrics-influxdb
[fathom-metrics-librato]: https://github.com/gitblit/fathom/tree/master/fathom-metrics-librato
[fathom-quartz]: https://github.com/gitblit/fathom/tree/master/fathom-quartz
[fathom-rest]: https://github.com/gitblit/fathom/tree/master/fathom-rest
[fathom-rest-security]: https://github.com/gitblit/fathom/tree/master/fathom-rest-security
[fathom-rest-shiro]: https://github.com/gitblit/fathom/tree/master/fathom-rest-shiro
[fathom-rest-swagger]: https://github.com/gitblit/fathom/tree/master/fathom-rest-swagger
[fathom-security]: https://github.com/gitblit/fathom/tree/master/fathom-security
[fathom-security-htpasswd]: https://github.com/gitblit/fathom/tree/master/fathom-security-htpasswd
[fathom-security-jdbc]: https://github.com/gitblit/fathom/tree/master/fathom-security-jdbc
[fathom-security-ldap]: https://github.com/gitblit/fathom/tree/master/fathom-security-ldap
[fathom-security-pam]: https://github.com/gitblit/fathom/tree/master/fathom-security-pam
[fathom-security-redis]: https://github.com/gitblit/fathom/tree/master/fathom-security-redis
[fathom-security-windows]: https://github.com/gitblit/fathom/tree/master/fathom-security-windows
[fathom-test-tools]: https://github.com/gitblit/fathom/tree/master/fathom-test-tools
[fathom-archetype-standard]: https://github.com/gitblit/fathom/tree/master/fathom-archetype-standard
