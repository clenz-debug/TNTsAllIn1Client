package com.tntsallin1client.debug;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GLX;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudElement;
import com.tntsallin1client.hud.HudLayout;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;

/**
 * The computer's specs and the game version on a page of their own, switched on and off with F3
 * plus a key (`ModKeyBindings.SYSTEM_INFO`). The lines are the ones the game's debug screen shows
 * in its top right corner, built the same way (bytecode-read from
 * `DebugScreenOverlay#getSystemInformation`).
 *
 * <p>Movable and resizable like every other HUD element although whether it shows is not a
 * setting: that is the key's doing and forgotten when the game closes, like the debug screen.
 */
public final class SystemInfoHud extends HudElement {
	/** Not saved - hidden again after every start. */
	public static boolean visible;

	private static final int BACKGROUND_COLOR = 0x90000000;
	private static final int PADDING = 4;
	private static final long BYTES_PER_MIB = 1024L * 1024L;

	@Override
	public String labelKey() {
		return "gui.tntsallin1client.menu.system_info";
	}

	@Override
	public boolean isEnabled(ClientConfig config) {
		return visible;
	}

	/** There is no switch to turn it on first - it can always be placed. */
	@Override
	public boolean isEditable(ClientConfig config) {
		return true;
	}

	@Override
	public HudLayout layout(ClientConfig config) {
		return config.systemInfoHudLayout;
	}

	private static List<String> lines(Minecraft client) {
		Runtime runtime = Runtime.getRuntime();
		long max = runtime.maxMemory();
		long total = runtime.totalMemory();
		long used = total - runtime.freeMemory();
		List<String> lines = new ArrayList<String>();
		lines.add(String.format("Java: %s %dbit", System.getProperty("java.version"), client.is64Bit() ? 64 : 32));
		lines.add(String.format("Mem: % 2d%% %03d/%03dMB", used * 100L / max, used / BYTES_PER_MIB, max / BYTES_PER_MIB));
		lines.add(String.format("Allocated: % 2d%% %03dMB", total * 100L / max, total / BYTES_PER_MIB));
		lines.add(String.format("CPU: %s", GLX.getCpuInfo()));
		lines.add(String.format("Display: %dx%d (%s)", client.window.getWidth(), client.window.getHeight(), GLX.getVendor()));
		lines.add(GLX.getRenderer());
		lines.add(GLX.getOpenGLVersion());
		lines.add("Minecraft " + SharedConstants.getCurrentVersion().getName());
		return lines;
	}

	@Override
	public int width(Minecraft client) {
		int width = 0;
		for (String line : lines(client)) {
			width = Math.max(width, client.font.width(line));
		}
		return width;
	}

	@Override
	public int height(Minecraft client) {
		return lines(client).size() * client.font.lineHeight;
	}

	@Override
	public int defaultX(Minecraft client, int screenWidth, int screenHeight) {
		return (screenWidth - width(client)) / 2;
	}

	@Override
	public int defaultY(Minecraft client, int screenWidth, int screenHeight) {
		return (screenHeight - height(client)) / 2;
	}

	@Override
	protected void draw(Minecraft client, ClientConfig config) {
		GuiComponent.fill(-PADDING, -PADDING, width(client) + PADDING, height(client) + PADDING, BACKGROUND_COLOR);
		int lineY = 0;
		for (String line : lines(client)) {
			client.font.drawShadow(line, 0, lineY, config.systemInfoTextColor);
			lineY += client.font.lineHeight;
		}
	}
}
