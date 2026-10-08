package com.tntsallin1client.tour;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import com.tntsallin1client.compat.EssentialCompat;
import com.tntsallin1client.design.ClientDesign;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.menu.TourFinishScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.tntsallin1client.hud.HudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The in-game part of the guided tour (own user request) - the launcher's tour ends with "show me
 * the menus in the game too?", and on yes the launcher leaves {@link #TRIGGER} in the config folder
 * before starting the game. The tour then starts on the first title screen: steps in
 * {@link InGameTourSteps}, drawn by {@link TourOverlay}. While a step is shown, clicks and keys on
 * its screen are held back by Fabric's allow-events - only the highlighted part (where a step wants
 * it clicked) and the explanation's own buttons get through.
 */
public final class InGameTour {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path TRIGGER = FabricLoader.getInstance().getConfigDir().resolve("tntsallin1client-tour.json");

	private static boolean pendingStart;
	private static boolean active;
	private static List<TourStep> steps = List.of();
	private static int index;
	private static boolean startedClient;
	private static boolean designAtStepStart;

	private InGameTour() {
	}

	public static void register() {
		pendingStart = consumeTrigger();
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (pendingStart && screen instanceof TitleScreen) {
				pendingStart = false;
				start();
			}
			ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) -> TourOverlay.renderScreen(s, graphics));
			ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> allowClick(s, mouseX, mouseY));
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> allowPointer(s, mouseX, mouseY));
			ScreenKeyboardEvents.allowKeyPress(screen).register(InGameTour::allowKey);
		});
		ClientTickEvents.END_CLIENT_TICK.register(InGameTour::tick);
		HudElements.add((graphics, deltaTracker) -> TourOverlay.renderHud(graphics));
	}

	/** Reads and removes the launcher's request - a crash before this point never replays the tour on a later launch. */
	private static boolean consumeTrigger() {
		if (!Files.exists(TRIGGER)) return false;
		try {
			JsonObject json = new Gson().fromJson(Files.readString(TRIGGER), JsonObject.class);
			Files.deleteIfExists(TRIGGER);
			return json != null && json.has("start") && json.get("start").getAsBoolean();
		} catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
			LOGGER.warn("Failed to read the tour request from {}", TRIGGER, e);
			return false;
		}
	}

	private static void start() {
		startedClient = ClientDesign.isClient();
		steps = InGameTourSteps.build(startedClient, !EssentialCompat.isLoaded());
		index = -1;
		active = true;
		advance();
	}

	// --- State -------------------------------------------------------------------------------

	public static @Nullable TourStep current() {
		return active && index >= 0 && index < steps.size() ? steps.get(index) : null;
	}

	/** The design when the current step began - steps that ask for a design switch wait for it to change. */
	static boolean designAtStepStart() {
		return designAtStepStart;
	}

	/** Position among the steps that apply right now, from 1 - for "Schritt X von Y". */
	static int stepNumber() {
		int number = 0;
		for (int i = 0; i <= index && i < steps.size(); i++) {
			if (steps.get(i).available().getAsBoolean()) number++;
		}
		return Math.max(number, 1);
	}

	static int stepCount() {
		return (int) steps.stream().filter(step -> step.available().getAsBoolean()).count();
	}

	/** "Weiter": the step's own action first (it may open a screen), then the next step. */
	static void next() {
		TourStep step = current();
		if (step == null) return;
		if (step.onNext() != null) {
			step.onNext().accept(Minecraft.getInstance());
		}
		advance();
	}

	private static void advance() {
		do {
			index++;
		} while (index < steps.size() && !steps.get(index).available().getAsBoolean());
		designAtStepStart = ClientDesign.isClient();
		if (index >= steps.size()) {
			finish();
		}
	}

	/** Past the last step: the question which design to keep (own user request - asked at the very end). */
	private static void finish() {
		active = false;
		Minecraft.getInstance().setScreen(new TourFinishScreen());
	}

	/** "Tour beenden": back to the design the player had before the tour switched it around. */
	static void end() {
		active = false;
		if (!EssentialCompat.isLoaded() && ClientDesign.isClient() != startedClient) {
			ClientDesign.setClient(startedClient);
			Screen screen = Minecraft.getInstance().screen;
			if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
				// Rebuilds the layout for the restored design, through Fabric's init events like the switch itself.
				screen.resize(Minecraft.getInstance(), screen.width, screen.height);
			}
		}
	}

	private static void tick(Minecraft minecraft) {
		TourStep step = current();
		if (step != null && step.done() != null && step.done().test(minecraft)) {
			advance();
		}
	}

	// --- Input -------------------------------------------------------------------------------

	/** Whether the current step holds back input on this screen at all. */
	private static boolean blocking(Screen screen) {
		TourStep step = current();
		return step != null && step.kind() != TourStep.Kind.WAIT && step.screen().test(screen) && !TitleScreenDesign.isRunning();
	}

	private static boolean allowClick(Screen screen, double x, double y) {
		TourStep step = current();
		if (step == null) return true;
		if (TourOverlay.endLink != null && TourOverlay.endLink.contains(x, y)) {
			end();
			return false;
		}
		if (TourOverlay.nextButton != null && TourOverlay.nextButton.contains(x, y)) {
			next();
			return false;
		}
		if (!blocking(screen)) return true;
		if (TourOverlay.box != null && TourOverlay.box.contains(x, y)) return false;
		return step.targetClickable() && TourOverlay.target != null && TourOverlay.target.contains(x, y);
	}

	private static boolean allowPointer(Screen screen, double x, double y) {
		if (!blocking(screen)) return true;
		TourStep step = current();
		return step != null && step.targetClickable() && TourOverlay.target != null && TourOverlay.target.contains(x, y);
	}

	/** Enter is "Weiter"; every other key waits - Escape included, so no step's screen gets closed by accident. */
	private static boolean allowKey(Screen screen, int key, int scancode, int modifiers) {
		if (!blocking(screen)) return true;
		TourStep step = current();
		if (step != null && step.hasNextButton() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
			next();
		}
		return false;
	}
}
