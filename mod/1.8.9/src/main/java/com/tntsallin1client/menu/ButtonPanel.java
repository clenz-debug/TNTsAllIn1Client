package com.tntsallin1client.menu;

import java.util.function.Supplier;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.Window;
import org.lwjgl.input.Mouse;

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
	private ButtonWidget button;

	ButtonPanel(Supplier<String> label, Runnable onClick) {
		this.label = label;
		this.onClick = onClick;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	private ButtonWidget buttonAt(int x, int y, int width) {
		if (this.button == null || this.button.getWidth() != width) {
			this.button = new ButtonWidget(0, x, y, width, HEIGHT, "");
		}
		this.button.x = x;
		this.button.y = y;
		this.button.message = this.label.get();
		return this.button;
	}

	@Override
	public void render(int x, int y, int width) {
		// A panel is not told where the mouse is - worked out the way the game does it for a screen.
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = new Window(client);
		int mouseX = Mouse.getX() * window.getWidth() / client.width;
		int mouseY = window.getHeight() - Mouse.getY() * window.getHeight() / client.height - 1;
		buttonAt(x, y, width).render(client, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		MinecraftClient client = MinecraftClient.getInstance();
		ButtonWidget button = buttonAt(x, y, width);
		if (!button.isMouseOver(client, mouseX, mouseY)) {
			return false;
		}
		button.playDownSound(client.getSoundManager());
		this.onClick.run();
		ClientConfig.get().save();
		return true;
	}
}
