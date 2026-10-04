package com.tntsallin1client.mixin;

import java.awt.image.BufferedImage;

import com.tntsallin1client.resourcepack.Items3d;
import net.minecraft.client.resource.AnimationMetadata;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 3D items shaped like their entities (boat, minecart, sign - see {@link Items3d}): a model can only
 * wear a picture the game has among its block and item pictures, and this version takes none there
 * that isn't square unless it is an animation. Those entities' pictures are twice as wide as high,
 * so each is made square as the game reads it - the method that does so has no name in this
 * version's name table.
 */
@Mixin(Sprite.class)
public abstract class SpriteMixin {
	@Shadow
	public abstract String getName();

	@Inject(method = "method_7009([Ljava/awt/image/BufferedImage;Lnet/minecraft/client/resource/AnimationMetadata;)V", at = @At("HEAD"))
	private void tnt$squareEntityPicture(BufferedImage[] images, AnimationMetadata animation, CallbackInfo ci) {
		if (animation == null && images[0] != null) {
			images[0] = Items3d.squared(this.getName(), images[0]);
		}
	}
}
