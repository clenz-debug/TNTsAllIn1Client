package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
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
 *
 * <p><b>Bugfix (crafting table / furnace / player inventory never showed a
 * preview):</b> this used to inject into {@code AbstractContainerScreen#render}.
 * That method is only reliably called for screens that don't override
 * {@code render()} themselves (chests, villager trading, ...). Any screen
 * based on {@code AbstractRecipeBookScreen} - the player inventory, the
 * crafting table, furnace/blast furnace/smoker - overrides {@code render()}
 * and calls {@code super.renderContents(...)} directly instead of
 * {@code super.render(...)}, skipping {@code AbstractContainerScreen#render}
 * (and this injection) entirely. {@code renderContents} is the method both
 * paths actually share - it's also where {@code hoveredSlot} itself gets
 * refreshed - so injecting there instead covers every {@code AbstractContainerScreen}
 * subclass regardless of which one it is.
 *
 * <p>Phase 5r addition: dark inventory background. Unlike the quirk above,
 * {@code renderBackground} (which calls the per-subclass {@code renderBg} that
 * paints the actual gray panel texture) is a completely different, always-called
 * lifecycle step - {@code Screen#renderWithTooltipAndSubtitles} calls
 * {@code this.renderBackground(...)} and {@code this.render(...)} as two
 * separate, unconditional steps, so the {@code render()}-skipping quirk above
 * never applies here. Verified nothing under {@code screens/inventory/} other
 * than {@code AbstractContainerScreen} itself overrides {@code renderBackground}
 * (grepped the decompiled sources), so one inject on this one method's tail
 * covers every container screen (chest, furnace, crafting table, anvil,
 * enchanting table, villager trading, creative inventory's item-picker tabs,
 * ...) without needing a mixin per screen type. A flat semi-transparent black
 * {@code guiGraphics.fill(...)} over the panel's own bounds darkens it - alpha
 * blending onto black is a pure "multiply by a constant" in disguise, so the
 * existing bevel/shading detail of the vanilla texture stays intact, just
 * dimmed - rather than needing to intercept/retexture the individual blit
 * calls each subclass makes for its own background image.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
	private static final int DARK_OVERLAY_COLOR = 0xB0000000;

	@Shadow
	private @Nullable Slot hoveredSlot;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;
	@Shadow
	protected int imageWidth;
	@Shadow
	protected int imageHeight;

	@Inject(method = "renderContents", at = @At("TAIL"))
	private void tntsallin1client$onRenderContentsTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ShulkerPreviewRenderer.capture(
				this.hoveredSlot != null && this.hoveredSlot.hasItem() ? this.hoveredSlot.getItem() : null,
				mouseX, mouseY);
	}

	@Inject(method = "renderBackground", at = @At("TAIL"))
	private void tntsallin1client$onRenderBackgroundTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!ClientConfig.get().darkInventoryEnabled) {
			return;
		}
		guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, DARK_OVERLAY_COLOR);
	}
}
