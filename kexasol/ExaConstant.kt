package com.badoo.kexasol


object ExaConstant {
    const val DRIVER_NAME: String = "KExasol"
    const val DRIVER_VERSION: String = "0.1.0"

    const val DEFAULT_CONNECTION_TIMEOUT: Long = 10
    const val DEFAULT_SOCKET_TIMEOUT: Long = 30
    const val DEFAULT_LOGGER_JSON_MAX_LENGTH: Int = 20000

    const val DEFAULT_EXASOL_PORT: Int = 8563
    const val DEFAULT_FETCH_SIZE: Long = 5 * 1024 * 1024

    const val SNAPSHOT_EXECUTION_PREFIX: String = "/*snapshot execution*/"
}
