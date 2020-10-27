# Reference

This page contains complete reference of KExasol public API.

- [ExaConnectionOptions](#ExaConnectionOptions)
  - functions:
    - [connect](#connect)
    - [streamWorker](#streamworker)
- [ExaConnection](#ExaConnection)
  - functions:
    - [execute](#execute)
    - [commit](#commit)
    - [rollback](#rollback)
    - [abortQuery](#abortquery)
    - [streamImport](#streamimport)
    - [streamImportWriter](#streamimportwriter)
    - [streamImportIterator](#streamimportiterator)
    - [streamExport](#streamexport)
    - [streamExportReader](#streamexportreader)
    - [streamParallelNodes](#streamparallelnodes)
    - [streamParallelImport](#streamparallelimport)
    - [streamParallelExport](#streamparallelexport)
    - [setAutocommit](#setautocommit)
    - [setQueryTimeout](#setquerytimeout)
    - [openSchema](#openschema)
    - [setAttributes](#setattributes)
    - [close](#close)
  - properties:
    - [.loginInfo](#logininfo)
    - [.attributes](#attributes)
    - [.sessionId](#sessionid)
    - [.protocolVersion](#protocolversion)
    - [.currentSchema](#currentschema)
- [ExaStatement](#ExaStatement)
  - functions:
    - [forEach](#exastatementforeach)
    - [asSequence](#exastatementassequence)
    - [onlyRow](#exastatementonlyrow)
    - [findColumn](#exastatementfindcolumn)
    - [close](#exastatementclose)
  - properties:
    - [.columns](#columns)
    - [.columnNamesMap](#columnnamesmap)
    - [.rowCount](#rowcount)
    - [.formattedQuery](#formattedquery)
    - [.executionTimeMillis](#executiontimemillis)
- [ExaStreamCSVReader](#ExaStreamCSVReader)
  - functions:
    - [forEach](#exastreamcsvreaderforeach)
    - [asSequence](#exastreamcsvreaderassequence)
    - [close](#exastreamcsvreaderclose)
- [ExaStreamCSVWriter](#ExaStreamCSVWriter)
  - functions:
    - [writeRow](#exastreamcsvwriterwriterow)
    - [close](#exastreamcsvwriterclose)
- [ExaStreamWorker](#ExaStreamWorker)
  - functions:
    - [streamImport](#exastreamworkerstreamimport)
    - [streamImportWriter](#exastreamworkerstreamimportwriter)
    - [streamExport](#exastreamworkerstreamexport)
    - [streamExportReader](#exastreamworkerstreamexportreader)
  - properties:
    - [.internalAddress](#exastreamworkerinternaladdress)
    
---
    
## ExaConnectionOptions

| ARG | TYPE | EXAMPLE | DESCRIPTION |
| --- | --- | --- | --- |
| dsn | String | `exasolpool1..5` | Exasol connection string, same format as standard JDBC / ODBC drivers |
| user | String | `sys` | Username |
| password | String| `exasol` | Password |
| schema | String| `sys` | Open schema after connection |
| autocommit | Boolean | `true` | Enable autocommit after connection |
| compression | Boolean | `false` | Enable zlib compression for WebSocket requests and for CSV streaming |
| encryption | `ExaEncrptionMode.*` | `DISABLED` | Enable SSL encryption for WebSocket requests and for CSV streaming |
| quoteIdentifier | Boolean | `false` | Add double quotes and escape identifiers passed to relevant functions. It is useful if have to refer to object names with lower-cased letter or special characters. |
| connectionTimeout | Long | `10L` | Timeout for opening WebSocket connection to Exasol node in seconds |
| socketTimeout | Long | `30L` | Timeout for WebSocket reads and writes in seconds. Normally it should never be triggered due to PONG frames sent by Exasol server every second. |
| queryTimeout | Long | `0L` | Query timeout in seconds. 0 means "no timeout". |
| clientName | String | `Custom Client` | Custom client application name stored in Exasol system tables |
| clientVersion | String | `1.2.3` | Custom client application version stored in Exasol system tables |
| verboseException | Boolean | `true` | Add additional debug information for toString() call of exceptions derived from `ExaException` |
| loggerJsonMaxLength | Int | `20000` | Maximum length of JSON dumped into debug logs, helps to prevent unnecessary log bloating |

### connect()

Open new Exasol connection using provided connection options.

Return: [ExaConnection](#ExaConnection)

### streamWorker()

Initiate new stream worker in child process or thread using provided connection options.

`dsn`, `compression` and `encryption` should match the parent process.
`User` and `password` are not used by stream workers, so you may leave it empty.

| ARG | TYPE | EXAMPLE | DESCRIPTION |
| --- | --- | --- | --- |
| nodeAddress | `ExaNodeAddress` | `10.10.1.15` | Specific Exasol node address for StreamWorker to connect |

Return: [ExaStreamWorker](#ExaStreamWorker)

---

## ExaConnection

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| options | `ExaConnectionOptions` | Exasol connection options |

### execute()

Execute SQL query with optional [formatting](SQL_FORMATTING.md).

Optional snapshot execution mode prevents locks while querying system tables and views.
Learn more: https://www.exasol.com/support/browse/EXASOL-2646

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| query | String | SQL query with named and typed placeholders |
| queryParams | Map<String, Any?>? | Map of names and substitution values for SQL query placeholders |
| snapshotExecution | Boolean | Execute query in a snapshot mode |

Return: [ExaStatement](#ExaStatement)

### commit()

COMMIT transaction.

Return: [ExaStatement](#ExaStatement)

### rollback()

ROLLBACK transaction.

Return: [ExaStatement](#ExaStatement)

### abortQuery()

Abort query which is currently running by this connection.

This function does not block and returns immediately. It is supposed to be called from another thread.

There are three possible outcomes of this call:

1. Execution finishes successfully before the `abortQuery` takes effect;
2. Execution is aborted, [ExaQueryAbortException] will be thrown;
3. Execution is aborted, but Exasol server terminates the entire connection;

Please see example [08_abort_query.kt](/examples/08_abort_query.kt) for more details.

### streamImport()

IMPORT a large amount of data into table using raw CSV streaming.

Explicit list of column names is required to protect from unexpected changes in table structure.

Please see example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| table | String | Table name in the |
| columns | List<String> | List of ordered column names |
| operation | Function | Lambda which accepts `okio.BufferedSink` and writes CSV data into it |

Return: [ExaStatement](#ExaStatement)

### streamImportWriter()

IMPORT a large amount of data into table using [ExaStreamCSVWriter](#ExaStreamCSVWriter) wrapper.

Explicit list of column names is required to protect from unexpected changes in table structure.

Please see example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| table | String | Table name in the |
| columns | List<String> | List of ordered column names |
| operation | Function | Lambda which accepts [ExaStreamCSVWriter](#ExaStreamCSVWriter) and writes raw CSV data using it |

Return: [ExaStatement](#ExaStatement)

### streamImportIterator()

IMPORT data into table from Iterator emitting Lists of values or Maps of values.

This is a convenient substitution for basic "INSERT multiple rows" scenario.

Please see example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| table | String | Table name in the |
| columns | List<String> | List of ordered column names |
| iterator | `Iterator<List<Any?>>` or `Iterator<Map<String, Any?>>` | Iterator emitting Lists of values or Maps of values |

Return: [ExaStatement](#ExaStatement)

### streamExport()

EXPORT a large amount from table or SQL query using raw CSV streaming.

First line contains column names.

Please see example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| queryOrTable | String | Table name in the current schema or SQL query with named and typed placeholders |
| queryParams | `Map<String, Any?>?` | Map of names and substitution values for SQL query placeholders |
| columns | `List<String>` |  List of ordered column names |
| operation | Function | Lambda which accepts `okio.BufferedSource` and reads raw CSV data from it |

Return: [ExaStatement](#ExaStatement)

### streamExportReader()

EXPORT a large amount from table or SQL query using [ExaStreamCSVReader](#ExaStreamCSVReader) wrapper.

Please see example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| queryOrTable | String | Table name in the current schema or SQL query with named and typed placeholders |
| queryParams | `Map<String, Any?>?` | Map of names and substitution values for SQL query placeholders |
| columns | `List<String>` |  List of ordered column names |
| operation | Function | Lambda which accepts [ExaStreamCSVReader](#ExaStreamCSVReader) and reads CSV data using it |

Return: [ExaStatement](#ExaStatement)

### streamParallelNodes()

Return shuffled list of Exasol node addresses used to initiate workers for parallel CSV steaming.

Please see examples [10_stream_parallel_export.kt](/examples/10_stream_parallel_export.kt), [11_stream_parallel_import.kt](/examples/11_stream_parallel_import.kt) for more details.

If number of workers is larger than amount of Exasol nodes, addresses will be wrapped around and repeated.
If number of workers is omitted, return exactly one address per Exasol node.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| numberOfWorkers | Integer | Number of workers for parallel CSV streaming |

Return: `List<ExaNodeAddress>`

### streamParallelImport()

IMPORT a large amount of data in parallel using CSV streaming.

This function builds and executes IMPORT SQL query only.

The actual processing is done by workers (processes or threads), which should be initiated before calling this function.
List of `ExaStreamInternalAddress` should be obtained from workers and passed as an argument.

Please see example [11_stream_parallel_import.kt](/examples/11_stream_parallel_import.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| internalAddressList | `List<ExaStreamInternalAddress>` | List of Exasol internal IP addresses obtained from workers |
| table | String | Table name in the current schema |
| columns | `List<String>` | List of ordered column names |

Return: [ExaStatement](#ExaStatement)

### streamParallelExport()

EXPORT a large amount of data in parallel using CSV streaming.

This function builds and executes EXPORT SQL query only.

The actual processing is done by workers (processes or threads), which should be initiated before calling this function.
List of `ExaStreamInternalAddress` should be obtained from workers and passed as an argument.

Please see example [10_stream_parallel_export.kt](/examples/10_stream_parallel_export.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| internalAddressList | `List<ExaStreamInternalAddress>` | List of Exasol internal IP addresses obtained from workers |
| queryOrTable | String | Table name in the current schema or SQL query with named and typed placeholders |
| queryParams | `Map<String, Any?>?` | Map of names and substitution values for SQL query placeholders |
| columns | `List<String>` | List of ordered column names |

Return: [ExaStatement](#ExaStatement)

### setAutocommit()

Autocommit mode TRUE means all SQL queries will be committed implicitly. With mode FALSE you can run multiple SQL queries in one transaction and call [commit](#commit) or [rollback](#rollback) functions explicitly to finish transaction.

Please see example [05_transaction.kt](/examples/05_transaction.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| value | Boolean | Enable or disable autocommit |

### setQueryTimeout()

Exasol query timeout in seconds, managed by Exasol server.
Queries terminated by timeout will throw `ExaQueryTimeoutException`.

Please see example [08_abort_query.kt](/examples/08_abort_query.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| value | Long | Timeout in seconds, "0" means no timeout |

### openSchema()

Change currently opened schema. Please note: [rollback](#rollback) call may revert current schema to original value.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| value | String | Schema name to open |

### setAttributes()

Set multiple connection attributes in one call.
Full list of possible attributes: https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV2.md#attributes-session-and-database-properties

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| value | `Map<String, Any>` | Map of new values for attributes |

### close()

Close() should always be called on connection objects.

If you forget to do so, connection on Exasol server side will not be terminated until garbage collector cleans up the WebSocket client.

Also, `LOGOUT_TIME` in system tables will be missing or inconsistent.

### .loginInfo

Response data from successful login command, does not change.
https://github.com/exasol/websocket-api/blob/master/docs/commands/loginV1.md

Return: `Map<String, Any>`

### .attributes

Connection attributes, may change after any command.
https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV2.md#attributes-session-and-database-properties

Return: `Map<String, Any>`

### .sessionId

SESSION_ID of current connection. Can be empty if connection was not established.

Return: String

### .protocolVersion

Actual protocol version of current connection.
Learn more about protocol versions: https://github.com/exasol/websocket-api

Return: Int

### .currentSchema

Currently opened schema, can be empty if schema was not opened. It can be changed dynamically by calling [openSchema](#openschema).

Return: String

---

## ExaStatement

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| connection | `ExaConnection` | Exasol connection object |
| query | String | SQL query with named and typed placeholders |
| queryParams | `Map<String, Any?>?` | Map of names and substitution values for SQL query placeholders |
| snapshotExecution | Boolean | Execute query in a snapshot mode |

### ExaStatement.forEach()

The preferred way of iteration for statement data sets is a standard `.forEach {}` block. You can process a data set of any size using this method.

Example:

```
val st = exa.execute("SELECT user_name, created FROM exa_all_users LIMIT 5")

st.forEach { row ->
    println(row)
}
```

### ExaStatement.asSequence()

It is possible to wrap data set iteration into Kotlin Sequence. Data will be fetched in a lazy manner.

Example:

```
val st = exa.execute("SELECT user_name, created FROM exa_all_users LIMIT 5")

val list = st.asSequence().filter { it.getString(0).length > 5 }.take(3).toList()
```

### ExaStatement.onlyRow()

Return single `ExaStatementRow` row for data sets with exactly one row.

It helps to avoid unnecessary iteration.

Return: `ExaStatementRow`

### ExaStatement.findColumn()

Find column index by column name. Column names in Exasol are normally upper cased.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| columnName | String | Name of the column |

Return: Int

### ExaStatement.close()

Release data set handle on server side.

Statements are closed automatically once the iteration is finished.

You do not need to call close() for statements explicitly in the most of cases.

### .columns

Ordered List of result set columns.

Column data types and properties are described here: Data types and properties: https://github.com/exasol/websocket-api/blob/master/docs/WebsocketAPIV1.md#data-types-type-names-and-properties

Return: `List<ExaStatementColumn>`

### .columnNamesMap

Map of result set column names, useful for efficient [findColumn](#exastatementfindcolumn)-like calls.

Return: `Map<String,Int>`

### .rowCount

Number of rows selected or affected by SQL query.

Return: Long

### .formattedQuery

The final SQL query text which was sent to Exasol for execution after formatting.

Return: String

### .executionTimeMillis

Execution time of SQL query in milliseconds, measured by WebSocket response time.

Return: Long

---

## ExaStreamCSVReader

Convenient wrapper for reading from CSV BufferedSource initiated during streaming EXPORT.

Iterate over CSV rows and output as `ExaStreamExportRow` objects.

Please check example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| dataPipeSource | `okio.BufferedSource` | BufferedSource with CSV data |

### ExaStreamCSVReader.forEach

The preferred way of iteration for CSV streaming is a standard `.forEach {}` block. You can process a data set of any size using this method.

Example:

```
exa.streamExportReader("SELECT * FROM users ORDER BY user_id LIMIT 3") { reader ->
    reader.forEach { row ->
        println(row)
    }
}
```

### ExaStreamCSVReader.asSequence

It is possible to wrap data set iteration into Kotlin Sequence. Data will be fetched in a lazy manner.

### ExaStreamCSVReader.close

Reader must be fully exhausted and closed.

---

## ExaStreamCSVWriter

Convenient wrapper for writing into CSV BufferedSink initiated during streaming IMPORT.

Please check example [02_stream.kt](/examples/02_stream.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| columns | `List<String>` | List of ordered column names
| dataPipeSink | `okio.BufferedSink` | BufferedSink for CSV data |

### ExaStreamCSVWriter.writeRow()

Write CSV row constructed by individual set*() calls.

For example:
```
writer.setLong(0, 123L)
writer.setString(1, "ABC")
writer.writeRow()
```

Alternatively, write an entire row from List or Map.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| list | `List<Any?>` | Row to write in a form of List |

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| map | `Map<String,Any?>` | Row to write in a form or Map |

### ExaStreamCSVWriter.close

Writer must be closed explicitly when writing was finished.

---

## ExaStreamWorker

This class is supposed to be instantiated in child processes during parallel CSV streaming.

1. Open "proxy" connection to Exasol using `ExaNodeAddress` obtained from [streamParallelNodes](#streamparallelnodes).
2. Get `internalAddress` from Exasol server and make it available for [streamParallelImport](#streamparallelimport) or [streamParallelExport](#streamparallelexport) function.
3. Call `stream*` function of worker to wait for incoming HTTP request from Exasol and do the actual work by reading or writing data.
 
| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| options | `ExaConnectionOptions` | Exasol connection options (user and password can be omitted) |
| nodeAddress | `ExaNodeAddress` | Exasol node addressed obtained from streamParallelNodes |

### ExaStreamWorker.streamImport()

IMPORT a large amount of data from child process using raw CSV streaming.

Please see example [11_stream_parallel_import.kt](/examples/11_stream_parallel_import.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| operation | Function | Lambda which accepts `okio.BufferedSink` and writes raw CSV data into it |


### ExaStreamWorker.streamImportWriter()

IMPORT a large amount of data from child process using [ExaStreamCSVWriter](#ExaStreamCSVWriter) wrapper.

Please see example [11_stream_parallel_import.kt](/examples/11_stream_parallel_import.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| operation | Function | Lambda which accepts [ExaStreamCSVWriter](#ExaStreamCSVWriter) and writes data using it |


### ExaStreamWorker.streamExport()

EXPORT a large amount of data into child process using raw CSV streaming.

First line contains column names.

Please see example [10_stream_parallel_export.kt](/examples/10_stream_parallel_export.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| operation | Function | Lambda which accepts `okio.BufferedSource` and reads raw CSV data from it |


### ExaStreamWorker.streamExportReader()

EXPORT a large amount of data into child process using [ExaStreamCSVReader](#ExaStreamCSVReader) wrapper.

Please see example [10_stream_parallel_export.kt](/examples/10_stream_parallel_export.kt) for more details.

| ARG | TYPE | DESCRIPTION |
| --- | --- | --- |
| operation | Function | Lambda which accepts [ExaStreamCSVReader](#ExaStreamCSVReader) and reads data from it |

### ExaStreamWorker.internalAddress

Internal IPv4 address from Exasol private network.
Get these addresses from all child processes and use it for [streamParallelImport](#streamparallelimport) or [streamParallelExport](#streamparallelexport) call.

Return: `ExaStreamInternalAddress`
