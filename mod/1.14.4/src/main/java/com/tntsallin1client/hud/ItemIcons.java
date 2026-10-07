package com.tntsallin1client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Draws item icons on the HUD. */
public final class ItemIcons {
	public static final int SIZE = 16;

	private ItemIcons() {
	}

	/** Draws an item the way the hotbar does, with its top left corner at `x`/`y`. Nothing for null or an empty stack. */
	public static void draw(Minecraft client, ItemStack stack, int x, int y) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		// Text drawn before leaves its color set, which would tint the item.
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		Lighting.turnOnGui();
		client.getItemRenderer().renderAndDecorateItem(stack, x, y);
		Lighting.turnOff();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();
	}
}
