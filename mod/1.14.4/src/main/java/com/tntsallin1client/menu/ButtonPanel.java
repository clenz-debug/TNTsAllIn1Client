package com.tntsallin1client.menu;

import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

/**
 * A button as a panel - for buttons that share a row (see {@link SplitPanel}); a row that is one
 * button is a plain option of the {@link FeatureOptionsScreen}. A click runs the action, saves the
 * config and reads the label anew.
 */
final class ButtonPanel implements OptionPanel {
	private static final int HEIGHT = 20;

	private final Supplier<String> label;
	private final Runnable onClick;
	/** Made anew when the row's width changes - the label is always read fresh. */
	private Button button;

	ButtonPanel(Supplier<String> label, Runnable onClick) {
		this.label = label;
		this.onClick = onClick;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	private Button buttonAt(int x, int y, int width) {
		if (this.button == null || this.button.getWidth() != width) {
			// Clicked by the panel itself, not by the game - so the button needs no action of its own.
			this.button = new Button(x, y, width, HEIGHT, "", pressed -> { });
		}
		this.button.x = x;
		this.button.y = y;
		this.button.setMessage(this.label.get());
		return this.button;
	}

	@Override
	public void render(int x, int y, int width, int mouseX, int mouseY) {
		buttonAt(x, y, width).render(mouseX, mouseY, 0.0F);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		Button button = buttonAt(x, y, width);
		if (!button.isMouseOver(mouseX, mouseY)) {
			return false;
		}
		button.playDownSound(Minecraft.getInstance().getSoundManager());
		this.onClick.run();
		ClientConfig.get().save();
		return true;
	}
}
