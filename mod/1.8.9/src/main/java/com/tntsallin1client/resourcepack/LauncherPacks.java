package com.tntsallin1client.resourcepack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.ResourcePackLoader;

/**
 * The resource packs that come with the client - the ones the launcher builds into this instance
 * ({@link NewTextures}, {@link DarkMode}) and the mod's own ({@link Blocks3d}) - each known by its
 * name in `resourcepacks/`. A switch in the mod menu only puts such a
 * pack into the game's list of active packs or takes it out again - the same thing the game's own
 * resource pack screen does.
 */
final class LauncherPacks {
	/** What the game marks a pack with that was made for this version. */
	private static final int PACK_FORMAT = 1;

	private LauncherPacks() {
	}

	/** The pack's place in `resourcepacks/`, whether or not it is there. */
	static File folder(String packName) {
		return new File(MinecraftClient.getInstance().getResourcePackLoader().getResourcePackDir(), packName);
	}

	/** Whether a pack by that name is in `resourcepacks/` - a folder, or a zip file. */
	static boolean isAvailable(String packName) {
		return folder(packName).exists();
	}

	static boolean isEnabled(String packName) {
		return find(packName, MinecraftClient.getInstance().getResourcePackLoader().getSelectedResourcePacks()) != null;
	}

	/** Switches the pack on or off, saves that in the game's options and loads the textures anew - which takes a moment. */
	static void setEnabled(String packName, boolean enabled) {
		apply(Collections.singletonMap(packName, enabled));
	}

	/** Switches several packs at once (name -> on or off), with a single reload - and none if nothing changes. */
	static void apply(Map<String, Boolean> packs) {
		MinecraftClient client = MinecraftClient.getInstance();
		ResourcePackLoader loader = client.getResourcePackLoader();
		List<ResourcePackLoader.Entry> selected = new ArrayList<ResourcePackLoader.Entry>(loader.getSelectedResourcePacks());
		boolean changed = false;
		boolean rescanned = false;
		for (Map.Entry<String, Boolean> pack : packs.entrySet()) {
			ResourcePackLoader.Entry active = find(pack.getKey(), selected);
			if (pack.getValue() == (active != null)) {
				continue;
			}
			if (pack.getValue()) {
				if (!rescanned) {
					// Looks through the folder again - the game only knows the packs that were there when it last did.
					loader.initResourcePacks();
					rescanned = true;
				}
				ResourcePackLoader.Entry available = find(pack.getKey(), loader.getAvailableResourcePacks());
				if (available == null) {
					continue;
				}
				// First in the list is lowest in rank: packs the player chose themselves stay on top of ours.
				selected.add(0, available);
			} else {
				selected.remove(active);
			}
			changed = true;
		}
		if (!changed) {
			return;
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

	private static ResourcePackLoader.Entry find(String packName, List<ResourcePackLoader.Entry> packs) {
		for (ResourcePackLoader.Entry pack : packs) {
			if (packName.equals(pack.getName())) {
				return pack;
			}
		}
		return null;
	}
}
