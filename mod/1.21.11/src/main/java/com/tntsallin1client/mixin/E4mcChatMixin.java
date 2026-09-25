package com.tntsallin1client.mixin;

import com.tntsallin1client.friends.WorldInvites;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * World invitations (Phase 8b): e4mc keeps the public address it gets for an opened world nowhere
 * we could ask for it - it only announces it in chat, under its own translation key. While our
 * invite flow opened the world, that message is caught here: the address goes to
 * {@link WorldInvites} (and from there only to invited friends), the message itself never shows,
 * so the address stays secret even on the host's screen. Hooks vanilla chat, not e4mc's code - if
 * e4mc ever renames the key, inviting just stops working (it times out), nothing crashes.
 *
 * <p>In 1.21.11 e4mc's chat helper calls {@code addMessage(Component)} directly (26.1.2 renamed it
 * to {@code addClientSystemMessage}, where e4mc falls back via reflection).
 */
@Mixin(ChatComponent.class)
public class E4mcChatMixin {
	private static final String DOMAIN_ASSIGNED = "text.e4mc_minecraft.domainAssigned";
	private static final String CLOSE_SERVER = "text.e4mc_minecraft.closeServer";

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$catchE4mcAddress(Component message, CallbackInfo ci) {
		if (!WorldInvites.isInviteOnly() || !(message.getContents() instanceof TranslatableContents contents)) {
			return;
		}
		if (DOMAIN_ASSIGNED.equals(contents.getKey()) && contents.getArgs().length > 0 && contents.getArgs()[0] instanceof Component domain) {
			String address = domain.getStyle().getClickEvent() instanceof ClickEvent.CopyToClipboard copy ? copy.value() : domain.getString();
			WorldInvites.onAddressAssigned(address);
			ci.cancel();
		} else if (CLOSE_SERVER.equals(contents.getKey())) {
			WorldInvites.onHostingStopped();
		}
	}
}
