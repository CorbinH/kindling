package io.github.inductiveautomation.kindling.docker.services.ignition.model

@Suppress("DuplicatedCode")
object IgnitionVersionComparator : Comparator<String> {
    override fun compare(o1: String?, o2: String?): Int {
        if (o1 === o2) return 0
        if (o1 == null) return -1
        if (o2 == null) return 1

        check(VERSION_REGEX.containsMatchIn(o1) && VERSION_REGEX.containsMatchIn(o2))

        // Nightly is greater than anything else
        if (o1.equals("NIGHTLY", true)) return 1000
        if (o2.equals("NIGHTLY", true)) return -1000

        // Also account for major-version specific nightly builds
        if (o1.equals("8.1-nightly", ignoreCase = true)) return 950
        if (o2.equals("8.1-nightly", ignoreCase = true)) return -950

        if (o1.equals("8.3-nightly", ignoreCase = true)) return 900
        if (o2.equals("8.3-nightly", ignoreCase = true)) return -900

        // Latest is also greater than anything else
        if (o1.equals("LATEST", true)) return 100
        if (o2.equals("LATEST", true)) return -100

        val o1Split = o1.split(".", "-")
        val v1 = when (o1Split.size) {
            3 -> {
                val m = o1Split.map { it.toInt() }.toMutableList()
                m.add(1000)
                m
            }
            4 -> {
                o1Split.mapIndexed { index, value ->
                    if (index == 3) {
                        value.last().digitToInt()
                    } else {
                        value.toInt()
                    }
                }
            }
            else -> error("Malformed version: $o1")
        }

        val o2Split = o2.split(".", "-")
        val v2 = when (o2Split.size) {
            3 -> {
                val m = o2Split.map { it.toInt() }.toMutableList()
                m.add(1000) // Non release candidate is greater than release candidate
                m
            }
            4 -> {
                o2Split.mapIndexed { index, value ->
                    if (index == 3) {
                        value.last().digitToInt()
                    } else {
                        value.toInt()
                    }
                }
            }
            else -> error("Malformed version: $o1")
        }

        for ((version1, version2) in v1.zip(v2)) {
            val result = version1.compareTo(version2)
            if (result != 0) return result
        }

        return 0
    }

    val VERSION_REGEX = """latest|(?:\d\.\d-)?nightly|(?:\d\.\d+\.\d+)""".toRegex()
}
