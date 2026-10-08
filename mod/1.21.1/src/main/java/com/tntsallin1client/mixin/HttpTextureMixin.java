package com.tntsallin1client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.tntsallin1client.skinlayers.SkinLayerMeshes;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D skin layers: a downloaded skin's texture is there before its picture is - it shows the default
 * skin until {@code upload} puts the real one in. The meshes built from the old picture are dropped
 * then, see {@link SkinLayerMeshes}.
 */
@Mixin(HttpTexture.class)
public class HttpTextureMixin {
	@Inject(method = "upload", at = @At("TAIL"))
	private void tntsallin1client$rebuildSkinLayers(NativeImage image, CallbackInfo ci) {
		SkinLayerMeshes.invalidate();
	}
}
