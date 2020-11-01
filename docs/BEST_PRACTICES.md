# Best practices

This page explains how to use KExasol with maximum efficiency.

## Keep autocommit enabled by default

Exasol can write indexes and flush statistics even after basic SELECT queries. If autocommit is disabled, indexes will not be preserved. Every subsequent SELECT will cause the index recalculation once again, potentially consuming a lot of resources.

It is generally advised to have autocommit being enabled in all cases, unless you really need to run multiple statements in on transaction.

## Close connections properly

KExasol connections should always be closed when no longer needed.

It is advised to take advantage of Kotlin [`.use{}`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html) function to call close automatically in case of exception.

## Consider enabling network traffic compression

If network connection is a bottleneck, you may set option `compression=true` to enable zlib-compression. It applies both to normal data fetching and to CSV streaming.

In some cases it may improve overall performance by a factor of 4-8x.

## Use CSV streaming for large volumes of data

Normal fetching protocol has an extra overhead of JSON de-serialisation and reading data in small batches. It is OK to fetch relatively small data sets up to 1 million of records using normal fetching.

For larger datasets it is recommended to use CSV streaming exclusively. Overhead of this protocol is much smaller. Also, you can run CSV streaming in multiple processes running in parallel to achieve linear scalability.

## Avoid using INSERT statements with individual values

INSERT statements do not scale very well. Also, prepared statements have a number of technical issues and are not supported on purpose.

It is advised to use [streamImportIterator](REFERENCE.md#streamimportiterator) instead. It provides convenient interface which is similar to INSERT, but wrapped into efficient CSV streaming and IMPORT command instead.

## Do not share connection object by multiple concurrent threads

Exasol connection can only execute one query at a time. If you try to execute another query in another thread, it will be blocked until the first query is finished.

It is generally advised to open fewer connections, but use a basic SQL query queueing system instead.

Also, opening more connections and running more queries on the same table will NOT improve performance. You should consider running one big query and reading from it in parallel using CSV streaming instead of running a lot of small queries.

## Use snapshot execution mode for meta data queries

In order to reduce the amount of locks, you may run SQL queries accessing Exasol system views in snapshot isolation mode.

Just set the `snapshotExecution=true` while calling `execute()` function to enable this feature.

Please note, it only works for system views. You should not use it for normal tables.

Learn more: https://www.exasol.com/support/browse/EXASOL-2646