package com.tntsallin1client.mixin;

import com.tntsallin1client.screenshot.ScreenshotChatLink;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Phase 5n rebuild: handles clicks on the "[Copy]" screenshot chat link
 * before vanilla's own {@code handleComponentClicked} would fall through to
 * {@code defaultHandleGameClickEvent}, which for a {@code ClickEvent.Custom}
 * just round-trips it through a server packet - useless for a purely
 * client-side action like this. Mirrors vanilla's own handling of its
 * internal {@code ChatComponent.QUEUE_EXPAND_ID} custom click case, which
 * this same private method already special-cases the same way.
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	@Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
	private void tntsallin1client$onComponentClicked(Style style, boolean insertionMode, CallbackInfoReturnable<Boolean> cir) {
		ClickEvent clickEvent = style.getClickEvent();
		if (clickEvent instanceof ClickEvent.Custom custom && custom.id().equals(ScreenshotChatLink.COPY_CLICK_ID)) {
			custom.payload().ifPresent(tag -> {
				if (tag instanceof StringTag stringTag) {
					ScreenshotChatLink.copyToClipboard(new File(stringTag.value()));
				}
			});
			cir.setReturnValue(true);
		}
	}
}
