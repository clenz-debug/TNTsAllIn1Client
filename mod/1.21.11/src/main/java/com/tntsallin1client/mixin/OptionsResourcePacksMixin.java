package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@code updateResourcePacks} leaves every fixed-position pack out of options.txt - vanilla only
 * ever pins always-on packs like "Default", which get re-added on load anyway. Our pinned packs
 * (see {@link BundledResourcePacks}) are fixed but optional, so without this they'd never be saved,
 * and since the saved list then looks unchanged, the resource reload never runs either (pack shows
 * as selected, textures never load). WrapOperation rather than Redirect: Fabric API wraps this same
 * call to hide its own internal packs, and both wrappers have to chain.
 */
@Mixin(Options.class)
public class OptionsResourcePacksMixin {
	@WrapOperation(
			method = "updateResourcePacks",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/Pack;isFixedPosition()Z")
	)
	private boolean tntsallin1client$savePinnedPacks(Pack pack, Operation<Boolean> original) {
		return original.call(pack) && !BundledResourcePacks.isPinned(pack);
	}
}
