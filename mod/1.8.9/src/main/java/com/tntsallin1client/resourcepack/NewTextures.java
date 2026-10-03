package com.tntsallin1client.resourcepack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.ResourcePackLoader;

/**
 * "New Textures": Minecraft's current look for this version's blocks, items and mobs, as a switch in
 * the mod menu. The textures themselves are a resource pack the launcher builds into this instance
 * from the player's own copy of the newest Minecraft version (`newTexturesPack.ts` in the launcher) -
 * the switch only puts that pack into the game's list of active packs or takes it out again, the
 * same thing the game's own resource pack screen does.
 *
 * <p>Without the launcher (or before it could build the pack) there is no such pack, and the mod
 * menu has no row for it.
 */
public final class NewTextures {
	/** The pack's folder in `resourcepacks/` - `NEW_TEXTURES_PACK_NAME` in the launcher. */
	public static final String PACK_NAME = "TNT New Textures";
	/** What the game marks a pack with that was made for this version. */
	private static final int PACK_FORMAT = 1;

	private NewTextures() {
	}

	/** Whether the launcher has put the pack into this instance. */
	public static boolean isAvailable() {
		return new File(MinecraftClient.getInstance().getResourcePackLoader().getResourcePackDir(), PACK_NAME).isDirectory();
	}

	public static boolean isEnabled() {
		return find(MinecraftClient.getInstance().getResourcePackLoader().getSelectedResourcePacks()) != null;
	}

	/** Switches the pack on or off, saves that in the game's options and loads the textures anew - which takes a moment. */
	public static void setEnabled(boolean enabled) {
		MinecraftClient client = MinecraftClient.getInstance();
		ResourcePackLoader loader = client.getResourcePackLoader();
		List<ResourcePackLoader.Entry> selected = new ArrayList<ResourcePackLoader.Entry>(loader.getSelectedResourcePacks());
		ResourcePackLoader.Entry active = find(selected);
		if (enabled == (active != null)) {
			return;
		}
		if (enabled) {
			// Looks through the folder again - the game only knows the packs that were there when it last did.
			loader.initResourcePacks();
			ResourcePackLoader.Entry pack = find(loader.getAvailableResourcePacks());
			if (pack == null) {
				return;
			}
			// First in the list is lowest in rank: packs the player chose themselves stay on top of ours.
			selected.add(0, pack);
		} else {
			selected.remove(active);
		}

		loader.setSelectedResourcePacks(selected);
		client.options.resourcePacks.clear();
		client.options.incompatibleResourcePacks.clear();
		for (ResourcePackLoader.Entry entry : selected) {
			client.options.resourcePacks.add(entry.getName());
			if (entry.getFormat() != PACK_FORMAT) {
				client.options.incompatibleResourcePacks.add(entry.getName());
			}
		}
		client.options.save();
		client.reloadResources();
	}

	private static ResourcePackLoader.Entry find(List<ResourcePackLoader.Entry> packs) {
		for (ResourcePackLoader.Entry pack : packs) {
			if (PACK_NAME.equals(pack.getName())) {
				return pack;
			}
		}
		return null;
	}
}
