package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientLayoutAccess;
import com.tntsallin1client.design.PauseScreenDesign;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.menu.FriendsInGameScreen;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mod's buttons in the pause menu ("Client Mods", "Client Design", "Friends"), and the client
 * design for it - see {@link PauseScreenDesign}. Decides on each `init` which layout to build and
 * draws the design switch animation, the same way `TitleScreenMixin` does for the title screen.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen implements ClientLayoutAccess {
	@Unique
	private boolean tnt$clientLayout;
	/** Set by a click on the logo; acted on when the screen is drawn next - see `TitleScreenMixin`. */
	@Unique
	private boolean tnt$switchToMinecraftPending;

	@Override
	public boolean tnt$isClientLayout() {
		return this.tnt$clientLayout;
	}

	@Inject(method = "init()V", at = @At("HEAD"))
	private void tnt$chooseLayout(CallbackInfo ci) {
		this.tnt$clientLayout = TitleScreenDesign.useClientLayout();
	}

	@Inject(method = "init()V", at = @At("RETURN"))
	private void tnt$addClientModsButton(CallbackInfo ci) {
		MenuButtons.addToPauseMenu(this.buttons, this.tnt$clientLayout);
		if (this.tnt$clientLayout) {
			PauseScreenDesign.buildClientLayout(this.buttons, this.width, this.height);
		} else {
			PauseScreenDesign.measure(this.buttons);
		}
		MenuButtons.addFriendsButton(this.buttons);
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
	private void tnt$openClientMods(ButtonWidget button, CallbackInfo ci) {
		if (button.id == MenuButtons.OPEN_MENU_BUTTON_ID) {
			this.client.setScreen(ClientMenus.create(this));
		} else if (button.id == MenuButtons.FRIENDS_BUTTON_ID) {
			this.client.setScreen(new FriendsInGameScreen(this));
		} else if (button.id == MenuButtons.DESIGN_BUTTON_ID) {
			if (this.tnt$clientLayout) {
				this.tnt$switchToMinecraftPending = true;
			} else {
				TitleScreenDesign.switchToClient();
			}
			ci.cancel();
		}
	}

	@Inject(method = "render(IIF)V", at = @At("HEAD"))
	private void tnt$switchIfPending(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		if (this.tnt$switchToMinecraftPending) {
			this.tnt$switchToMinecraftPending = false;
			// The game's own buttons are built right away, under the animation.
			TitleScreenDesign.switchToMinecraft(PauseScreenDesign.clientLogoRect(this.width, this.height));
			this.init(this.client, this.width, this.height);
		}
	}

	/** The "Game Menu" heading - the client layout has the logo in its place. */
	@Redirect(method = "render(IIF)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/GameMenuScreen;drawCenteredString(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
	private void tnt$drawHeading(GameMenuScreen screen, TextRenderer textRenderer, String text, int centerX, int y, int color) {
		if (!this.tnt$clientLayout) {
			this.drawCenteredString(textRenderer, text, centerX, y, color);
		}
	}

	@Inject(method = "render(IIF)V", at = @At("TAIL"))
	private void tnt$renderTransition(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		if (TitleScreenDesign.renderTransition(this.width, this.height, PauseScreenDesign.clientLogoRect(this.width, this.height))
				&& TitleScreenDesign.useClientLayout()) {
			this.init(this.client, this.width, this.height);
		}
	}

	/** No clicks mid-animation - the buttons underneath are about to be replaced. */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (TitleScreenDesign.isRunning() || this.tnt$switchToMinecraftPending) {
			return;
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void removed() {
		TitleScreenDesign.cancel();
		super.removed();
	}
}
