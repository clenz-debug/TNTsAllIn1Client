package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.TitleScreenDesign;
import com.tntsallin1client.design.TitleScreenLayoutAccess;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$renderClientLayout(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		if (this.tntsallin1client$clientLayout) {
			tntsallin1client$setWidgetAlpha(TitleScreenDesign.clientWidgetAlpha());
			TitleScreenDesign.renderClientBackground(graphics, this.font, this.width, this.height);
			super.render(graphics.pose(), mouseX, mouseY, a);
			ci.cancel();
		}
	}

	/**
	 * This version's title screen sets every widget's alpha itself each frame, right before drawing them
	 * (its own fade-in, 1 once that is over) - a value set at the start of {@code render} would not last.
	 * While the animation runs back to the Minecraft design, the already rebuilt vanilla widgets fade in
	 * under it instead.
	 */
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
	private void tntsallin1client$fadeWidgetsUnderTransition(AbstractWidget widget, float alpha) {
		widget.setAlpha(TitleScreenDesign.isRunningToMinecraft() ? TitleScreenDesign.progress() : alpha);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void tntsallin1client$renderTransition(PoseStack poseStack, int mouseX, int mouseY, float a, CallbackInfo ci) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		if (TitleScreenDesign.renderTransition(graphics, this.width, this.height, TitleScreenDesign.clientLogoRect(this.width, this.height))) {
			if (TitleScreenDesign.useClientLayout()) {
				tntsallin1client$rebuild();
			} else {
				tntsallin1client$setWidgetAlpha(1.0f);
			}
		}
	}

	/** No clicks mid-animation - the widgets underneath are about to be replaced or are still fading in. */
	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$blockClicksDuringTransition(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
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
		this.resize(this.minecraft, this.width, this.height);
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
