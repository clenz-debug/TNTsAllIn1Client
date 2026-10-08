package com.tntsallin1client.resourcepack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.UnopenedResourcePack;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * The resource packs that come with the client (the launcher puts them into the instance's
 * `resourcepacks/`), each known by how its file name begins - the rest is the Minecraft version. A
 * switch in the mod menu only puts such a pack into the game's list of active packs or takes it
 * out again - the same thing the game's own resource pack screen does.
 */
final class ClientPacks {
	/** What the game puts in front of the name of a pack from `resourcepacks/`. */
	private static final String FILE_PACK = "file/";

	private ClientPacks() {
	}

	private static PackRepository<UnopenedResourcePack> repository() {
		return Minecraft.getInstance().getResourcePackRepository();
	}

	private static UnopenedResourcePack find(String fileNameStart, Iterable<UnopenedResourcePack> packs) {
		for (UnopenedResourcePack pack : packs) {
			if (pack.getId().startsWith(FILE_PACK + fileNameStart)) {
				return pack;
			}
		}
		return null;
	}

	/** Whether such a pack is in `resourcepacks/`, as far as the game has looked. */
	static boolean isAvailable(String fileNameStart) {
		return find(fileNameStart, repository().getAvailable()) != null;
	}

	static boolean isEnabled(String fileNameStart) {
		return find(fileNameStart, repository().getSelected()) != null;
	}

	/** Switches the pack on or off, saves that in the game's options and loads the textures anew - which takes a moment. */
	static void setEnabled(String fileNameStart, boolean enabled) {
		apply(Collections.singletonMap(fileNameStart, enabled));
	}

	/**
	 * Switches several packs at once (file name start -> on or off), with a single reload - and none
	 * if nothing changes. Of packs switched on together, one named later ends up below one named
	 * earlier.
	 */
	static void apply(Map<String, Boolean> packs) {
		Minecraft client = Minecraft.getInstance();
		PackRepository<UnopenedResourcePack> repository = repository();
		List<UnopenedResourcePack> selected = new ArrayList<UnopenedResourcePack>(repository.getSelected());
		boolean changed = false;
		boolean rescanned = false;
		for (Map.Entry<String, Boolean> pack : packs.entrySet()) {
			UnopenedResourcePack active = find(pack.getKey(), selected);
			if (pack.getValue() == (active != null)) {
				continue;
			}
			if (pack.getValue()) {
				if (!rescanned) {
					// Looks through the folder again - the game only knows the packs that were there when it last did.
					repository.reload();
					rescanned = true;
				}
				UnopenedResourcePack available = find(pack.getKey(), repository.getAvailable());
				if (available == null) {
					continue;
				}
				// First in the list is lowest in rank. Ours goes right above the game's own packs: the
				// ones the player chose themselves stay on top of it.
				int index = 0;
				while (index < selected.size() && !selected.get(index).getId().startsWith(FILE_PACK)) {
					index++;
				}
				selected.add(index, available);
			} else {
				selected.remove(active);
			}
			changed = true;
		}
		if (!changed) {
			return;
		}

		repository.setSelected(selected);
		client.options.resourcePacks.clear();
		client.options.incompatibleResourcePacks.clear();
		for (UnopenedResourcePack pack : repository.getSelected()) {
			// The game's own packs are always there and not part of this list.
			if (!pack.isFixedPosition()) {
				client.options.resourcePacks.add(pack.getId());
				// A pack made for another version of the game (Bushy Vegetation's is one version ahead) has
				// to be named here too, or the game takes it off again at its next start.
				if (!pack.getCompatibility().isCompatible()) {
					client.options.incompatibleResourcePacks.add(pack.getId());
				}
			}
		}
		client.options.save();
		client.reloadResourcePacks();
	}
}
