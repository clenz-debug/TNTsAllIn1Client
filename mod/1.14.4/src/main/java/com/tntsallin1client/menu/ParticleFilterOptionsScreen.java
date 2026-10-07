package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * Options of the particle filter: one on/off row per kind of particle ({@link ParticleFilter}),
 * below the feature's own switch and a pair of buttons for all of them at once - the same rows in
 * the same order as in the other versions.
 */
public class ParticleFilterOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.particle_filter_options.";

	public ParticleFilterOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.particleFilterEnabled, value -> config.particleFilterEnabled = value);
		addPanel(new SplitPanel(
				new ButtonPanel(() -> I18n.get(KEY + "all_on"), () -> setAllShown(true)),
				new ButtonPanel(() -> I18n.get(KEY + "all_off"), () -> setAllShown(false))));
		for (final String id : ParticleFilter.ids()) {
			addNamedToggle(() -> ParticleFilter.name(id), () -> ParticleFilter.isShown(id), value -> ParticleFilter.setShown(id, value));
		}
		setResettable(ConfigReset.Feature.PARTICLE_FILTER);
	}

	private void setAllShown(boolean shown) {
		ParticleFilter.setAllShown(shown);
		// Every row below says something else now - laid out anew, which reads the labels fresh.
		rebuild();
	}
}
