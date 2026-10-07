package com.tntsallin1client.particle;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.multiplayer.MultiPlayerLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;

/**
 * Particle filter: every kind of particle can be switched off on its own (own user request). The
 * kinds are the game's own particle types, whatever this version has registered - nothing is listed
 * by hand.
 *
 * <p>A hidden particle is never created (`ParticleEngineMixin`); the ones already in the air when
 * it is switched off live out their time. The pieces of a block being mined or broken are built
 * apart from that and count as {@code block}. A firework explosion asks for its sparks and its flash
 * and uses them unchecked, so those two are stopped where they are asked for (`FireworkStarterMixin`).
 */
public final class ParticleFilter {
	private static final String NAME_KEY = "gui.tntsallin1client.particle.";
	private static final String VANILLA_NAMESPACE = "minecraft";

	/** The config's set {@link #HIDDEN} was built from - a reset puts a new one in its place. */
	private static Set<String> source;
	private static final Set<ParticleType<?>> HIDDEN = new HashSet<ParticleType<?>>();

	private ParticleFilter() {
	}

	/** Asked for every particle the game is about to create. */
	public static boolean hides(ParticleType<?> type) {
		ClientConfig config = ClientConfig.get();
		if (!config.particleFilterEnabled) {
			return false;
		}
		if (config.particleFilterHidden != source) {
			HIDDEN.clear();
			for (String id : config.particleFilterHidden) {
				ResourceLocation parsed = ResourceLocation.tryParse(id);
				if (parsed != null) {
					Registry.PARTICLE_TYPE.getOptional(parsed).ifPresent(HIDDEN::add);
				}
			}
			source = config.particleFilterHidden;
		}
		return HIDDEN.contains(type);
	}

	/** The id of every particle type of this game version, in the order of their names. */
	public static List<String> ids() {
		List<String> ids = new ArrayList<String>();
		for (ResourceLocation id : Registry.PARTICLE_TYPE.keySet()) {
			ids.add(id.toString());
		}
		final Collator collator = Collator.getInstance();
		ids.sort((first, second) -> collator.compare(name(first), name(second)));
		return ids;
	}

	/**
	 * The game has no names for its particles. Ours are in the language files; a particle without one
	 * (another mod's) gets its id made readable: {@code dust_plume} - "Dust Plume".
	 */
	public static String name(String id) {
		int colon = id.indexOf(':');
		String namespace = id.substring(0, colon);
		String path = id.substring(colon + 1);
		if (namespace.equals(VANILLA_NAMESPACE) && I18n.exists(NAME_KEY + path)) {
			return I18n.get(NAME_KEY + path);
		}
		StringBuilder readable = new StringBuilder();
		for (String word : path.split("[_/]")) {
			if (word.isEmpty()) {
				continue;
			}
			if (readable.length() > 0) {
				readable.append(' ');
			}
			readable.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return readable.toString();
	}

	public static boolean isShown(String id) {
		return !ClientConfig.get().particleFilterHidden.contains(id);
	}

	/** The caller saves the config. */
	public static void setShown(String id, boolean shown) {
		ClientConfig config = ClientConfig.get();
		if (shown) {
			config.particleFilterHidden.remove(id);
		} else {
			config.particleFilterHidden.add(id);
		}
		source = null;
	}

	/** The caller saves the config. */
	public static void setAllShown(boolean shown) {
		ClientConfig config = ClientConfig.get();
		config.particleFilterHidden.clear();
		if (!shown) {
			config.particleFilterHidden.addAll(ids());
		}
		source = null;
	}

	/** A particle that is in no world and draws nothing - for code that asked for a hidden one and uses the answer unchecked. */
	public static Particle placeholder(MultiPlayerLevel level) {
		return new PlaceholderParticle(level);
	}
}
