package us.hawtrey.capacity_simulator

data class Agent(val name: String, var score: Double, val leadsPerYear: Int = 0) {
    constructor(score: Double, leadsPerYear: Int) : this(
        "a-${(score * 100).toInt()}",
        score,
        leadsPerYear
    )
}
