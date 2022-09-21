package garden.ephemeral.composevr.openvr

import androidx.compose.ui.unit.IntSize
import org.lwjgl.openvr.HmdMatrix34
import org.lwjgl.openvr.VR.*
import org.lwjgl.openvr.VRSystem.*
import org.lwjgl.system.MemoryStack.stackPush

object VRSystem {
    fun getRecommendedRenderTargetSize(): IntSize {
        stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            VRSystem_GetRecommendedRenderTargetSize(width, height)
            return IntSize(width[0], height[0])
        }
    }

    fun getStringTrackedDeviceProperty(deviceIndex: Int, propertyId: Int): String {
        stackPush().use { stack ->
            val errorPointer = stack.mallocInt(1)
            val result = VRSystem_GetStringTrackedDeviceProperty(deviceIndex, propertyId, errorPointer)
            checkError(errorPointer.get(), "VRSystem.GetStringTrackedDeviceProperty")
            return result
        }
    }

    fun getTrackedDeviceIndexForControllerRole(deviceType: Int): Int {
        return VRSystem_GetTrackedDeviceIndexForControllerRole(deviceType)
    }

    private fun checkError(error: Int, functionName: String) {
        if (error != 0) {
            throw RuntimeException("Got error: $error calling function: $functionName")
        }
    }

    fun x(overlay: VROverlay) {
        val leftHandID = getTrackedDeviceIndexForControllerRole(ETrackedControllerRole_TrackedControllerRole_LeftHand)
        if (leftHandID != 0 && leftHandID != -1) {
            // error getting left hand
            return
        }

        val transform = HmdMatrix34.create().apply {
            m(0, 1.0f)
            m(5, 1.0f)
            m(10, 1.0f)
        }

        overlay.setOverlayTransformTrackedDeviceRelative(leftHandID, transform)
    }
}