
abstract class KExasolExample {
    val credentials: KExasolTestCredentials

    init {
        credentials = KExasolTestCredentials(
            dsn = System.getenv("EXAHOST") ?: "localhost",
            user = System.getenv("EXAUID") ?: "SYS",
            password = System.getenv("EXAPWD") ?: "exasol",
            schema = System.getenv("EXASCHEMA") ?: "KEXASOL_TEST"
        )
    }

    data class KExasolTestCredentials(
        val dsn: String,
        val user: String,
        val password: String,
        val schema: String
    )
}
