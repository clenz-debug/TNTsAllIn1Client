package com.tntsallin1client.mixin;

import com.tntsallin1client.design.ClientLayoutAccess;
import com.tntsallin1client.design.ThemedButton;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.menu.MenuButtons;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mod's buttons on the title screen ("Client Mods", "Client Design", "Friends") - the mod's settings are
 * reachable before joining a world too - and the client design for it, see
 * {@link TitleScreenDesign}: rearranges what `init` built and takes over `render` while the client
 * layout is active, and draws the switch animation on top of the game's own layout while one is
 * running.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements ClientLayoutAccess {
	/** After this long the game's own layout has faded in completely (the background first, then the buttons). */
	@Unique
	private static final long FADE_IN_MS = 2000L;

	/** When the game's own layout began to fade in after the start of the game; 0 until it is drawn for the first time. */
	@Shadow
	private long fadeInStart;

	@Unique
	private boolean tnt$clientLayout;
	/** Set by a click on the logo; acted on when the screen is drawn next - see {@link #tntsallin1client$switchToMinecraft}. */
	@Unique
	private boolean tnt$switchToMinecraftPending;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Override
	public boolean tnt$isClientLayout() {
		return this.tnt$clientLayout;
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void tntsallin1client$addButtons(CallbackInfo ci) {
		this.tnt$clientLayout = TitleScreenDesign.useClientLayout();
		if (this.tnt$clientLayout) {
			TitleScreenDesign.buildClientLayout(this, this.buttons, this::tntsallin1client$clearButtons, this::addButton,
					this::tntsallin1client$switchToMinecraft);
		} else {
			MenuButtons.addToTitleScreen(this, this.buttons, this::addButton);
			MenuButtons.addFriendsButton(this, this::addButton, false);
		}
	}

	@Unique
	private void tntsallin1client$clearButtons() {
		this.buttons.clear();
		this.children.clear();
	}

	/**
	 * The logo: back to the game's own layout. Not right away: the game is still going through the
	 * buttons for this click, and with its own buttons put in their place right now it could click
	 * whichever of them lies under the cursor too.
	 */
	@Unique
	private void tntsallin1client$switchToMinecraft() {
		this.tnt$switchToMinecraftPending = true;
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$renderClientLayout(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (this.tnt$switchToMinecraftPending) {
			this.tnt$switchToMinecraftPending = false;
			// The game's own buttons are built right away, under the animation.
			TitleScreenDesign.switchToMinecraft(TitleScreenDesign.clientLogoRect(this.width, this.height));
			this.init(this.minecraft, this.width, this.height);
		}
		if (!this.tnt$clientLayout) {
			return;
		}
		if (this.fadeInStart == 0L) {
			// Started in the client design: the game's own layout would only begin its fade-in on a
			// switch to it - an empty second in which it draws nothing, the switch animation included.
			this.fadeInStart = Util.getMillis() - FADE_IN_MS;
		}
		float fade = TitleScreenDesign.clientWidgetAlpha();
		String tooltip = null;
		for (AbstractWidget button : this.buttons) {
			if (button instanceof ThemedButton) {
				ThemedButton themed = (ThemedButton) button;
				themed.fade = fade;
				if (themed.tooltip != null && button.isMouseOver(mouseX, mouseY)) {
					tooltip = themed.tooltip;
				}
			}
		}
		TitleScreenDesign.renderClientBackground(this.width, this.height);
		super.render(mouseX, mouseY, partialTick);
		if (tooltip != null) {
			this.renderTooltip(tooltip, mouseX, mouseY);
		}
		ci.cancel();
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$renderTransition(int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (TitleScreenDesign.renderTransition(this.width, this.height, TitleScreenDesign.clientLogoRect(this.width, this.height))
				&& TitleScreenDesign.useClientLayout()) {
			this.init(this.minecraft, this.width, this.height);
		}
	}

	/** No clicks mid-animation - the buttons underneath are about to be replaced. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockClicksDuringTransition(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (TitleScreenDesign.isRunning() || this.tnt$switchToMinecraftPending) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void tntsallin1client$endTransitionOnLeave(CallbackInfo ci) {
		TitleScreenDesign.cancel();
	}
}
