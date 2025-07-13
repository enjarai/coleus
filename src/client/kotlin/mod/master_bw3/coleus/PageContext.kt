package mod.master_bw3.coleus

import java.nio.file.Path

public interface PageContext {

    public fun addSearchEntry(searchEntry: SearchEntry)

    public val pagePath: Path

    public val bookDir: Path

    public val assetsDir: Path
}