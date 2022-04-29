package us.hawtrey.capacity_simulator

enum class Month(val idx: Int, val dayCount: Int, val backFillChance: Int) {
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
