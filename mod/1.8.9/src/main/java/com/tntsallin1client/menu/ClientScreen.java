package com.tntsallin1client.menu;

import com.tntsallin1client.tour.TourScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;

/**
 * Base of the mod's own screens: a title on top, Escape leads back to the screen it was opened
 * from (vanilla's default closes straight into the game), and - for a screen that has one - a
 * {@link ColorPickerPanel} gets its input. This version's screens don't manage anything but buttons
 * themselves, so text fields and the picker's drag areas need every event passed on by hand; doing
 * that here once keeps each feature's options screen down to its own rows.
 */
public abstract class ClientScreen extends Screen implements TourScreen.Closable {
	private static final int ESCAPE_KEY = 1;
	private static final int TITLE_Y = 14;

	protected final Screen parent;
	private final String titleKey;
	/** Set in {@link #init()} by screens that have a color to pick. */
	protected ColorPickerPanel colorPicker;

	protected ClientScreen(Screen parent, String titleKey) {
		this.parent = parent;
		this.titleKey = titleKey;
	}

	protected void back() {
		this.client.setScreen(this.parent);
	}

	@Override
	public void closeForTour() {
		back();
	}

	@Override
	protected void keyPressed(char character, int code) {
		if (code == ESCAPE_KEY) {
			back();
		} else if (this.colorPicker != null) {
			this.colorPicker.keyPressed(character, code);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		if (this.colorPicker != null) {
			this.colorPicker.mouseClicked(mouseX, mouseY, button);
		}
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long timeSinceClick) {
		if (this.colorPicker != null) {
			this.colorPicker.mouseDragged(mouseX, mouseY);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		super.mouseReleased(mouseX, mouseY, button);
		if (this.colorPicker != null) {
			this.colorPicker.mouseReleased();
		}
	}

	@Override
	public void tick() {
		if (this.colorPicker != null) {
			this.colorPicker.tick();
		}
	}

	/** What lies behind the screen - vanilla's darkened game view (or dirt without a world), in the client design the plain theme background (`ScreenMixin`) - unless a screen wants something else. */
	protected void renderScreenBackground() {
		this.renderBackground();
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		renderScreenBackground();
		MenuText.centered(I18n.translate(this.titleKey), this.width / 2, TITLE_Y, 0xFFFFFF);
		super.render(mouseX, mouseY, tickDelta);
		if (this.colorPicker != null) {
			this.colorPicker.render(0xFFFFFF);
		}
	}
}
