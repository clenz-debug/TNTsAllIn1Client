package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scrollbars on themed screens ({@link ThemedUi}) - same flat track and accent thumb as the Client Mods card menu. */
@Mixin(AbstractScrollArea.class)
public abstract class AbstractScrollAreaThemeMixin extends AbstractWidget {
	protected AbstractScrollAreaThemeMixin(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	@Shadow
	protected abstract boolean scrollable();

	@Shadow
	protected abstract int scrollBarX();

	@Shadow
	public abstract int scrollBarY();

	@Shadow
	protected abstract int scrollerHeight();

	@Shadow
	public abstract int scrollbarWidth();

	@Inject(method = "extractScrollbar", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$drawThemedScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ThemedUi.active()) {
			return;
		}
		if (this.scrollable()) {
			ClientTheme theme = ClientTheme.get();
			int x = this.scrollBarX();
			int width = this.scrollbarWidth();
			graphics.fill(x, this.getY(), x + width, this.getBottom(), theme.background2);
			graphics.fill(x, this.scrollBarY(), x + width, this.scrollBarY() + this.scrollerHeight(), theme.accent3);
		}
		ci.cancel();
	}
}
