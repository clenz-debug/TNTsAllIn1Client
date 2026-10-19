package com.tntsallin1client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 1.18.2 stand-in for 1.19.3's {@code Button.builder(...)}: 1.18.2 only has {@code Button}'s
 * constructor. Same defaults as the real builder (150 x 20 at 0, 0).
 */
public final class Buttons {
	private Buttons() {
	}

	public static Builder builder(Component message, Button.OnPress onPress) {
		return new Builder(message, onPress);
	}

	public static final class Builder {
		private final Component message;
		private final Button.OnPress onPress;
		@Nullable
		private Tooltip tooltip;
		private int x;
		private int y;
		private int width = 150;
		private int height = 20;

		private Builder(Component message, Button.OnPress onPress) {
			this.message = message;
			this.onPress = onPress;
		}

		public Builder pos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public Builder width(int width) {
			this.width = width;
			return this;
		}

		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder bounds(int x, int y, int width, int height) {
			return this.pos(x, y).size(width, height);
		}

		public Builder tooltip(@Nullable Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public Button build() {
			Button button = new Button(this.x, this.y, this.width, this.height, this.message, this.onPress);
			button.setTooltip(this.tooltip);
			return button;
		}
	}
}
