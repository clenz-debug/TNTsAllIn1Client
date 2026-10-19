package com.tntsallin1client.friends;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Controls the bundled e4mc (Phase 8b, world invitations). e4mc normally makes *every* "Open to LAN"
 * reachable from the internet; we switch that off for good and only turn it on for the moment our
 * invite flow opens the world ({@link WorldInvites}). e4mc decides when the LAN server starts
 * listening, by reading its {@code hostEnabled} setting - so an override set just before
 * {@code publishServer} and removed right after is all it takes.
 *
 * <p>Uses e4mc's config library's own runtime override ({@code TrackedValue.setOverride}), which
 * never touches e4mc's config file. Reflection, since e4mc is not a compile dependency: if a future
 * e4mc changes any of this, inviting just reports itself unavailable instead of crashing.
 */
public final class E4mcControl {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static Object hostEnabled;
	private static Method setOverride;
	private static boolean resolved;

	private E4mcControl() {
	}

	/** Called once at startup - keeps a normal "Open to LAN" local. */
	public static void init() {
		setHosting(false);
	}

	/** e4mc is installed and its hosting switch could be found. */
	public static synchronized boolean isAvailable() {
		resolve();
		return setOverride != null;
	}

	public static synchronized boolean setHosting(boolean enabled) {
		resolve();
		if (setOverride == null) {
			return false;
		}
		try {
			setOverride.invoke(hostEnabled, enabled);
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.warn("Could not switch e4mc hosting {}", enabled ? "on" : "off", e);
			return false;
		}
	}

	private static void resolve() {
		if (resolved) {
			return;
		}
		resolved = true;
		if (!FabricLoader.getInstance().isModLoaded("e4mc")) {
			return;
		}
		try {
			Class<?> config = Class.forName("link.e4mc.Config");
			Object instance = config.getField("INSTANCE").get(null);
			Object value = config.getField("hostEnabled").get(instance);
			for (Method method : value.getClass().getMethods()) {
				if (method.getName().equals("setOverride") && method.getParameterCount() == 1) {
					method.setAccessible(true);
					hostEnabled = value;
					setOverride = method;
					return;
				}
			}
			LOGGER.warn("e4mc found, but not its hosting switch - world invitations are unavailable");
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.warn("e4mc found, but its config couldn't be read - world invitations are unavailable", e);
		}
	}
}
