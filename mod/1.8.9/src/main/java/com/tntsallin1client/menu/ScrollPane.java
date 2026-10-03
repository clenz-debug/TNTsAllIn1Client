package com.tntsallin1client.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The scrolling of a column of rows that may be taller than the room it has: how far it is scrolled,
 * the mouse wheel, the draggable bar beside it and the clipping while it is drawn. The screen keeps
 * its rows itself and only asks {@link #offset()} where to put them.
 *
 * <p>Buttons inside the pane can't be in the screen's own button list - the game would draw them
 * over whatever lies above and below the pane and take clicks on the parts scrolled out of view. The
 * screen draws them between {@link #beginClip} and {@link #endClip} and clicks them itself, for
 * clicks {@link #contains} says are inside.
 */
final class ScrollPane {
	static final int SCROLLBAR_GAP = 8;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLL_STEP = 16;
	private static final int MIN_THUMB_HEIGHT = 32;

	/** Moves the screen's rows to where {@link #offset()} now puts them. */
	private final Runnable onScroll;
	private int top;
	private int bottom;
	private int scrollbarX;
	private int offset;
	private int maxScroll;
	private boolean dragging;
	/** Where on the scrollbar's thumb it was grabbed. */
	private int thumbGrabOffset;

	ScrollPane(Runnable onScroll) {
		this.onScroll = onScroll;
	}

	/** Called from the screen's `init()`. How far it was scrolled is kept, as far as the new size allows. */
	void layout(int top, int bottom, int scrollbarX, int contentHeight) {
		this.top = top;
		this.bottom = bottom;
		this.scrollbarX = scrollbarX;
		this.maxScroll = Math.max(0, contentHeight - (bottom - top));
		this.dragging = false;
		scrollTo(this.offset);
	}

	/** How many pixels of the rows lie above the visible part. */
	int offset() {
		return this.offset;
	}

	int top() {
		return this.top;
	}

	boolean contains(int mouseY) {
		return mouseY >= this.top && mouseY < this.bottom;
	}

	private void scrollTo(int newOffset) {
		this.offset = MathHelper.clamp(newOffset, 0, this.maxScroll);
		this.onScroll.run();
	}

	/** The game hands a screen clicks and drags but not the wheel - call this from the screen's `handleMouse()`. */
	void handleWheel() {
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			scrollTo(this.offset - Integer.signum(wheel) * SCROLL_STEP);
		}
	}

	/** @return whether the click was on the scrollbar and is dealt with */
	boolean mouseClicked(int mouseX, int mouseY) {
		if (this.maxScroll <= 0 || !contains(mouseY) || mouseX < this.scrollbarX || mouseX >= this.scrollbarX + SCROLLBAR_WIDTH) {
			return false;
		}
		// Grabbed on the thumb it keeps that spot under the cursor, clicked beside it the thumb jumps there.
		boolean onThumb = mouseY >= thumbY() && mouseY < thumbY() + thumbHeight();
		this.thumbGrabOffset = onThumb ? mouseY - thumbY() : thumbHeight() / 2;
		this.dragging = true;
		mouseDragged(mouseY);
		return true;
	}

	void mouseDragged(int mouseY) {
		if (this.dragging) {
			int thumbTravel = this.bottom - this.top - thumbHeight();
			scrollTo(Math.round((mouseY - this.thumbGrabOffset - this.top) * (float) this.maxScroll / thumbTravel));
		}
	}

	void mouseReleased() {
		this.dragging = false;
	}

	private int thumbHeight() {
		int height = this.bottom - this.top;
		return MathHelper.clamp(height * height / (height + this.maxScroll), MIN_THUMB_HEIGHT, height - 8);
	}

	private int thumbY() {
		return this.top + this.offset * (this.bottom - this.top - thumbHeight()) / this.maxScroll;
	}

	/** From here until {@link #endClip} nothing is drawn outside the pane. */
	void beginClip(MinecraftClient client) {
		// The clip area is given in window pixels, counted from the bottom edge.
		int scale = new Window(client).getScaleFactor();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(0, client.height - this.bottom * scale, client.width, (this.bottom - this.top) * scale);
	}

	void endClip() {
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	/** Same look as the scrollbar of the game's own lists; nothing if everything fits. */
	void renderScrollbar() {
		if (this.maxScroll <= 0) {
			return;
		}
		int thumbY = thumbY();
		int thumbHeight = thumbHeight();
		DrawableHelper.fill(this.scrollbarX, this.top, this.scrollbarX + SCROLLBAR_WIDTH, this.bottom, 0xFF000000);
		DrawableHelper.fill(this.scrollbarX, thumbY, this.scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFF808080);
		DrawableHelper.fill(this.scrollbarX, thumbY, this.scrollbarX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight - 1, 0xFFC0C0C0);
	}
}
