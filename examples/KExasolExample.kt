import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.slf4j.LoggerFactory

abstract class KExasolExample {
    val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val credentials: KExasolTestCredentials

    init {
        logger.level = Level.INFO

        credentials = KExasolTestCredentials(
            dsn = System.getenv("EXAHOST") ?: "localhost",
            user = System.getenv("EXAUID") ?: "SYS",
            password = System.getenv("EXAPWD") ?: "exasol",
            schema = System.getenv("EXASCHEMA") ?: "KEXASOL_TEST"
        )
    }

    fun enableDebugLog() {
        logger.level = Level.DEBUG
    }

    @BeforeEach
    fun displayExampleName(info: TestInfo) {
        println("----- Starting: ${info.displayName} -----")
    }

    @AfterEach
    fun addNewlineAfterExample() {
        println("")
    }

    data class KExasolTestCredentials(
        val dsn: String,
        val user: String,
        val password: String,
        val schema: String
    )
}
