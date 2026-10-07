package com.tntsallin1client.particle;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.client.resource.language.I18n;

/**
 * Particle filter: every kind of particle can be switched off on its own (own user request). The
 * kinds are the game's own particle types.
 *
 * <p>A hidden particle is never created (`ParticleManagerMixin`); the ones already in the air when
 * it is switched off live out their time. Two kinds don't come through the particle manager's one
 * creation method and are stopped where they are made instead: the pieces of a block being mined or
 * broken count as {@link ParticleType#BLOCK_CRACK}, the sparks of a firework explosion
 * (`FireworkParticleMixin`) as {@link ParticleType#FIREWORK_SPARK}.
 *
 * <p>{@link ParticleType#ITEM_TAKE} is left out: it is an item flying to whoever picked it up, not
 * a particle anyone would look for here, and the game never makes it by its type.
 */
public final class ParticleFilter {
	private static final String NAME_KEY = "gui.tntsallin1client.particle.";

	/** The config's set {@link #hidden} was built from - a reset puts a new one in its place. */
	private static Set<String> source;
	/** By the type's number, which is what the game creates particles by. */
	private static boolean[] hidden = new boolean[0];

	private ParticleFilter() {
	}

	/** Asked for every particle the game is about to create. */
	public static boolean hides(int typeId) {
		ClientConfig config = ClientConfig.get();
		if (!config.particleFilterEnabled) {
			return false;
		}
		if (config.particleFilterHidden != source) {
			boolean[] rebuilt = new boolean[ParticleType.values().length];
			for (ParticleType type : ParticleType.values()) {
				int id = type.getId();
				if (id >= 0 && id < rebuilt.length) {
					rebuilt[id] = config.particleFilterHidden.contains(type.name());
				}
			}
			hidden = rebuilt;
			source = config.particleFilterHidden;
		}
		return typeId >= 0 && typeId < hidden.length && hidden[typeId];
	}

	public static boolean hides(ParticleType type) {
		return hides(type.getId());
	}

	/** Every particle type that can be switched, in the order of their names. */
	public static List<ParticleType> types() {
		List<ParticleType> types = new ArrayList<ParticleType>();
		for (ParticleType type : ParticleType.values()) {
			if (type != ParticleType.ITEM_TAKE) {
				types.add(type);
			}
		}
		final Collator collator = Collator.getInstance();
		Collections.sort(types, new Comparator<ParticleType>() {
			@Override
			public int compare(ParticleType first, ParticleType second) {
				return collator.compare(I18n.translate(nameKey(first)), I18n.translate(nameKey(second)));
			}
		});
		return types;
	}

	/** The game has no names for its particles - ours are in the language files. */
	public static String nameKey(ParticleType type) {
		return NAME_KEY + type.name().toLowerCase(Locale.ROOT);
	}

	public static boolean isShown(ParticleType type) {
		return !ClientConfig.get().particleFilterHidden.contains(type.name());
	}

	/** The caller saves the config. */
	public static void setShown(ParticleType type, boolean shown) {
		ClientConfig config = ClientConfig.get();
		if (shown) {
			config.particleFilterHidden.remove(type.name());
		} else {
			config.particleFilterHidden.add(type.name());
		}
		source = null;
	}

	/** The caller saves the config. */
	public static void setAllShown(boolean shown) {
		ClientConfig config = ClientConfig.get();
		config.particleFilterHidden.clear();
		if (!shown) {
			for (ParticleType type : types()) {
				config.particleFilterHidden.add(type.name());
			}
		}
		source = null;
	}
}
