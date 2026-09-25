package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Offline mode: vanilla's conversion of old 64x32 skins to the 64x64 layout (plus its transparency
 * fixes), reused for the skin the launcher caches - see {@code OfflineProfile}.
 */
@Mixin(SkinTextureDownloader.class)
public interface SkinTextureDownloaderInvoker {
	@Invoker("processLegacySkin")
	static NativeImage tntsallin1client$processLegacySkin(NativeImage image, String name) {
		throw new AssertionError();
	}
}
