package us.hawtrey.capacity_simulator

import kotlin.random.Random

@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String>) {
    CapacitySimulator().runSim()
}

class CapacitySimulator {
    private val oneYear = 365
    private val startMon = Month.JAN
    private val backFillCnt = oneYear * 2 - 1 // subtract 1 to make it 0 based

    fun runSim(
        range: Int = 30,
        rangePercent: Double = 0.2,
        yearsToRun: Int = 10,
        shouldAddThreshold: Double = 1.0
    ) {
        val runCnt = oneYear * yearsToRun - 1

        val midRange = range / 2

        val leads = backFillLeadData()

        for (idx in 0..runCnt) {
            val today = leads.size
            val currentCount = leads.subList(today - midRange, today).sum()
            val trailingCount =
                leads.subList(today - midRange - range - 1, today - midRange - 1).sum()
            val yoyCurrentCount =
                leads.subList(today - midRange - oneYear, today + midRange - oneYear).sum()
            val yoyTrailingCount = leads.subList(
                today - midRange - range - 1 - oneYear, today - midRange - 1 - oneYear
            ).sum()

            val expectedCount =
                (trailingCount.toDouble() / yoyTrailingCount.toDouble()) * yoyCurrentCount
            val expectedCountFloor = expectedCount - (expectedCount * rangePercent)

            val capacityMultiplier = expectedCountFloor / (2 * currentCount)

            val shouldAdd = capacityMultiplier >= shouldAddThreshold
            if (shouldAdd) leads.add(1) else leads.add(0)
        }

        printLeads(leads)
    }

    private fun backFillLeadData(): MutableList<Int> {
        var currentMon = startMon
        var currentMonIdx = startMon.idx
        var currentDay = 0

        val pastLeads = mutableListOf<Int>()

        for (idx in 0..backFillCnt) {
            currentDay++

            val random = Random.nextInt(1, 100)
            val leadValue = if (random <= currentMon.backFillChance) 1 else 0
            pastLeads.add(leadValue)

            if (currentDay == currentMon.dayCount) {
                currentMonIdx = if (currentMonIdx == 12) 1 else currentMonIdx + 1
                currentMon = Month.byIdx(currentMonIdx)
                currentDay = 0
            }
        }

        return pastLeads
    }

    private fun printLeads(pastLeads: MutableList<Int>) {
        var currentMon = startMon
        var currentMonIdx = startMon.idx
        var currentDay = 0
        var yearNum = 1

        for (idx in 0..pastLeads.size) {
            currentDay++
            if (currentDay == currentMon.dayCount || idx == pastLeads.size) {
                val fromIdx = idx + 1 - currentMon.dayCount
                val monthCnt = pastLeads.subList(fromIdx, idx).sum()
                println("${currentMon.name} - $monthCnt")

                if (currentMon == Month.DEC) {
                    val fromYrIdx = if (idx - oneYear < 0) 0 else idx - oneYear
                    val yearCnt = pastLeads.subList(fromYrIdx, idx).sum()
                    println("*** year ${yearNum++} - $yearCnt")
                }

                currentMonIdx = if (currentMonIdx == 12) 1 else currentMonIdx + 1
                currentMon = Month.byIdx(currentMonIdx)
                currentDay = 0
            }
        }
    }
}
