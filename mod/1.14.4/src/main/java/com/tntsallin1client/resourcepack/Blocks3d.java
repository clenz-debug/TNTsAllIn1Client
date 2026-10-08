package com.tntsallin1client.resourcepack;

import java.util.LinkedHashMap;
import java.util.Map;

import com.tntsallin1client.config.ClientConfig;

/**
 * "3D Block Models": our own 3D models for a number of blocks (ladders, rails, doors, bars, ...),
 * as a switch in the mod menu. They are a resource pack (`resourcepacks/3d-blocks/early.py`), which
 * the launcher puts into the instance - the switch only puts that pack into the game's list of
 * active packs or takes it out again (see {@link ClientPacks}). The blocks' items are 3D with it
 * too; what the inventory and the hand show of them is {@link Items3d}'s business.
 *
 * <p>The row switches an optional pack along with ours, as in the newer versions: the third-party
 * Bushy Vegetation (bushier grass, ferns, leaves, flowers), which the row's options screen can turn
 * off on its own - remembered in {@link ClientConfig}, so switching the row back on brings back
 * what was chosen.
 */
public final class Blocks3d {
	/** How the packs' file names begin - the rest is the version. */
	private static final String PACK_FILE = "TNT-3D-Blocks-";
	private static final String BUSHY_VEGETATION_FILE = "Bushy-Vegetation-";

	private Blocks3d() {
	}

	/** Whether the launcher has put the pack into this instance. */
	public static boolean isAvailable() {
		return ClientPacks.isAvailable(PACK_FILE);
	}

	public static boolean isEnabled() {
		return ClientPacks.isEnabled(PACK_FILE);
	}

	/** Switches the whole row: on = our pack plus Bushy Vegetation if it is chosen in the options screen. */
	public static void setEnabled(boolean enabled) {
		Map<String, Boolean> packs = new LinkedHashMap<String, Boolean>();
		// Ours first: Bushy Vegetation goes below it, so where both change a block (vines, sugar cane) ours shows.
		packs.put(PACK_FILE, enabled);
		packs.put(BUSHY_VEGETATION_FILE, enabled && ClientConfig.get().blockModels3dBushyVegetation);
		ClientPacks.apply(packs);
	}

	public static boolean isBushyVegetationAvailable() {
		return ClientPacks.isAvailable(BUSHY_VEGETATION_FILE);
	}

	/** Chosen in the options screen: switched right away while the row is on. */
	public static void setBushyVegetation(boolean enabled) {
		ClientConfig.get().blockModels3dBushyVegetation = enabled;
		if (isEnabled()) {
			ClientPacks.setEnabled(BUSHY_VEGETATION_FILE, enabled);
		}
	}
}
