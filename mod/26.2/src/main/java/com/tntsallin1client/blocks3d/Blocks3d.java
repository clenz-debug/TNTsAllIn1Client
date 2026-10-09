package com.tntsallin1client.blocks3d;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.Collection;
import java.util.List;

/**
 * Our own 3D block models (resource pack "TNT 3D-Blöcke", resourcepacks/3d-blocks/ in the repo).
 * Most of it is a plain resource pack; the few things Minecraft draws in code instead of block
 * models are switched on together with it (up to 26.1 also the hanging sign chains drawn in code;
 * since 26.2 signs are plain block models, so 3D chains belong into the resource pack).
 *
 * The client menu's "3D block models" row switches this pack together with optional ones, which its
 * options screen can turn off on their own - remembered in {@link ClientConfig}, so switching the
 * row back on brings back exactly the chosen ones.
 */
public final class Blocks3d {
	/** "file/" + the bundled file name (vanilla's FolderRepositorySource scheme). */
	public static final String PACK_ID = "file/TNT-3D-Blocks-26.2.zip";
	/** Our 3D bushes, a pack of their own so they can be switched on their own (own user request). */
	public static final String BUSHES_PACK_ID = "file/TNT-3D-Bushes-26.2.zip";
	/** Every pack the "3D block models" row switches. */
	public static final List<String> ALL_PACK_IDS = List.of(PACK_ID, BUSHES_PACK_ID);

	private static Collection<Pack> lastSelection;
	private static boolean active;

	private Blocks3d() {
	}

	/** Whether the "3D block models" row is on: any of its packs selected. */
	public static boolean anySelected(PackRepository repository) {
		return ALL_PACK_IDS.stream().anyMatch(repository.getSelectedIds()::contains);
	}

	/** Switches the whole row: on = this pack plus the optional ones chosen in its options screen. */
	public static void setEnabled(PackRepository repository, boolean enabled) {
		ALL_PACK_IDS.forEach(repository::removePack);
		if (enabled) {
			ClientConfig config = ClientConfig.get();
			repository.addPack(PACK_ID);
			if (config.blockModels3dBushes) {
				repository.addPack(BUSHES_PACK_ID);
			}
		}
	}

	/** One optional pack, chosen in the options screen: switched right away while the row is on. */
	public static void setOptional(PackRepository repository, String packId, boolean enabled) {
		if (!enabled) {
			repository.removePack(packId);
		} else if (repository.getSelectedIds().contains(PACK_ID)) {
			repository.addPack(packId);
		}
	}

	/**
	 * Whether the pack is switched on right now. Checked while rendering, so it's cached: Minecraft
	 * replaces its selection list whenever packs are switched, a new list is the only reason to look again.
	 */
	public static boolean active() {
		Collection<Pack> selection = Minecraft.getInstance().getResourcePackRepository().getSelectedPacks();
		if (selection != lastSelection) {
			lastSelection = selection;
			active = selection.stream().anyMatch(pack -> PACK_ID.equals(pack.getId()));
		}
		return active;
	}
}
