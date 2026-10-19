package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.gui.Tooltip;
import com.tntsallin1client.gui.WidgetAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements {@link WidgetAccess} - the 1.19.3+ position and tooltip methods the mod's menus call -
 * on 1.19.2's widgets, and draws a widget's {@link Tooltip} right after the widget while the mouse is
 * over it (like 1.19.2's own button tooltips; the tooltip's depth puts it above widgets drawn later).
 */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetAccessMixin implements WidgetAccess {
	@Shadow
	public int x;

	@Shadow
	public int y;

	@Shadow
	protected boolean isHovered;

	@Shadow
	public boolean visible;

	@Unique
	@Nullable
	private Tooltip tntsallin1client$tooltip;

	@Override
	public int getX() {
		return this.x;
	}

	@Override
	public int getY() {
		return this.y;
	}

	@Override
	public void setY(int y) {
		this.y = y;
	}

	@Override
	public void setTooltip(@Nullable Tooltip tooltip) {
		this.tntsallin1client$tooltip = tooltip;
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$renderTooltip(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.screen;
		if (this.tntsallin1client$tooltip != null && this.visible && this.isHovered && screen != null) {
			screen.renderTooltip(poseStack, this.tntsallin1client$tooltip.toCharSequence(minecraft), mouseX, mouseY);
		}
	}
}
