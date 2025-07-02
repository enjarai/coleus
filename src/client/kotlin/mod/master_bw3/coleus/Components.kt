package mod.master_bw3.coleus

import com.glisco.isometricrenders.render.ItemRenderable
import com.glisco.isometricrenders.render.RenderableDispatcher
import j2html.TagCreator
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.Tag
import j2html.tags.specialized.ImgTag
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import java.nio.file.Path
import kotlin.io.path.relativeTo

public object Components {

    @JvmStatic
    public fun item(itemStack: ItemStack, pagePath: Path, imageOutPath: Path, size: Int = 100): ImgTag {
        val image = RenderableDispatcher.drawIntoImage(ItemRenderable(itemStack), 0f, size)
        imageOutPath.parent.toFile().mkdirs()
        image.writeTo(imageOutPath)
        return TagCreator.img().withSrc(imageOutPath.relativeTo(pagePath.parent).toString())
    }

    @JvmStatic
    public fun text(text: Text): DomContent {
        return constructText(text)
    }

    private fun constructText(text: Text): Tag<*> {
        val out = TagCreator.span()
        var current: ContainerTag<*> = out

        val style = text.style
        if (style.isBold) {
            val child = TagCreator.strong()
            current.with(child)
            current = child
        }
        if (style.isItalic) {
            val child = TagCreator.em()
            current.with(child)
            current = child
        }
        if (style.isUnderlined) {
            current.withClass("underline")
        }
        if (style.isObfuscated) {
            current.withClass("obfuscated")
        }
        val color = style.getColor();
        if (color != null) {
            current.withStyle("color: " + color.hexCode)
        }

        println(text.copyContentOnly().string)
        current.withText(text.copyContentOnly().string)

        for (sibling in text.siblings) {
            current.with(constructText(sibling))
        }

        return out
    }

}