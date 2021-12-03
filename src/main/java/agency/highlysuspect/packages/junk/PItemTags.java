package agency.highlysuspect.packages.junk;

import agency.highlysuspect.packages.PackagesInit;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;

public class PItemTags {
	public static Tag<Item> BANNED_FROM_PACKAGE_MAKER;
	
	public static void onInitialize() {
		BANNED_FROM_PACKAGE_MAKER = TagRegistry.item(new ResourceLocation(PackagesInit.MODID, "banned_from_package_maker"));
	}
}
