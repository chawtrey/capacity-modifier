package us.hawtrey.capacity_simulator

import kotlin.random.Random

//@Suppress("UNUSED_PARAMETER")
//fun main(args: Array<String>) {
//    CapacitySimulator().runSim(shouldAddThreshold = 1.0)
//}

class CapacitySimulator {
    private val oneYear = 365
    private val startMon = Month.JAN
    private val backFillCnt = oneYear * 2

    private val leads = mutableListOf<Int>()

    fun runSim(
        range: Int = 90,
        rangePercent: Double = 0.2,
        yearsToRun: Int = 10,
        shouldAddThreshold: Double = 1.0
    ) {
        val runCnt = oneYear * yearsToRun

        val midRange = range / 2

        backFillLeadData()

        for (idx in 1..runCnt) {
            val today = leads.size
//            val currentCount = leadCountForRange(today - range, today)
//            val trailingCount =
//                leadCountForRange(today - range - range - 1, today - range - 1)
//            val yoyCurrentCount =
//                leadCountForRange(today - range - oneYear, today - oneYear)
//            val yoyTrailingCount =
//                leadCountForRange(
//                    today - range - range - 1 - oneYear,
//                    today - range - 1 - oneYear
//                )
            val currentCount = leadCountForRange(today - midRange, today)
            val trailingCount =
                leadCountForRange(today - midRange - range - 1, today - midRange - 1)
            val yoyCurrentCount =
                leadCountForRange(today - midRange - oneYear, today + midRange - oneYear)
            val yoyTrailingCount =
                leadCountForRange(
                    today - midRange - range - 1 - oneYear,
                    today - midRange - 1 - oneYear
                )

            val percentageChange = trailingCount.toDouble() / yoyTrailingCount.toDouble()
            val expectedCount =
                percentageChange * yoyCurrentCount
            val expectedCountFloor = expectedCount - (expectedCount * rangePercent)

//            val capacityMultiplier = expectedCount / currentCount
//            val capacityMultiplier = expectedCountFloor / (2 * currentCount)
//            val shouldAdd = capacityMultiplier >= shouldAddThreshold

            val shouldAdd = currentCount * 2 < expectedCountFloor
            if (shouldAdd) leads.add(1) else leads.add(0)
        }

        printLeads()
    }

    private fun leadCountForRange(fromIndex: Int, toIndex: Int) =
        leads.subList(fromIndex, toIndex).sum()

    private fun backFillLeadData(): MutableList<Int> {
        var currentMon = startMon
        var currentMonIdx = startMon.idx
        var currentDay = 0

        for (idx in 1..backFillCnt) {
            currentDay++

            val random = Random.nextInt(1, 100)
            val leadValue = if (random <= currentMon.backFillChance) 1 else 0
            leads.add(leadValue)

            if (currentDay == currentMon.dayCount) {
                currentMonIdx = if (currentMonIdx == 12) 1 else currentMonIdx + 1
                currentMon = Month.byIdx(currentMonIdx)
                currentDay = 0
            }
        }

        return leads
    }

    private fun printLeads() {
        var currentMon = startMon
        var currentMonIdx = startMon.idx
        var currentDay = 0
        var yearNum = 1

        for (idx in 0..leads.size) {
            currentDay++

            if (currentDay > currentMon.dayCount || idx == leads.size) {
                val fromIdx = idx - currentMon.dayCount
                val monthCnt = leads.subList(fromIdx, idx).sum()
                println("${currentMon.name} - $monthCnt")

                if (currentMon == Month.DEC) {
                    val fromYrIdx = idx - oneYear
                    val yearCnt = leads.subList(fromYrIdx, idx).sum()
                    println("*** year ${yearNum++} - $yearCnt")
                }

                currentMonIdx = if (currentMonIdx == 12) 1 else currentMonIdx + 1
                currentMon = Month.byIdx(currentMonIdx)
                currentDay = 1
            }
        }
    }
}
