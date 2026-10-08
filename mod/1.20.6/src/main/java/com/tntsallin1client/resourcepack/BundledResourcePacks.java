package com.tntsallin1client.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Keeps the resource packs the launcher bundles for our own features (dark mode, 3D block models,
 * ...) plus Continuity's built-in packs pinned directly above vanilla's "Default" pack and below
 * every pack the user picks themselves, per user request - so a user's own texture pack always wins
 * over ours, and ours are never accidentally shuffled around.
 *
 * Uses vanilla's own mechanism for this instead of anything custom: the built-in "Default" pack is
 * {@code fixedPosition} + {@link Pack.Position#BOTTOM}, and a pack with the same selection config
 * gets inserted right above it by {@link Pack.Position#insert} and can't be moved past by the
 * pack-selection screen's arrows. Which folder packs count as "ours" comes from the launcher, which
 * writes the filenames it just synced from its resourcepacks bundle into {@link #FILE} on every
 * launch (see {@code bundleSync.ts#syncBundledContent}) - the mod itself never hardcodes that list,
 * so a bundle update with renamed/added packs needs no mod change.
 */
public final class BundledResourcePacks {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-bundled-resourcepacks.json");
	/** Same as vanilla's "Default" pack minus {@code required} - the user can still turn ours off. */
	private static final PackSelectionConfig PINNED_ABOVE_DEFAULT = new PackSelectionConfig(false, Pack.Position.BOTTOM, true);

	/** Our own packs' file names (the 3D blocks, the 3D bushes). */
	private static final String OWN_PACK_PREFIX = "file/TNT-";
	/** "file/" + our own dark mode pack's file name (resourcepacks/dark-mode), which ends in the Minecraft version. */
	private static final String DARK_MODE_PACK_PREFIX = "file/TNT-Dark-Mode-";
	/** The third-party "Default Dark Mode" pack ours replaces - still recognized as long as an instance has it. */
	private static final String LEGACY_DARK_MODE_PACK_PREFIX = "file/Default-Dark-Mode-";

	private static Set<String> bundledFileNames;

	private BundledResourcePacks() {
	}

	/** The selection config a pack should get - {@link #PINNED_ABOVE_DEFAULT} for ours, {@code original} for everything else. */
	public static PackSelectionConfig selectionConfigFor(PackLocationInfo location, PackSelectionConfig original) {
		if (isContinuityPack(location.id())) {
			return PINNED_ABOVE_DEFAULT;
		}
		// PackSource.DEFAULT + "file/" is the resourcepacks folder (see Minecraft's FolderRepositorySource
		// setup) - keeps a world's datapacks folder, which uses the same "file/" id scheme, out of this.
		if (location.source() != PackSource.DEFAULT || !location.id().startsWith("file/")) {
			return original;
		}
		return bundledFileNames().contains(location.id().substring("file/".length())) ? PINNED_ABOVE_DEFAULT : original;
	}

	/**
	 * Selected packs (bottom-first) regrouped into: the foundation (vanilla's "Default" plus Fabric's
	 * hidden per-mod resource packs, which Fabric itself sorts right above "Default"), then ours, then
	 * the user's own packs, then fixed-top packs - each group in its existing relative order. Needed
	 * on top of the selection config itself because {@code PackRepository#addPack} (used by our
	 * client menu's pack toggles) just appends to the top, an order saved in options.txt before this
	 * existed is restored as-is, and vanilla's insert logic would put ours below Fabric's mod packs.
	 *
	 * Within ours, per user request: Continuity's packs always lowest, the dark-mode pack always
	 * highest - it only works when no other pack of ours overrides it.
	 */
	public static List<Pack> regroup(List<Pack> selected) {
		List<Pack> foundation = new ArrayList<>();
		List<Pack> continuity = new ArrayList<>();
		List<Pack> ours = new ArrayList<>();
		List<Pack> darkMode = new ArrayList<>();
		List<Pack> user = new ArrayList<>();
		List<Pack> top = new ArrayList<>();
		for (Pack pack : selected) {
			if (isPinned(pack)) {
				if (isContinuityPack(pack.getId())) {
					continuity.add(pack);
				} else if (isDarkModePack(pack)) {
					darkMode.add(pack);
				} else {
					ours.add(pack);
				}
			} else if (pack.isFixedPosition()) {
				(pack.getDefaultPosition() == Pack.Position.BOTTOM ? foundation : top).add(pack);
			} else if (pack.isRequired() && user.isEmpty()) {
				// Fabric's per-mod resource packs: required but not fixed, auto-placed above their parent
				// pack - "Default" for the main ones. The ones above a user pack (e.g. Programmer Art's
				// mod variants) stay with that pack instead.
				foundation.add(pack);
			} else {
				user.add(pack);
			}
		}
		foundation.addAll(continuity);
		// Our own packs above the third-party ones, each group in its order: Bushy Vegetation has models
		// of its own for bushes, vines and sugar cane - switching it off and on again (it's switchable on
		// its own) put it on top, and its models won over ours. The sort is stable.
		ours.sort(Comparator.comparing(pack -> pack.getId().startsWith(OWN_PACK_PREFIX)));
		foundation.addAll(ours);
		foundation.addAll(darkMode);
		foundation.addAll(user);
		foundation.addAll(top);
		return foundation;
	}

	/** Whether this is one of our pinned packs (fixed like "Default", but optional). */
	public static boolean isPinned(Pack pack) {
		return pack.selectionConfig().equals(PINNED_ABOVE_DEFAULT);
	}

	/** Continuity's built-in packs ("continuity:default", "continuity:glass_pane_culling_fix", ...), registered through Fabric's built-in resource pack API. */
	private static boolean isContinuityPack(String packId) {
		return packId.startsWith("continuity:");
	}

	/** Our own dark mode pack, or the third-party one it replaces - any version of either. */
	public static boolean isDarkModePackId(String packId) {
		return packId.startsWith(DARK_MODE_PACK_PREFIX) || packId.startsWith(LEGACY_DARK_MODE_PACK_PREFIX);
	}

	private static boolean isDarkModePack(Pack pack) {
		return isDarkModePackId(pack.getId());
	}

	/** The dark mode pack the client menu's switch turns on: ours if the launcher bundled it, else the third-party one, else null. */
	public static String darkModePackId(PackRepository repository) {
		return firstAvailable(repository, DARK_MODE_PACK_PREFIX)
				.or(() -> firstAvailable(repository, LEGACY_DARK_MODE_PACK_PREFIX))
				.orElse(null);
	}

	private static Optional<String> firstAvailable(PackRepository repository, String prefix) {
		return repository.getAvailableIds().stream().filter(id -> id.startsWith(prefix)).findFirst();
	}

	/**
	 * Someone who had the third-party dark mode pack switched on keeps a dark mode once the launcher
	 * stops bundling it: its entry in the saved pack list ({@code Options#resourcePacks}) is replaced
	 * by our own pack. Runs before vanilla reads that list - vanilla drops entries whose pack is gone,
	 * and dark mode would silently be off.
	 */
	public static void replaceLegacyDarkMode(List<String> savedPackIds, PackRepository repository) {
		Optional<String> own = firstAvailable(repository, DARK_MODE_PACK_PREFIX);
		if (own.isEmpty()) {
			return;
		}
		for (int i = savedPackIds.size() - 1; i >= 0; i--) {
			String id = savedPackIds.get(i);
			if (!id.startsWith(LEGACY_DARK_MODE_PACK_PREFIX) || repository.isAvailable(id)) {
				continue;
			}
			if (savedPackIds.contains(own.get())) {
				savedPackIds.remove(i);
			} else {
				savedPackIds.set(i, own.get());
			}
		}
	}

	private static synchronized Set<String> bundledFileNames() {
		if (bundledFileNames == null) {
			bundledFileNames = load();
		}
		return bundledFileNames;
	}

	private static Set<String> load() {
		if (!Files.exists(FILE)) {
			// Started outside our launcher (e.g. a dev runClient) - nothing is bundled then.
			return Set.of();
		}
		try {
			String[] names = new Gson().fromJson(Files.readString(FILE), String[].class);
			return names != null ? Set.of(names) : Set.of();
		} catch (IOException | JsonParseException | IllegalArgumentException e) {
			LOGGER.warn("Failed to read bundled resource pack list from {}", FILE, e);
			return Set.of();
		}
	}
}
