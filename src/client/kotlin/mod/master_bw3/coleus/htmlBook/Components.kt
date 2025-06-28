package mod.master_bw3.coleus.htmlBook

import com.glisco.isometricrenders.render.ItemRenderable
import com.glisco.isometricrenders.render.RenderableDispatcher
import j2html.TagCreator.*
import j2html.tags.specialized.ImgTag
import net.minecraft.item.ItemStack


import java.nio.file.Path
import kotlin.io.path.relativeTo

object Components {
    fun item(itemStack: ItemStack, pagePath: Path, imageOutPath: Path, size: Int = 100): ImgTag {
        val image = RenderableDispatcher.drawIntoImage(ItemRenderable(itemStack), 0f, size)
        imageOutPath.parent.toFile().mkdirs()
        image.writeTo(imageOutPath)
        return img().withSrc(imageOutPath.relativeTo(pagePath.parent).toString())
    }

}