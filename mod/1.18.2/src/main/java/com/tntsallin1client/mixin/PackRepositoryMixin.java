package com.tntsallin1client.mixin;

import com.google.common.collect.ImmutableList;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Keeps fixed-position packs (vanilla's "Default" plus our bundled ones, see
 * {@link BundledResourcePacks}) in their group no matter how the selection was built - an old
 * options.txt order or the pack screen's choice ({@code rebuildSelected}; 1.18.2's pack screen hands its
 * list over only when it closes) or a plain append from our client menu's toggles ({@code addPack},
 * which 1.18.2 does not have - {@code PackRepositoryAccessMixin} regroups there itself).
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {
	@Inject(method = "rebuildSelected", at = @At("RETURN"), cancellable = true)
	private void tntsallin1client$regroupRebuilt(CallbackInfoReturnable<List<Pack>> cir) {
		cir.setReturnValue(ImmutableList.copyOf(BundledResourcePacks.regroup(cir.getReturnValue())));
	}
}
