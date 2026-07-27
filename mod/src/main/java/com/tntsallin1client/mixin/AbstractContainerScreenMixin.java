package com.tntsallin1client.mixin;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
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
 *
 * <p>Bugfix round after the first live test: (1) the overlay alpha ({@code
 * 0xB0}, ~31% brightness left) crushed icon-style details drawn as part of
 * some subclasses' own {@code renderBg} (the disabled-hammer icon in
 * {@code AnvilScreen}, the book icon in {@code SmithingScreen}/
 * {@code EnchantmentScreen}) to the point of being unrecognizable - briefly
 * lowered to {@code 0x70}, but a second round of feedback (with a screenshot)
 * asked for the opposite: keep the background exactly as dark as the
 * original {@code 0xB0}, and instead brighten *only* the specific
 * icons/text sitting on top of it, the same way {@code
 * CreativeModeInventoryScreenMixin} now re-renders each tab's item icon at
 * full brightness after darkening its background. So the panel alpha is back
 * to {@code 0xB0} here; the anvil/smithing/enchanting icons mentioned above
 * don't have an equivalent per-icon fix yet (each uses a different, more
 * involved icon system - a plain sprite, a stateful {@code
 * CyclingSlotBackground}, and book-page animation frames, respectively) and
 * will look dark again until that's built - a known, explicitly accepted
 * trade-off for now rather than a regression nobody noticed. (2) the title
 * text ("Furnace", "Crafting Table", ...) drawn by {@code renderLabels} kept
 * vanilla's own fixed dark-gray color ({@code -12566464} / {@code 0xFF404040}),
 * which vanilla only ever needed to contrast against the *light* panel -
 * once the panel itself got darker, that same dark-gray text lost most of its
 * contrast. Fake-extends {@link Screen} (same trick as
 * {@code ItemEntityRendererMixin}) purely to get {@code font}/{@code title}
 * (declared there, inherited by {@code AbstractContainerScreen}) as ordinary
 * compile-time-inherited fields, on top of {@code @Shadow} for the extra
 * fields {@code renderLabels} itself needs that
 * {@code AbstractContainerScreen} declares directly. Covers every screen that
 * doesn't override {@code renderLabels} itself (the large majority - chest,
 * furnace variants, crafting table, brewing stand, grindstone, loom,
 * stonecutter, cartography table, hopper, shulker box, dispenser, crafter,
 * horse/llama, ...); {@code InventoryScreen} overrides it with its own single-line
 * version and gets the same fix via {@code InventoryScreenMixin} instead.
 * Anvil/beacon/creative-inventory/villager-trading also override
 * {@code renderLabels} with their own (more involved) positioning logic and
 * aren't covered yet - not reported as unreadable so far, left for a later
 * pass if that changes.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
	private static final int DARK_OVERLAY_COLOR = 0xB0000000;
	private static final int LIGHT_LABEL_COLOR = 0xFFE0E0E0;

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
	@Shadow
	protected int titleLabelX;
	@Shadow
	protected int titleLabelY;
	@Shadow
	protected int inventoryLabelX;
	@Shadow
	protected int inventoryLabelY;
	@Shadow
	@Final
	protected Component playerInventoryTitle;

	protected AbstractContainerScreenMixin(Component component) {
		super(component);
	}

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

	@Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onRenderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ClientConfig.get().darkInventoryEnabled) {
			return;
		}
		guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LIGHT_LABEL_COLOR, false);
		guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LIGHT_LABEL_COLOR, false);
		ci.cancel();
	}
}
