package mod.master_bw3.coleus.internal


import com.glisco.isometricrenders.IsometricRenders
import com.glisco.isometricrenders.property.DefaultPropertyBundle
import com.glisco.isometricrenders.render.DefaultRenderable
import com.glisco.isometricrenders.render.Renderable
import com.glisco.isometricrenders.screen.IsometricUI
import com.glisco.isometricrenders.util.ExportPathSpec
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Size
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix4fStack
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public class OwoUIComponentRenderable public constructor(private val component: Component, private val size: Int) :

    DefaultRenderable<OwoUIComponentRenderable.OwoUIComponentPropertyBundle>() {
    override fun emitVertices(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, tickDelta: Float) {
        val client = MinecraftClient.getInstance()
        var tickCounter = client.renderTickCounter;
        val context = DrawContext(client, client.bufferBuilders.entityVertexConsumers)

        context.matrices.push()
//        context.matrices.translate(-size / 2f, -size / 2f, 0f)
        component.inflate(Size.of(size, size))
        component.mount(null, 0, 0)
        component.draw(OwoUIDrawContext.of(context), 0, 0, tickCounter.getTickDelta(false), tickCounter.lastFrameDuration)
        context.matrices.pop()
        context.draw()
    }

    override fun properties(): OwoUIComponentRenderable.OwoUIComponentPropertyBundle {
        return OwoUIComponentPropertyBundle.INSTANCE
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun exportPath(): ExportPathSpec {
        return ExportPathSpec.of("component", "${component.id() ?: ""}_${Uuid.random()}")
    }

    public class OwoUIComponentPropertyBundle public constructor() : DefaultPropertyBundle() {
//        override fun buildGuiControls(renderable: Renderable<*>, container: FlowLayout) {
//            IsometricUI.sectionHeader(container, "transform_options", false)
//            IsometricUI.intControl(container, this.scale, "scale", 10)
//        }

        override fun applyToViewMatrix(modelViewStack: Matrix4fStack) {
            val scale = this.scale.get() / 10000f
            modelViewStack.scale(scale, scale, -scale)

            modelViewStack.translate(this.xOffset.get() / 260f, this.yOffset.get() / -260f, 0f)
            modelViewStack.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(180f))
            modelViewStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(180f))
        }

        public companion object {
            public val INSTANCE: OwoUIComponentPropertyBundle = OwoUIComponentPropertyBundle()
        }
    }
}
