package com.tntsallin1client.mixin;

import com.tntsallin1client.inventory.QuickSortUi;
import com.tntsallin1client.keybind.ModKeyBindings;
import com.tntsallin1client.recipe.PinnedRecipeManager;
import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shulker box preview, see {@link ShulkerPreviewRenderer}, the pin key of the pinned recipes
 * ({@link PinnedRecipeManager}), and quick sort: the "Sort" button and the sort key on the player's
 * inventory screens (see {@link QuickSortUi}). Hooked into the base of all screens with slots
 * because that is where the panel's position and the handling of keys live; the survival and the
 * creative inventory both pass through here.
 *
 * <p>The button is one of the screen's own: the screen asks its buttons before it takes a click for
 * its slots (bytecode-checked), so a click on the button - which to the slots lies outside the
 * panel, where a click drops what is on the cursor - never gets that far.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
	@Shadow
	protected int imageWidth;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;

	@Shadow
	protected Slot hoveredSlot;

	@Unique
	private Button tntsallin1client$sortButton;

	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	/** Before every frame: the setting may have changed, and the screen builds its buttons anew at times. */
	@Inject(method = "render", at = @At("HEAD"))
	private void tntsallin1client$placeSortButton(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Button button = this.tntsallin1client$sortButton;
		if (!QuickSortUi.appliesTo(this)) {
			if (button != null) {
				this.buttons.remove(button);
				this.children.remove(button);
				this.tntsallin1client$sortButton = null;
			}
			return;
		}
		if (button == null) {
			button = QuickSortUi.createButton(this);
			this.tntsallin1client$sortButton = button;
		}
		if (!this.buttons.contains(button)) {
			this.addButton(button);
		}
		QuickSortUi.position(button, this.leftPos + this.imageWidth, this.topPos);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void tntsallin1client$renderSortTooltip(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.tntsallin1client$sortButton != null && this.tntsallin1client$sortButton.isMouseOver(mouseX, mouseY)) {
			renderTooltip(I18n.get("gui.tntsallin1client.sort_button.tooltip"), mouseX, mouseY);
		}
	}

	/**
	 * Shulker box preview: every screen with slots draws the tooltip of the item under the cursor
	 * through this method, as the last thing of its frame. For a shulker box with the preview key held
	 * the panel with its contents is drawn instead.
	 */
	@Inject(method = "renderTooltip(II)V", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$previewShulkerBox(int mouseX, int mouseY, CallbackInfo ci) {
		if (this.minecraft.player.inventory.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()
				&& ShulkerPreviewRenderer.isPreviewable(this.hoveredSlot.getItem())) {
			ShulkerPreviewRenderer.draw(this.hoveredSlot.getItem(), mouseX, mouseY);
			ci.cancel();
		}
	}

	/** Pinned recipes: the pin key, where it is a mouse button. The click goes on to the screen as usual. */
	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void tntsallin1client$clickPinKey(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (ModKeyBindings.PIN_RECIPE.matchesMouse(button)) {
			PinnedRecipeManager.togglePin(this.minecraft, this);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$pressSortKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		// Pinned recipes: the pin key. The key goes on to the screen as usual.
		if (ModKeyBindings.PIN_RECIPE.matches(key, scanCode)) {
			PinnedRecipeManager.togglePin(this.minecraft, this);
		}
		if (QuickSortUi.keyPressed(this.minecraft, this, key, scanCode)) {
			cir.setReturnValue(true);
		}
	}
}
