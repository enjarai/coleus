package mod.master_bw3.coleus

import com.glisco.isometricrenders.render.ItemRenderable
import com.glisco.isometricrenders.render.RenderableDispatcher
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.Containers.stack
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Size
import io.wispforest.owo.ui.core.Sizing
import j2html.TagCreator.em
import j2html.TagCreator.img
import j2html.TagCreator.span
import j2html.TagCreator.strong
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.Tag
import j2html.tags.specialized.ImgTag
import mod.master_bw3.coleus.internal.OwoUIComponentRenderable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL30C
import java.nio.file.Path
import kotlin.io.path.relativeTo

public object Components {

    @JvmStatic
    public fun item(itemStack: ItemStack, pagePath: Path, imageOutPath: Path, size: Int = 100): ImgTag {
        val image = RenderableDispatcher.drawIntoImage(ItemRenderable(itemStack), 0f, size)
        imageOutPath.parent.toFile().mkdirs()
        image.writeTo(imageOutPath)
        return img().withSrc(imageOutPath.relativeTo(pagePath.parent).toString())
    }

    @JvmStatic
    public fun owo(component: Component, pagePath: Path, imageOutPath: Path, size: Int = 100): ImgTag {
        val size = 500;
        val component = Components.box(Sizing.fill(), Sizing.fill())
        component.color(Color.RED)
        component.fill(true)

        val client = MinecraftClient.getInstance()
        val framebuffer = SimpleFramebuffer(size, size, true, false)
        var tickCounter = client.renderTickCounter;
        val context = DrawContext(client, client.bufferBuilders.entityVertexConsumers)

        framebuffer.beginWrite(true)
        val adapter = OwoUIAdapter.createWithoutScreen(0, 0,  framebuffer.viewportHeight, framebuffer.viewportWidth, Containers::stack)
        adapter.rootComponent.child(component)
        adapter.inflateAndMount()
        adapter.render(context, 0, 0, tickCounter.lastFrameDuration)
        context.draw()
        framebuffer.endWrite()

        val image = NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);

        framebuffer.beginRead();
        image.loadFromTextureImage(0, false);
        framebuffer.delete();


        imageOutPath.parent.toFile().mkdirs()
        image.writeTo(imageOutPath)
        return img().withSrc(imageOutPath.relativeTo(pagePath.parent).toString())
    }

    @JvmStatic
    public fun text(text: Text): DomContent {
        return constructText(text)
    }

    private fun constructText(text: Text): Tag<*> {
        val out = span()
        var current: ContainerTag<*> = out

        val style = text.style
        if (style.isBold) {
            val child = strong()
            current.with(child)
            current = child
        }
        if (style.isItalic) {
            val child = em()
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
            current
                .withStyle("color: " + color.hexCode)
                .withClass(color.name)
        }

        current.withText(text.copyContentOnly().string)

        for (sibling in text.siblings) {
            current.with(constructText(sibling))
        }

        return out
    }

}