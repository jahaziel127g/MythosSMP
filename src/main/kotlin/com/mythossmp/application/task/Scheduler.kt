package com.mythossmp.application.task

import java.time.Duration

interface Scheduler {

    fun sync(task: () -> Unit)

    fun async(task: () -> Unit)

    fun later(delay: Duration, task: () -> Unit)

    fun repeat(interval: Duration, task: () -> Unit): Cancellable
}

interface Cancellable {
    fun cancel()
}
