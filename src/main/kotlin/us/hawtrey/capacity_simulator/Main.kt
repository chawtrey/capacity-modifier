package us.hawtrey.capacity_simulator

import java.sql.Connection
import java.sql.DriverManager
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import us.hawtrey.capacity_simulator.jooq.tables.OppRecords.Companion.OPP_RECORDS


fun main(args: Array<String>) {
    val url = "jdbc:postgresql://localhost:5432/capacity"
    val userName = "test"
    val password = ""

    Class.forName("org.postgresql.Driver")
    val connection = DriverManager.getConnection(url, userName, password)
    try {
        runYoyRunner(connection)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection.close()
    }
}

private fun runYoyRunner(conn: Connection?) {
    val context = DSL.using(conn, SQLDialect.POSTGRES)
    context.truncate(OPP_RECORDS).execute()
    val runner = YoyRunner(context)
    runner.run()
}
