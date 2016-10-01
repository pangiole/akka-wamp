# Change log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
... 

## [0.10.0] _ 2016-10-01

### Added
- Improve Payload Handling and Streaming support
- Implement deserialization of streamed text messages [\#27](https://github.com/angiolep/akka-wamp/issues/27)

### Fixed
- Both router and client don't validate RPC message types [\#34](https://github.com/angiolep/akka-wamp/issues/34)

    
## [0.9.0] _ 2016-09-29    

### Added
- Provide routed RPC capabilities [\#26](https://github.com/angiolep/akka-wamp/issues/26)  
  - Client API now provides ``register``, ``unregister`` and ``call`` operations  
  - Router now able to register procedures and route calls to clients  
- Improve Payload Handling and Streaming support
- Improve tests
- Improve documentation

## [0.8.0] _ 2016-09-19  

### Added  
- Validate dictionaries [\#19](https://github.com/angiolep/akka-wamp/issues/19)        
- Make decision on DeserializeException configurable [\#20](https://github.com/angiolep/akka-wamp/issues/20)  

### Fixed
- Lack of specification for repeated HELLOs [\#21](https://github.com/angiolep/akka-wamp/issues/21)
- Lack of specification specification in case of offending messages [\#22](https://github.com/angiolep/akka-wamp/issues/22)

### Changed
- Improve ScalaDoc comments [\#23](https://github.com/angiolep/akka-wamp/issues/23)
- Improve [ReadTheDocs](http://akka-wamp.readthedocs.io/) 


## [0.7.0] _ 2016-09-12

### Added
- Provide better API for payloads [\#16](https://github.com/angiolep/akka-wamp/issues/16)
- Parse incoming messages skipping the payload [\#17](https://github.com/angiolep/akka-wamp/issues/17)


## [0.6.0] _ 2016-08-24

### Added
- Validate against loose URIs by default but strict URIs validation now configurable [\#7](https://github.com/angiolep/akka-wamp/issues/7)
- Auto-create realms by default but abort session (when client requests unknown realm) now configurable
- Log ``SessionException`` as warning in console for some unspecified session handling scenarios [\#21](https://github.com/angiolep/akka-wamp/issues/21)
- Improve validators for WAMP Ids and URIs
- Update user's documentation


## [0.5.1] _ 2016-08-21

### Added
- Provide proper error handling in ``router.Connection`` [\#18](https://github.com/angiolep/akka-wamp/issues/18)


## 0.5.0 _ 2016-08-20

### Added
- WAMP Router with limited features
- Future based API for writing WAMP Clients with limited features
- Documentation

[Unreleased]: https://github.com/angiolep/akka-wamp/compare/v0.10.0...HEAD?&diff=split&name=HEAD
[0.10.0]: https://github.com/angiolep/akka-wamp/compare/v0.0.0...v0.10.0?diff=split&name=v0.9.0
[0.9.0]: https://github.com/angiolep/akka-wamp/compare/v0.8.0...v0.9.0?diff=split&name=v0.8.0
[0.8.0]: https://github.com/angiolep/akka-wamp/compare/v0.7.0...v0.8.0?diff=split&name=v0.7.0
[0.7.0]: https://github.com/angiolep/akka-wamp/compare/v0.6.0...v0.7.0?diff=split&name=v0.7.0
[0.6.0]: https://github.com/angiolep/akka-wamp/compare/v0.5.1...v0.6.0?diff=split&name=v0.6.0
[0.5.0]: https://github.com/angiolep/akka-wamp/compare/v0.5.0...v0.5.1?diff=split&name=v0.5.1