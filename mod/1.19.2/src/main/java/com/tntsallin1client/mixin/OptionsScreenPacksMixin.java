package com.tntsallin1client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Saving the pack screen's choice leaves every fixed-position pack out of options.txt - vanilla only
 * ever pins always-on packs like "Default", which get re-added on load anyway. Our pinned packs (see
 * {@link BundledResourcePacks}) are fixed but optional, so without this they'd never be saved, and
 * since the saved list then looks unchanged, the resource reload never runs either (pack shows as
 * selected, textures never load). In 1.19.2 that saving is the options screen's own
 * {@code updatePackList} (1.19.3 moved it into {@code Options#updateResourcePacks}, where the newer
 * versions hook it). WrapOperation so another mod's wrapper of the same call still chains.
 */
@Mixin(OptionsScreen.class)
public class OptionsScreenPacksMixin {
	@WrapOperation(
			method = "updatePackList",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/Pack;isFixedPosition()Z")
	)
	private boolean tntsallin1client$savePinnedPacks(Pack pack, Operation<Boolean> original) {
		return original.call(pack) && !BundledResourcePacks.isPinned(pack);
	}
}
