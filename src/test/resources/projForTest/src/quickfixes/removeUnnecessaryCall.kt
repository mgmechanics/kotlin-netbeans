package quickfixes

fun removeUnnecessarySafeCall() {
    val integer = 42
    integer?.toString()
}

private fun useOfRemoveUnnecessarySafeCall() = removeUnnecessarySafeCall()
