package com.mythossmp.task

import com.mythossmp.application.task.Cancellable
import com.mythossmp.application.task.Scheduler
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

class FakeScheduler : Scheduler {

    var syncCount = 0
        private set
    var asyncCount = 0
        private set

    private val laterQueue = CopyOnWriteArrayList<Pair<Duration, () -> Unit>>()
    val laterTasks: List<Pair<Duration, () -> Unit>> get() = laterQueue

    private val repeatQueue = CopyOnWriteArrayList<Pair<Duration, () -> Unit>>()
    val repeatTasks: List<Pair<Duration, () -> Unit>> get() = repeatQueue

    val cancelledRepeats = CopyOnWriteArrayList<Cancellable>()

    override fun sync(task: () -> Unit) {
        syncCount++
        task()
    }

    override fun async(task: () -> Unit) {
        asyncCount++
        task()
    }

    override fun later(delay: Duration, task: () -> Unit) {
        laterQueue.add(delay to task)
    }

    override fun repeat(interval: Duration, task: () -> Unit): Cancellable {
        val entry = interval to task
        repeatQueue.add(entry)
        return object : Cancellable {
            override fun cancel() {
                cancelledRepeats.add(this)
                repeatQueue.remove(entry)
            }
        }
    }

    fun runAllLater() {
        laterQueue.map { it.second }.forEach { it() }
        laterQueue.clear()
    }

    fun runAllRepeating() {
        repeatQueue.map { it.second }.forEach { it() }
    }

    fun hasRepeating(interval: Duration): Boolean = repeatQueue.any { it.first == interval }

    fun clear() {
        laterQueue.clear()
        repeatQueue.clear()
        cancelledRepeats.clear()
    }
}
