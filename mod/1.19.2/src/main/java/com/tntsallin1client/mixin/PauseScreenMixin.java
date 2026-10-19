package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.PauseScreenDesign;
import com.tntsallin1client.design.PauseScreenLayoutAccess;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client design for the pause menu - see {@link PauseScreenDesign}. Decides on each {@code init}
 * which layout to build ({@code PauseMenuIntegration} then arranges it), fades the buttons in and
 * draws the design switch animation, the same way {@code TitleScreenMixin} does for the title screen.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen implements PauseScreenLayoutAccess {
	@Unique
	private boolean tntsallin1client$clientLayout;

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	@Final
	private boolean showPauseMenu;

	@Override
	public boolean tntsallin1client$isClientLayout() {
		return this.tntsallin1client$clientLayout;
	}

	/** This version has no public {@code showsPauseMenu()} yet - only the field behind it. */
	@Override
	public boolean tntsallin1client$showsPauseMenu() {
		return this.showPauseMenu;
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void tntsallin1client$chooseLayout(CallbackInfo ci) {
		this.tntsallin1client$clientLayout = this.showPauseMenu && TitleScreenDesign.useClientLayout();
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void tntsallin1client$fadeWidgets(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.tntsallin1client$clientLayout) {
			tntsallin1client$setWidgetAlpha(TitleScreenDesign.clientWidgetAlpha());
		} else if (TitleScreenDesign.isRunningToMinecraft()) {
			// Vanilla widgets were already rebuilt - let them fade in under the animation.
			tntsallin1client$setWidgetAlpha(TitleScreenDesign.progress());
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$renderTransition(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		if (!this.showPauseMenu) {
			return;
		}
		if (TitleScreenDesign.renderTransition(graphics, this.width, this.height, PauseScreenDesign.clientLogoRect(this.width, this.height))) {
			if (TitleScreenDesign.useClientLayout()) {
				// Via resize, not rebuildWidgets - Fabric's init events have to fire for PauseMenuIntegration.
				this.resize(this.minecraft, this.width, this.height);
			} else {
				tntsallin1client$setWidgetAlpha(1.0f);
			}
		}
	}

	/**
	 * 1.19.2 draws the "Game Menu" heading straight in {@code render} (1.19.4 made it a widget, which
	 * {@code PauseScreenDesign} removes): in the client layout the client logo takes its place.
	 */
	@Redirect(method = "render", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/screens/PauseScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void tntsallin1client$hideHeadingInClientLayout(PoseStack poseStack, Font font, Component text, int x, int y, int color) {
		if (!this.tntsallin1client$clientLayout) {
			drawCenteredString(poseStack, font, text, x, y, color);
		}
	}

	/** No clicks mid-animation - the widgets underneath are about to be replaced or are still fading in. */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (TitleScreenDesign.isRunning()) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void removed() {
		TitleScreenDesign.cancel();
		super.removed();
	}

	@Unique
	private void tntsallin1client$setWidgetAlpha(float alpha) {
		for (GuiEventListener child : this.children()) {
			if (child instanceof AbstractWidget widget) {
				widget.setAlpha(alpha);
			}
		}
	}
}
