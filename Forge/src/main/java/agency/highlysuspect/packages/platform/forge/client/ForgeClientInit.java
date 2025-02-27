package agency.highlysuspect.packages.platform.forge.client;

import agency.highlysuspect.packages.Packages;
import agency.highlysuspect.packages.client.PackagesClient;
import agency.highlysuspect.packages.config.ConfigSchema;
import agency.highlysuspect.packages.net.ActionPacket;
import agency.highlysuspect.packages.platform.RegistryHandle;
import agency.highlysuspect.packages.platform.client.ClientsideHoldLeftClickCallback;
import agency.highlysuspect.packages.platform.client.ClientsideUseBlockCallback;
import agency.highlysuspect.packages.platform.client.EarlyClientsideLeftClickCallback;
import agency.highlysuspect.packages.platform.client.MyScreenConstructor;
import agency.highlysuspect.packages.platform.forge.ForgeBackedConfig;
import agency.highlysuspect.packages.platform.forge.ForgeInit;
import agency.highlysuspect.packages.platform.forge.client.model.NoConfigGeometryLoader;
import agency.highlysuspect.packages.platform.forge.client.model.ForgePackageMakerModel;
import agency.highlysuspect.packages.platform.forge.client.model.ForgePackageModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeClientInit extends PackagesClient {
	public static ForgeClientInit instanceForge;
	
	private final ForgeConfigSpec.Builder forgeSpec = new ForgeConfigSpec.Builder();
	
	private final List<MenuScreenEntry<?, ?>> menuScreensToRegister = new ArrayList<>();
	private final Map<ResourceLocation, List<ResourceLocation>> spritesToBake = new HashMap<>();
	private final List<BlockEntityRendererEntry<?>> blockEntityRenderersToRegister = new ArrayList<>();
	private final Map<RegistryHandle<? extends Block>, RenderType> renderTypesToRegister = new HashMap<>();
	
	/** @see agency.highlysuspect.packages.platform.forge.mixin.MixinMinecraft */
	public final List<EarlyClientsideLeftClickCallback> earlyLeftClickCallbacks = new ArrayList<>();
	
	/** @see agency.highlysuspect.packages.platform.forge.mixin.MixinMultiPlayerGameMode */
	public final List<ClientsideHoldLeftClickCallback> holdLeftClickCallbacksForCreativeMode = new ArrayList<>();
	
	private static record MenuScreenEntry<T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>>(RegistryHandle<MenuType<T>> type, MyScreenConstructor<T, U> cons) {
		private void register() { MenuScreens.register(type.get(), cons::create); } //generics moment
	}
	
	private static record BlockEntityRendererEntry<T extends BlockEntity>(RegistryHandle<? extends BlockEntityType<T>> type, BlockEntityRendererProvider<? super T> renderer) {
		private void register(EntityRenderersEvent.RegisterRenderers e) { e.registerBlockEntityRenderer(type.get(), renderer); } //generics moment
	}
	
	public ForgeClientInit() {
		if(instanceForge != null) throw new IllegalStateException("Packages ForgeClientInit instantiated twice!");
		instanceForge = this;
		
		earlySetup();
		
		//finish up config (earlySetup populated the forgeSpec)
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, forgeSpec.build(), "packages-client.toml");
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadConfig);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onReloadConfig);
		
		//misc events (generally, toppling the dominoes that earlySetup stood)
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::actuallyRegisterMenuScreens);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::actuallyBakeSpritesOnto);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::actuallySetBlockEntityRenderers);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::actuallySetRenderTypes);
	}
	
	@Override
	public void setupCustomModelLoaders() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener((ModelEvent.RegisterGeometryLoaders e) -> {
			e.register("forge_package_model_loader"      , new NoConfigGeometryLoader<>(ForgePackageModel::new));
			e.register("forge_package_maker_model_loader", new NoConfigGeometryLoader<>(ForgePackageMakerModel::new));
		});
	}
	
	///
	
	@Override
	public <T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> void registerMenuScreen(RegistryHandle<MenuType<T>> type, MyScreenConstructor<T, U> cons) {
		menuScreensToRegister.add(new MenuScreenEntry<>(type, cons));
	}
	
	@Override
	public <T extends BlockEntity> void setBlockEntityRenderer(RegistryHandle<? extends BlockEntityType<T>> type, BlockEntityRendererProvider<? super T> renderer) {
		blockEntityRenderersToRegister.add(new BlockEntityRendererEntry<>(type, renderer));
	}
	
	@Override
	public void setRenderType(RegistryHandle<? extends Block> block, RenderType type) {
		renderTypesToRegister.put(block, type);
	}
	
	@Override
	public void installEarlyClientsideLeftClickCallback(EarlyClientsideLeftClickCallback callback) {
		//I was going to be like "hey, credit where it's due, I needed a kludge for this on Fabric".
		//Turns out that ClickInputEvent is fired in both Minecraft#startAttack ***and Minecraft#continueAttack***???
		//This is the exact situation I am trying to use lower-level input-based events to *avoid*!!!
		//
		//Cancelling mining the block from the LeftClickBlock event (which is more of a "start mining block" event)
		//kinda causes you to restart trying to mine the block every tick, which is a problem when I am only trying
		//to detect the first left click. This makes sense tbh and happens on Fabric too.
		//That's why I use a lower-level click event anyway, I'm only interested in the first time you try to
		//mine the block, not all the other spam times.
		//
		//But because Forge fires this event in continueAttack as well, completely defeating the purpose
		//of offering a click event separate from LeftClickBlock in the first place, mixin it is then. Auuhgh.
		//
		//See MixinMinecraft.
		earlyLeftClickCallbacks.add(callback);
	}
	
	@Override
	public void installClientsideHoldLeftClickCallback(ClientsideHoldLeftClickCallback callback) {
		//I originally only used PlayerInteractEvent.LeftClickBlock in all situations. However, in Creative mode, this
		//event is fired *after* sending the START_DESTROY_BLOCK action to the server, and there is no way to cancel the
		//event in a way that suppresses the packet. (See Forge's patches to MultiPlayerGameMode, ctrl-f for "LeftClick".)
		//
		//I need to suppress the packet because sending the action seems to tick up the serverside creative mode breaking
		//timer, or something? Basically, after clicking the package for more than ~5ish cumulative ticks, it would break.
		//
		//This isn't an issue on Fabric because cancelling the "i am about to start breaking this block" event on the client
		//suppresses the "i am about to start breaking this block" packet too, which is arguably the correct behavior.
		//I'm not sure in what cases Forge behavior makes sense.
		//
		//See MixinMultiPlayerGameMode.
		holdLeftClickCallbacksForCreativeMode.add(callback);
		
		//Still, let's try to be good netizens and use the standard LeftClickBlock event in noncreative.
		//In non creative gamemodes it works just fine for my purposes.
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
			if(event.isCanceled() || event.getSide() != LogicalSide.CLIENT || event.getEntity().isSpectator()) return;
			if(event.getEntity().isCreative()) return; //Creative mode handled via mixin
			
			InteractionResult r = callback.interact(event.getEntity(), event.getLevel(), event.getHand(), event.getPos(), event.getFace());
			if(r.consumesAction()) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.CONSUME);
				event.setUseBlock(Event.Result.DENY); //I handled it
				event.setUseItem(Event.Result.DENY);
			}
		});
	}
	
	@Override
	public void installClientsideUseBlockCallback(ClientsideUseBlockCallback callback) {
		//I originally tried PlayerInteractEvent.RightClickBlock. However, there is also no way to cancel this event
		//in a way that suppresses the "i just right clicked this item" packet as well. This is on purpose?
		//There's a line of code in MultiPlayerGameMode#useItemOn that sends a use-item packet when the event *is* cancelled.
		//This honestly just seems like a mistake, or poor api design that I can't wrap my head around.
		
		MinecraftForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
			if(event.isCanceled() || !event.isUseItem()) return;
			
			Minecraft minecraft = Minecraft.getInstance();
			Player player = minecraft.player;
			Level level = minecraft.level;
			HitResult hit = minecraft.hitResult;
			if(player == null || level == null || !(hit instanceof BlockHitResult bhr)) return;
			
			//TODO: Since I can't use RightClickBlock, I might have to reimplement hand logic?
			//In practice the mod's "take items from places other than the active hand" system seems to work okay
			InteractionResult r = callback.interact(player, level, InteractionHand.MAIN_HAND, bhr);
			if(r.consumesAction()) event.setCanceled(true);
		});
	}
	
	@Override
	public void sendActionPacket(ActionPacket packet) {
		ForgeInit.instanceForge.channel.sendToServer(packet);
	}
	
	private void onLoadConfig(ModConfigEvent.Loading e) {
		if(e.getConfig().getModId().equals(Packages.MODID)) refreshConfig();
	}
	
	private void onReloadConfig(ModConfigEvent.Reloading e) {
		if(e.getConfig().getModId().equals(Packages.MODID)) refreshConfig();
	}
	
	private void actuallyRegisterMenuScreens(FMLClientSetupEvent e) {
		menuScreensToRegister.forEach(MenuScreenEntry::register);
	}
	
	private void actuallyBakeSpritesOnto(TextureStitchEvent/*.Pre*/ event) {
		//List<ResourceLocation> sprites = spritesToBake.get(event.getAtlas().location());
		//if(sprites != null) sprites.forEach(event::addSprite);
		
		// "Use atlas info JSON files instead"
	}
	
	private void actuallySetBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		blockEntityRenderersToRegister.forEach(entry -> entry.register(event));
	}
	
	@SuppressWarnings("removal") //ItemBlockRenderTypes is deprecated in favor of some bespoke forge json bullshit, no thanks
	private void actuallySetRenderTypes(FMLClientSetupEvent e) {
		renderTypesToRegister.forEach((handle, layer) -> ItemBlockRenderTypes.setRenderLayer(handle.get(), layer));
	}
	
	@Override
	public ConfigSchema.Bakery clientConfigBakery() {
		return new ForgeBackedConfig.Bakery(forgeSpec);
	}
}
