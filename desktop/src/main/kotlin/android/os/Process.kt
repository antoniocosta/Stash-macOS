package android.os

/** Desktop shim for android.os.Process: real JVM pid; killProcess(own pid) halts the JVM like SIGKILL. */
object Process {
    @JvmStatic fun myPid(): Int = ProcessHandle.current().pid().toInt()

    @JvmStatic fun killProcess(pid: Int) {
        if (pid == myPid()) Runtime.getRuntime().halt(0)
        ProcessHandle.of(pid.toLong()).ifPresent { it.destroyForcibly() }
    }
}
