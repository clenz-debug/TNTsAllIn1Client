package com.tntsallin1client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;

/** Draws item icons on the HUD. */
final class ItemIcons {
	static final int SIZE = 16;

	private ItemIcons() {
	}

	/** Draws an item the way the hotbar does, with its top left corner at `x`/`y`. Nothing for null. */
	static void draw(MinecraftClient client, ItemStack stack, int x, int y) {
		if (stack == null) {
			return;
		}
		// Text drawn before leaves its color set, which would tint the item.
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		DiffuseLighting.enable();
		client.getItemRenderer().renderInGuiWithOverrides(stack, x, y);
		DiffuseLighting.disable();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();
	}
}
