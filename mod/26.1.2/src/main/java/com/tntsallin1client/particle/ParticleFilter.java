package com.tntsallin1client.particle;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Particle filter: every kind of particle can be switched off on its own (own user request). The
 * kinds are the game's own particle types, whatever this version has registered - nothing is listed
 * by hand, so a particle a later version adds shows up by itself.
 *
 * <p>A hidden particle is never created ({@link com.tntsallin1client.mixin.ParticleEngineMixin});
 * the ones already in the air when it is switched off live out their time. Three kinds don't come
 * through the engine's one creation method and are stopped where they are made instead: the pieces
 * of a block being mined or broken ({@link com.tntsallin1client.mixin.ClientLevelMixin}) count as
 * {@code block}, the sparks of a firework explosion
 * ({@link com.tntsallin1client.mixin.FireworkStarterMixin}) as {@code firework}.
 */
public final class ParticleFilter {
	private static final String NAME_KEY = "gui.tntsallin1client.particle.";
	private static final String VANILLA_NAMESPACE = "minecraft";

	/** The config's set {@link #HIDDEN} was built from - a reset puts a new one in its place. */
	private static Set<String> source;
	private static final Set<ParticleType<?>> HIDDEN = new HashSet<>();

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
				Identifier parsed = Identifier.tryParse(id);
				if (parsed != null) {
					BuiltInRegistries.PARTICLE_TYPE.getOptional(parsed).ifPresent(HIDDEN::add);
				}
			}
			source = config.particleFilterHidden;
		}
		return HIDDEN.contains(type);
	}

	/** The id of every particle type of this game version, in the order of their names. */
	public static List<String> ids() {
		List<String> ids = new ArrayList<>();
		for (Identifier id : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
			ids.add(id.toString());
		}
		Collator collator = Collator.getInstance();
		ids.sort((first, second) -> collator.compare(name(first).getString(), name(second).getString()));
		return ids;
	}

	/**
	 * The game has no names for its particles. Ours are in the language files; a particle without one
	 * (a newer version's, another mod's) gets its id made readable: {@code dust_plume} - "Dust Plume".
	 */
	public static Component name(String id) {
		int colon = id.indexOf(':');
		String namespace = id.substring(0, colon);
		String path = id.substring(colon + 1);
		if (namespace.equals(VANILLA_NAMESPACE) && Language.getInstance().has(NAME_KEY + path)) {
			return Component.translatable(NAME_KEY + path);
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
		return Component.literal(readable.toString());
	}

	public static boolean isShown(String id) {
		return !ClientConfig.get().particleFilterHidden.contains(id);
	}

	public static void setShown(String id, boolean shown) {
		ClientConfig config = ClientConfig.get();
		if (shown) {
			config.particleFilterHidden.remove(id);
		} else {
			config.particleFilterHidden.add(id);
		}
		source = null;
		config.save();
	}

	public static void setAllShown(boolean shown) {
		ClientConfig config = ClientConfig.get();
		config.particleFilterHidden.clear();
		if (!shown) {
			config.particleFilterHidden.addAll(ids());
		}
		source = null;
		config.save();
	}
}
