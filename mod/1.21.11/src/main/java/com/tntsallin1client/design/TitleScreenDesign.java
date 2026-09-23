package com.tntsallin1client.design;

import com.mojang.realmsclient.RealmsMainScreen;
import com.tntsallin1client.menu.ClientMenus;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.function.Consumer;

/**
 * The client design's title screen (own user sketch: client logo on a plain theme-colored
 * background, Singleplayer/Multiplayer/Realms/Options/Client Mods below it, quit as an X in the top
 * right) plus the animated switch between that and the vanilla title screen. The vanilla
 * {@link TitleScreen} itself stays the screen object either way - {@code TitleScreenMixin} just
 * swaps what its {@code init}/render do - so every other screen and mod still sees a normal title
 * screen as parent/current screen.
 *
 * <p>The switch animation: the theme background fades over (or away from) the vanilla title screen
 * while the logo grows out of the vanilla layout's Client Design button into its big spot in the
 * client layout (or shrinks back into it). Only one title screen exists at a time, so the transition state is static.
 */
public final class TitleScreenDesign {
	private static final long TRANSITION_MS = 800;
	private static final long BUTTON_FADE_MS = 300;

	private static final int BUTTON_WIDTH = 160;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 6;
	private static final int BUTTON_COUNT = 5;
	private static final int LOGO_GAP = 14;
	private static final int QUIT_SIZE = 20;

	/** Logo height at the vanilla layout's end of the animation - small enough to sit on the Client Design button. */
	private static final float ANCHOR_LOGO_HEIGHT = 16;

	private static long transitionStart = -1;
	private static boolean transitionToClient;
	private static Rect transitionFrom;
	private static long buttonFadeStart = -1;
	/** Where the logo starts from / shrinks back into in the vanilla layout: centered on its Client Design button. */
	private static Rect minecraftAnchor;

	private TitleScreenDesign() {
	}

	/** A logo position: top-left corner plus height (width follows from {@link ClientLogo#widthFor}). */
	public record Rect(float x, float y, float height) {
		float width() {
			return ClientLogo.widthFor(this.height);
		}

		static Rect centered(float centerX, float top, float height) {
			return new Rect(centerX - ClientLogo.widthFor(height) / 2, top, height);
		}

		Rect lerp(Rect to, float t) {
			return new Rect(this.x + (to.x - this.x) * t, this.y + (to.y - this.y) * t, this.height + (to.height - this.height) * t);
		}
	}

	/** Whether {@code init} should build the client layout right now - not while the switch towards it is still animating over the vanilla one. */
	public static boolean useClientLayout() {
		return ClientDesign.isClient() && !(isRunning() && transitionToClient) && !Minecraft.getInstance().isDemo();
	}

	// --- Layout -------------------------------------------------------------------------------

	/** Called by {@code TitleScreenIntegration} whenever it places the vanilla layout's Client Design button. */
	public static void setMinecraftAnchor(int x, int y, int width, int height) {
		minecraftAnchor = Rect.centered(x + width / 2.0f, y + (height - ANCHOR_LOGO_HEIGHT) / 2, ANCHOR_LOGO_HEIGHT);
	}

	private static Rect minecraftAnchor(int width, int height) {
		return minecraftAnchor != null ? minecraftAnchor : Rect.centered(width / 2.0f, height / 2.0f, ANCHOR_LOGO_HEIGHT);
	}

	/** The big logo of the client layout, sized to whatever room the button column leaves. */
	public static Rect clientLogoRect(int width, int height) {
		int buttonsHeight = BUTTON_COUNT * BUTTON_HEIGHT + (BUTTON_COUNT - 1) * BUTTON_GAP;
		float logoHeight = Math.max(40, Math.min(150, height - buttonsHeight - LOGO_GAP - 40));
		float top = Math.max(8, (height - logoHeight - LOGO_GAP - buttonsHeight) / 2);
		return Rect.centered(width / 2.0f, top, logoHeight);
	}

	/** Adds the client layout's widgets - called by {@code TitleScreenMixin} instead of vanilla's own {@code init}. */
	public static void buildClientLayout(TitleScreen screen, int width, int height, Consumer<AbstractWidget> add, Runnable rebuild) {
		Minecraft minecraft = Minecraft.getInstance();
		Rect logo = clientLogoRect(width, height);
		add.accept(new LogoButton(Math.round(logo.x), Math.round(logo.y), Math.round(logo.height),
				() -> switchToMinecraft(logo, rebuild)));

		int x = (width - BUTTON_WIDTH) / 2;
		int y = Math.round(logo.y + logo.height) + LOGO_GAP;
		boolean multiplayerAllowed = minecraft.allowsMultiplayer();
		Tooltip multiplayerTooltip = multiplayerAllowed ? null : Tooltip.create(Component.translatable("title.multiplayer.disabled"));

		add.accept(button(x, y, "menu.singleplayer", () -> minecraft.setScreen(new SelectWorldScreen(screen))));
		ThemedButton multiplayer = button(x, y += BUTTON_HEIGHT + BUTTON_GAP, "menu.multiplayer",
				() -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(screen) : new SafetyScreen(screen)));
		multiplayer.active = multiplayerAllowed;
		multiplayer.setTooltip(multiplayerTooltip);
		add.accept(multiplayer);
		ThemedButton realms = button(x, y += BUTTON_HEIGHT + BUTTON_GAP, "menu.online", () -> minecraft.setScreen(new RealmsMainScreen(screen)));
		realms.active = multiplayerAllowed;
		realms.setTooltip(multiplayerTooltip);
		add.accept(realms);
		add.accept(button(x, y += BUTTON_HEIGHT + BUTTON_GAP, "menu.options",
				() -> minecraft.setScreen(new OptionsScreen(screen, minecraft.options))));
		add.accept(button(x, y += BUTTON_HEIGHT + BUTTON_GAP, "gui.tntsallin1client.menu.open_button",
				() -> minecraft.setScreen(ClientMenus.create(screen))));

