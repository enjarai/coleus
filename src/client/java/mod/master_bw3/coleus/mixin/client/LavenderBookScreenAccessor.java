package mod.master_bw3.coleus.mixin.client;

import io.wispforest.lavender.client.LavenderBookScreen;
import io.wispforest.lavender.md.features.RecipeFeature;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(LavenderBookScreen.class)
public interface LavenderBookScreenAccessor {

	@Accessor(value = "RECIPE_HANDLERS", remap = false)
	static Map<Identifier, Map<RecipeType<?>, RecipeFeature.RecipePreviewBuilder<?>>> getRecipeHandler() {
		throw new AssertionError();
	}
}