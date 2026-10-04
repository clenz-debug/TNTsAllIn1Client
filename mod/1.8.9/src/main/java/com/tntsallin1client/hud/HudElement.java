package com.tntsallin1client.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;

/**
 * One thing the mod draws on the HUD. An element only says what it shows and how big that is;
 * where it sits and at what scale comes from its {@link HudLayout}, applied here - so every element
 * can be moved and resized in the HUD editor ({@code HudEditorScreen}) without code of its own for
 * it. A new element is added to {@link HudElements#ALL}, nothing else.
 */
public abstract class HudElement {
	/** Translation key of the element's name - the same one its row in the mod menu uses. */
	public abstract String labelKey();

	public abstract boolean isEnabled(ClientConfig config);

	/** Whether the HUD editor offers the element - normally exactly when it is switched on. */
	public boolean isEditable(ClientConfig config) {
		return isEnabled(config);
	}

	public abstract HudLayout layout(ClientConfig config);

	/** Unscaled size of what {@link #draw} currently draws. */
	public abstract int width(MinecraftClient client);

	public abstract int height(MinecraftClient client);

	/** Where the element sits until the player moves it. */
	public abstract int defaultX(MinecraftClient client, int screenWidth, int screenHeight);

	public abstract int defaultY(MinecraftClient client, int screenWidth, int screenHeight);

	/** Draws the element with its top left corner at 0/0, unscaled. */
	protected abstract void draw(MinecraftClient client, ClientConfig config);

	/** For an element that steps aside in some situation although it is switched on. */
	protected boolean isHidden(MinecraftClient client) {
		return false;
	}

	public final float x(MinecraftClient client, ClientConfig config, int screenWidth, int screenHeight) {
		HudLayout layout = layout(config);
		return layout.customPosition ? layout.x : defaultX(client, screenWidth, screenHeight);
	}

	public final float y(MinecraftClient client, ClientConfig config, int screenWidth, int screenHeight) {
		HudLayout layout = layout(config);
		return layout.customPosition ? layout.y : defaultY(client, screenWidth, screenHeight);
	}

	/** Draws the element in place during the game. */
	public final void render(MinecraftClient client, ClientConfig config, int screenWidth, int screenHeight) {
		if (isEnabled(config) && !isHidden(client)) {
			renderInPlace(client, config, screenWidth, screenHeight);
		}
	}

	/** Draws the element in place whether or not the game would show it right now - for the HUD editor's preview. */
	public final void renderInPlace(MinecraftClient client, ClientConfig config, int screenWidth, int screenHeight) {
		float scale = layout(config).scale;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x(client, config, screenWidth, screenHeight), y(client, config, screenWidth, screenHeight), 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		draw(client, config);
		GlStateManager.popMatrix();
		// Drawing text leaves its color set - whatever the game draws next would be tinted with it.
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
