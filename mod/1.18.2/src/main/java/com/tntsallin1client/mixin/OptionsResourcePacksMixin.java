package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.Options;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Replaces a dark mode pack of an older version in the saved list on load. Saving the pinned packs
 * (what this mixin also does in the newer versions' {@code updateResourcePacks}) lives in
 * {@link OptionsScreenPacksMixin} and {@code OptionsAccessMixin} in 1.18.2.
 */
@Mixin(Options.class)
public class OptionsResourcePacksMixin {
	@Shadow
	public List<String> resourcePacks;

	/** See {@link BundledResourcePacks#replaceLegacyDarkMode}. */
	@Inject(method = "loadSelectedResourcePacks", at = @At("HEAD"))
	private void tntsallin1client$replaceLegacyDarkMode(PackRepository repository, CallbackInfo ci) {
		BundledResourcePacks.replaceLegacyDarkMode(this.resourcePacks, repository);
	}
}
