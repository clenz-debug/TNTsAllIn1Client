package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientLayoutAccess;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.menu.ClientMenus;
import com.tntsallin1client.menu.FriendsInGameScreen;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mod's buttons on the title screen ("Client Mods", "Client Design", "Friends") - the mod's
 * settings are reachable before joining a world too - and the client design for it, see
 * {@link TitleScreenDesign}: takes over `init` and `render` while the client layout is active, and
 * draws the switch animation on top of the game's own layout while one is running.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements ClientLayoutAccess {
	@Unique
	private boolean tnt$clientLayout;
	/** Set by a click on the logo; acted on when the screen is drawn next - see {@link #tnt$openClientMods}. */
	@Unique
	private boolean tnt$switchToMinecraftPending;

	@Override
	public boolean tnt$isClientLayout() {
		return this.tnt$clientLayout;
	}

	@Inject(method = "init()V", at = @At("HEAD"), cancellable = true)
	private void tnt$initClientLayout(CallbackInfo ci) {
		this.tnt$clientLayout = TitleScreenDesign.useClientLayout();
		if (this.tnt$clientLayout) {
			TitleScreenDesign.buildClientLayout(this.buttons, this.width, this.height);
			ci.cancel();
		}
	}

	@Inject(method = "init()V", at = @At("RETURN"))
	private void tnt$addClientModsButton(CallbackInfo ci) {
		// Not reached in the client layout, which has these buttons built in.
		MenuButtons.addToTitleScreen(this.buttons, this.width);
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
				// The logo: back to the game's own layout. Not from here: the game is still going through
				// the buttons for this click, and with its own buttons put in their place right now it
				// would click whichever of them lies under the cursor too (own user report: the switch
				// sometimes led into the multiplayer menu).
				this.tnt$switchToMinecraftPending = true;
			} else {
				TitleScreenDesign.switchToClient();
			}
			ci.cancel();
		}
	}

	@Inject(method = "render(IIF)V", at = @At("HEAD"), cancellable = true)
	private void tnt$renderClientLayout(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		if (this.tnt$switchToMinecraftPending) {
			this.tnt$switchToMinecraftPending = false;
			// The game's own buttons are built right away, under the animation.
			TitleScreenDesign.switchToMinecraft(TitleScreenDesign.clientLogoRect(this.width, this.height));
			tnt$rebuild();
		}
		if (!this.tnt$clientLayout) {
			return;
		}
		float alpha = TitleScreenDesign.clientWidgetAlpha();
		String tooltip = null;
		for (ButtonWidget button : this.buttons) {
			if (button instanceof ThemedButton) {
				ThemedButton themed = (ThemedButton) button;
				themed.alpha = alpha;
				if (themed.tooltip != null && button.isMouseOver(this.client, mouseX, mouseY)) {
					tooltip = themed.tooltip;
				}
			}
		}
		TitleScreenDesign.renderClientBackground(this.width, this.height);
		super.render(mouseX, mouseY, tickDelta);
		if (tooltip != null) {
			this.renderTooltip(tooltip, mouseX, mouseY);
		}
		ci.cancel();
	}

	@Inject(method = "render(IIF)V", at = @At("TAIL"))
	private void tnt$renderTransition(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
		if (TitleScreenDesign.renderTransition(this.width, this.height, TitleScreenDesign.clientLogoRect(this.width, this.height))
				&& TitleScreenDesign.useClientLayout()) {
			tnt$rebuild();
		}
	}

	/** No clicks mid-animation - the buttons underneath are about to be replaced. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void tnt$blockClicksDuringTransition(int mouseX, int mouseY, int button, CallbackInfo ci) {
		if (TitleScreenDesign.isRunning() || this.tnt$switchToMinecraftPending) {
			ci.cancel();
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void tnt$endTransitionOnLeave(CallbackInfo ci) {
		TitleScreenDesign.cancel();
	}

	/** Builds the screen's buttons anew - the way the game does it when the window is resized. */
	@Unique
	private void tnt$rebuild() {
		this.init(this.client, this.width, this.height);
	}
}
