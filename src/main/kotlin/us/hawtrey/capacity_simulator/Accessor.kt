package us.hawtrey.capacity_simulator

import java.sql.Connection
import java.sql.DriverManager

class Accessor(private val connection: Connection) {

//    init {
//        Class.forName("org.h2.Driver")
//        connection = DriverManager.getConnection("jdbc:h2:~/connection_simulator_h2db", "sa", "")
//        Class.forName("org.postgresql.Driver")
//        connection =
//            DriverManager.getConnection("jdbc:postgresql://localhost:5432/capacity", "test", "")
//    }

    fun update(sqlStatement: String) = update(listOf(sqlStatement))

    fun update(sqlStatements: List<String>) {
        val statement = connection.createStatement()
        try {
            sqlStatements.forEach {
                statement.executeUpdate(it)
            }
        } finally {
            statement.close()
        }
    }

    fun count(sqlStatement: String): Int {
        val statement = connection.createStatement()
        try {
            val resultSet = statement.executeQuery(sqlStatement)
            while (resultSet.next()) {
                return resultSet.getInt("cnt")
            }
        } finally {
            statement.close()
        }
        return 0
    }

    fun allCounts(sqlStatement: String): Map<String, Int> {
        val statement = connection.createStatement()
        try {
            val resultSet = statement.executeQuery(sqlStatement)
            val resultMap = mutableMapOf<String, Int>()
            while (resultSet.next()) {
                resultMap[resultSet.getString("name")] = resultSet.getInt("cnt")
            }
            return resultMap
        } finally {
            statement.close()
        }
        return mapOf()
    }
}
