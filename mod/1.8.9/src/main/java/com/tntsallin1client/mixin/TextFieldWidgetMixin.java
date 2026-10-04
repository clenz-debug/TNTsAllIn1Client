package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text fields on themed screens ({@link ThemedUi}): the box is swapped for a themed one - the frame
 * first, then the inside, as the game draws them - and the text is in the client font (own user
 * request). The field measures its text with the font it draws it in: every place it asks the
 * game's font how wide a text is or how much of it fits gets the client font's answer there, so the
 * cursor, the selection and clicks into the text stay where the letters are.
 */
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
	private static final String TRIM = "Lnet/minecraft/client/font/TextRenderer;trimToWidth(Ljava/lang/String;I)Ljava/lang/String;";
	private static final String TRIM_FROM_END = "Lnet/minecraft/client/font/TextRenderer;trimToWidth(Ljava/lang/String;IZ)Ljava/lang/String;";
	private static final String DRAW = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;FFI)I";
	private static final String WIDTH = "Lnet/minecraft/client/font/TextRenderer;getStringWidth(Ljava/lang/String;)I";
	/** A capital letter of the client font sits half a pixel lower in its line than one of the game's font. */
	private static final float TEXT_LIFT = 0.5F;
	private static final String FILL = "Lnet/minecraft/client/gui/widget/TextFieldWidget;fill(IIIII)V";

	@Shadow
	public abstract boolean isFocused();

	@Redirect(method = {"mouseClicked(III)V", "render()V", "setSelectionEnd(I)V"}, at = @At(value = "INVOKE", target = TRIM))
	private String tnt$trimInClientFont(TextRenderer renderer, String text, int width) {
		return ThemedUi.active() ? ClientFont.trim(text, width, false) : renderer.trimToWidth(text, width);
	}

	@Redirect(method = "setSelectionEnd(I)V", at = @At(value = "INVOKE", target = TRIM_FROM_END))
	private String tnt$trimFromEndInClientFont(TextRenderer renderer, String text, int width, boolean fromEnd) {
		return ThemedUi.active() ? ClientFont.trim(text, width, fromEnd) : renderer.trimToWidth(text, width, fromEnd);
	}

	@Redirect(method = "render()V", at = @At(value = "INVOKE", target = WIDTH))
	private int tnt$widthInClientFont(TextRenderer renderer, String text) {
		return ThemedUi.active() ? ClientFont.width(text) : renderer.getStringWidth(text);
	}

	/** Like the game's own, this says where the text ended - the field puts its cursor there. */
	@Redirect(method = "render()V", at = @At(value = "INVOKE", target = DRAW))
	private int tnt$drawInClientFont(TextRenderer renderer, String text, float x, float y, int color) {
		if (!ThemedUi.active()) {
			return renderer.drawWithShadow(text, x, y, color);
		}
		ClientFont.draw(text, x, y - TEXT_LIFT, ThemedUi.textColor(color));
		return (int) x + ClientFont.width(text) + 1;
	}

	@Redirect(method = "render()V", at = @At(value = "INVOKE", target = FILL, ordinal = 0))
	private void tnt$drawThemedFrame(int x1, int y1, int x2, int y2, int color) {
		ClientTheme theme = ClientTheme.get();
		DrawableHelper.fill(x1, y1, x2, y2, !ThemedUi.active() ? color : this.isFocused() ? theme.accent4 : theme.accent2);
	}

	@Redirect(method = "render()V", at = @At(value = "INVOKE", target = FILL, ordinal = 1))
	private void tnt$drawThemedInside(int x1, int y1, int x2, int y2, int color) {
		DrawableHelper.fill(x1, y1, x2, y2, ThemedUi.active() ? ClientTheme.get().background1 : color);
	}
}
