package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text fields on themed screens ({@link ThemedUi}): only the box is swapped for a themed one - the
 * frame first, then the inside, as the game draws them. The text and cursor stay the game's, so
 * their positions still match what the field measures.
 */
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
	private static final String FILL = "Lnet/minecraft/client/gui/widget/TextFieldWidget;fill(IIIII)V";

	@Shadow
	public abstract boolean isFocused();

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
