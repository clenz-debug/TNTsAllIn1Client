package com.tntsallin1client.compat;

import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;

/**
 * Continuity's own two resource packs: the connected glass and bookshelf textures, and the fix for
 * glass panes next to them. Without them the mod has nothing to connect. The Continuity builds for the
 * newer versions come with both switched on; the 1.19.2 (2.0.x), 1.19.4 and 1.20.1 builds leave them for the player to find in
 * the resource pack screen (own user report: the "Connected Textures" switch did nothing) - so here
 * the client switches them on itself.
 */
public final class ContinuityPacks {
	/**
	 * Fabric API for 1.19.2 names a mod's built-in pack "namespace/path" (from 1.19.3 on it is the
	 * identifier, "continuity:default") - the newer ids found nothing here (own user report).
	 */
	private static final List<String> PACK_IDS = List.of("continuity/default", "continuity/glass_pane_culling_fix");

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
