package com.tntsallin1client.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Draws (and drag-scrolls) a vertical scrollbar, matching vanilla's own
 * list-widget scrollbar look (same sprites {@link AbstractScrollArea} uses
 * and the same size/position math) - for screens that scroll a handful of
 * hand-positioned widgets ({@link CrosshairOptionsScreen},
 * {@link KeystrokesOptionsScreen}) instead of a real
 * {@code ContainerObjectSelectionList} like {@link ClientMenuScreen}'s own
 * list, which gets a scrollbar for free. Owns no scroll state itself -
 * reads/writes the owning screen's existing {@code scrollOffset} via the
 * supplier/consumer passed in, same "small helper class, screen keeps the
 * state" shape as {@link ColorPickerPanel}.
 *
 * <p>{@code x} is the scrollbar's own left edge, chosen by the caller -
 * originally pinned to the far screen edge, moved to sit right next to the
 * centered content column instead after user feedback that the screen-edge
 * position put it too far from the buttons it scrolls.
 */
public class ScrollBarHelper {
	public static final int WIDTH = 6;
	private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("widget/scroller");
	private static final Identifier SCROLLER_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("widget/scroller_background");

	private final int x;
	private final int viewportTop;
	private final int viewportHeight;
	private final IntSupplier scrollOffset;
	private final IntSupplier maxScroll;
	private final IntConsumer onDrag;
	private boolean dragging;

	public ScrollBarHelper(int x, int viewportTop, int viewportHeight,
			IntSupplier scrollOffset, IntSupplier maxScroll, IntConsumer onDrag) {
		this.x = x;
		this.viewportTop = viewportTop;
		this.viewportHeight = viewportHeight;
		this.scrollOffset = scrollOffset;
		this.maxScroll = maxScroll;
		this.onDrag = onDrag;
	}

	public void render(GuiGraphics guiGraphics) {
		if (this.maxScroll.getAsInt() <= 0) {
			return;
		}
		int thumbHeight = thumbHeight();
		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_SPRITE, this.x, this.viewportTop, WIDTH, this.viewportHeight);
		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, this.x, thumbY(thumbHeight), WIDTH, thumbHeight);
	}

	public boolean mouseClicked(MouseButtonEvent event) {
		if (this.maxScroll.getAsInt() <= 0 || !isOverScrollbar(event.x(), event.y())) {
			return false;
		}
		this.dragging = true;
		return true;
	}

	/** {@code dragY} is the same per-event drag delta {@code Screen#mouseDragged} already receives. */
	public boolean mouseDragged(double dragY) {
		if (!this.dragging) {
			return false;
		}
		int max = this.maxScroll.getAsInt();
		double scale = Math.max(1.0, (double) max / (this.viewportHeight - thumbHeight()));
		int newOffset = Mth.clamp((int) Math.round(this.scrollOffset.getAsInt() + dragY * scale), 0, max);
		this.onDrag.accept(newOffset);
		return true;
	}

	public boolean mouseReleased() {
		boolean wasDragging = this.dragging;
		this.dragging = false;
		return wasDragging;
	}

	private boolean isOverScrollbar(double mouseX, double mouseY) {
		return mouseX >= this.x && mouseX < this.x + WIDTH && mouseY >= this.viewportTop && mouseY < this.viewportTop + this.viewportHeight;
	}

	private int thumbHeight() {
		return Mth.clamp(this.viewportHeight * this.viewportHeight / (this.viewportHeight + this.maxScroll.getAsInt()), 32, this.viewportHeight - 8);
	}

	private int thumbY(int thumbHeight) {
		return this.viewportTop + this.scrollOffset.getAsInt() * (this.viewportHeight - thumbHeight) / this.maxScroll.getAsInt();
	}
}
