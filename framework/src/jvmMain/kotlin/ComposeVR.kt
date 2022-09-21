package garden.ephemeral.composevr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.node.Ref
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.application
import garden.ephemeral.composevr.openvr.VR
import garden.ephemeral.composevr.openvr.VROverlay
import garden.ephemeral.composevr.openvr.VRSystem
import org.jetbrains.skia.*
import org.jetbrains.skiko.FrameDispatcher
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL32.*
import org.lwjgl.openvr.HmdMatrix34
import org.lwjgl.openvr.OpenVR
import org.lwjgl.openvr.Texture
import org.lwjgl.openvr.VR.*
import java.util.logging.Logger
import kotlin.system.exitProcess

private val logger = Logger.getLogger("garden.ephemeral.composevr.ComposeVRKt")

// FIXME: All this state should be initialised once per application and
//        properly shut down at the end.
//        There's probably more state which should be in here as well,
//        e.g. the dummy window.
object GlobalInit {
    private val token: Int
    init {
        preInitDiagnostics()
        token = VR.initInternal()
        OpenVR.create(token)
        postInitDiagnostics()
    }
    fun init() = Unit
}

fun singleOverlayApplication(appContent: @Composable () -> Unit) = application {
    GlobalInit.init()
    Overlay(appContent = appContent)
}

@Suppress("FunctionName")
@Composable
fun Overlay(
    state: OverlayState = rememberOverlayState(),
    visible: Boolean = true,
    // TODO: visible: Boolean = true,
    appContent: @Composable () -> Unit
) {
    // TODO: Mimic what's going on for Window
    //       val currentState by rememberUpdatedState(state)

    val overlayRef = remember { Ref<VROverlay>() }
    fun overlay() = overlayRef.value!!

    DisposableEffect(Unit) {
        val overlay = VROverlay.create("Compose", "Compose")
        overlayRef.value = overlay

        overlay.overlayWidthInMeters = 3.0f
        overlay.setOverlayTransformAbsolute(
            ETrackingUniverseOrigin_TrackingUniverseStanding,
            HmdMatrix34.create().apply {
                // guessing row major, docs do not say
                //  1  0  0  0
                //  0  1  0  1
                //  0  0  1 -2
                m(0, 1.0f)
                m(5, 1.0f)
                m(7, 1.0f)
                m(10, 1.0f)
                m(11, -2.0f)
            }
        )

        overlay().showOverlay()

        // TODO: This probably shouldn't be called more than once, will have to
        //       move it into some ComposeOverlay utility class?
        //       Or is it SideEffect / LaunchedEffect?
        bootstrapCompose(state.size.width, state.size.height, overlay, appContent)

        onDispose {
            overlay().destroyOverlay()
        }
    }

//    // Updates - in the Compose code we see them using UpdateEffect {},
//    // but that is internal, so we don't get to use it.
//    // TODO: What's the alternative? I don't want this called more than once :(
//    if (visible) {
//        overlay().showOverlay()
//    } else {
//        overlay().hideOverlay()
//    }
}


private fun preInitDiagnostics() {
    logger.fine("VR_IsRuntimeInstalled() = ${VR.isRuntimeInstalled()}")
    logger.fine("VR_RuntimePath() = ${VR.runtimePath()}")
    logger.fine("VR_IsHmdPresent() = ${VR.isHmdPresent()}")
}

private fun postInitDiagnostics() {
    val modelNumber = VRSystem.getStringTrackedDeviceProperty(
        k_unTrackedDeviceIndex_Hmd,
        ETrackedDeviceProperty_Prop_ModelNumber_String,
    )
    val serialNumber = VRSystem.getStringTrackedDeviceProperty(
        k_unTrackedDeviceIndex_Hmd,
        ETrackedDeviceProperty_Prop_SerialNumber_String
    )
    val recommendedRenderTargetSize = VRSystem.getRecommendedRenderTargetSize()
    logger.fine("Model Number : $modelNumber")
    logger.fine("Serial Number: $serialNumber")
    logger.fine("Recommended size: $recommendedRenderTargetSize")
}

/**
 * Creates a framebuffer along with its render texture.
 *
 * @param width the desired width in pixels.
 * @param height the desired height in pixels.
 * @return info about the created framebuffer.
 */
