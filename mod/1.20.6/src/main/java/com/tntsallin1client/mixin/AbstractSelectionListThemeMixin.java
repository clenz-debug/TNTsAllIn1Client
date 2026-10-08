package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lists on themed screens ({@link ThemedUi}): no panel behind them, thin accent lines instead of the separator
 * sprites, and the same flat scrollbar track and accent thumb as the Client Mods card menu.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListThemeMixin extends AbstractWidget {
	protected AbstractSelectionListThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$skipBackground(GuiGraphics graphics, CallbackInfo ci) {
		if (ThemedUi.active()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemedSeparators(GuiGraphics graphics, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		int color = ClientTheme.get().accent2;
		graphics.fill(this.getX(), this.getY() - 1, this.getRight(), this.getY(), color);
		graphics.fill(this.getX(), this.getBottom(), this.getRight(), this.getBottom() + 1, color);
		ci.cancel();
	}

	/** The scrollbar's two sprites in {@code renderWidget} (bytecode-verified): the track first, then the thumb. */
	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
	private void tntsallin1client$drawThemedTrack(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
		if (ThemedUi.active()) {
			graphics.fill(x, y, x + width, y + height, ClientTheme.get().background2);
		} else {
			graphics.blitSprite(sprite, x, y, width, height);
		}
	}

	@Redirect(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
	private void tntsallin1client$drawThemedThumb(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
		if (ThemedUi.active()) {
			graphics.fill(x, y, x + width, y + height, ClientTheme.get().accent3);
		} else {
			graphics.blitSprite(sprite, x, y, width, height);
		}
	}
}
