package garden.ephemeral.composevr.openvr

import org.lwjgl.openvr.VR
import org.lwjgl.system.MemoryStack.stackPush

object VR {
    fun isRuntimeInstalled() = VR.VR_IsRuntimeInstalled()
    fun runtimePath() = VR.VR_RuntimePath()
    fun isHmdPresent() = VR.VR_IsHmdPresent()

    fun initInternal(): Int {
        stackPush().use { stack ->
            val peError = stack.mallocInt(1)
            val token = VR.VR_InitInternal(peError, VR.EVRApplicationType_VRApplication_Overlay)
            val error = peError[0]
            if (error != 0) {
                val symbol = VR.VR_GetVRInitErrorAsSymbol(error)
                val description = VR.VR_GetVRInitErrorAsEnglishDescription(error)
                throw IllegalStateException("Failed to initialise OpenVR ($symbol, $description)")
            }
            return token
        }
    }

    fun shutdownInternal() {
        VR.VR_ShutdownInternal()
    }
}