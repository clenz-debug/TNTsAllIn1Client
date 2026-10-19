package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.GuiGraphics;
import com.tntsallin1client.menu.OptionsChrome;
import com.tntsallin1client.shulker.ShulkerPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * What the newer versions hang onto {@code Screen#renderWithTooltip} (1.19.4+), which 1.18.2 does not
 * have - the game calls {@code Screen#render} straight from here:
 * <ul>
 *   <li>themed screens ({@link ThemedUi}) are marked for the widget mixins while the screen draws, and
 *   the mod's options screens get their client design top bar ({@link OptionsChrome}) afterwards;</li>
 *   <li>the pending shulker preview grid ({@link ShulkerPreviewRenderer}) is drawn last, over the item
 *   tooltip, which 1.18.2 draws inside the screen's own {@code render}.</li>
 * </ul>
 * Fabric's screen render events hook the same call. Priority 1100 applies this mixin after Fabric's, so
 * its "before" part runs after Fabric's before-event and its "after" part before Fabric's after-event -
 * the same nesting as in 1.19.4, where Fabric wraps {@code renderWithTooltip} (the in-game tour's overlay,
 * drawn in the after-event, stays on top of the top bar).
 */
@Mixin(value = GameRenderer.class, priority = 1100)
public class GameRendererScreenMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	@Nullable
	private Screen tntsallin1client$renderingScreen;

	@Inject(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void tntsallin1client$beforeScreen(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		this.tntsallin1client$renderingScreen = this.minecraft.screen;
		ThemedUi.begin(this.tntsallin1client$renderingScreen);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void tntsallin1client$afterScreen(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		Screen screen = this.tntsallin1client$renderingScreen;
		this.tntsallin1client$renderingScreen = null;
		if (screen == null) {
			ThemedUi.end();
			return;
		}
		// The same mouse position and (identity) pose the game hands to the screen.
		Window window = this.minecraft.getWindow();
		int mouseX = (int) (this.minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
		int mouseY = (int) (this.minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
		GuiGraphics graphics = GuiGraphics.of(new PoseStack());
		// The top bar of the mod's options screens in the client design, drawn after the screen itself
		// - see OptionsChrome for why the screen does not draw it.
		OptionsChrome.renderTopBar(screen, graphics, mouseX, mouseY, this.minecraft.getDeltaFrameTime());
		ThemedUi.end();
		ShulkerPreviewRenderer.drawIfPending(graphics);
	}
}
