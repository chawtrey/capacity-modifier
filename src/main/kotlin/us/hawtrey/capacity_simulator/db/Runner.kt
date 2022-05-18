package us.hawtrey.capacity_simulator.db

import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.random.Random


fun main(args: Array<String>) {
    val accessor = Accessor()
    try {
        Runner(accessor).run()
    } finally {
        accessor.close()
    }
}

class Runner(private val accessor: Accessor) {
    private val startDay: LocalDate = LocalDate.parse("2022-01-01")
    private val startDayYM: YearMonth = YearMonth.from(startDay)

//    private val agents = listOf(
//        Agent("Matthew", 0.88, 450),
//        Agent("Mark", 0.77, 400),
//        Agent("Luke", 0.76, 480),
//        Agent("John", 0.65, 500),
//        Agent("Mary", 0.64, 510),
//        Agent("Paul", 0.53, 300)
//    )
    private val agents = listOf(
        Agent("a01-95", 0.95, 100),
        Agent("a02-94", 0.94, 100),
        Agent("a03-90", 0.90, 100),
        Agent("a04-89", 0.89, 100),
        Agent("a05-87", 0.87, 100),
        Agent("a06-84", 0.84, 100),
        Agent("a07-81", 0.81, 100),
        Agent("a08-75", 0.75, 100),
        Agent("a09-71", 0.71, 100),
        Agent("a10-68", 0.68, 100),
        Agent("a11-67", 0.67, 100),
        Agent("a12-65", 0.65, 100)
    )

//    Simulation variables

    private val yearsToRun = 5L
    private val backFillYears = 1L
    private val minMonthlyLeads = 1
    private val capPercent = 0.15

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d")

    fun run() {
        accessor.update("create table if not exists opp_records (id serial primary key, name varchar(80), date date not null)")
        accessor.update("truncate table opp_records")
        backFill()
        runSimulation()
    }

    private fun runSimulation() {
        val endDay = startDay.plusYears(yearsToRun)
        val dayCount = startDay.until(endDay, ChronoUnit.DAYS)
        println("Simulation date range: $startDay to $endDay - total days: $dayCount")

        val totalLeadsPerYear = agents.sumOf { it.leadsPerYear } / 3
        println("total leads per year: $totalLeadsPerYear")

        val firstDay = startDay.dayOfMonth
        var leadsPerDay = 1

        for (i in 0..dayCount) {
            val today = LocalDate.from(startDay).plusDays(i)
            if (today.dayOfMonth == firstDay) {
                leadsPerDay = leadsPerDay(totalLeadsPerYear, today)
                println("[$today] leads per day: $leadsPerDay")
            }

            for (j in 1..leadsPerDay) {
                val rankedAgents =
                    agents.map { modifyAgentScore(it, today) }.sortedByDescending { it.score }
                val inserts =
                    rankedAgents.filterIndexed { idx, _ -> idx < 3  }.map { buildInsert(it, today) }
                accessor.update(inserts)
            }
        }
    }

    private fun modifyAgentScore(agent: Agent, today: LocalDate): Agent {
        val currentCount = leadCountForRange(agent, today.withDayOfMonth(1), today)
        val expectedCount = calculateExpectedCount(today, agent)
        val percentThru = today.dayOfMonth.toDouble() / today.lengthOfMonth().toDouble()
        val relativeCount = (percentThru * expectedCount).toInt()

        val multiplier = calculateSimpleMultiplier(currentCount, relativeCount)
        val score = agent.score * multiplier
        if (today.dayOfMonth == 15)
            println("agent = ${agent.name} - currentCount = $currentCount - expectedCount = $expectedCount - relativeCount = $relativeCount - multiplier = $multiplier - score = ${agent.score} * $multiplier = $score")
        return Agent(agent.name, score)
    }

    private fun calculateSimpleMultiplier(currentCount: Int, relativeCount: Int): Double {
        return when {
            currentCount < minMonthlyLeads  -> 100.0
//            currentCount < relativeCount -> 1.0
//            currentCount < relativeCount -> calculateArcTanMultiplier(currentCount, relativeCount)
//            currentCount <= relativeCount -> 1.0 / (currentCount.toDouble() / relativeCount.toDouble())
//            currentCount > (relativeCount * 2) -> 0.1
//            else -> 0.5
            else -> calculateStraightMultiplier(currentCount, relativeCount)
//            else -> calculateArcTanMultiplier(currentCount, relativeCount)
        }
    }
    private fun calculateStraightMultiplier(currentCount: Int, relativeCount: Int): Double {
        val xPrime = currentCount.toDouble() / relativeCount.toDouble()
        val x = when {
            xPrime > 2 -> 2.0
            xPrime < 0 -> 0.0
            else -> xPrime
        }
        return -0.9 * (x - 1.0) + 1.0
    }