		ThemedButton quit = new ThemedButton(width - QUIT_SIZE - 6, 6, QUIT_SIZE, QUIT_SIZE, Component.translatable("menu.quit"), minecraft::stop) {
			@Override
			protected void renderLabel(GuiGraphics graphics, int text) {
				ClientLogo.drawCross(graphics, this.getX() + this.getWidth() / 2.0f, this.getY() + this.getHeight() / 2.0f, 8.0f, 1.5f, text);
			}
		};
		quit.setTooltip(Tooltip.create(Component.translatable("menu.quit")));
		add.accept(quit);
	}

	private static ThemedButton button(int x, int y, String key, Runnable onPress) {
		return new ThemedButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable(key), onPress);
	}

	/** Theme background plus the version line - everything of the client layout that isn't a widget. */
	public static void renderClientBackground(GuiGraphics graphics, Font font, int width, int height) {
		ClientTheme theme = ClientTheme.get();
		graphics.fill(0, 0, width, height, theme.background1);
		String version = "Minecraft " + SharedConstants.getCurrentVersion().name();
		graphics.drawString(font, ClientFont.of(version), 2, height - 10, ClientTheme.withAlpha(theme.text, 0.5f), false);
	}

	/** Buttons fade in right after the switch animation ends, instead of popping in all at once. */
	public static float clientWidgetAlpha() {
		if (buttonFadeStart < 0) {
			return 1.0f;
		}
		float t = (Util.getMillis() - buttonFadeStart) / (float) BUTTON_FADE_MS;
		if (t >= 1.0f) {
			buttonFadeStart = -1;
			return 1.0f;
		}
		return Math.max(0.0f, t);
	}

	// --- Switching ----------------------------------------------------------------------------

	/** Client Design button in the vanilla layout: animate over to the client design. */
	public static void switchToClient() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientDesign.setClient(true);
		transitionFrom = minecraftAnchor(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
		transitionToClient = true;
		transitionStart = Util.getMillis();
	}

	/** Logo button in the client layout: back to vanilla - the vanilla widgets are built right away and fade in under the animation.
	 * {@code rebuild} must go through Fabric's init events (see {@code TitleScreenMixin}), or our own vanilla-layout buttons go missing. */
	private static void switchToMinecraft(Rect from, Runnable rebuild) {
		ClientDesign.setClient(false);
		transitionFrom = from;
		transitionToClient = false;
		transitionStart = Util.getMillis();
		rebuild.run();
	}

	public static boolean isRunning() {
		return transitionStart >= 0;
	}

	public static boolean isRunningToMinecraft() {
		return isRunning() && !transitionToClient;
	}

	/** Eased animation progress, 0..1. */
	public static float progress() {
		if (!isRunning()) {
			return 1.0f;
		}
		float t = Math.min(1.0f, (Util.getMillis() - transitionStart) / (float) TRANSITION_MS);
		return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	/**
	 * Draws the animation on top of the vanilla title screen and reports whether it just finished -
	 * the caller then rebuilds the widgets (towards the client design) or simply stops.
	 */
	public static boolean renderTransition(GuiGraphics graphics, int width, int height) {
		if (!isRunning()) {
			return false;
		}
		float t = progress();
		float backgroundAlpha = transitionToClient ? t : 1.0f - t;
		graphics.fill(0, 0, width, height, ClientTheme.withAlpha(ClientTheme.get().background1, backgroundAlpha));
		Rect to = transitionToClient ? clientLogoRect(width, height) : minecraftAnchor(width, height);
		Rect logo = transitionFrom.lerp(to, t);
		ClientLogo.draw(graphics, logo.x, logo.y, logo.height, 1.0f);

		if (Util.getMillis() - transitionStart < TRANSITION_MS) {
			return false;
		}
		if (transitionToClient) {
			buttonFadeStart = Util.getMillis();
		}
		transitionStart = -1;
		return true;
	}

	/** Leaving the title screen mid-animation (shouldn't be possible, clicks are blocked) just ends it. */
	public static void cancel() {
		transitionStart = -1;
	}
}
