package agency.highlysuspect.packages.platform.fabric.client.model;

import agency.highlysuspect.packages.Packages;
import agency.highlysuspect.packages.client.PackageModelBakery;
import agency.highlysuspect.packages.junk.PackageStyle;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class is basically identical to FrapiMeshPackageMakerModel.
 * It uses PackageStyle instead of PackageMakerStyle, and uses a different style of choosing the model from the item stack.
 */
public class FrapiMeshPackageModel implements UnbakedModel {
	public static final ResourceLocation BLOCK_SPECIAL = Packages.id("special/package");
	public static final ResourceLocation ITEM_SPECIAL = Packages.id("item/package");
	
	private final PackageModelBakery.Factory<Mesh> bakeryFactory = new PackageModelBakery.Factory<>(Packages.id("block/package")) {
		@Override
		public PackageModelBakery<Mesh> make(BakedModel baseModel, TextureAtlasSprite specialFrameSprite, TextureAtlasSprite specialInnerSprite) {
			return new FrapiMeshModelBakery(baseModel, specialFrameSprite, specialInnerSprite);
		}
	};
	
	@Override
	public Collection<ResourceLocation> getDependencies() {
		return bakeryFactory.getDependencies();
	}
	
	@Override
	public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
		return bakeryFactory.getMaterials(unbakedModelGetter, unresolvedTextureReferences);
	}
	
	@Override
	public BakedModel bake(ModelBakery loader, Function<Material, TextureAtlasSprite> textureGetter, ModelState rotationContainer, ResourceLocation modelId) {
		return new Baked(bakeryFactory.make(loader, textureGetter, rotationContainer, modelId));
	}
	
	private static class Baked extends ForwardingBakedModel {
		public Baked(PackageModelBakery<Mesh> bakery) {
			this.wrapped = bakery.getBaseModel();
			this.bakery = bakery;
		}
		
		private final PackageModelBakery<Mesh> bakery;
		
		@Override
		public boolean isVanillaAdapter() {
			return false;
		}
		
		@Override
		public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
			if(((RenderAttachedBlockView) blockView).getBlockEntityRenderAttachment(pos) instanceof PackageStyle style) {
				context.meshConsumer().accept(bakery.bake(style));
			}
		}
		
		@Override
		public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
			context.meshConsumer().accept(bakery.bake(PackageStyle.fromItemStack(stack)));
		}
	}
}