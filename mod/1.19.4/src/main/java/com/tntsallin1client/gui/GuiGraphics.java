package com.tntsallin1client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 1.19.x stand-in for 1.20's {@code net.minecraft.client.gui.GuiGraphics}. Before 1.20 the GUI is
 * drawn through a bare {@link PoseStack} handed to {@link GuiComponent}'s static helpers, {@link Font}
 * and {@link net.minecraft.client.renderer.entity.ItemRenderer} - this wrapper offers the 1.20 method
 * names on top of them, so the mod's menus, HUD and overlays stay the same code as in the newer
 * versions (only the import differs). Everything is drawn right away; 1.20's batching does not exist
 * here, {@link #flush()} has nothing to do.
 *
 * <p>Vanilla methods that hand a {@code PoseStack} to an override or a mixin get wrapped with
 * {@link #of(PoseStack)}; calls back into vanilla pass {@link #pose()} on.
 */
public final class GuiGraphics {
	private final Minecraft minecraft;
	private final PoseStack pose;

	private GuiGraphics(PoseStack pose) {
		this.minecraft = Minecraft.getInstance();
		this.pose = pose;
	}

	public static GuiGraphics of(PoseStack pose) {
		return new GuiGraphics(pose);
	}

	public int guiWidth() {
		return this.minecraft.getWindow().getGuiScaledWidth();
	}

	public int guiHeight() {
		return this.minecraft.getWindow().getGuiScaledHeight();
	}

	public PoseStack pose() {
		return this.pose;
	}

	public MultiBufferSource.BufferSource bufferSource() {
		return this.minecraft.renderBuffers().bufferSource();
	}

	/** Draws immediately anyway; kept so code shared with 1.20+ reads the same. */
	public void flush() {
		RenderSystem.disableDepthTest();
		this.bufferSource().endBatch();
		RenderSystem.enableDepthTest();
	}

	public void hLine(int minX, int maxX, int y, int color) {
		if (maxX < minX) {
			int swap = minX;
			minX = maxX;
			maxX = swap;
		}
		this.fill(minX, y, maxX + 1, y + 1, color);
	}

	public void vLine(int x, int minY, int maxY, int color) {
		if (maxY < minY) {
			int swap = minY;
			minY = maxY;
			maxY = swap;
		}
		this.fill(x, minY + 1, x + 1, maxY, color);
	}

	public void enableScissor(int minX, int minY, int maxX, int maxY) {
		GuiComponent.enableScissor(minX, minY, maxX, maxY);
	}

	public void disableScissor() {
		GuiComponent.disableScissor();
	}

	public void setColor(float red, float green, float blue, float alpha) {
		RenderSystem.setShaderColor(red, green, blue, alpha);
	}

	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		GuiComponent.fill(this.pose, minX, minY, maxX, maxY, color);
	}

	public void fill(int minX, int minY, int maxX, int maxY, int z, int color) {
		GuiComponent.fill(this.pose, minX, minY, maxX, maxY, z, color);
	}

	public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
		this.fillGradient(minX, minY, maxX, maxY, 0, colorFrom, colorTo);
	}

	public void fillGradient(int minX, int minY, int maxX, int maxY, int z, int colorFrom, int colorTo) {
		Gradient.fill(this.pose, minX, minY, maxX, maxY, z, colorFrom, colorTo);
	}

	public void drawCenteredString(Font font, String text, int x, int y, int color) {
		this.drawString(font, text, x - font.width(text) / 2, y, color);
	}

	public void drawCenteredString(Font font, Component text, int x, int y, int color) {
		FormattedCharSequence sequence = text.getVisualOrderText();
		this.drawString(font, sequence, x - font.width(sequence) / 2, y, color);
	}

	public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) {
		this.drawString(font, text, x - font.width(text) / 2, y, color);
	}

	public int drawString(Font font, @Nullable String text, int x, int y, int color) {
		return this.drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, @Nullable String text, int x, int y, int color, boolean shadow) {
		if (text == null) {
			return 0;
		}
		return shadow ? font.drawShadow(this.pose, text, x, y, color) : font.draw(this.pose, text, x, y, color);
	}

	public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
		return this.drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		return shadow ? font.drawShadow(this.pose, text, x, y, color) : font.draw(this.pose, text, x, y, color);
	}

	public int drawString(Font font, Component text, int x, int y, int color) {
		return this.drawString(font, text, x, y, color, true);
	}

	public int drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
		return this.drawString(font, text.getVisualOrderText(), x, y, color, shadow);
	}

	public void drawWordWrap(Font font, FormattedText text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			this.drawString(font, line, x, y, color, false);
			y += 9;
		}
	}

	public void renderOutline(int x, int y, int width, int height, int color) {
		GuiComponent.renderOutline(this.pose, x, y, width, height, color);
	}

	public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
		RenderSystem.setShaderTexture(0, texture);
		GuiComponent.blit(this.pose, x, y, u, v, width, height);
	}

	public void blit(ResourceLocation texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		RenderSystem.setShaderTexture(0, texture);
		GuiComponent.blit(this.pose, x, y, z, u, v, width, height, textureWidth, textureHeight);
	}

	public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		RenderSystem.setShaderTexture(0, texture);
		GuiComponent.blit(this.pose, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
	}

	public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		RenderSystem.setShaderTexture(0, texture);
		GuiComponent.blit(this.pose, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public void blitNineSliced(ResourceLocation texture, int x, int y, int width, int height, int sliceWidth, int sliceHeight, int uWidth, int vHeight, int u, int v) {
		RenderSystem.setShaderTexture(0, texture);
		GuiComponent.blitNineSliced(this.pose, x, y, width, height, sliceWidth, sliceHeight, uWidth, vHeight, u, v);
	}

	public void renderItem(ItemStack stack, int x, int y) {
		this.minecraft.getItemRenderer().renderAndDecorateItem(this.pose, stack, x, y);
	}

	public void renderItem(ItemStack stack, int x, int y, int seed) {
		this.minecraft.getItemRenderer().renderAndDecorateItem(this.pose, stack, x, y, seed);
	}

	public void renderItem(ItemStack stack, int x, int y, int seed, int z) {
		this.minecraft.getItemRenderer().renderAndDecorateItem(this.pose, stack, x, y, seed, z);
	}

	public void renderFakeItem(ItemStack stack, int x, int y) {
		this.minecraft.getItemRenderer().renderAndDecorateFakeItem(this.pose, stack, x, y);
	}

	public void renderItem(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
		this.minecraft.getItemRenderer().renderAndDecorateItem(this.pose, entity, stack, x, y, seed);
	}

	public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
		this.minecraft.getItemRenderer().renderGuiItemDecorations(this.pose, font, stack, x, y);
	}

	public void renderItemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String countText) {
		this.minecraft.getItemRenderer().renderGuiItemDecorations(this.pose, font, stack, x, y, countText);
	}

	/**
	 * Tooltips are drawn by the open screen in 1.19.x (1.20 moved them here). Without a screen - on
	 * the HUD - there is nothing to draw them with, which matches where the mod uses them.
	 */
	public void renderTooltip(Font font, ItemStack stack, int x, int y) {
		Screen screen = this.minecraft.screen;
		if (screen != null) {
			screen.renderTooltip(this.pose, screen.getTooltipFromItem(stack), stack.getTooltipImage(), x, y);
		}
	}

	public void renderTooltip(Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
		Screen screen = this.minecraft.screen;
		if (screen != null) {
			screen.renderTooltip(this.pose, lines, image, x, y);
		}
	}

	public void renderTooltip(Font font, Component line, int x, int y) {
		Screen screen = this.minecraft.screen;
		if (screen != null) {
			screen.renderTooltip(this.pose, line, x, y);
		}
	}

	public void renderComponentTooltip(Font font, List<Component> lines, int x, int y) {
		Screen screen = this.minecraft.screen;
		if (screen != null) {
			screen.renderComponentTooltip(this.pose, lines, x, y);
		}
	}

	public void renderTooltip(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
		Screen screen = this.minecraft.screen;
		if (screen != null) {
			screen.renderTooltip(this.pose, lines, x, y);
		}
	}

	/** {@link GuiComponent#fillGradient} is protected - reached through a subclass. */
	private static final class Gradient extends GuiComponent {
		static void fill(PoseStack pose, int minX, int minY, int maxX, int maxY, int z, int colorFrom, int colorTo) {
			GuiComponent.fillGradient(pose, minX, minY, maxX, maxY, colorFrom, colorTo, z);
		}
	}
}
