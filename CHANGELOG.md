# Change log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Provide routed RPC capabilities [\#26](https://github.com/angiolep/akka-wamp/issues/26)
  - Client API now provides ``register`` and ``invoke`` operations
  - Router now able to register procedures and route invocations to clients
### Changed
- Improve ScalaDoc comments [\#23](https://github.com/angiolep/akka-wamp/issues/23)

## [0.7.0] (2016-09-12)
### Added
- Provide better API for payloads [\#16](https://github.com/angiolep/akka-wamp/issues/16)
- Parse incoming messages skipping the payload [\#17](https://github.com/angiolep/akka-wamp/issues/17)


## [0.6.0] (2016-08-24)
### Added
- Validate against loose URIs by default but strict URIs validation now configurable [\#7](https://github.com/angiolep/akka-wamp/issues/7)
- Auto-create realms by default but abort session (when client requests unknown realm) now configurable
- Log ``SessionException`` as warning in console for some unspecified session handling scenarios [\#21](https://github.com/angiolep/akka-wamp/issues/21)
- Improve validators for WAMP Ids and URIs
- Update user's documentation


## [0.5.1] (2016-08-21)
### Added
- Provide proper error handling in ``router.Transport`` [\#18](https://github.com/angiolep/akka-wamp/issues/18)


## 0.5.0 (2016-08-20)
### Added
- WAMP Router with limited features
- Future based API for writing WAMP Clients with limited features
- Documentation

[Unreleased]: https://github.com/angiolep/akka-wamp/compare/v0.7.0...HEAD?&diff=split&name=HEAD
[0.7.0]: https://github.com/angiolep/akka-wamp/compare/v0.6.0...v0.7.0?diff=split&name=v0.7.0
[0.6.0]: https://github.com/angiolep/akka-wamp/compare/v0.5.1...v0.6.0?diff=split&name=v0.6.0
[0.5.0]: https://github.com/angiolep/akka-wamp/compare/v0.5.0...v0.5.1?diff=split&name=v0.5.1