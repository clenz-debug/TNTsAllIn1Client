package com.tntsallin1client.mixin;

import net.minecraft.locale.Language;
import com.tntsallin1client.friends.WorldInvites;
import net.minecraft.network.chat.Component;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/**
 * World invitations (Phase 8b): the actual lock. While the host's world is open through our invite
 * flow, everyone who wasn't invited is turned away at login - the same check vanilla uses for bans
 * and the whitelist. The UUID is verified by Mojang (an opened world runs in online mode), so it
 * can't be faked. The reason has a fallback text for joiners without our mod.
 */
@Mixin(PlayerList.class)
public class InviteOnlyLoginMixin {
	@Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$inviteOnly(SocketAddress address, GameProfile player, CallbackInfoReturnable<Component> cir) {
		if (WorldInvites.isInviteOnly() && !WorldInvites.mayJoin(player.getId())) {
			// 1.19.2 has no translation fallback for players without our mod: the text goes out in the
			// host's language (the integrated server runs in the host's game), English if that lacks it.
			String key = "gui.tntsallin1client.friends.invite.not_invited";
			Language language = Language.getInstance();
			cir.setReturnValue(Component.literal(language.has(key) ? language.getOrDefault(key) : "You weren't invited into this world."));
		}
	}
}
