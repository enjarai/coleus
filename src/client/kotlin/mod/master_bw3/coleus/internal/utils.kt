package mod.master_bw3.coleus.internal
import java.net.ServerSocket

internal fun isPortFree(port: Int): Boolean = runCatching {
    ServerSocket(port).use {}
}.isSuccess

internal fun firstAvailablePort(startPort: Int, maxPort: Int = 65535): Int? =
    (startPort..maxPort).firstOrNull { isPortFree(it) }