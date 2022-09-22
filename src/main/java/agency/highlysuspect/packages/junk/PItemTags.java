package agency.highlysuspect.packages.junk;

import agency.highlysuspect.packages.Packages;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class PItemTags {
	public static TagKey<Item> BANNED_FROM_PACKAGE_MAKER;
	public static TagKey<Item> BANNED_FROM_PACKAGE;
	public static TagKey<Item> THINGS_YOU_NEED_FOR_PACKAGE_CRAFTING;
	
	public static void onInitialize() {
		BANNED_FROM_PACKAGE_MAKER = TagKey.create(Registry.ITEM_REGISTRY, Packages.id("banned_from_package_maker"));
		BANNED_FROM_PACKAGE = TagKey.create(Registry.ITEM_REGISTRY, Packages.id("banned_from_package"));
		THINGS_YOU_NEED_FOR_PACKAGE_CRAFTING = TagKey.create(Registry.ITEM_REGISTRY, Packages.id("things_you_need_for_package_crafting"));
	}
}
