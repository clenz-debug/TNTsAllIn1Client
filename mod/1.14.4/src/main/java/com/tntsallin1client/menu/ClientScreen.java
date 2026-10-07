package com.tntsallin1client.menu;

import com.tntsallin1client.tour.TourScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Base of the mod's own screens: a title on top, and Escape leads back to the screen it was opened
 * from (vanilla's default closes straight into the game).
 */
public abstract class ClientScreen extends Screen implements TourScreen.Closable {
	private static final int TITLE_Y = 14;

	protected final Screen parent;
	private final String titleKey;

	protected ClientScreen(Screen parent, String titleKey) {
		super(new TranslatableComponent(titleKey));
		this.parent = parent;
		this.titleKey = titleKey;
	}

	protected void back() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void closeForTour() {
		back();
	}

	/** What Escape does. */
	@Override
	public void onClose() {
		back();
	}

	/** Builds the screen's buttons anew, the way the game does it when the window is resized. */
	protected final void rebuild() {
		this.init(this.minecraft, this.width, this.height);
	}

	/** What lies behind the screen - vanilla's darkened game view (or dirt without a world) - unless a screen wants something else. */
	protected void renderScreenBackground() {
		this.renderBackground();
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		renderScreenBackground();
		MenuText.centered(I18n.get(this.titleKey), this.width / 2, TITLE_Y, 0xFFFFFF);
		super.render(mouseX, mouseY, partialTick);
	}
}
