package com.tntsallin1client.resourcepack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "New Textures": Minecraft's current look for this version's blocks, items and mobs, as a switch in
 * the mod menu. The textures themselves are a resource pack the launcher builds into this instance
 * from the player's own copy of the newest Minecraft version (`newTexturesPack.ts` in the launcher) -
 * the switch only puts that pack into the game's list of active packs or takes it out again (see
 * {@link LauncherPacks}).
 *
 * <p>Without the launcher (or before it could build the pack) there is no such pack, and the mod
 * menu has no row for it.
 */
public final class NewTextures {
	/** The pack's folder in `resourcepacks/` - `NEW_TEXTURES_PACK_NAME` in the launcher. */
	public static final String PACK_NAME = "TNT New Textures";

	private NewTextures() {
	}

	/** Whether the launcher has put the pack into this instance. */
	public static boolean isAvailable() {
		return LauncherPacks.isAvailable(PACK_NAME);
	}

	public static boolean isEnabled() {
		return LauncherPacks.isEnabled(PACK_NAME);
	}

	/** Also swaps the 3D block models, where they are on, for the ones shaped after the pictures now shown (see {@link Blocks3d}). */
	public static void setEnabled(boolean enabled) {
		Map<String, Boolean> packs = new LinkedHashMap<String, Boolean>();
		packs.put(PACK_NAME, enabled);
		if (Blocks3d.isEnabled()) {
			packs.putAll(Blocks3d.packsFor(true, enabled));
		}
		LauncherPacks.apply(packs);
	}
}
