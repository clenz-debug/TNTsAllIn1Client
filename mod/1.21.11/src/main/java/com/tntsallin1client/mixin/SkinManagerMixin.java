package com.tntsallin1client.mixin;

import com.mojang.authlib.GameProfile;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Offline mode: the local player's skin/cape come from the launcher's cached copy instead of the
 * internet - see {@link OfflineProfile}. {@code get} is the one lookup every skin request goes
 * through ({@code createLookup} calls it too). Cape Provider only replaces the cape afterwards if it
 * loaded one itself, which it can't offline, so the cached cape stays.
 */
@Mixin(SkinManager.class)
public class SkinManagerMixin {
	@Inject(method = "get", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$offlineSkin(GameProfile profile, CallbackInfoReturnable<CompletableFuture<Optional<PlayerSkin>>> cir) {
		if (!Minecraft.getInstance().isLocalPlayer(profile.id())) {
			return;
		}
		CompletableFuture<Optional<PlayerSkin>> offlineSkin = OfflineProfile.localSkin();
		if (offlineSkin != null) {
			cir.setReturnValue(offlineSkin);
		}
	}
}
