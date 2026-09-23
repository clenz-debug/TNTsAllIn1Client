package com.tntsallin1client.mixin;

import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.design.TitleScreenLayoutAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client design for the title screen - see {@link TitleScreenDesign}. Takes over {@code init} and
 * rendering while the client layout is active, and draws the switch animation on top of the vanilla
 * layout while one is running.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements TitleScreenLayoutAccess {
	@Unique
	private boolean tntsallin1client$clientLayout;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Override
	public boolean tntsallin1client$isClientLayout() {
		return this.tntsallin1client$clientLayout;
	}

	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$initClientLayout(CallbackInfo ci) {
		this.tntsallin1client$clientLayout = TitleScreenDesign.useClientLayout();
		if (this.tntsallin1client$clientLayout) {
			TitleScreenDesign.buildClientLayout((TitleScreen) (Object) this, this.width, this.height,
					widget -> this.addRenderableWidget(widget), this::tntsallin1client$rebuild);
			ci.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$renderClientLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.tntsallin1client$clientLayout) {
			tntsallin1client$setWidgetAlpha(TitleScreenDesign.clientWidgetAlpha());
			TitleScreenDesign.renderClientBackground(graphics, this.font, this.width, this.height);
			super.extractRenderState(graphics, mouseX, mouseY, a);
			ci.cancel();
		} else if (TitleScreenDesign.isRunningToMinecraft()) {
			// Vanilla widgets were already rebuilt - let them fade in under the animation.
			tntsallin1client$setWidgetAlpha(TitleScreenDesign.progress());
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void tntsallin1client$renderTransition(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (TitleScreenDesign.renderTransition(graphics, this.width, this.height)) {
			if (TitleScreenDesign.useClientLayout()) {
				tntsallin1client$rebuild();
			} else {
				tntsallin1client$setWidgetAlpha(1.0f);
			}
		}
	}

	/** No clicks mid-animation - the widgets underneath are about to be replaced or are still fading in. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockClicksDuringTransition(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (TitleScreenDesign.isRunning()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void tntsallin1client$endTransitionOnLeave(CallbackInfo ci) {
		TitleScreenDesign.cancel();
	}

	/**
	 * Rebuilds the widgets via {@code resize} rather than {@code rebuildWidgets} - Fabric fires its
	 * screen init events only for {@code init(int, int)} and {@code resize}, and without them
	 * {@code TitleScreenIntegration}'s Client Mods/Client Design buttons would be missing after switching back.
	 */
	@Unique
	private void tntsallin1client$rebuild() {
		this.resize(this.width, this.height);
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
