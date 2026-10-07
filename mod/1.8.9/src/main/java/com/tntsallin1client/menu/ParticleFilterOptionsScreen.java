package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.config.ConfigReset;
import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.client.resource.language.I18n;

/**
 * Options of the particle filter: one on/off row per kind of particle ({@link ParticleFilter}),
 * below the feature's own switch and a pair of buttons for all of them at once - the same rows in
 * the same order as in the Fabric versions.
 */
public class ParticleFilterOptionsScreen extends FeatureOptionsScreen {
	private static final String KEY = "gui.tntsallin1client.particle_filter_options.";

	public ParticleFilterOptionsScreen(Screen parent) {
		super(parent, KEY + "title");
		final ClientConfig config = ClientConfig.get();
		addToggle(KEY + "enabled", () -> config.particleFilterEnabled, value -> config.particleFilterEnabled = value);
		addPanel(new SplitPanel(
				new ButtonPanel(() -> I18n.translate(KEY + "all_on"), () -> setAllShown(true)),
				new ButtonPanel(() -> I18n.translate(KEY + "all_off"), () -> setAllShown(false))));
		for (final ParticleType type : ParticleFilter.types()) {
			addToggle(ParticleFilter.nameKey(type), () -> ParticleFilter.isShown(type), value -> ParticleFilter.setShown(type, value));
		}
		setResettable(ConfigReset.Feature.PARTICLE_FILTER);
	}

	private void setAllShown(boolean shown) {
		ParticleFilter.setAllShown(shown);
		// Every row below says something else now - laid out anew, which reads the labels fresh.
		this.buttons.clear();
		init();
	}
}
