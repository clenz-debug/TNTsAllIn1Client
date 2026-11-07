package com.tntsallin1client.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The game opens or closes the debug screen when F3 is let go - unless a key was pressed with it
 * that meant something, which it notes here. Our own F3 combination (the system info page) notes
 * itself the same way.
 */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
	@Accessor("handledDebugKey")
	void tntsallin1client$setHandledDebugKey(boolean handled);
}