    private fun calculateArcTanMultiplier(currentCount: Int, relativeCount: Int): Double {
        val xPrime = currentCount.toDouble() / relativeCount.toDouble()
        val x = when {
            xPrime > 2 -> 2.0
            xPrime < 0 -> 0.0
            else -> xPrime
        }
        return -0.4 * atan((x - 1.0)) + 1.0
    }

    private fun calculateExpectedCount(today: LocalDate, agent: Agent): Double {
        val trailingDay = today.minusMonths(1)
        val trailingCount = leadCountForRange(
            agent,
            trailingDay.withDayOfMonth(1),
            trailingDay.withDayOfMonth(trailingDay.lengthOfMonth())
        )

        return trailingCount.toDouble()
//        val yoyDay = today.minusYears(1)
//        val yoyCurrentCount = leadCountForRange(
//            agent, yoyDay.withDayOfMonth(1), yoyDay.withDayOfMonth(yoyDay.lengthOfMonth())
//        )
//        val yoyTrailingDay = yoyDay.minusMonths(1)
//        val yoyTrailingCount = leadCountForRange(
//            agent,
//            yoyTrailingDay.withDayOfMonth(1),
//            yoyTrailingDay.withDayOfMonth(yoyTrailingDay.lengthOfMonth()),
//        )

//        val percentageChange = trailingCount.toDouble() / yoyTrailingCount.toDouble()

//        return percentageChange * yoyCurrentCount.toDouble()
    }

    private fun leadCountForRange(agent: Agent, startDate: LocalDate, endDate: LocalDate): Int {
        return accessor.count("select count(*) as cnt from opp_records where name ='${agent.name}' and date between '$startDate' and '$endDate'")
    }

    private fun backFill() {
        val backFillMonths = backFillYears * 12
        for (idx in backFillMonths downTo 1) {
            val ym = startDayYM.minusMonths(idx)
            for (agent in agents) {
                val count = leadsPerMonth(agent.leadsPerYear, ym)
                val inserts = mutableListOf<String>()
                for (jdx in 1..count) {
                    val day = Random.nextInt(1, ym.lengthOfMonth())
                    val date = LocalDate.parse("$ym-$day", formatter)
                    inserts.add(buildInsert(agent, date))
                }
                accessor.update(inserts)
            }
        }
    }
}

private fun leadsPerDay(leadsPerYear: Int, localDate: LocalDate): Int {
    val count =
        (leadsPerMonth(leadsPerYear, localDate).toDouble() / localDate.lengthOfMonth()).roundToInt()
    return if (count <= 0) 1 else count
}

private fun leadsPerMonth(leadsPerYear: Int, localDate: LocalDate) =
    leadsPerMonth(leadsPerYear, YearMonth.from(localDate))

private fun leadsPerMonth(leadsPerYear: Int, yearMonth: YearMonth) =
    (leadsPerYear.toDouble() * distro[yearMonth.month]!!).roundToInt()

private fun buildInsert(agent: Agent, date: LocalDate?) =
    "insert into opp_records(name, date) values ('${agent.name}', '${date}')"

class Accessor {
    private val connection: Connection

    init {
//        Class.forName("org.h2.Driver")
//        connection = DriverManager.getConnection("jdbc:h2:~/connection_simulator_h2db", "sa", "")
        Class.forName("org.postgresql.Driver")
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/capacity", "test", "")
    }

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

    fun close() {
        connection.close()
    }
}

data class Agent(val name: String, var score: Double, val leadsPerYear: Int = 0)

val distro = mapOf(
    Month.JANUARY to 0.07,
    Month.FEBRUARY to 0.07,
    Month.MARCH to 0.08,
    Month.APRIL to 0.08,
    Month.MAY to 0.09,
    Month.JUNE to 0.1,
    Month.JULY to 0.1,
    Month.AUGUST to 0.1,
    Month.SEPTEMBER to 0.08,
    Month.OCTOBER to 0.08,
    Month.NOVEMBER to 0.08,
    Month.DECEMBER to 0.07
)


