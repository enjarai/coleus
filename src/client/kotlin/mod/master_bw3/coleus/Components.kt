package mod.master_bw3.coleus

import com.glisco.isometricrenders.render.ItemRenderable
import com.glisco.isometricrenders.render.RenderableDispatcher
import com.mojang.blaze3d.platform.GlConst
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.systems.VertexSorter
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.core.*
import j2html.TagCreator.*
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.Tag
import j2html.tags.specialized.ImgTag
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.texture.NativeImage
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import org.joml.Matrix4f
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
        val client = MinecraftClient.getInstance()
        val framebuffer = SimpleFramebuffer(size, size, true, false)
        val tickCounter = client.renderTickCounter;
        val context = DrawContext(client, client.bufferBuilders.entityVertexConsumers)

        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC)
        val matrix4f = Matrix4f()
            .setOrtho(
                0.0f,
                size.toFloat(),
                size.toFloat(),
                0.0f,
                1000.0f,
                21000.0f
            )
        RenderSystem.backupProjectionMatrix()
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_Z)
        val modelViewStack = RenderSystem.getModelViewStack()
        modelViewStack.pushMatrix()
        modelViewStack.translation(0.0f, 0.0f, -11000.0f)
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.enableGuiDepthLighting()

        framebuffer.beginWrite(true)
        component.inflate(Size.of(framebuffer.textureWidth, framebuffer.textureHeight))
        component.mount(null, 0, 0)
        component.draw(OwoUIDrawContext.of(context), 0, 0, tickCounter.getTickDelta(false), tickCounter.lastFrameDuration)
        context.draw()
        framebuffer.endWrite()

        modelViewStack.popMatrix()
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.disableGuiDepthLighting()
        RenderSystem.restoreProjectionMatrix()

        val image = NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);

        framebuffer.beginRead();
        image.loadFromTextureImage(0, false);
        image.mirrorVertically()
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