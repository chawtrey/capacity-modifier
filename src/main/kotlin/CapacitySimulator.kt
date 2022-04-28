import kotlin.random.Random

fun main(args: Array<String>) {
    CapacitySimulator().runSim()
}

class CapacitySimulator {
    private val oneYear = 365
    private val startMon = Month.JAN
    private val backFillCnt = oneYear * 2 - 1 // roughly 2 years

    fun runSim(range: Int = 30, rangePercent: Double = 0.2, yearsToRun: Int = 10) {
        val runCnt = oneYear * yearsToRun - 1

        val midRange = range / 2

        val pastLeads = mutableListOf<Int>()

        backFillLeadData(pastLeads)

        for (idx in 0..runCnt) {
            val today = pastLeads.size
            val currentCount = pastLeads.subList(today - midRange, today).sum()
            val trailingCount =
                pastLeads.subList(today - midRange - range - 1, today - midRange - 1).sum()
            val yoyCurrentCount =
                pastLeads.subList(today - midRange - oneYear, today + midRange - oneYear).sum()
            val yoyTrailingCount =
                pastLeads.subList(
                    today - midRange - range - 1 - oneYear,
                    today - midRange - 1 - oneYear
                ).sum()

            val expectedCount =
                (trailingCount.toDouble() / yoyTrailingCount.toDouble()) * yoyCurrentCount
            val expectedCountFloor = expectedCount - (expectedCount * rangePercent)

            val capacityMultiplier = expectedCountFloor / (2 * currentCount)
            val shouldAdd = capacityMultiplier >= 1.0

            if (shouldAdd) pastLeads.add(1) else pastLeads.add(0)
        }

        printLeads(pastLeads)
    }

    private fun backFillLeadData(pastLeads: MutableList<Int>) {
        var currentMon = startMon
        var currentMonIdx = startMon.idx
        var currentDay = 0

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
    }

    private fun printLeads(pastLeads: MutableList<Int>) {
        println("***************")
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

    private enum class Month(val idx: Int, val dayCount: Int, val backFillChance: Int) {
        JAN(1, 31, 25),
        FEB(2, 28, 25),
        MAR(3, 31, 35),
        APR(4, 30, 40),
        MAY(5, 31, 45),
        JUN(6, 30, 50),
        JUL(7, 31, 50),
        AUG(8, 31, 45),
        SEP(9, 30, 35),
        OCT(10, 31, 30),
        NOV(11, 30, 25),
        DEC(12, 31, 20);

        companion object {
            fun byIdx(idx: Int) = values().find { it.idx == idx }!!
        }
    }
}
