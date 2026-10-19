package com.tntsallin1client.mixin;

import com.mojang.authlib.GameProfile;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Offline mode: the local player's skin, cape and arm width come from the launcher's cached copy
 * instead of the internet - see {@link OfflineProfile}. In this version every skin of a player in
 * the world - the player model, the tab list, the head in the social screen - is asked for here; the
 * newer versions' one skin lookup ({@code SkinManager#getOrLoad}) doesn't exist yet. Our own capes
 * are not asked for in an offline launch (see {@code AbstractClientPlayerMixin}), so the cached cape
 * stays.
 */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
	@Shadow
	@Final
	private GameProfile profile;

	@Inject(method = "getSkinLocation", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$offlineSkin(CallbackInfoReturnable<ResourceLocation> cir) {
		if (tntsallin1client$isLocalPlayerOffline() && OfflineProfile.skin() != null) {
			cir.setReturnValue(OfflineProfile.skin());
		}
	}

	@Inject(method = "getCapeLocation", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$offlineCape(CallbackInfoReturnable<ResourceLocation> cir) {
		if (tntsallin1client$isLocalPlayerOffline() && OfflineProfile.cape() != null) {
			cir.setReturnValue(OfflineProfile.cape());
		}
	}

	/** Wide or slim arms - they have to match the skin. */
	@Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$offlineModel(CallbackInfoReturnable<String> cir) {
		if (tntsallin1client$isLocalPlayerOffline() && OfflineProfile.model() != null) {
			cir.setReturnValue(OfflineProfile.model());
		}
	}

	/** Only the player playing on this client is of this kind - every other player keeps what the game shows. */
	@Unique
	private boolean tntsallin1client$isLocalPlayerOffline() {
		return OfflineProfile.isOfflineLaunch() && this.profile.getId() != null
				&& this.profile.getId().equals(Minecraft.getInstance().getUser().getGameProfile().getId());
	}
}
