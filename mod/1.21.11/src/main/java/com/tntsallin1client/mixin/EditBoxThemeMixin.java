package com.tntsallin1client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text fields on themed screens ({@link ThemedUi}): only the border sprite is swapped for a themed
 * box - the text and cursor stay vanilla, so their positions still match what the field measures.
 */
@Mixin(EditBox.class)
public abstract class EditBoxThemeMixin extends AbstractWidget {
	protected EditBoxThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void tntsallin1client$drawThemedBorder(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		if (!ThemedUi.active()) {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
			return;
		}
		ClientTheme theme = ClientTheme.get();
		graphics.fill(x, y, x + width, y + height, theme.background1);
		graphics.renderOutline(x, y, width, height, this.isFocused() ? theme.accent4 : theme.accent2);
	}
}
