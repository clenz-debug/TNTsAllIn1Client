package com.tntsallin1client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The frames of the last second - 1.19.2 has no {@code Minecraft#getFps()} yet, only the private static field. */
@Mixin(Minecraft.class)
public interface MinecraftFpsAccessor {
	@Accessor("fps")
	static int tntsallin1client$getFps() {
		throw new AssertionError();
	}
}
