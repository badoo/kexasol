[![Build Status](https://travis-ci.com/badoo/kexasol.svg?branch=master)](https://travis-ci.com/badoo/kexasol)
[![Release](https://jitpack.io/v/badoo/kexasol.svg)](https://jitpack.io/#badoo/kexasol)

KExasol is a custom database driver for [Exasol](https://www.exasol.com) implemented in Kotlin (JVM).

It offers unique features compared to standard JDBC driver:

- based on [native WebSocket API](https://github.com/exasol/websocket-api);
- takes advantage of modern I/O libraries: [Okio](https://github.com/square/okio), [OkHttp](https://github.com/square/okhttp), [Moshi](https://github.com/square/moshi);
- efficient data transport via CSV streaming;
- read and write data in parallel processes with linear scaling;
- network traffic compression;
- rich SQL query formatting with named and typed placeholders;

KExasol encourages best practices that are specific for Exasol distributed architecture and massively parallel data processing. It will help you to fully leverage capabilities of this analytical DBMS.

## Quick links
- [Getting started](#getting-started)
- [Reference](REFERENCE.md)
- [Examples](EXAMPLES.md)
- [Data types](DATA_TYPES.md)
- [Best practices](BEST_PRACTICES.md)
- [SQL formatting](SQL_FORMATTING.md)
- [Changelog](CHANGELOG.md)

## System requirements

- Exasol >= 6.0
- Kotlin >= 1.4
- Java >= 8

## Getting started

Add JitPack Maven repository to `build.gradle.kts`:

```
repositories {
    ...
    maven { setUrl("https://jitpack.io") }
}
```

Add dependency:

```
implementation("com.github.badoo:kexasol:0.2.1")
```

---

Run basic query:

```kotlin
val exa = ExaConnectionOptions(dsn = "<host:port>", user = "sys", password = "exasol").connect()

exa.use {
    val st = exa.execute("SELECT user_name, created FROM exa_all_users LIMIT 5")

    st.forEach { row ->
        println(row)
    }
}
```

Read data via CSV streaming:

```kotlin
val exa = ExaConnectionOptions(dsn = "<host:port>", user = "sys", password = "exasol").connect()

exa.use {
    exa.streamExportReader("EXA_ALL_USERS", columns = listOf("USER_NAME", "CREATED")) { reader ->
        reader.forEach { row ->
            println(row.getString("USER_NAME"))
            println(row.getLocalDateTime("CREATED"))
        }
    }
}
```

Create a new table and import some data in transaction:

```kotlin
val exa = ExaConnectionOptions(dsn = "<host:port>", user = "sys", password = "exasol").connect()

val cols = listOf("USER_ID", "USER_NAME", "IS_ACTIVE")

val data = listOf<List<Any?>>(
    listOf(1L, "Alice", true),
    listOf(2L, "Bob", false),
    listOf(3L, "Cindy", null),
)

exa.use {
    exa.setAutocommit(false)

    exa.execute("CREATE SCHEMA IF NOT EXISTS kexasol_test")

    exa.execute("""
        CREATE OR REPLACE TABLE test_table
        (
            user_id       DECIMAL(9,0),
            user_name     VARCHAR(255),
            is_active     BOOLEAN
        )
    """)

    val importSt = exa.streamImportIterator("test_table", cols, data.iterator())
    println("IMPORT affected rows: ${importSt.rowCount}")

    exa.commit()
}
```

Check other [examples](EXAMPLES.md) and [best practices](BEST_PRACTICES.md).

## Created by
Vitaly Markov, 2020

Enjoy!

## License

    Copyright 2020 Badoo

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.