private fun createFramebuffer(width: Int, height: Int): FramebufferInfo {
    if (!glfwInit()) {
        throw RuntimeException("Couldn't initialise GLFW")
    }

    // glfwMakeContextCurrent requires a window so we make a dummy invisible one
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    val window = glfwCreateWindow(width, height, "Hidden window because cursed API is cursed", 0, 0)
    if (window == 0L) {
        throw RuntimeException("Couldn't create GLFW window")
    }

    // createCapabilities only works with a GL context, and all the GL calls
    // below crash if you don't have capabilities set
    glfwMakeContextCurrent(window)
    createCapabilities()

    val framebuffer = glGenFramebuffers()
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)

    val renderedTexture = glGenTextures()
    glBindTexture(GL_TEXTURE_2D, renderedTexture)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, renderedTexture, 0)
    glDrawBuffers(intArrayOf(GL_COLOR_ATTACHMENT0))

    val framebufferStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER)
    if (framebufferStatus != GL_FRAMEBUFFER_COMPLETE) {
        throw IllegalStateException("Framebuffer is not complete, status = $framebufferStatus")
    }

    return FramebufferInfo(width, height, framebuffer, renderedTexture)
}

private fun bootstrapCompose(
    width: Int,
    height: Int,
    overlay: VROverlay,
    appContent: @Composable () -> Unit
) {
    // Creating GL framebuffer
    val framebufferInfo = createFramebuffer(width, height)

    // Creating Skia surface
    val context = DirectContext.makeGL()
    // FIXME: This is not yet closed
    var surface = createSurface(framebufferInfo, context)

    // a custom coroutine dispatcher, in which Compose will run
    val glfwDispatcher = GlfwCoroutineDispatcher()

    lateinit var composeScene: ComposeScene

    fun render() {
        surface.canvas.clear(Color.WHITE)
        composeScene.constraints = Constraints(
            maxWidth = framebufferInfo.width,
            maxHeight = framebufferInfo.height
        )
        composeScene.render(surface.canvas, System.nanoTime())

        context.flush()

        // This basically becomes the equivalent of glfwSwapBuffers
        overlay.setOverlayTexture(Texture.create().apply {
            eType(ETextureType_TextureType_OpenGL)
            eColorSpace(EColorSpace_ColorSpace_Gamma)
            handle(framebufferInfo.textureHandle.toUInt().toLong())
        })
    }

    val frameDispatcher = FrameDispatcher(glfwDispatcher) { render() }

    // TODO: Find a sensible density, 1.0 probably isn't sane for VR
    val density = Density(1.0f)

    composeScene = ComposeScene(
        glfwDispatcher,
        density,
        invalidate = frameDispatcher::scheduleFrame
    )

    // XXX: If we ever resize the overlay:
//        glfwSetWindowSizeCallback(windowHandle) { _, windowWidth, windowHeight ->
//            width = windowWidth
//            height = windowHeight
//            surface.close()
//   TODO would also have to recreate the framebuffer here, or at least its image?
//            surface = createSurface(width, height, context)
//
//            glfwSwapInterval(0)
//            render()
//            glfwSwapInterval(1)
//        }

    composeScene.setContent(appContent)

    glfwDispatcher.runLoop()

    composeScene.close()
    exitProcess(0)
}

/**
 * Creates a Skia Surface, bound to the given OpenGL framebuffer.
 *
 * @param framebufferInfo info about the framebuffer to use.
 * @param context context for rendering.
 * @return the created surface. Once done with it, the surface should be closed.
 */
private fun createSurface(framebufferInfo: FramebufferInfo, context: DirectContext): Surface {
    val renderTarget = BackendRenderTarget.makeGL(
        framebufferInfo.width,
        framebufferInfo.height,
        0,
        8,
        framebufferInfo.framebufferHandle,
        FramebufferFormat.GR_GL_RGBA8
    )
    return Surface.makeFromBackendRenderTarget(
        context,
        renderTarget,
        SurfaceOrigin.BOTTOM_LEFT,
        SurfaceColorFormat.RGBA_8888,
        ColorSpace.sRGB
    ) ?: throw IllegalStateException("Surface parameters were invalid")
}

/**
 * Holder for related data about one framebuffer.
 */
private data class FramebufferInfo(
    val width: Int,
    val height: Int,
    val framebufferHandle: Int,
    val textureHandle: Int
)
