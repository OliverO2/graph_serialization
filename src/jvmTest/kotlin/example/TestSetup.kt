package example

actual fun initializeTests() {
    // Special setup to make non-coroutine tests from the commonTest module work on the JVM.
    ServerSerializationSession.initializeSingleInstance()
}