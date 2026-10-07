package com.tntsallin1client.menu;

/**
 * Several panels side by side as one row of a {@link FeatureOptionsScreen}, each the same width -
 * for values that belong together, like the three coordinates of a position.
 */
final class SplitPanel implements OptionPanel {
	private static final int GAP = 6;

	private final OptionPanel[] parts;

	SplitPanel(OptionPanel... parts) {
		this.parts = parts;
	}

	private int partWidth(int width) {
		return (width - GAP * (this.parts.length - 1)) / this.parts.length;
	}

	private int partX(int x, int width, int index) {
		return x + index * (partWidth(width) + GAP);
	}

	@Override
	public int height() {
		int height = 0;
		for (OptionPanel part : this.parts) {
			height = Math.max(height, part.height());
		}
		return height;
	}

	@Override
	public void render(int x, int y, int width, int mouseX, int mouseY) {
		for (int index = 0; index < this.parts.length; index++) {
			this.parts[index].render(partX(x, width, index), y, partWidth(width), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, int mouseX, int mouseY) {
		for (int index = 0; index < this.parts.length; index++) {
			if (this.parts[index].mouseClicked(partX(x, width, index), y, partWidth(width), mouseX, mouseY)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void mouseDragged(int x, int y, int width, int mouseX, int mouseY) {
		for (int index = 0; index < this.parts.length; index++) {
			this.parts[index].mouseDragged(partX(x, width, index), y, partWidth(width), mouseX, mouseY);
		}
	}

	@Override
	public void mouseReleased() {
		for (OptionPanel part : this.parts) {
			part.mouseReleased();
		}
	}

	@Override
	public void unfocus() {
		for (OptionPanel part : this.parts) {
			part.unfocus();
		}
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		for (OptionPanel part : this.parts) {
			if (part.keyPressed(key, scanCode, modifiers)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean charTyped(char character, int modifiers) {
		for (OptionPanel part : this.parts) {
			if (part.charTyped(character, modifiers)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void reload() {
		for (OptionPanel part : this.parts) {
			part.reload();
		}
	}

	@Override
	public void tick() {
		for (OptionPanel part : this.parts) {
			part.tick();
		}
	}
}
