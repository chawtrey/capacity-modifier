package us.hawtrey.capacity_simulator

enum class Month(val idx: Int, val dayCount: Int, val backFillChance: Int) {
    JAN(1, 31, 20),
    FEB(2, 28, 20),
    MAR(3, 31, 20),
    APR(4, 30, 20),
    MAY(5, 31, 20),
    JUN(6, 30, 20),
    JUL(7, 31, 20),
    AUG(8, 31, 20),
    SEP(9, 30, 20),
    OCT(10, 31, 20),
    NOV(11, 30, 20),
    DEC(12, 31, 20);

    companion object {
        fun byIdx(idx: Int) = values().find { it.idx == idx }!!
    }
}
