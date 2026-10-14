package com.tntsallin1client.design;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;

import java.util.List;

/**
 * The client design's pause menu (own user request: same style as the client title screen) - the
 * vanilla pause menu's own buttons, restyled by the theme mixins ({@link ThemedUi}) and moved down
 * under the client logo, which replaces the "Game Menu" heading and doubles as the design switch.
 * Every button keeps its vanilla action, and every screen behind them stays vanilla. Switching runs
 * the title screen's animation ({@link TitleScreenDesign}).
 */
public final class PauseScreenDesign {
	private static final int LOGO_GAP = 14;
	private static final float MIN_LOGO_HEIGHT = 30;
	private static final float MAX_LOGO_HEIGHT = 90;

	/** Height of the pause menu's button grid, measured on each layout - the logo gets whatever room it leaves. */
	private static int gridHeight = 150;

	private PauseScreenDesign() {
	}

	/** The logo's spot in the client layout - also the switch animation's target from the vanilla layout. */
	public static TitleScreenDesign.Rect clientLogoRect(int width, int height) {
		float logoHeight = Math.max(MIN_LOGO_HEIGHT, Math.min(MAX_LOGO_HEIGHT, height - gridHeight - LOGO_GAP - 24));
		float top = Math.max(6, (height - logoHeight - LOGO_GAP - gridHeight) / 2);
		return TitleScreenDesign.Rect.centered(width / 2.0f, top, logoHeight);
	}

	/** Vanilla layout: only measures the button grid, for where the animation's logo ends up. */
	public static void measure(List<AbstractWidget> widgets) {
		int top = Integer.MAX_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (AbstractWidget widget : widgets) {
			if (!(widget instanceof StringWidget)) {
				top = Math.min(top, widget.getY());
				bottom = Math.max(bottom, widget.getY() + widget.getHeight());
			}
		}
		if (bottom > top) {
			gridHeight = bottom - top;
		}
	}

	/**
	 * Client layout: drops the heading, moves the button grid down under the logo and adds the logo
	 * button. {@code rebuild} must go through Fabric's init events, like the title screen's.
	 */
	public static void buildClientLayout(List<AbstractWidget> widgets, int width, int height, Runnable rebuild) {
		widgets.removeIf(widget -> widget instanceof StringWidget);
		measure(widgets);
		int gridTop = Integer.MAX_VALUE;
		for (AbstractWidget widget : widgets) {
			gridTop = Math.min(gridTop, widget.getY());
		}

		TitleScreenDesign.Rect logo = clientLogoRect(width, height);
		int shift = Math.round(logo.y() + logo.height()) + LOGO_GAP - gridTop;
		for (AbstractWidget widget : widgets) {
			widget.setY(widget.getY() + shift);
		}
		widgets.add(new LogoButton(Math.round(logo.x()), Math.round(logo.y()), Math.round(logo.height()),
				() -> TitleScreenDesign.switchToMinecraft(logo, rebuild)));
	}
}
