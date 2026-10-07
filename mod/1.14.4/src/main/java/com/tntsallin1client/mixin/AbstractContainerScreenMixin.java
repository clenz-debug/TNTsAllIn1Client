package com.tntsallin1client.mixin;

import com.tntsallin1client.inventory.QuickSortUi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quick sort: the "Sort" button and the sort key on the player's inventory screens (see
 * {@link QuickSortUi}). Hooked into the base of all screens with slots because that is where the
 * panel's position and the handling of keys live; the survival and the creative inventory both pass
 * through here.
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

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$pressSortKey(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (QuickSortUi.keyPressed(this.minecraft, this, key, scanCode)) {
			cir.setReturnValue(true);
		}
	}
}
