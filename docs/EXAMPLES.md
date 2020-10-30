## Preparation

KExasol uses Kotlin test + TestNG to run examples. In order to run examples yourself, please follow the following instructions:

1. Make sure [Gradle](https://gradle.org/install/) is installed.
2. Make sure you have an Exasol instance up and running. You may use [Exasol Community Edition](https://www.exasol.com/portal/display/DOWNLOAD/Free+Trial) or [Exasol Docker version](https://github.com/exasol/docker-db) for tests.
3. Download [KExasol source code](https://github.com/badoo/kexasol/archive/master.zip) and unzip it.
4. Run the first example via `gradle test` to prepare data for other examples. Set Exasol credentials via environment variables:

```
EXAHOST=<dsn> EXAUID=<user> EXAPWD=<password> EXASCHEMA=<schema> gradle test --tests Example00*
```

Default credentials are:

```
EXAHOST=localhost
EXAUID=sys
EXAPWD=exasol
EXASCHEMA=kexasol_test
```

That's all. Now you may run other examples in any order:

```
EXAHOST=<dsn> EXAUID=<user> EXAPWD=<password> EXASCHEMA=<schema> gradle test --tests Example01*
EXAHOST=<dsn> EXAUID=<user> EXAPWD=<password> EXASCHEMA=<schema> gradle test --tests Example02*
EXAHOST=<dsn> EXAUID=<user> EXAPWD=<password> EXASCHEMA=<schema> gradle test --tests Example03*
...
```

You may also run examples directly in IntelliJ IDEA. In order to set credentials, go to "Run -> Edit Configurations -> Templates -> Gradle" and update the "Environment variables" field.

### Debug logging

In order to activate detailed debug logging, please run examples with project parameter `-P EXADEBUG`:

```
EXAHOST=<dsn> EXAUID=<user> EXAPWD=<password> EXASCHEMA=<schema> gradle test --tests Example01* -P EXADEBUG
```

## Examples

- [00_prepare.kt](/examples/00_prepare.kt) - prepare test data for other examples;
- [01_fetch.kt](/examples/01_fetch.kt) - run basic SELECT queries, fetch data;
- [02_stream.kt](/examples/02_stream.kt) - run various types of CSV streaming to EXPORT and IMPORT a large amounts of data;
- [03_format.kt](/examples/03_format.kt) - SQL query formatting;
- [04_edge_case.kt](/examples/04_edge_case.kt) - make sure KExasol can handle edge cases related to Exasol data types;
- [05_transaction.kt](/examples/05_transaction.kt) - run queries in transaction, use ROLLBACK and COMMIT;
- [06_redundancy.kt](/examples/06_redundancy.kt) - DSN parsing and ability of KExasol to tolerate offline nodes;
- [07_quote_ident.kt](/examples/07_quote_ident.kt) - ability to refer to Exasol identifiers with lower-cased and special characters (e.g. `camelCase!`);
- [08_abort_query.kt](/examples/08_abort_query.kt) - abort a running query from another thread;
- [09_snapshot.kt](/examples/09_snapshot.kt) - snapshot execution to prevent locks during meta data queries;
- [10_stream_parallel_export.kt](/examples/10_stream_parallel_export.kt) - parallel CSV reading using multiple child processes;
- [11_stream_parallel_import.kt](/examples/11_stream_parallel_import.kt) - parallel CSV writing using multiple child processes;
- [12_stream_parallel_export_import.kt](/examples/12_stream_parallel_export_import.kt) - parallel CSV reading & writing using multiple child processes;
