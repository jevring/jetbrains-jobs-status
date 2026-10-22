<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# jetbrains-jobs-status Changelog

## [Unreleased]

## [0.0.8] - 2026-10-22

### Added

- Names and "urls" of tests in the endpoint
- Separate the process into a separate object in the response, so that it can be evaluated independently of the test itself. In theory, there could be tests
  without processes, so this would then match.

### Fixed

- Plugin couldn't be unloaded due to clean-up task (you will likely need to restart IntelliJ IDEA to update)

## [0.0.7] - 2026-10-21

### Added

- More details about failures, including exit code
- Earlier detection of process start, and clearer handling of termination
- Failures now specify names, including via annotations like `@DisplayName` and `@ParameterizedTest`.
- `verbose=true` to include the names of all known tests and their states. Failures are included, but failure *details* are in `failures`

## [0.0.6] - 2026-10-20

### Fixed

- Handled some nulls and range conditions

## [0.0.5] - 2026-10-19

### Added

- More timestamps in the REST endpoint
- Build statuses in the REST endpoint
- PID (if available) in the REST endpoint for tests

### Fixed

- Better error handling in the REST endpoint

## [0.0.4] - 2026-10-18

### Added

- Always keep the most recent inactive job of each type, no matter how old

## [0.0.3] - 2026-10-18

### Added

- Actual changelog

## [0.0.2] - 2026-10-18

### Fixed

- Stop using internal APIs

## [0.0.1] - 2026-10-18

### Added

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Expose test runs via REST endpoint at http://127.0.0.1:63342/api/jobs
- Expose project builds via REST endpoint at http://127.0.0.1:63342/api/jobs
