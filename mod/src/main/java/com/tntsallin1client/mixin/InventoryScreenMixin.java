package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5r follow-up: the player's own inventory screen overrides {@code
 * renderLabels} itself (a single "Inventory" title line, no second player-
 * inventory label since it *is* the player inventory) instead of using
 * {@code AbstractContainerScreen}'s default - so {@code
 * AbstractContainerScreenMixin}'s light-text fix, which only reaches the
 * default implementation, never runs for this, the single most commonly
 * opened container screen of all. Same fix, mirrored here: fake-extends
 * {@code AbstractContainerScreen<InventoryMenu>} to get {@code font}/{@code
 * title} (from {@code Screen}, two levels up) and {@code titleLabelX}/{@code
 * titleLabelY} (from {@code AbstractContainerScreen}, one level up) as
 * ordinary inherited fields - no {@code @Shadow} needed at all here, unlike
 * {@code AbstractContainerScreenMixin}, since none of the fields this
 * override needs are declared directly on {@code InventoryScreen} itself.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
	private static final int LIGHT_LABEL_COLOR = 0xFFE0E0E0;

	protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component component) {
		super(menu, inventory, component);
	}

	@Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ClientConfig.get().darkInventoryEnabled) {
			return;
		}
		guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LIGHT_LABEL_COLOR, false);
		ci.cancel();
	}
}
