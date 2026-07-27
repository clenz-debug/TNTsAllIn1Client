package com.tntsallin1client.menu;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Slider over a fixed, ordered list of values rather than a continuous
 * numeric range - needed for the crosshair size, which has non-uniformly
 * spaced steps (0.25/0.5/0.75 below 1, then whole numbers 1-6 above) that a
 * plain linear [min, max] mapping can't represent. The slider's own
 * {@code value} (normalized [0.0, 1.0], from vanilla's
 * {@link AbstractSliderButton}) maps to a list index instead of a number
 * directly.
 */
public class DiscreteSliderButton<T> extends AbstractSliderButton {
	private final List<T> values;
	private final Function<T, Component> labelFactory;
	private final Consumer<T> onChange;

	public DiscreteSliderButton(int x, int y, int width, int height, List<T> values, T initial,
			Function<T, Component> labelFactory, Consumer<T> onChange) {
		super(x, y, width, height, Component.empty(), normalize(values, initial));
		this.values = values;
		this.labelFactory = labelFactory;
		this.onChange = onChange;
		this.updateMessage();
	}

	private static <T> double normalize(List<T> values, T initial) {
		int index = values.indexOf(initial);
		if (index < 0 || values.size() <= 1) {
			return 0.0;
		}
		return (double) index / (values.size() - 1);
	}

	public T currentValue() {
		int index = Mth.clamp((int) Math.round(this.value * (this.values.size() - 1)), 0, this.values.size() - 1);
		return this.values.get(index);
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.labelFactory.apply(this.currentValue()));
	}

	@Override
	protected void applyValue() {
		this.onChange.accept(this.currentValue());
	}
}
