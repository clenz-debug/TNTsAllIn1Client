package com.tntsallin1client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The fog renderer hands out the "no fog" buffer {@code WorldShapes} draws its see-through lines with. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Accessor("fogRenderer")
	FogRenderer tntsallin1client$getFogRenderer();
}
