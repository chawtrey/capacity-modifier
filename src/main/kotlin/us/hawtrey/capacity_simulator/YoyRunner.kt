@file:Suppress("DuplicatedCode")

package us.hawtrey.capacity_simulator

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import us.hawtrey.capacity_simulator.jooq.tables.OppRecords.Companion.OPP_RECORDS


class YoyRunner(private val context: DSLContext) {
    private val startDay: LocalDate = LocalDate.parse("2022-01-01")

    private val agents = listOf(
        Agent(0.95, 136),
        Agent(0.90, 136),
        Agent(0.85, 136),
        Agent(0.82, 136),
        Agent(0.80, 136),
        Agent(0.75, 136),
        Agent(0.70, 136),
        Agent(0.65, 136),
        Agent(0.0, 0)
    )

    private val yearsToRun = 7L

    fun run() {
        backFill()
        runSimulation()
    }

    private fun runSimulation() {
        val endDay = startDay.plusYears(yearsToRun)
        val dayCount = startDay.until(endDay, ChronoUnit.DAYS) - 1
        println("Simulation date range: $startDay to $endDay - total days: $dayCount")

        for (i in 0..dayCount) {
            val today = startDay.plusDays(i)

            val rankedAgents =
                agents.map { modifyAgentScore(it, today) }.sortedByDescending { it.score }

            var insert = context.insertInto(OPP_RECORDS, OPP_RECORDS.NAME, OPP_RECORDS.DATE)
            for (agent in rankedAgents.subList(0, 3)) {
                insert = insert.values(agent.name, today)
            }
            insert.execute()
        }
    }

    private fun modifyAgentScore(agent: Agent, today: LocalDate): Agent {
        val currentCount = leadCountForRange(agent, today.withDayOfYear(1), today)
        val expectedCount = calculateExpectedCount(today, agent)
        val percentThru = today.dayOfYear.toDouble() / today.lengthOfYear().toDouble()
        val relativeCount = (percentThru * expectedCount).roundToInt()

        val multiplier = calculateSimpleMultiplier(currentCount, relativeCount)
        val score = calculateScore(agent.score, multiplier)
        if (today.dayOfMonth == 15)
            println("agent = ${agent.name} - currentCount = $currentCount - expectedCount = $expectedCount - relativeCount = $relativeCount - multiplier = $multiplier - score = ${agent.score} * $multiplier = $score")
        return Agent(agent.name, score)
    }

    private fun calculateScore(rawScore: Double, multiplier: Double): Double {
        val xScore = if (rawScore == 0.0) 0.82 else rawScore
        return xScore * multiplier
    }

    private fun calculateSimpleMultiplier(currentCount: Int, relativeCount: Int): Double =
        if (currentCount == 0) 100.0
        else calculateStraightMultiplier(currentCount, relativeCount)

    private fun calculateStraightMultiplier(currentCount: Int, relativeCount: Int): Double {
        val xPrime = currentCount.toDouble() / relativeCount.toDouble()
        val x = when {
            xPrime > 2 -> 2.0
            xPrime < 0 -> 0.0
            else -> xPrime
        }
        return -1 * (x - 1.0) + 0.5
    }

    private fun calculateExpectedCount(today: LocalDate, agent: Agent): Double {
        val trailingDay = today.minusYears(1)
        val trailingCount = leadCountForRange(
            agent,
            trailingDay.withDayOfYear(1),
            trailingDay.withDayOfYear(trailingDay.lengthOfYear())
        )

        return if (trailingCount < 60) 60.0 else trailingCount.toDouble()
    }

    private fun leadCountForRange(agent: Agent, startDate: LocalDate, endDate: LocalDate): Int {
        val result = context
            .select(count())
            .from(OPP_RECORDS)
            .where(OPP_RECORDS.NAME.eq(agent.name))
            .and(OPP_RECORDS.DATE.between(startDate, endDate))
            .fetch()
        return result[0].value1()
    }

    private fun backFill() {
        var insert = context.insertInto(OPP_RECORDS, OPP_RECORDS.NAME, OPP_RECORDS.DATE)
        for (agent in agents) {
            for (jdx in 1..agent.leadsPerYear) {
                val day = startDay.minusDays(jdx.toLong())
                insert = insert.values(agent.name, day)
            }
        }
        insert.execute()
    }
}
