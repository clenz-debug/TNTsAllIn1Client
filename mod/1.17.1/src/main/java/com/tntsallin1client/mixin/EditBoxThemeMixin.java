package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.BiFunction;

/**
 * Text fields on the mod's own screens. In this version the border is two plain fills in
 * {@code renderButton} (bytecode-verified): the outer frame, one pixel around the field, then its black
 * inside. The newer versions draw their frame sprite inside the field - so here a field on one of our
 * screens was two pixels wider than the buttons under it, and the name field at the top of the
 * waypoint pages lost its top line to the scrolling area's clip (own user report). On our screens the
 * frame is therefore drawn inside the field, in both designs; vanilla screens keep the game's.
 *
 * <p>Themed screens ({@link ThemedUi}): the border is swapped for a themed box, and the text is in
 * the client font and the theme's text color, without shadow (own user request, like in 1.8.9). The
 * field measures its text with the font it draws it in: every place it asks the game's font how wide
 * a text is or how much of it fits gets the client font's answer there, so the cursor, the selection
 * and clicks into the text stay where the letters are.
 */
@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin extends AbstractWidget {
	private static final String TRIM = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;";
	private static final String TRIM_FROM_END = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;";
	private static final String WIDTH = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I";
	private static final String DRAW_TEXT = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/util/FormattedCharSequence;FFI)I";
	private static final String DRAW_STRING = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I";
	private static final String CURSOR = "Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V";

	@Shadow
	private int textColor;

	@Shadow
	private int textColorUneditable;

	protected EditBoxThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Redirect(method = "renderButton", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/components/EditBox;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"))
	private void tntsallin1client$drawFrame(PoseStack poseStack, int left, int top, int right, int bottom, int color) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
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
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/gui/components/EditBox;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"))
	private void tntsallin1client$drawInside(PoseStack poseStack, int left, int top, int right, int bottom, int color) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		if (ThemedUi.active()) {
			return;
		}
		if (ThemedUi.ownScreen()) {
			graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, color);
		} else {
			graphics.fill(left, top, right, bottom, color);
		}
	}

	@Redirect(method = {"mouseClicked", "renderButton", "setHighlightPos"}, at = @At(value = "INVOKE", target = TRIM))
	private String tntsallin1client$trimInClientFont(Font font, String text, int maxWidth) {
		return ThemedUi.clientFontFields() ? ClientFont.trim(font, text, maxWidth, false) : font.plainSubstrByWidth(text, maxWidth);
	}

	@Redirect(method = "setHighlightPos", at = @At(value = "INVOKE", target = TRIM_FROM_END))
	private String tntsallin1client$trimFromEndInClientFont(Font font, String text, int maxWidth, boolean fromEnd) {
		return ThemedUi.clientFontFields() ? ClientFont.trim(font, text, maxWidth, fromEnd) : font.plainSubstrByWidth(text, maxWidth, fromEnd);
	}

	@Redirect(method = {"renderButton", "getScreenX"}, at = @At(value = "INVOKE", target = WIDTH))
	private int tntsallin1client$widthInClientFont(Font font, String text) {
		return ThemedUi.clientFontFields() ? ClientFont.width(font, text) : font.width(text);
	}

	/** The text as the field draws and measures it - with any coloring a formatter gave it. */
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object tntsallin1client$formatInClientFont(BiFunction<String, Integer, FormattedCharSequence> formatter, Object text, Object offset) {
		FormattedCharSequence formatted = formatter.apply((String) text, (Integer) offset);
		return ThemedUi.active() ? ClientFont.of(formatted) : formatted;
	}

	@Redirect(method = "renderButton", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;textColor:I"))
	private int tntsallin1client$themedTextColor(EditBox box) {
		return ThemedUi.active() ? ThemedUi.fieldTextColor(this.textColor) : this.textColor;
	}

	@Redirect(method = "renderButton", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;textColorUneditable:I"))
	private int tntsallin1client$themedUneditableColor(EditBox box) {
		return ThemedUi.active() ? ThemedUi.fieldTextColor(this.textColorUneditable) : this.textColorUneditable;
	}

	/**
	 * Without shadow, but answering like the game's shadowed text - one pixel past its end, where the
	 * field puts the cursor.
	 */
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = DRAW_TEXT))
	private int tntsallin1client$drawTextWithoutShadow(Font font, PoseStack poseStack, FormattedCharSequence text, float x, float y, int color) {
		if (ThemedUi.active()) {
			return font.draw(poseStack, text, x, y, color) + 1;
		}
		return font.drawShadow(poseStack, text, x, y, color);
	}

	/** The suggestion after the text and the "_" cursor at its end. */
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = DRAW_STRING))
	private int tntsallin1client$drawStringInClientFont(Font font, PoseStack poseStack, String text, float x, float y, int color) {
		if (ThemedUi.active()) {
			return font.draw(poseStack, ClientFont.of(text), x, y, ThemedUi.textColor(color)) + 1;
		}
		return font.drawShadow(poseStack, text, x, y, color);
	}

	/** The cursor line between two letters - the game draws it in a fixed light gray. */
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = CURSOR))
	private void tntsallin1client$drawThemedCursor(PoseStack poseStack, int left, int top, int right, int bottom, int color) {
		GuiComponent.fill(poseStack, left, top, right, bottom, ThemedUi.active() ? ThemedUi.textColor(0xFFFFFFFF) : color);
	}
}
