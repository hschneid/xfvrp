# Changelog

All notable changes to the XFVRP project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- `Node.copy()` now deep-copies preset lists and blacklist; no longer returns null on clone failure
- Block optimization failures in `XFVRPOptSplitter` are propagated as `XFVRPException` instead of being silently swallowed; the executor service is shut down after use
- `ReportSummary.getOverload()`/`getOverloads()` no longer throw an NPE when no route has overload entries

### Changed
- Optimization type fields in `XFVRPOptTypes` are final now
- Removed dead code (`Debug`, `Application`) and unused Gradle build files
- Removed empty `checkMaxWaiting` pre-check stub (constraint is enforced during route evaluation)
- Updated test dependencies: Spock 2.4-groovy-5.0, byte-buddy 1.17.8, Surefire 3.6.1

### Added
- End-to-end spec for the `XFVRP` entry class
- Benchmark specs are executed by a nightly CI job (`mvn test -P benchmarks`) instead of being `@Ignore`d
- VRPTT support (`XFVRPOptTypes.VRPTT`, `VRPTT_ILS`): lorries and trailers with additive train capacity,
  lorry/trailer customer accessibility, transshipment locations (time window, transfer time) and
  synchronization-checked load transfer between vehicles; validated on 109 synthetic Drexl-style
  benchmark instances — see [docs/VRPTT.md](docs/VRPTT.md) and [docs/VRPTT-SPEC.md](docs/VRPTT-SPEC.md)
- User documentation for the VRPTT with an example case ([docs/VRPTT.md](docs/VRPTT.md)), linked in the README

## [12.2.0] - 2026-11-12

### Added
- `CostMixedFleetHeuristic` (`xf.xfvrp.opt.fleetmix`): improves the plan of the default mixed fleet heuristic across
  vehicle types by cost — merges two routes into one route of the vehicle of either and moves a route to another type
  with a vehicle left, each planned again and accepted only if valid and cheaper; vehicle counts are never exceeded.

### Removed
- `fhg-gitlab-registry` profile and its deploy step (registry no longer available).

## [12.1.0] - 2025-12-03

### Changed
- Updated Maven Compiler Plugin to version 3.14.1
- Updated Java target version to 21
- Updated Apache Groovy to version 5.0.2 (from 4.0.25)
- Updated Spock Framework to version 2.4-M7-groovy-5.0 (from 2.4-M5-groovy-4.0)
- Updated Lombok to version 1.18.42

### Added
- Added GitLab registry profiles for distribution management
  - `gitlab-registry` profile (default)
  - `fhg-gitlab-registry` profile

### Technical Details
- Full compatibility with Java 21
- Groovy 5.0.2 provides enhanced performance and language features
- Spock 2.4-M7 includes improved testing capabilities with Groovy 5.0 support

## [12.0.0] - Previous Release

### Initial
- eXtreme Fast Vehicle Routing Problem with F-IML modification
- Initial public release with Java 17 support
