package com.tntsallin1client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.BiFunction;

/**
 * Text fields on themed screens ({@link ThemedUi}): the border sprite is swapped for a themed box, and
 * the text is in the client font and the theme's text color, without shadow (own user request, like in
 * 1.8.9). The field measures its text with the font it draws it in: every place it asks the game's font
 * how wide a text is or how much of it fits gets the client font's answer there, so the cursor, the
 * selection and clicks into the text stay where the letters are.
 */
@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin extends AbstractWidget {
	private static final String TRIM = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;";
	private static final String TRIM_FROM_END = "Lnet/minecraft/client/gui/Font;plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;";
	private static final String WIDTH = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I";
	private static final String DRAW_TEXT = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V";
	private static final String DRAW_STRING = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V";
	private static final String DRAW_HINT = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V";
	/** What a hint is drawn in: dimmed like the hints of 1.8.9's fields, not the game's dark gray. */
	private static final int HINT_COLOR = 0xFFA0A0A0;

	@Shadow
	private int textColor;

	@Shadow
	private int textColorUneditable;

	protected EditBoxThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
	private void tntsallin1client$drawThemedBorder(GuiGraphics graphics, RenderPipeline pipeline, ResourceLocation sprite, int x, int y, int width, int height) {
		if (!ThemedUi.active()) {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
			return;
		}
		ClientTheme theme = ClientTheme.get();
		graphics.fill(x, y, x + width, y + height, theme.background1);
		graphics.renderOutline(x, y, width, height, this.isFocused() ? theme.accent4 : theme.accent2);
	}

	@Redirect(method = {"onClick", "renderWidget", "updateTextPosition", "scrollTo"}, at = @At(value = "INVOKE", target = TRIM))
	private String tntsallin1client$trimInClientFont(Font font, String text, int maxWidth) {
		return ThemedUi.clientFontFields() ? ClientFont.trim(font, text, maxWidth, false) : font.plainSubstrByWidth(text, maxWidth);
	}

	@Redirect(method = "scrollTo", at = @At(value = "INVOKE", target = TRIM_FROM_END))
	private String tntsallin1client$trimFromEndInClientFont(Font font, String text, int maxWidth, boolean fromEnd) {
		return ThemedUi.clientFontFields() ? ClientFont.trim(font, text, maxWidth, fromEnd) : font.plainSubstrByWidth(text, maxWidth, fromEnd);
	}

	@Redirect(method = {"renderWidget", "updateTextPosition", "getScreenX"}, at = @At(value = "INVOKE", target = WIDTH))
	private int tntsallin1client$widthInClientFont(Font font, String text) {
		return ThemedUi.clientFontFields() ? ClientFont.width(font, text) : font.width(text);
	}

	/** The text as the field draws and measures it - with any coloring a formatter gave it. */
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object tntsallin1client$formatInClientFont(BiFunction<String, Integer, FormattedCharSequence> formatter, Object text, Object offset) {
		FormattedCharSequence formatted = formatter.apply((String) text, (Integer) offset);
		return ThemedUi.active() ? ClientFont.of(formatted) : formatted;
	}

	@Redirect(method = "renderWidget", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;textColor:I"))
	private int tntsallin1client$themedTextColor(EditBox box) {
		return ThemedUi.active() ? ThemedUi.fieldTextColor(this.textColor) : this.textColor;
	}

	@Redirect(method = "renderWidget", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/EditBox;textColorUneditable:I"))
	private int tntsallin1client$themedUneditableColor(EditBox box) {
		return ThemedUi.active() ? ThemedUi.fieldTextColor(this.textColorUneditable) : this.textColorUneditable;
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = DRAW_TEXT))
	private void tntsallin1client$drawTextWithoutShadow(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		graphics.drawString(font, text, x, y, color, shadow && !ThemedUi.active());
	}

	/** The suggestion after the text and the "_" cursor at its end. */
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = DRAW_STRING))
	private void tntsallin1client$drawStringInClientFont(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
		if (ThemedUi.active()) {
			graphics.drawString(font, ClientFont.of(text), x, y, ThemedUi.textColor(color), false);
		} else {
			graphics.drawString(font, text, x, y, color, shadow);
		}
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = DRAW_HINT))
	private void tntsallin1client$drawHintInClientFont(GuiGraphics graphics, Font font, Component hint, int x, int y, int color) {
		if (ThemedUi.active()) {
			graphics.drawString(font, ClientFont.of(hint.getString()), x, y, ThemedUi.textColor(HINT_COLOR), false);
		} else {
			graphics.drawString(font, hint, x, y, color);
		}
	}
}
