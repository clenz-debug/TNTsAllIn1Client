package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gives our launcher-bundled resource packs vanilla's "Default"-pack placement (fixed, bottom) -
 * see {@link BundledResourcePacks}. {@code readMetaAndCreate} is the one factory every folder pack
 * goes through.
 */
@Mixin(Pack.class)
public class PackMixin {
	@ModifyArg(
			method = "readMetaAndCreate",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/Pack;<init>(Lnet/minecraft/server/packs/PackLocationInfo;Lnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/repository/Pack$Metadata;Lnet/minecraft/server/packs/PackSelectionConfig;)V"),
			index = 3
	)
	private static PackSelectionConfig tntsallin1client$pinBundledPacks(PackLocationInfo location, Pack.ResourcesSupplier resources, Pack.Metadata metadata, PackSelectionConfig selectionConfig) {
		return BundledResourcePacks.selectionConfigFor(location, selectionConfig);
	}
}
