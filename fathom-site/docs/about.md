## What Fathom-Core Is

**Fathom-Core** is an injectable module loader integrated with a high-performance webserver and tied together by a structured configuration file.

**Fathom-Core** provides a structure for your microservice by specifying:

- a conventional directory structure
- a command-line arguments parser
- a highly configurable [logging framework](logging.md)
- a structured [configuration file](configuration.md) with runtime specification of alternate configs
- several runtime modes (PROD, TEST, DEV)
- an http/https engine
- a dependency injection mechanism
- a flexible [module](modules.md) loader
- a [service](services.md) infrastructure

## What Fathom-Core Is Not

**Fathom-Core** is not bloated.

While **Fathom-Core** is opinionated about it's core bootstrap dependencies, it does not prescribe any higher-level components.

It does not require using template engines, SQL ORMs, databases, NOSQL servers, caching providers, mail libraries, schedulers, or any other dependency.
