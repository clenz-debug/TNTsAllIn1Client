package com.tntsallin1client.tour;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.menu.TourFinishScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The in-game part of the guided tour, as in the Fabric versions - the launcher's tour ends with
 * "show me the menus in the game too?", and on yes the launcher leaves `config/tntsallin1client-tour.json`
 * before starting the game. The tour then starts on the first title screen: steps in
 * {@link InGameTourSteps}, drawn by {@link TourOverlay}. While a step is shown, clicks and keys on
 * its screen are held back - only the highlighted part (where a step wants it clicked) and the
 * explanation's own buttons get through.
 *
 * <p>This version has no events to hang that on: the tick comes from `MinecraftClientMixin`, the
 * drawing from `GameRendererMixin` (after a screen) and `HudElements` (without one), and `ScreenMixin`
 * asks {@link #allowMouse} and {@link #allowKey} before a screen gets a mouse or keyboard event.
 */
public final class InGameTour {
	private static final Logger LOGGER = LogManager.getLogger("tntsallin1client");
	private static final String TRIGGER_FILE = "tntsallin1client-tour.json";

	private static boolean triggerRead;
	private static boolean pendingStart;
	private static boolean active;
	private static List<TourStep> steps = Collections.emptyList();
	private static int index;
	private static boolean startedClient;
	private static boolean designAtStepStart;

	private InGameTour() {
	}

	/** Reads and removes the launcher's request - a crash before this point never replays the tour on a later launch. */
	private static boolean consumeTrigger(MinecraftClient client) {
		File file = new File(new File(client.runDirectory, "config"), TRIGGER_FILE);
		if (!file.isFile()) {
			return false;
		}
		try {
			JsonObject json = new Gson().fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), JsonObject.class);
			Files.deleteIfExists(file.toPath());
			return json != null && json.has("start") && json.get("start").getAsBoolean();
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException | ClassCastException e) {
			LOGGER.warn("Failed to read the tour request from " + file, e);
			return false;
		}
	}

	private static void start() {
		startedClient = ClientDesign.isClient();
		steps = InGameTourSteps.build(startedClient);
		index = -1;
		active = true;
		advance();
	}

	// --- State -------------------------------------------------------------------------------

	public static TourStep current() {
		return active && index >= 0 && index < steps.size() ? steps.get(index) : null;
	}

	/** The design when the current step began - steps that ask for a design switch wait for it to change. */
	static boolean designAtStepStart() {
		return designAtStepStart;
	}

	/** Position among the steps that apply right now, from 1 - for "Step X of Y". */
	static int stepNumber() {
		int number = 0;
		for (int i = 0; i <= index && i < steps.size(); i++) {
			if (steps.get(i).available.getAsBoolean()) {
				number++;
			}
		}
		return Math.max(number, 1);
	}

	static int stepCount() {
		int count = 0;
		for (TourStep step : steps) {
			if (step.available.getAsBoolean()) {
				count++;
			}
		}
		return count;
	}

	/** "Next": the step's own action first (it may open a screen), then the next step. */
	static void next() {
		TourStep step = current();
		if (step == null) {
			return;
		}
		if (step.onNext != null) {
			step.onNext.accept(MinecraftClient.getInstance());
		}
		advance();
	}

	private static void advance() {
		do {
			index++;
		} while (index < steps.size() && !steps.get(index).available.getAsBoolean());
		designAtStepStart = ClientDesign.isClient();
		if (index >= steps.size()) {
			finish();
		}
	}

	/** Past the last step: the question which design to keep - asked at the very end. */
	private static void finish() {
		active = false;
		MinecraftClient.getInstance().setScreen(new TourFinishScreen());
	}

	/** "End tour": back to the design the player had before the tour switched it around. */
	static void end() {
		active = false;
		if (ClientDesign.isClient() != startedClient) {
			ClientDesign.setClient(startedClient);
			MinecraftClient client = MinecraftClient.getInstance();
			Screen screen = client.currentScreen;
			if (screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
				// Rebuilds the layout for the restored design.
				screen.init(client, screen.width, screen.height);
			}
		}
	}

	/** Once per game tick. */
	public static void tick(MinecraftClient client) {
		if (!triggerRead) {
			triggerRead = true;
			pendingStart = consumeTrigger(client);
		}
		if (pendingStart && client.currentScreen instanceof TitleScreen) {
			pendingStart = false;
			start();
		}
		TourStep step = current();
		if (step != null && step.done != null && step.done.test(client)) {
			advance();
		}
	}

	// --- Input -------------------------------------------------------------------------------

	/** Whether the current step holds back input on this screen at all. */
	private static boolean blocking(Screen screen) {
		TourStep step = current();
		return step != null && step.kind != TourStep.Kind.WAIT && step.screen.test(screen) && !TitleScreenDesign.isRunning();
	}

	/** Whether the screen gets the mouse event the game is handing out right now. */
	public static boolean allowMouse(Screen screen) {
		TourStep step = current();
		if (step == null) {
			return true;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		// The same sum the game's screens do to get from window pixels to their own coordinates.
		int x = Mouse.getEventX() * screen.width / client.width;
		int y = screen.height - Mouse.getEventY() * screen.height / client.height - 1;
		if (Mouse.getEventButton() < 0) {
			// Moved, or the wheel.
			return allowPointer(screen, step, x, y);
		}
		// A button let go always arrives - a slider held when the step began has to be released.
		return !Mouse.getEventButtonState() || allowClick(screen, step, x, y);
	}

	private static boolean allowClick(Screen screen, TourStep step, int x, int y) {
		if (TourOverlay.endLink != null && TourOverlay.endLink.contains(x, y)) {
			end();
			return false;
		}
		if (TourOverlay.nextButton != null && TourOverlay.nextButton.contains(x, y)) {
			next();
			return false;
		}
		if (!blocking(screen)) {
			return true;
		}
		if (TourOverlay.box != null && TourOverlay.box.contains(x, y)) {
			return false;
		}
		return step.targetClickable() && TourOverlay.target != null && TourOverlay.target.contains(x, y);
	}

	private static boolean allowPointer(Screen screen, TourStep step, int x, int y) {
		if (!blocking(screen)) {
			return true;
		}
		return step.targetClickable() && TourOverlay.target != null && TourOverlay.target.contains(x, y);
	}

	/**
	 * Whether the screen gets the key event the game is handing out right now. Enter is "Next"; every
	 * other key waits - Escape included, so no step's screen gets closed by accident.
	 */
	public static boolean allowKey(Screen screen) {
		if (!blocking(screen)) {
			return true;
		}
		TourStep step = current();
		int key = Keyboard.getEventKey();
		if (step != null && step.hasNextButton() && Keyboard.getEventKeyState() && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
			next();
		}
		return false;
	}
}
