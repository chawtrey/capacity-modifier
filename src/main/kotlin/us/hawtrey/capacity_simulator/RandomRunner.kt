@file:Suppress("DuplicatedCode")

package us.hawtrey.capacity_simulator

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.random.Random


//fun main(args: Array<String>) {
//    val accessor = Accessor()
//    try {
//        RandomRunner(accessor).run()
//    } finally {
//        accessor.close()
//    }
//}

class RandomRunner(private val accessor: Accessor) {
    private val startDay: LocalDate = LocalDate.parse("2022-01-01")
    private val startDayYM: YearMonth = YearMonth.from(startDay)

    private val agents = listOf(
        Agent("a-95", 0.95, 1000),
        Agent("a-94", 0.94, 800),
        Agent("a-90", 0.90, 110),
        Agent("a-89", 0.89, 98),
        Agent("a-87", 0.87, 415),
        Agent("a-84", 0.84, 120),
        Agent("a-81", 0.81, 500),
        Agent("a-75", 0.75, 700),
        Agent("a-71", 0.71, 820),
        Agent("a-68", 0.68, 100),
        Agent("a-67", 0.67, 60),
        Agent("a-65", 0.65, 60)
    )

//    Simulation variables

    private val yearsToRun = 5L
    private val backFillMonths = 12L
    private val minMonthlyLeads = 1
    private val capPercent = 0.15

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d")

    fun run() {
        accessor.update("create table if not exists random_ops (id serial primary key, name varchar(80), date date not null)")
        accessor.update("truncate table random_ops")
        backFill()
        runSimulation()
    }

    private fun runSimulation() {
        val endDay = startDay.plusYears(yearsToRun)
        val dayCount = startDay.until(endDay, ChronoUnit.DAYS)
        println("Simulation date range: $startDay to $endDay - total days: $dayCount")

        val leadsPerYear = ((agents.sumOf { it.leadsPerYear }).toDouble() / 3.0).roundToInt()
        var leadsPerDay = 0

        for (i in 0..dayCount) {
            val today = LocalDate.from(startDay).plusDays(i)
            if (today.dayOfMonth == 1) {
                leadsPerDay = leadsPerDay(leadsPerYear, today)
                println(today)
            }

            for (j in 1..leadsPerDay) {
                val rankedAgents = rankAgents(today)
                val inserts = rankedAgents.map { buildInsert(it, today) }
                accessor.update(inserts)
            }
        }
    }

    private fun rankAgents(today: LocalDate): List<String> {
        val oppCounts = allLeadCountsForRange(today.minusYears(1), today)
        val totalOpps = oppCounts.values.sum()
        val oppProbabilities =
            oppCounts.mapValues { (_, v) -> ((v.toDouble() / totalOpps) * 100).roundToInt() }
                .toSortedMap()

        var maxRandom = oppProbabilities.values.sum()
        val topAgents = mutableListOf<String>()
        for (i in 1..3) {
            var foo = Random.nextInt(1, maxRandom)
            var name: String? = null
            for (probability in oppProbabilities) {
                foo -= probability.value
                if (foo < 0) {
                    name = probability.key
                    maxRandom -= probability.value
                    break
                }
            }
            if (name != null) {
                topAgents.add(name)
                oppProbabilities.remove(name)
            }
        }
        return topAgents
    }

    private fun allLeadCountsForRange(startDate: LocalDate, endDate: LocalDate): Map<String, Int> {
        return accessor.allCounts("select name, count(*) as cnt from random_ops where date between '$startDate' and '$endDate' group by name")
    }

    private fun backFill() {
        for (idx in backFillMonths downTo 1) {
            val ym = startDayYM.minusMonths(idx)
            for (agent in agents) {
                val count = leadsPerMonth(agent.leadsPerYear, ym)
                val inserts = mutableListOf<String>()
                for (jdx in 1..count) {
                    val day = Random.nextInt(1, ym.lengthOfMonth())
                    val date = LocalDate.parse("$ym-$day", formatter)
                    inserts.add(buildInsert(agent.name, date))
                }
                accessor.update(inserts)
            }
        }
    }

    private fun leadsPerDay(leadsPerYear: Int, localDate: LocalDate): Int {
        val count =
            (leadsPerMonth(
                leadsPerYear,
                localDate
            ).toDouble() / localDate.lengthOfMonth()).roundToInt()
        return if (count <= 0) 1 else count
    }

    private fun leadsPerMonth(leadsPerYear: Int, localDate: LocalDate) =
        leadsPerMonth(leadsPerYear, YearMonth.from(localDate))

    private fun leadsPerMonth(leadsPerYear: Int, yearMonth: YearMonth) =
        (leadsPerYear.toDouble() * distro[yearMonth.month]!!).roundToInt()

    private fun buildInsert(agent: String, date: LocalDate?) =
        "insert into random_ops(name, date) values ('${agent}', '${date}')"

    private val distro = mapOf(
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
}
