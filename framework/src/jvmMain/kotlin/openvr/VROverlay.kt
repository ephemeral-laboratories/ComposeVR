package garden.ephemeral.composevr.openvr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import org.lwjgl.openvr.HmdMatrix34
import org.lwjgl.openvr.Texture
import org.lwjgl.openvr.VR.k_unVROverlayMaxKeyLength
import org.lwjgl.openvr.VR.k_unVROverlayMaxNameLength
import org.lwjgl.openvr.VROverlay.*
import org.lwjgl.openvr.VRTextureBounds
import org.lwjgl.system.MemoryStack.stackPush
import java.io.File
import java.util.logging.Logger

// key has to be unique, name doesn't matter
class VROverlay private constructor (private val handle: Long) {

    val key: String
        get() {
            stackPush().use { stack ->
                val result = stack.mallocInt(1)
                val key = VROverlay_GetOverlayKey(handle, k_unVROverlayMaxKeyLength, result)
                checkResult(result[0], "VROverlay.GetOverlayKey")
                return key
            }
        }

    var name: String
        get() {
            stackPush().use { stack ->
                val result = stack.mallocInt(1)
                val name = VROverlay_GetOverlayName(handle, k_unVROverlayMaxNameLength, result)
                checkResult(result[0], "VROverlay.GetOverlayName")
                return name
            }
        }
        set(value) {
            val result = VROverlay_SetOverlayName(handle, value)
            checkResult(result, "VROverlay.SetOverlayName")
        }

    var overlayColor: Color
        get() {
            stackPush().use { stack ->
                val red = stack.mallocFloat(1)
                val green = stack.mallocFloat(1)
                val blue = stack.mallocFloat(1)
                val result = VROverlay_GetOverlayColor(handle, red, green, blue)
                checkResult(result, "VROverlay.GetOverlayColor")
                return Color(red[0], green[0], blue[0])
            }
        }
        set(value) {
            val result = VROverlay_SetOverlayColor(handle, value.red, value.green, value.blue)
            checkResult(result, "VROverlay.SetOverlayColor")
        }

    var overlayAlpha: Float
        get() {
            stackPush().use { stack ->
                val alpha = stack.mallocFloat(1)
                val result = VROverlay_GetOverlayAlpha(handle, alpha)
                checkResult(result, "VROverlay.GetOverlayAlpha")
                return alpha[0]
            }
        }
        set(value) {
            val result = VROverlay_SetOverlayAlpha(handle, value)
            checkResult(result, "VROverlay.SetOverlayAlpha")
        }

    var overlayWidthInMeters: Float
        get() {
            stackPush().use { stack ->
                val widthInMeters = stack.mallocFloat(1)
                val result = VROverlay_GetOverlayWidthInMeters(handle, widthInMeters)
                checkResult(result, "VROverlay.GetOverlayWidthInMeters")
                return widthInMeters[0]
            }
        }
        set(value) {
            val result = VROverlay_SetOverlayWidthInMeters(handle, value)
            checkResult(result, "VROverlay.SetOverlayWidthInMeters")
        }

    var overlayTextureBounds: VRTextureBounds
        get() {
            val bounds = VRTextureBounds.create()
            val result = VROverlay_GetOverlayTextureBounds(handle, bounds)
            checkResult(result, "VROverlay.GetOverlayTextureBounds")
            return bounds
        }
        set(value) {
            val result = VROverlay_SetOverlayTextureBounds(handle, value)
            checkResult(result, "VROverlay.SetOverlayTextureBounds")
        }

    var overlayTextureColorSpace: Int
        get() {
            stackPush().use { stack ->
                val textureColorSpace = stack.mallocInt(1)
                val result = VROverlay_GetOverlayTextureColorSpace(handle, textureColorSpace)
                checkResult(result, "VROverlay.GetOverlayTextureColorSpace")
                return textureColorSpace[0]
            }
        }
        set(value) {
            val result = VROverlay_SetOverlayTextureColorSpace(handle, value)
            checkResult(result, "VROverlay.SetOverlayTextureColorSpace")
        }

    val overlayTextureSize: IntSize
        get() {
            stackPush().use { stack ->
                val width = stack.mallocInt(1)
                val height = stack.mallocInt(1)
                val result = VROverlay_GetOverlayTextureSize(handle, width, height)
                checkResult(result, "VROverlay.GetOverlayTextureSize")
                return IntSize(width[0], height[0])
            }
        }

    fun setOverlayTexture(texture: Texture) {
        val result = VROverlay_SetOverlayTexture(handle, texture)
        checkResult(result, "VROverlay.SetOverlayTexture")
    }

    fun setOverlayFromFile(path: File) {
        val result = VROverlay_SetOverlayFromFile(handle, path.absolutePath)
        checkResult(result, "VROverlay.SetOverlayFromFile")
    }

    fun clearOverlayTexture() {
        val result = VROverlay_ClearOverlayTexture(handle)
        checkResult(result, "VROverlay.ClearOverlayTexture")
    }

    fun showOverlay() {
        val result = VROverlay_ShowOverlay(handle)
        checkResult(result, "VROverlay.ShowOverlay")
    }

    fun hideOverlay() {
        val result = VROverlay_HideOverlay(handle)
        checkResult(result, "VROverlay.HideOverlay")
    }

    fun destroyOverlay() {
        val result = VROverlay_DestroyOverlay(handle)
        checkResult(result, "VROverlay.DestroyOverlay")
    }

    fun setOverlayTransformAbsolute(eTrackingOrigin: Int, transform: HmdMatrix34) {
        val result = VROverlay_SetOverlayTransformAbsolute(handle, eTrackingOrigin, transform)
        checkResult(result, "VROverlay.SetOverlayTransformAbsolute")
    }

    fun setOverlayTransformTrackedDeviceRelative(trackedDeviceID: Int, transform: HmdMatrix34) {
        val result = VROverlay_SetOverlayTransformTrackedDeviceRelative(handle, trackedDeviceID, transform)
        checkResult(result, "VROverlay.SetOverlayTransformTrackedDeviceRelative")
    }

    companion object {
        private val logger = Logger.getLogger(this::class.qualifiedName)

        fun create(key: CharSequence, name: CharSequence): VROverlay {
            stackPush().use { stack ->
                val handlePointer = stack.mallocLong(1)
                val result = VROverlay_CreateOverlay(key, name, handlePointer)
                checkResult(result, "VROverlay.CreateOverlay")
                val handle = handlePointer[0]
                return VROverlay(handle)
            }
        }

        private fun checkResult(result: Int, functionCalled: String) {
            if (result != 0) {
                val errorName = VROverlay_GetOverlayErrorNameFromEnum(result)
                logger.warning("Got error: $result ($errorName) from function: $functionCalled")
            }
        }
    }
}