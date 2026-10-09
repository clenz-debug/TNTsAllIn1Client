package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.WorldInvites;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
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
	private void tntsallin1client$inviteOnly(SocketAddress address, NameAndId player, CallbackInfoReturnable<Component> cir) {
		if (WorldInvites.isInviteOnly() && !WorldInvites.mayJoin(player.id())) {
			cir.setReturnValue(Component.translatableWithFallback("gui.tntsallin1client.friends.invite.not_invited",
					"You weren't invited into this world."));
		}
	}
}
