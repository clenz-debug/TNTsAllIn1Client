package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text fields on the mod's own screens. In this version the border is two plain fills in
 * {@code renderWidget} (bytecode-verified): the outer frame, one pixel around the field, then its black
 * inside. The newer versions draw their frame sprite inside the field - so here a field on one of our
 * screens was two pixels wider than the buttons under it, and the name field at the top of the
 * waypoint pages lost its top line to the scrolling area's clip (own user report). On our screens the
 * frame is therefore drawn inside the field, in both designs; vanilla screens keep the game's.
 *
 * <p>Themed screens ({@link ThemedUi}): only the border is swapped for a themed box - the text and
 * cursor stay vanilla, so their positions still match what the field measures.
 */
@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin extends AbstractWidget {
	protected EditBoxThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void tntsallin1client$drawFrame(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		if (ThemedUi.active()) {
			ClientTheme theme = ClientTheme.get();
			graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, theme.background1);
			graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, this.isFocused() ? theme.accent4 : theme.accent2);
		} else if (ThemedUi.ownScreen()) {
			graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
		} else {
			graphics.fill(left, top, right, bottom, color);
		}
	}

	/** The black inside: none in the client design (it would cover the themed box), one pixel in from the edge on our screens. */
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void tntsallin1client$drawInside(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		if (ThemedUi.active()) {
			return;
		}
		if (ThemedUi.ownScreen()) {
			graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, color);
		} else {
			graphics.fill(left, top, right, bottom, color);
		}
	}
}
