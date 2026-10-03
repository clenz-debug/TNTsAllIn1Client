package com.tntsallin1client.menu;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;

/**
 * A {@link ColorPickerPanel} as a row of a {@link FeatureOptionsScreen}: it sits between the other
 * rows and scrolls with them, the way the Fabric versions' options screens put a picker directly
 * below the switch it belongs to. Every change is saved right away.
 */
final class ColorPanel implements OptionPanel {
	private final IntSupplier getter;
	private final IntConsumer setter;
	/** Made when first drawn, and anew after a reset - it holds a copy of the color in its fields. */
	private ColorPickerPanel picker;

	ColorPanel(IntSupplier getter, IntConsumer setter) {
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public int height() {
		return ColorPickerPanel.totalHeight();
	}

	@Override
	public int width(int columnWidth) {
		return ColorPickerPanel.totalWidth();
	}

	/** The picker, moved to where the row is right now. */
	private ColorPickerPanel pickerAt(int x, int y, int width) {
		int pickerX = x + (width - ColorPickerPanel.totalWidth()) / 2;
		if (this.picker == null) {
			final IntConsumer setter = this.setter;
			this.picker = new ColorPickerPanel(MinecraftClient.getInstance().textRenderer, pickerX, y, this.getter.getAsInt(), argb -> {
				setter.accept(argb);
				ClientConfig.get().save();
			});
		}
		this.picker.moveTo(pickerX, y);
		return this.picker;
	}

	@Override
	public void render(int x, int y, int width) {
		pickerAt(x, y, width).render(0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		int pickerX = x + (width - ColorPickerPanel.totalWidth()) / 2;
		if (mouseX < pickerX || mouseX >= pickerX + ColorPickerPanel.totalWidth() || mouseY < y || mouseY >= y + height()) {
			return false;
		}
		pickerAt(x, y, width).mouseClicked(mouseX, mouseY, 0);
		return true;
	}

	@Override
	public void mouseDragged(int x, int y, int width, int mouseX, int mouseY) {
		if (this.picker != null) {
			pickerAt(x, y, width).mouseDragged(mouseX, mouseY);
		}
	}

	@Override
	public void mouseReleased() {
		if (this.picker != null) {
			this.picker.mouseReleased();
		}
	}

	@Override
	public void unfocus() {
		if (this.picker != null) {
			this.picker.unfocus();
		}
	}

	@Override
	public boolean keyPressed(char character, int code) {
		if (this.picker == null || !this.picker.hasFocus()) {
			return false;
		}
		this.picker.keyPressed(character, code);
		return true;
	}

	@Override
	public void reload() {
		this.picker = null;
	}

	@Override
	public void tick() {
		if (this.picker != null) {
			this.picker.tick();
		}
	}
}
