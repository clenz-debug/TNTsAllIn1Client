package com.tntsallin1client.menu;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.screen.Screen;

/** A screen that is nothing but the color picker for one color - for a feature that has more than one. */
public class ColorOptionsScreen extends FeatureOptionsScreen {
	public ColorOptionsScreen(Screen parent, String titleKey, IntSupplier getter, IntConsumer setter) {
		super(parent, titleKey);
		setColor(getter, setter);
	}
}
