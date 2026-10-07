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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

/**
 * The in-game part of the guided tour, as in the other versions - the launcher's tour ends with
 * "show me the menus in the game too?", and on yes the launcher leaves `config/tntsallin1client-tour.json`
 * before starting the game. The tour then starts on the first title screen: steps in
 * {@link InGameTourSteps}, drawn by {@link TourOverlay}. While a step is shown, clicks and keys on
 * its screen are held back - only the highlighted part (where a step wants it clicked) and the
 * explanation's own buttons get through.
 *
 * <p>This version's Fabric API has no screen events to hang that on: the drawing comes from
 * `GameRendererMixin` (after a screen) and `HudElements` (without one), and `MouseHandlerMixin` and
 * `KeyboardHandlerMixin` ask here before the screen on display gets a click, the wheel or a key.
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
	private static boolean consumeTrigger(Minecraft client) {
		File file = new File(new File(client.gameDirectory, "config"), TRIGGER_FILE);
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
			step.onNext.accept(Minecraft.getInstance());
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
		Minecraft.getInstance().setScreen(new TourFinishScreen());
	}

	/** "End tour": back to the design the player had before the tour switched it around. */
	static void end() {
		active = false;
		if (ClientDesign.isClient() != startedClient) {
			ClientDesign.setClient(startedClient);
			Minecraft client = Minecraft.getInstance();
			Screen screen = client.screen;
			if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
				// Rebuilds the layout for the restored design.
				screen.init(client, screen.width, screen.height);
			}
		}
	}

	/** Once per game tick. */
	public static void tick(Minecraft client) {
		if (!triggerRead) {
			triggerRead = true;
			pendingStart = consumeTrigger(client);
		}
		if (pendingStart && client.screen instanceof TitleScreen) {
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

	/**
	 * Whether the screen gets a mouse button pressed at this spot (in its own coordinates). A button
	 * let go is never asked about - a slider held when the step began has to be released.
	 */
	public static boolean allowClick(Screen screen, int x, int y) {
		TourStep step = current();
		if (step == null) {
			return true;
		}
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

	/** Whether the screen gets the wheel turned at this spot. */
	public static boolean allowWheel(Screen screen, int x, int y) {
		TourStep step = current();
		if (step == null || !blocking(screen)) {
			return true;
		}
		return step.targetClickable() && TourOverlay.target != null && TourOverlay.target.contains(x, y);
	}

	/**
	 * Whether the screen gets a key pressed (`pressed`) or held down. Enter is "Next"; every other key
	 * waits - Escape included, so no step's screen gets closed by accident.
	 */
	public static boolean allowKey(Screen screen, int key, boolean pressed) {
		if (!blocking(screen)) {
			return true;
		}
		TourStep step = current();
		if (step != null && step.hasNextButton() && pressed && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
			next();
		}
		return false;
	}

	/** Whether the screen gets a typed character. */
	public static boolean allowChar(Screen screen) {
		return !blocking(screen);
	}
}
