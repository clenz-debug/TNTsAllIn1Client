package com.tntsallin1client.mixin;

import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers our key bindings. The game reads `options.txt` into the key bindings it knows at that
 * moment (the constructor ends with `load()`), so ours are added right before - otherwise a key the
 * player chose for them would be forgotten on every start.
 */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
	@Shadow
	public KeyBinding[] allKeys;

	@Inject(method = "load()V", at = @At("HEAD"))
	private void tnt$registerKeyBindings(CallbackInfo ci) {
		this.allKeys = ModKeyBindings.appendTo(this.allKeys);
	}
}
