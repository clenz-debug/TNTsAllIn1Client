package com.tntsallin1client.compat;

import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;

/**
 * Continuity's own two resource packs: the connected glass and bookshelf textures, and the fix for
 * glass panes next to them. Without them the mod has nothing to connect. The Continuity builds for the
 * newer versions come with both switched on; the 1.21.1 build leaves them for the player to find in
 * the resource pack screen (own user report: the "Connected Textures" switch did nothing) - so here
 * the client switches them on itself.
 */
public final class ContinuityPacks {
	private static final List<String> PACK_IDS = List.of("continuity:default", "continuity:glass_pane_culling_fix");

	private ContinuityPacks() {
	}

	/**
	 * Selects whichever of the packs is there and not selected yet.
	 *
	 * @return whether the selection changed - the caller then has the game reload its resources
	 */
	public static boolean select(PackRepository repository) {
		boolean changed = false;
		for (String id : PACK_IDS) {
			if (repository.isAvailable(id) && !repository.getSelectedIds().contains(id)) {
				changed |= repository.addPack(id);
			}
		}
		return changed;
	}
}
