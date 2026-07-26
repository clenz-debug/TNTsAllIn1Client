package com.tntsallin1client.screenshot;

import com.tntsallin1client.TNTsAllIn1ClientMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Phase 5n: brief popup after a screenshot with "Open" and "Copy" buttons.
 * "Open" reuses {@code Util.getPlatform().openFile(...)} - the exact same
 * mechanism vanilla's own chat-message click-to-open link already uses.
 * "Copy" reads the PNG back into a {@link BufferedImage} and puts it on the
 * OS clipboard as actual image data via {@code java.awt} - vanilla's own
 * {@code ClickEvent.CopyToClipboard} only ever copies text, there's no
 * built-in equivalent for images.
 *
 * <p>Doesn't pause singleplayer ({@link #isPauseScreen()}) and reuses
 * {@link com.tntsallin1client.menu.HudEditorScreen}'s no-op
 * {@link #renderBackground} trick so the live game stays visible underneath
 * - this is meant to read as a transient toast, not a modal interruption.
 * Auto-closes after a few seconds if left alone.
 */
public class ScreenshotToastScreen extends Screen {
	private static final int AUTO_CLOSE_TICKS = 100;
	private static final int BUTTON_WIDTH = 76;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 8;

	private final File file;
	private int ticksOpen = 0;

	public ScreenshotToastScreen(File file) {
		super(Component.translatable("gui.tntsallin1client.screenshot_toast.title"));
		this.file = file;
	}

	@Override
	protected void init() {
		int x = (this.width - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2;
		int y = this.height - 60;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.screenshot_toast.open"),
						button -> {
							Util.getPlatform().openFile(this.file);
							this.onClose();
						})
				.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.screenshot_toast.copy"),
						button -> {
							copyToClipboard();
							this.onClose();
						})
				.bounds(x + BUTTON_WIDTH + BUTTON_GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// Intentionally empty, same as HudEditorScreen: keep the live game visible underneath.
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font,
				Component.translatable("gui.tntsallin1client.screenshot_toast.message", this.file.getName()),
				this.width / 2, this.height - 80, 0xFFFFFFFF);
	}

	@Override
	public void tick() {
		ticksOpen++;
		if (ticksOpen > AUTO_CLOSE_TICKS) {
			this.onClose();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(null);
	}

	private void copyToClipboard() {
		try {
			BufferedImage image = ImageIO.read(this.file);
			if (image == null) {
				TNTsAllIn1ClientMod.LOGGER.warn("[{}] Couldn't read screenshot for clipboard copy: {}", TNTsAllIn1ClientMod.MOD_ID, this.file);
				return;
			}
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new ImageTransferable(image), null);
		} catch (IOException e) {
			TNTsAllIn1ClientMod.LOGGER.warn("[{}] Failed to copy screenshot to clipboard.", TNTsAllIn1ClientMod.MOD_ID, e);
		}
	}
}
