package com.tntsallin1client.compat;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The "Client Capes" switch of the mod menu. The capes players set in the launcher are shown by the
 * bundled Cape Provider mod: our {@code fabric.mod.json} names the address they lie under
 * ({@code custom.cape}), which Cape Provider lists as one of its cape providers. Switching our capes
 * off takes that provider out of Cape Provider's active ones - the same thing its own provider screen
 * does, through the same calls - so the state lives in Cape Provider's config and both places always
 * agree.
 *
 * <p>Cape Provider is not a dependency: a player can take it out of an instance in the launcher, and
 * then there is nothing to switch ({@link #isAvailable()}). Its classes are therefore only reached by
 * name. The calls used exist unchanged in every Cape Provider version we bundle (checked with javap).
 */
public final class CapeProviderCompat {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String MOD_ID = "cape-provider";
	private static final String OWN_MOD_ID = "tntsallin1client";

	private CapeProviderCompat() {
	}

	/** Cape Provider is installed and lists our capes. */
	public static boolean isAvailable() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID) && ownProviderId() != null;
	}

	public static boolean isEnabled() {
		String id = ownProviderId();
		try {
			return id != null && activeProviderIds(config(capes())).contains(id);
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.warn("Failed to read Cape Provider's active providers", e);
			return false;
		}
	}

	public static void setEnabled(boolean enabled) {
		String id = ownProviderId();
		if (id == null) {
			return;
		}
		try {
			Object capes = capes();
			Object config = config(capes);
			// Ours first: where several providers have a cape for a player, the first one wins.
			Set<String> ids = new LinkedHashSet<>();
			if (enabled) {
				ids.add(id);
			}
			for (String other : activeProviderIds(config)) {
				if (!other.equals(id)) {
					ids.add(other);
				}
			}
			config.getClass().getMethod("setActiveProviderIds", Collection.class).invoke(config, ids);
			// Saves, then drops the capes already loaded so every player's cape is looked up anew.
			capes.getClass().getMethod("saveConfigAndMarkRefresh").invoke(capes);
			capes.getClass().getMethod("refreshIfMarked").invoke(capes);
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.warn("Failed to switch our capes in Cape Provider", e);
		}
	}

	/**
	 * The id Cape Provider gave the provider it made from our mod's metadata - derived from our mod
	 * id, so looked for by that instead of repeating Cape Provider's naming here.
	 */
	private static String ownProviderId() {
		if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
			return null;
		}
		try {
			Object capes = capes();
			Map<?, ?> providers = (Map<?, ?>) capes.getClass().getMethod("getAllProviders").invoke(capes);
			for (Object id : providers.keySet()) {
				if (id instanceof String name && name.contains(OWN_MOD_ID)) {
					return name;
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.warn("Failed to read Cape Provider's providers", e);
		}
		return null;
	}

	private static Object capes() throws ReflectiveOperationException {
		Object capes = Class.forName("net.litetex.capes.Capes").getMethod("instance").invoke(null);
		if (capes == null) {
			throw new IllegalStateException("Cape Provider is not initialized yet");
		}
		return capes;
	}

	private static Object config(Object capes) throws ReflectiveOperationException {
		return capes.getClass().getMethod("config").invoke(capes);
	}

	@SuppressWarnings("unchecked")
	private static Set<String> activeProviderIds(Object config) throws ReflectiveOperationException {
		return (Set<String>) config.getClass().getMethod("getActiveProviderIds").invoke(config);
	}
}
