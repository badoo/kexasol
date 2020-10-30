# Changelog

## [0.2.1] - 2020-10-30

- Reworked connection close logic to send `CLOSE` WebSocket control frame only. "Disconnect" command will no longer be sent.
- Refactored examples to close connections properly.
- Upgraded to Gradle 6.7.
- Switched from JUni5 to TestNG.
- Added project parameter `-P EXADEBUG` which can be used to activate KExasol debug logging in examples.
- Fixed the problem of `streamImport` skipping the first row.
- Fixed the problem of "dangling name data" in `ExecuteV1` Moshi adapter.
- Added Travis CI to test examples automatically against all major Exasol versions.

## [0.1.1]

- Initial version of KExasol was made public.
