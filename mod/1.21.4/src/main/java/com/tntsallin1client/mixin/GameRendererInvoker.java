package com.tntsallin1client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Waypoint off-screen arrows: the vertical FOV (degrees) the level is actually rendered with - the
 * same {@code getFov(camera, partialTick, true)} call {@code GameRenderer#renderLevel} feeds into
 * {@code getProjectionMatrix}, so zoom, sprinting and the FOV effect scale are all included.
 * See {@code WaypointArrowHud}.
 */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
	@Invoker("getFov")
	float tntsallin1client$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
