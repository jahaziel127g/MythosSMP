package com.mythossmp.infrastructure.task

import com.mythossmp.application.task.Cancellable
import com.mythossmp.application.task.Scheduler
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration

class PaperScheduler(private val plugin: JavaPlugin) : Scheduler {

    override fun sync(task: () -> Unit) {
        plugin.server.scheduler.runTask(plugin, Runnable { task() })
    }

    override fun async(task: () -> Unit) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { task() })
    }

    override fun later(delay: Duration, task: () -> Unit) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable { task() }, millisToTicks(delay))
    }

    override fun repeat(interval: Duration, task: () -> Unit): Cancellable {
        val ticks = millisToTicks(interval).coerceAtLeast(1)
        val bukkitTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable { task() }, ticks, ticks)
        return object : Cancellable {
            override fun cancel() {
                bukkitTask.cancel()
            }
        }
    }

    private fun millisToTicks(duration: Duration): Long {
        return duration.toMillis() / 50L
    }
}
