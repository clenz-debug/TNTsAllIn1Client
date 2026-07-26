package com.tntsallin1client.mixin;

import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

/**
 * Phase 5k rebuild, part 1/2: captures the hovered slot's item (if any) for
 * {@link ShulkerPreviewRenderer} once {@code hoveredSlot} is up to date for
 * this frame. See {@link ShulkerPreviewRenderer} for why this is split
 * across two mixins instead of one.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	@Shadow
	private @Nullable Slot hoveredSlot;

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ShulkerPreviewRenderer.capture(
				this.hoveredSlot != null && this.hoveredSlot.hasItem() ? this.hoveredSlot.getItem() : null,
				mouseX, mouseY);
	}
}
