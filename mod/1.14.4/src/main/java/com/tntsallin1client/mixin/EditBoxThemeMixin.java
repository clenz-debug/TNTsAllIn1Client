package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientFont;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text fields on themed screens ({@link ThemedUi}): the box is swapped for a themed one - the frame
 * first, then the inside, as the game draws them - and the text is in the client font (own user
 * request). The field measures its text with the font it draws it in: every place it asks the
 * game's font how wide a text is or how much of it fits gets the client font's answer there, so the
 * cursor, the selection and clicks into the text stay where the letters are.
 */
@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin {
	private static final String TRIM = "Lnet/minecraft/client/gui/Font;substrByWidth(Ljava/lang/String;I)Ljava/lang/String;";
	private static final String TRIM_FROM_END = "Lnet/minecraft/client/gui/Font;substrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;";
	private static final String DRAW = "Lnet/minecraft/client/gui/Font;drawShadow(Ljava/lang/String;FFI)I";
	private static final String WIDTH = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I";
	private static final String FILL = "Lnet/minecraft/client/gui/components/EditBox;fill(IIIII)V";
	/** A capital letter of the client font sits half a pixel lower in its line than one of the game's font. */
	private static final float TEXT_LIFT = 0.5F;

	@Redirect(method = {"mouseClicked", "renderButton", "setHighlightPos"}, at = @At(value = "INVOKE", target = TRIM))
	private String tntsallin1client$trimInClientFont(Font font, String text, int width) {
		return ThemedUi.active() ? ClientFont.trim(text, width, false) : font.substrByWidth(text, width);
	}

	@Redirect(method = "setHighlightPos", at = @At(value = "INVOKE", target = TRIM_FROM_END))
	private String tntsallin1client$trimFromEndInClientFont(Font font, String text, int width, boolean fromEnd) {
		return ThemedUi.active() ? ClientFont.trim(text, width, fromEnd) : font.substrByWidth(text, width, fromEnd);
	}

	@Redirect(method = {"renderButton", "getScreenX"}, at = @At(value = "INVOKE", target = WIDTH))
	private int tntsallin1client$widthInClientFont(Font font, String text) {
		return ThemedUi.active() ? ClientFont.width(text) : font.width(text);
	}

	/** Like the game's own, this says where the text ended - the field puts its cursor there. */
	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = DRAW))
	private int tntsallin1client$drawInClientFont(Font font, String text, float x, float y, int color) {
		if (!ThemedUi.active()) {
			return font.drawShadow(text, x, y, color);
		}
		ClientFont.draw(text, x, y - TEXT_LIFT, ThemedUi.textColor(color));
		return (int) x + ClientFont.width(text) + 1;
	}

	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = FILL, ordinal = 0))
	private void tntsallin1client$drawThemedFrame(int x1, int y1, int x2, int y2, int color) {
		ClientTheme theme = ClientTheme.get();
		GuiComponent.fill(x1, y1, x2, y2, !ThemedUi.active() ? color : ((AbstractWidget) (Object) this).isFocused() ? theme.accent4 : theme.accent2);
	}

	@Redirect(method = "renderButton", at = @At(value = "INVOKE", target = FILL, ordinal = 1))
	private void tntsallin1client$drawThemedInside(int x1, int y1, int x2, int y2, int color) {
		GuiComponent.fill(x1, y1, x2, y2, ThemedUi.active() ? ClientTheme.get().background1 : color);
	}
}
