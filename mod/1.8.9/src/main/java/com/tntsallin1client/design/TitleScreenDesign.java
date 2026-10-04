package com.tntsallin1client.design;

import java.util.List;

import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;

/**
 * The client design's title screen, as in the Fabric versions (client logo on a plain theme-colored
 * background, Singleplayer/Multiplayer/Realms/Options/Client Mods below it, quit as an X in the top
 * right) plus the animated switch between that and the game's own title screen. The game's
 * `TitleScreen` stays the screen object either way - `TitleScreenMixin` just swaps what its `init`
 * and `render` do.
 *
 * <p>The buttons carry the ids of the game's own title screen buttons, so the game itself acts on a
 * click, exactly as it does in its own layout.
 *
 * <p>The switch animation: the theme background fades over (or away from) the game's title screen
 * while the logo grows out of that layout's Client Design button into its big spot in the client
 * layout (or shrinks back into it).
 */
public final class TitleScreenDesign {
	/** The game's own ids of its title screen buttons. */
	private static final int OPTIONS_BUTTON_ID = 0;
	private static final int SINGLEPLAYER_BUTTON_ID = 1;
	private static final int MULTIPLAYER_BUTTON_ID = 2;
	private static final int QUIT_BUTTON_ID = 4;
	private static final int REALMS_BUTTON_ID = 14;

	private static final long TRANSITION_MS = 800;
	private static final long BUTTON_FADE_MS = 300;

	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 6;
	private static final int BUTTON_COUNT = 5;
	private static final int LOGO_GAP = 14;
	private static final int QUIT_SIZE = 20;
	private static final String VERSION_LINE = "Minecraft 1.8.9";

	/** Logo height at the game's own layout's end of the animation - small enough to sit on the Client Design button. */
	private static final float ANCHOR_LOGO_HEIGHT = 16;

	private static long transitionStart = -1;
	private static boolean transitionToClient;
	private static Rect transitionFrom;
	private static long buttonFadeStart = -1;
	/** Where the logo starts from / shrinks back into in the game's own layout: centered on its Client Design button. */
	private static Rect minecraftAnchor;

	private TitleScreenDesign() {
	}

	/** A logo position: top-left corner plus height (width follows from {@link ClientLogo#widthFor}). */
	public static final class Rect {
		public final float x;
		public final float y;
		public final float height;

		Rect(float x, float y, float height) {
			this.x = x;
			this.y = y;
			this.height = height;
		}

		public static Rect centered(float centerX, float top, float height) {
			return new Rect(centerX - ClientLogo.widthFor(height) / 2, top, height);
		}

		Rect lerp(Rect to, float t) {
			return new Rect(this.x + (to.x - this.x) * t, this.y + (to.y - this.y) * t, this.height + (to.height - this.height) * t);
		}
	}

	/** Whether `init` should build the client layout right now - not while the switch towards it is still animating over the game's own one. */
	public static boolean useClientLayout() {
		return ClientDesign.isClient() && !(isRunning() && transitionToClient) && !MinecraftClient.getInstance().isDemo();
	}

	// --- Layout -------------------------------------------------------------------------------

	/** Called by {@link MenuButtons} whenever it places the game's own layout's Client Design button. */
	public static void setMinecraftAnchor(int x, int y, int width, int height) {
		minecraftAnchor = Rect.centered(x + width / 2.0F, y + (height - ANCHOR_LOGO_HEIGHT) / 2, ANCHOR_LOGO_HEIGHT);
	}

	private static Rect minecraftAnchor(int width, int height) {
		return minecraftAnchor != null ? minecraftAnchor : Rect.centered(width / 2.0F, height / 2.0F, ANCHOR_LOGO_HEIGHT);
	}

	/** The big logo of the client layout, sized to whatever room the button column leaves. */
	public static Rect clientLogoRect(int width, int height) {
		int buttonsHeight = BUTTON_COUNT * BUTTON_HEIGHT + (BUTTON_COUNT - 1) * BUTTON_GAP;
		float logoHeight = Math.max(40, Math.min(150, height - buttonsHeight - LOGO_GAP - 40));
		float top = Math.max(8, (height - logoHeight - LOGO_GAP - buttonsHeight) / 2);
		return Rect.centered(width / 2.0F, top, logoHeight);
	}

