package com.tntsallin1client.mixin;

import com.tntsallin1client.inventory.QuickSortUi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quick sort: the "Sort" button and the sort key on the player's inventory screens (see
 * {@link QuickSortUi}). Hooked into the base of all screens with slots because that is where the
 * panel's position and the handling of clicks and keys live; the survival and the creative
 * inventory both pass through here.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
	private static final int LEFT_MOUSE_BUTTON = 0;

	@Shadow
	protected int backgroundWidth;

	@Shadow
	protected int x;

	@Shadow
	protected int y;

	@Unique
	private ButtonWidget tnt$sortButton;

	/** The screen draws its buttons itself once the sort button is among them. */
	@Inject(method = "render(IIF)V", at = @At("HEAD"))
	private void tnt$placeSortButton(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		this.tnt$sortButton = QuickSortUi.place(this, this.buttons, this.tnt$sortButton, this.x + this.backgroundWidth, this.y);
	}

	@Inject(method = "render(IIF)V", at = @At("RETURN"))
	private void tnt$renderSortTooltip(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		if (this.tnt$sortButton != null && this.tnt$sortButton.isMouseOver(this.client, mouseX, mouseY)) {
			renderTooltip(I18n.translate("gui.tntsallin1client.sort_button.tooltip"), mouseX, mouseY);
		}
	}

	/**
	 * Taken before the screen sees the click: to it the button lies outside the panel, where a click
	 * drops what is on the cursor.
	 */
	@Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
	private void tnt$clickSortButton(int mouseX, int mouseY, int button, CallbackInfo ci) {
		if (button == LEFT_MOUSE_BUTTON && this.tnt$sortButton != null && this.tnt$sortButton.isMouseOver(this.client, mouseX, mouseY)) {
			this.tnt$sortButton.playDownSound(this.client.getSoundManager());
			QuickSortUi.trySort(this.client, this);
			ci.cancel();
		}
	}

	@Inject(method = "keyPressed(CI)V", at = @At("HEAD"), cancellable = true)
	private void tnt$pressSortKey(char character, int code, CallbackInfo ci) {
		if (QuickSortUi.keyPressed(this.client, this, code)) {
			ci.cancel();
		}
	}
}
