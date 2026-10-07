package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientLayoutAccess;
import com.tntsallin1client.design.PauseScreenDesign;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mod's buttons in the pause menu ("Client Mods", "Client Design"), and the client design for
 * it - see {@link PauseScreenDesign}. Decides on each `init` which layout to build and draws the
 * design switch animation, the same way `TitleScreenMixin` does for the title screen.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen implements ClientLayoutAccess {
	@Unique
	private boolean tnt$clientLayout;
	/** Set by a click on the logo; acted on when the screen is drawn next - see `TitleScreenMixin`. */
	@Unique
	private boolean tnt$switchToMinecraftPending;

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Override
	public boolean tnt$isClientLayout() {
		return this.tnt$clientLayout;
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void tntsallin1client$addButtons(CallbackInfo ci) {
		this.tnt$clientLayout = TitleScreenDesign.useClientLayout() && !this.buttons.isEmpty();
		MenuButtons.addToPauseMenu(this, this.buttons, this::addButton, this.tnt$clientLayout);
		if (this.tnt$clientLayout) {
			PauseScreenDesign.buildClientLayout(this.buttons, this.width, this.height, this::addButton, () -> this.tnt$switchToMinecraftPending = true);
		} else {
			PauseScreenDesign.measure(this.buttons);
		}
		// Not on the game's bare pause screen, which has no buttons at all.
		if (!this.buttons.isEmpty()) {
			MenuButtons.addFriendsButton(this, this::addButton, false);
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void tntsallin1client$switchIfPending(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.tnt$switchToMinecraftPending) {
			this.tnt$switchToMinecraftPending = false;
			// The game's own buttons are built right away, under the animation.
			TitleScreenDesign.switchToMinecraft(PauseScreenDesign.clientLogoRect(this.width, this.height));
			this.init(this.minecraft, this.width, this.height);
		}
		float fade = TitleScreenDesign.clientWidgetAlpha();
		for (AbstractWidget button : this.buttons) {
			if (button instanceof ThemedButton) {
				((ThemedButton) button).fade = fade;
			}
		}
	}

	/** The "Game Menu" heading - the client layout has the logo in its place. */
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/PauseScreen;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
	private void tntsallin1client$drawHeading(PauseScreen screen, Font font, String text, int centerX, int y, int color) {
		if (!this.tnt$clientLayout) {
			this.drawCenteredString(font, text, centerX, y, color);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$renderTransition(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.tnt$clientLayout) {
			for (AbstractWidget button : this.buttons) {
				if (button instanceof ThemedButton && ((ThemedButton) button).tooltip != null && button.isMouseOver(mouseX, mouseY)) {
					this.renderTooltip(((ThemedButton) button).tooltip, mouseX, mouseY);
				}
			}
		}
		if (TitleScreenDesign.renderTransition(this.width, this.height, PauseScreenDesign.clientLogoRect(this.width, this.height))
				&& TitleScreenDesign.useClientLayout()) {
			this.init(this.minecraft, this.width, this.height);
		}
	}

	/** No clicks mid-animation - the buttons underneath are about to be replaced. */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (TitleScreenDesign.isRunning() || this.tnt$switchToMinecraftPending) {
			return false;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void removed() {
		TitleScreenDesign.cancel();
		super.removed();
	}
}