	/** Adds the client layout's buttons - called by `TitleScreenMixin` instead of the game's own `init`. */
	public static void buildClientLayout(List<ButtonWidget> buttons, int width, int height) {
		Rect logo = clientLogoRect(width, height);
		buttons.add(new LogoButton(MenuButtons.DESIGN_BUTTON_ID, Math.round(logo.x), Math.round(logo.y), Math.round(logo.height)));

		int x = (width - BUTTON_WIDTH) / 2;
		int y = Math.round(logo.y + logo.height) + LOGO_GAP;
		int step = BUTTON_HEIGHT + BUTTON_GAP;
		buttons.add(button(SINGLEPLAYER_BUTTON_ID, x, y, "menu.singleplayer"));
		buttons.add(button(MULTIPLAYER_BUTTON_ID, x, y + step, "menu.multiplayer"));
		buttons.add(button(REALMS_BUTTON_ID, x, y + 2 * step, "menu.online"));
		buttons.add(button(OPTIONS_BUTTON_ID, x, y + 3 * step, "menu.options"));
		buttons.add(button(MenuButtons.OPEN_MENU_BUTTON_ID, x, y + 4 * step, "gui.tntsallin1client.menu.open_button"));

		ThemedButton quit = new ThemedButton(QUIT_BUTTON_ID, width - QUIT_SIZE - 6, 6, QUIT_SIZE, QUIT_SIZE, "") {
			@Override
			protected void renderLabel(int textColor) {
				ClientLogo.drawCross(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 8.0F, 1.5F, textColor);
			}
		};
		quit.tooltip = I18n.translate("menu.quit");
		buttons.add(quit);
		MenuButtons.addFriendsButton(buttons, true);
	}

	private static ThemedButton button(int id, int x, int y, String key) {
		return new ThemedButton(id, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.translate(key));
	}

	/** Theme background plus the version line - everything of the client layout that isn't a button. */
	public static void renderClientBackground(int width, int height) {
		ClientTheme theme = ClientTheme.get();
		DrawableHelper.fill(0, 0, width, height, theme.background1);
		ClientFont.draw(VERSION_LINE, 2, height - 10, ClientTheme.withAlpha(theme.text, 0.5F));
	}

	/** Buttons fade in right after the switch animation ends, instead of popping in all at once. */
	public static float clientWidgetAlpha() {
		if (buttonFadeStart < 0) {
			return 1.0F;
		}
		float t = (MinecraftClient.getTime() - buttonFadeStart) / (float) BUTTON_FADE_MS;
		if (t >= 1.0F) {
			buttonFadeStart = -1;
			return 1.0F;
		}
		return Math.max(0.0F, t);
	}

	// --- Switching ----------------------------------------------------------------------------

	/** Client Design button in the game's own layout: animate over to the client design. */
	public static void switchToClient() {
		Window window = new Window(MinecraftClient.getInstance());
		ClientDesign.setClient(true);
		transitionFrom = minecraftAnchor(window.getWidth(), window.getHeight());
		transitionToClient = true;
		transitionStart = MinecraftClient.getTime();
	}

	/**
	 * Logo button in the client layout: back to the game's own - whose buttons the caller builds right
	 * away, under the animation. `from` is where the logo sits in that screen's client layout.
	 */
	public static void switchToMinecraft(Rect from) {
		ClientDesign.setClient(false);
		transitionFrom = from;
		transitionToClient = false;
		transitionStart = MinecraftClient.getTime();
	}

	public static boolean isRunning() {
		return transitionStart >= 0;
	}

	/** Eased animation progress, 0..1. */
	public static float progress() {
		if (!isRunning()) {
			return 1.0F;
		}
		float t = Math.min(1.0F, (MinecraftClient.getTime() - transitionStart) / (float) TRANSITION_MS);
		return t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	/**
	 * Draws the animation on top of the game's own layout and reports whether it just finished - the
	 * caller then rebuilds the buttons (towards the client design) or simply stops. `clientLogo` is
	 * where the logo sits in that screen's client layout.
	 */
	public static boolean renderTransition(int width, int height, Rect clientLogo) {
		if (!isRunning()) {
			return false;
		}
		float t = progress();
		float backgroundAlpha = transitionToClient ? t : 1.0F - t;
		DrawableHelper.fill(0, 0, width, height, ClientTheme.withAlpha(ClientTheme.get().background1, backgroundAlpha));
		Rect to = transitionToClient ? clientLogo : minecraftAnchor(width, height);
		Rect logo = transitionFrom.lerp(to, t);
		ClientLogo.draw(logo.x, logo.y, logo.height, 1.0F);

		if (MinecraftClient.getTime() - transitionStart < TRANSITION_MS) {
			return false;
		}
		if (transitionToClient) {
			buttonFadeStart = MinecraftClient.getTime();
		}
		transitionStart = -1;
		return true;
	}

	/** Leaving the title screen mid-animation (shouldn't be possible, clicks are blocked) just ends it. */
	public static void cancel() {
		transitionStart = -1;
	}
}
