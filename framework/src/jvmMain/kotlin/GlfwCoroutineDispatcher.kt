package garden.ephemeral.composevr

import kotlinx.coroutines.CoroutineDispatcher
import org.lwjgl.glfw.GLFW.glfwPostEmptyEvent
import org.lwjgl.glfw.GLFW.glfwWaitEvents
import kotlin.coroutines.CoroutineContext

/**
 * Custom coroutine dispatcher for GLFW events.
 * Not actually a lot of use right now because GLFW events only come from a window,
 * and the only window we have is the dummy one.
 *
 * We'd want some custom dispatcher specifically for VR I think?
 */
class GlfwCoroutineDispatcher : CoroutineDispatcher() {
    private val tasks = mutableListOf<Runnable>()
    private val tasksCopy = mutableListOf<Runnable>()
    private var isStopped = false

    fun runLoop() {
        while (!isStopped) {
            synchronized(tasks) {
                tasksCopy.addAll(tasks)
                tasks.clear()
            }
            for (runnable in tasksCopy) {
                if (!isStopped) {
                    runnable.run()
                }
            }
            tasksCopy.clear()
            glfwWaitEvents()
        }
    }

    fun stop() {
        isStopped = true
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(tasks) {
            tasks.add(block)
        }
        glfwPostEmptyEvent()
    }
}