package com.tntsallin1client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tntsallin1client.design.ClientTheme;
import com.tntsallin1client.design.ThemedUi;
import com.tntsallin1client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Themed screens ({@link ThemedUi}): the whole screen render is marked as themed for the widget mixins
 * ({@code GameRendererScreenMixin} in 1.18.2, which has no {@code renderWithTooltip}); here the mod
 * replaces the menu background (darkened game view or dirt) with the plain theme background, like
 * the client design's title screen. In this version every screen draws that background itself, at the
 * start of its own {@code render}.
 */
@Mixin(Screen.class)
public class ScreenThemeMixin {
	/** Both of 1.18.2's {@code renderBackground} variants end in this one (the other passes a 0 offset). */
	@Inject(method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$themedBackground(PoseStack poseStack, int dirtOffset, CallbackInfo ci) {
		GuiGraphics graphics = GuiGraphics.of(poseStack);
		Screen self = (Screen) (Object) this;
		if (ThemedUi.isThemed(self)) {
			// The scrolling options screens clip everything they draw to their list area, the
			// background included - in a world the margins above and below would show the game
			// (own user report). The background is the whole screen's, so it is drawn unclipped.
			graphics.flush();
			boolean clipped = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
			int[] clip = new int[4];
			if (clipped) {
				GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, clip);
				RenderSystem.disableScissor();
			}
			graphics.fill(0, 0, self.width, self.height, ClientTheme.get().background1);
			graphics.flush();
			if (clipped) {
				RenderSystem.enableScissor(clip[0], clip[1], clip[2], clip[3]);
			}
			ci.cancel();
		}
	}
}
