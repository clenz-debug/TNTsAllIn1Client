package com.tntsallin1client.debug;

import com.mojang.blaze3d.platform.GlUtil;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudLayout;
import com.tntsallin1client.hud.HudElement;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Phase 5d, second iteration: CPU/GPU/Java/Minecraft-version info moved out of
 * the main F3 screen into its own page, toggled with F3+S (unused by vanilla in
 * this version - see the keyDebugXxx defaults in Options.java, none bind "S").
 * The lines are put together here from the same sources the debug screen
 * reads - its own method for them also wants a world and a targeted block,
 * and this page has to work from the title screen's HUD editor too.
 *
 * <p>Movable/resizable like every other HUD element (see the HUD editor) even though visibility
 * itself is the transient F3+S toggle, not a persisted "enabled" setting - positioning still needs
 * to persist independently of that, same {@link ClientConfig#systemInfoHudLayout} either way.
 */
public class SystemInfoOverlay implements HudElement {
	// Not persisted - resets to hidden on every launch, matching vanilla F3's own behavior.
	public static boolean visible = false;

	private static final long BYTES_PER_MIB = 1024L * 1024L;
	private static final int BACKGROUND_COLOR = 0x90000000;
	private static final int PADDING = 4;

	@Override
	public void render(GuiGraphics guiGraphics, float partialTick) {
		if (!visible) {
			return;
		}

		List<String> lines = buildLines();
		if (lines.isEmpty()) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		int width = contentWidth(font, lines);
		int height = contentHeight(font, lines);

		ClientConfig config = ClientConfig.get();
		HudLayout layout = config.systemInfoHudLayout;
		float x = layout.customPosition ? layout.x : defaultX(guiGraphics.guiWidth(), width);
		float y = layout.customPosition ? layout.y : defaultY(guiGraphics.guiHeight(), height);

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0.0F);
		guiGraphics.pose().scale(layout.scale, layout.scale, 1.0F);

		guiGraphics.fill(-PADDING, -PADDING, width + PADDING, height + PADDING, BACKGROUND_COLOR);
		int textColor = config.systemInfoTextColor;
		int lineY = 0;
		for (String line : lines) {
			guiGraphics.drawString(font, line, 0, lineY, textColor);
			lineY += font.lineHeight;
		}

		guiGraphics.pose().popPose();
	}

	/** Lines this overlay would currently show, in render order. Real hardware/version queries, no
	 * world or player needed - shared with the HUD editor so it can be positioned even from the
	 * title screen. */
	public static List<String> buildLines() {
		Minecraft client = Minecraft.getInstance();
		Runtime runtime = Runtime.getRuntime();
		long max = runtime.maxMemory();
		long total = runtime.totalMemory();
		long used = total - runtime.freeMemory();
		List<String> lines = new ArrayList<>();
		lines.add(String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")));
		lines.add(String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB", used * 100L / max, used / BYTES_PER_MIB, max / BYTES_PER_MIB));
		lines.add(String.format(Locale.ROOT, "Allocated: %2d%% %03dMB", total * 100L / max, total / BYTES_PER_MIB));
		lines.add(String.format(Locale.ROOT, "CPU: %s", GlUtil.getCpuInfo()));
		lines.add(String.format(Locale.ROOT, "Display: %dx%d (%s)", client.getWindow().getWidth(), client.getWindow().getHeight(), GlUtil.getVendor()));
		lines.add(GlUtil.getRenderer());
		lines.add(GlUtil.getOpenGLVersion());
		lines.add("Minecraft " + SharedConstants.getCurrentVersion().getName());
		return lines;
	}

	/** Unscaled pixel width of the whole box - shared with the HUD editor's drag bounds. */
	public static int contentWidth(Font font, List<String> lines) {
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		return width;
	}

	/** Unscaled pixel height of the whole box - shared with the HUD editor's drag bounds. */
	public static int contentHeight(Font font, List<String> lines) {
		return lines.size() * font.lineHeight;
	}

	public static float defaultX(int guiWidth, int width) {
		return (guiWidth - width) / 2f;
	}

	public static float defaultY(int guiHeight, int height) {
		return (guiHeight - height) / 2f;
	}
}
