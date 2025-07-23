package mod.master_bw3.coleus

import com.mojang.blaze3d.platform.GlConst
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.systems.VertexSorter
import io.wispforest.owo.ui.core.*
import j2html.TagCreator.*
import j2html.tags.ContainerTag
import j2html.tags.DomContent
import j2html.tags.Tag
import j2html.tags.specialized.DivTag
import j2html.tags.specialized.ImgTag
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.TooltipComponent
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.texture.NativeImage
import net.minecraft.text.Text
import org.joml.Matrix4f
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.name
import kotlin.io.path.relativeTo

public object Components {

    @JvmStatic
    public fun owo(
        component: Component,
        pagePath: Path,
        imageOutPath: Path,
        imageSize: Int = 100,
        scale: Int = 1
    ): ImgTag {
        component.inflate(Size.of(imageSize / scale, imageSize / scale))

        val client = MinecraftClient.getInstance()
        val framebuffer = SimpleFramebuffer(component.width() * scale, component.height() * scale, true, false)
        val tickCounter = client.renderTickCounter;
        val context = DrawContext(client, client.bufferBuilders.entityVertexConsumers)

        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC)
        val matrix4f = Matrix4f()
            .setOrtho(
                0.0f,
                framebuffer.textureWidth.toFloat(),
                framebuffer.textureHeight.toFloat(),
                0.0f,
                0f,
                21000.0f
            )
        RenderSystem.backupProjectionMatrix()
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_Z)
        val modelViewStack = RenderSystem.getModelViewStack()
        modelViewStack.pushMatrix()
        modelViewStack.translation(0.0f, 0.0f, -11000.0f)
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.enableGuiDepthLighting()

        context.matrices.push()
        context.matrices.scale(scale.toFloat(), scale.toFloat(), 50f)

        component.mount(null, 0, 0)

        framebuffer.beginWrite(true)
        component.draw(
            OwoUIDrawContext.of(context),
            0,
            0,
            tickCounter.getTickDelta(false),
            tickCounter.lastFrameDuration
        )
        context.draw()
        framebuffer.endWrite()

        context.matrices.pop()
        modelViewStack.popMatrix()
        RenderSystem.disableDepthTest()
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.disableGuiDepthLighting()
        RenderSystem.restoreProjectionMatrix()
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC)


        val image = NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);

        framebuffer.beginRead();
        image.loadFromTextureImage(0, false);
        image.mirrorVertically()
        framebuffer.delete();

        imageOutPath.parent.toFile().mkdirs()
        image.writeTo(imageOutPath)
        image.close()
        return img().withSrc(imageOutPath.relativeTo(pagePath.parent).toString())
    }

    @JvmStatic
    public fun tooltip(tooltip: List<TooltipComponent>, pagePath: Path, imageOutPath: Path, scale: Int = 1): ImgTag {
        val client = MinecraftClient.getInstance()
        val framebuffer = SimpleFramebuffer(200 * scale, 200 * scale, true, false)
        val context = OwoUIDrawContext.of(DrawContext(client, client.bufferBuilders.entityVertexConsumers))

        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC)
        val matrix4f = Matrix4f()
            .setOrtho(
                0.0f,
                framebuffer.textureWidth.toFloat(),
                framebuffer.textureHeight.toFloat(),
                0.0f,
                0f,
                21000.0f
            )
        RenderSystem.backupProjectionMatrix()
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_Z)
        val modelViewStack = RenderSystem.getModelViewStack()
        modelViewStack.pushMatrix()
        modelViewStack.translation(0.0f, 0.0f, -11000.0f)
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.enableGuiDepthLighting()

        context.matrices.push()
        context.matrices.translate(-8.0 * scale, 16.0 * scale, 0.0)
        context.matrices.scale(scale.toFloat(), scale.toFloat(), 0f)

        framebuffer.beginWrite(true)
        context.drawTooltip(client.textRenderer, 0, 0, tooltip)
        context.draw()
        framebuffer.endWrite()

        context.matrices.pop()
        modelViewStack.popMatrix()
        RenderSystem.disableDepthTest()
        RenderSystem.applyModelViewMatrix()
        DiffuseLighting.disableGuiDepthLighting()
        RenderSystem.restoreProjectionMatrix()
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC)


        val image = NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);

        framebuffer.beginRead();
        image.loadFromTextureImage(0, false);
        image.mirrorVertically()
        framebuffer.delete();

        imageOutPath.parent.toFile().mkdirs()
        image.writeTo(imageOutPath)
        image.close()
        return img().withSrc(imageOutPath.relativeTo(pagePath.parent).toString())
    }

    @JvmStatic
    public fun owoWithTooltip(
        component: Component, tooltip: List<TooltipComponent>, className: String,
        pagePath: Path, imageOutPath: Path, imageSize: Int = 100, scale: Int = 1
    ): DivTag {
        imageOutPath.parent.toFile().mkdirs()

        val div = div().withClass("embedded-component-container")
        div.with(
            owo(
                component,
                pagePath,
                imageOutPath,
                500,
                scale
            ).withClass("embedded-component $className"),

            tooltip(tooltip, pagePath, imageOutPath.parent.resolve("tooltip").resolve(imageOutPath.name), 2)
                .withClass("embedded-component-tooltip")
        )

        return div
    }

    @JvmStatic
    public fun text(text: Text): Tag<*> {
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
        val color = style.color;
        if (color != null) {
            if (color.name != color.hexCode) {
                current.withClass(color.name)
            } else {
                current.withStyle("color: " + color.hexCode)
            }
        }


        current.withText(text.copyContentOnly().string)

        for (sibling in text.siblings) {
            current.with(constructText(sibling))
        }

        return out
    }

}