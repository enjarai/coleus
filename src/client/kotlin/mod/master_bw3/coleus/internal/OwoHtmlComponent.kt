package mod.master_bw3.coleus.internal

import com.glisco.isometricrenders.mixin.access.FramebufferAccessor
import com.glisco.isometricrenders.render.Renderable
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.systems.VertexSorter
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Size
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

internal class OwoHtmlComponent(private val component: Component) {


    internal fun drawIntoImage(size: Int): NativeImage {
        return copyFramebufferIntoImage(drawIntoTexture(size));
    }

    private fun copyFramebufferIntoImage(framebuffer: Framebuffer): NativeImage {
        val img = NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);

        // This call internally binds the buffer's color attachment texture
        framebuffer.beginRead();

        // This method gets the pixels from the currently bound texture
        img.loadFromTextureImage(0, false);
        img.mirrorVertically();

        framebuffer.delete();

        return img;
    }

    private fun drawIntoTexture(size: Int): Framebuffer {
        val framebuffer = SimpleFramebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC)

        RenderSystem.enableBlend()
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC)

        framebuffer.setClearColor(0f, 0f, 0f, 0f)
        framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC)

        framebuffer.beginWrite(true)
        ///
        val modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();

        ////
        val scale = size / 10000f
        modelViewStack.scale(scale, scale, -scale)

//        modelViewStack.translate(this.xOffset.get() / 260f, this.yOffset.get() / -260f, 0f)
        modelViewStack.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(180f))
        modelViewStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(180f))
        ////

        RenderSystem.applyModelViewMatrix();

        RenderSystem.backupProjectionMatrix();
        val projectionMatrix = Matrix4f().setOrtho(-1f, 1f, -1f, 1f, -1000f, 3000f);

        // Unproject to get the camera position for vertex sorting
        var camPos = Vector4f(0f, 0f, 0f, 1f);
        camPos.mul(Matrix4f(projectionMatrix).invert()).mul(Matrix4f(modelViewStack).invert());
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.byDistance(-camPos.x, -camPos.y, -camPos.z));


        RenderSystem.runAsFancy {
            // Emit untransformed vertices
            val client = MinecraftClient.getInstance()
            var tickCounter = client.renderTickCounter;
            val context = DrawContext(client, client.bufferBuilders.entityVertexConsumers)

            context.matrices.push()
            component.inflate(Size.of(size, size))
            component.draw(OwoUIDrawContext.of(context), 0, 0, tickCounter.getTickDelta(false), tickCounter.lastFrameDuration)
            context.matrices.pop()
            context.draw()

            // --> Draw
            val lightDirection = Vector4f(90f, .35f, 1f, 0f);

            val lightTransform = Matrix4f(modelViewStack);
            lightTransform.invert();
            lightDirection.mul(lightTransform);

            val transformedLightDirection = Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);
            RenderSystem.setShaderLights(transformedLightDirection, transformedLightDirection);

            // Draw all buffers
            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
        }


        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.restoreProjectionMatrix();
        framebuffer.endWrite()

        // Release depth attachment and FBO to save on VRAM - we only need
        // the color attachment texture to later turn into an image
        val accessor = framebuffer as FramebufferAccessor
        TextureUtil.releaseTextureId(framebuffer.getDepthAttachment())
        accessor.`isometric$setDepthAttachment`(-1)

        GlStateManager._glDeleteFramebuffers(accessor.`isometric$getFbo`())
        accessor.`isometric$setFbo`(-1)

        return framebuffer
    }

}