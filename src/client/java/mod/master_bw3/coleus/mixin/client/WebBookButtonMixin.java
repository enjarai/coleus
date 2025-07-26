package mod.master_bw3.coleus.mixin.client;

import io.wispforest.lavender.book.Book;
import io.wispforest.lavender.client.LavenderBookScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.parsing.UIModel;
import mod.master_bw3.coleus.ColeusClient;
import mod.master_bw3.coleus.internal.mixin_impl.WebBookButtonMixinImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LavenderBookScreen.class)
public abstract class WebBookButtonMixin {
    @Final
    @Shadow
    public boolean isOverlay;

    @Final
    @Shadow
    public Book book;

    @Inject(method = "build(Lio/wispforest/owo/ui/container/FlowLayout;)V",
            at = @At(value = "INVOKE", target = "Lio/wispforest/lavender/client/LavenderBookScreen;rebuildContent(Lnet/minecraft/sound/SoundEvent;)V"))
    private void addWebBookButton(FlowLayout rootComponent, CallbackInfo ci) {
        if (!this.isOverlay) {
            this.component(FlowLayout.class, "primary-panel").child(
                    ((Component) ((ButtonComponent) this.template("web-book-button")).onPress(buttonComponent -> {
                        WebBookButtonMixinImpl.INSTANCE.webBookButtonCallback(book);
                    })).id("web-book-button").positioning(Positioning.relative(104, 90))
            );
        }
    }

    @Unique
    private Component template(String name) {
        UIModel model = null;
        var templates = MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of(ColeusClient.NAME, "owo_ui/templates.xml"));
        try (var inputStream = templates.get().getInputStream()) {
            model = UIModel.load(inputStream);
        } catch (Exception e) {
            ColeusClient.INSTANCE.getLogger$coleus_client().atError().log(e.toString());
        }

        return model.expandTemplate(Component.class, name, Map.of());
    }

    @Shadow
    protected abstract <C extends Component> @NotNull C component(Class<C> expectedClass, String id);
}
