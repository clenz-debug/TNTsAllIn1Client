package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The pack screen places a newly selected pack itself (vanilla's {@code Pack.Position#insert}), then
 * pushes its list to the repository, where {@link PackRepositoryMixin} applies
 * {@link BundledResourcePacks#regroup}'s extra rule (dark mode on top of our bundled packs). This
 * pulls that corrected order back into the screen right away, so what's shown matches what's used.
 */
@Mixin(PackSelectionModel.class)
public class PackSelectionModelMixin {
	@Shadow
	@Final
	private PackRepository repository;

	@Shadow
	@Final
	List<Pack> selected;

	@Shadow
	@Final
	Consumer<PackSelectionModel.EntryBase> onListChanged;

	@Inject(method = "updateRepoSelectedList", at = @At("TAIL"))
	private void tntsallin1client$syncRegroupedOrder(CallbackInfo ci) {
		// The screen lists packs top-first, the repository bottom-first. Only the packs the screen already
		// shows - Fabric keeps its hidden per-mod packs out of this list on purpose.
		List<Pack> repositoryOrder = new ArrayList<>(this.repository.getSelectedPacks());
		repositoryOrder.retainAll(this.selected);
		Collections.reverse(repositoryOrder);
		if (!repositoryOrder.equals(this.selected)) {
			this.selected.clear();
			this.selected.addAll(repositoryOrder);
			this.onListChanged.accept(null);
		}
	}
}
