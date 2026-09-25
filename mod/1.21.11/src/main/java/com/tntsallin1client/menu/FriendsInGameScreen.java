package com.tntsallin1client.menu;

import com.tntsallin1client.friends.E4mcControl;
import com.tntsallin1client.friends.FriendsBridge;
import com.tntsallin1client.friends.WorldInvites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pause menu's "Friends" screen (Phase 8b): world invitations to accept or decline, and - in
 * the player's own singleplayer world - online friends to invite into it (see
 * {@link WorldInvites}). Data comes from the launcher through {@link FriendsBridge}; rebuilt once a
 * second so invitations and their "sent" state show up without reopening.
 */
public class FriendsInGameScreen extends Screen {
	private static final int ROW_WIDTH = 320;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int BUTTON_WIDTH = 80;
	private static final int REBUILD_TICKS = 20;
	private static final int HEAD_SIZE = 16;
	/** Name text starts right of the head. */
	private static final int HEAD_TEXT_OFFSET = HEAD_SIZE + 4;

	private record Label(Component text, int x, int y, int color) {
	}

	/** A friend's face next to their name (own user request: see at a glance who's inviting). */
	private record Head(String uuid, int x, int y) {
	}

	/** One profile per player, so the skin cache recognizes it again every frame. Resolved by the
	 * game itself - the same lookup player head blocks use. */
	private static final Map<String, ResolvableProfile> PROFILES = new HashMap<>();

	private final Screen parent;
	private final List<Label> labels = new ArrayList<>();
	private final List<Head> heads = new ArrayList<>();
	private int ticks;

	public FriendsInGameScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.tntsallin1client.friends.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		labels.clear();
		heads.clear();
		int x = (this.width - ROW_WIDTH) / 2;
		int buttonsX = x + ROW_WIDTH - BUTTON_WIDTH;
		int y = 34;
		int lastRowY = this.height - 60;

		List<FriendsBridge.Invite> invites = FriendsBridge.invites();
		if (!invites.isEmpty()) {
			label(Component.translatable("gui.tntsallin1client.friends.invites_heading"), x, y, 0xFFFFFFFF);
			y += 14;
			for (FriendsBridge.Invite invite : invites) {
				if (y > lastRowY) break;
				heads.add(new Head(invite.fromUuid(), x, y + 2));
				label(Component.translatable("gui.tntsallin1client.friends.invite_row", invite.fromName(), invite.version()), x + HEAD_TEXT_OFFSET, y + 6, 0xFFE0E0E0);
				this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.friends.join"),
								button -> FriendsBridge.acceptInvite(this.minecraft, invite))
						.bounds(buttonsX - BUTTON_WIDTH - 4, y, BUTTON_WIDTH, ROW_HEIGHT).build());
				this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.friends.decline"), button -> {
							FriendsBridge.declineInvite(invite);
							this.rebuildWidgets();
						})
						.bounds(buttonsX, y, BUTTON_WIDTH, ROW_HEIGHT).build());
				y += ROW_SPACING;
			}
			y += 8;
		}

		label(Component.translatable("gui.tntsallin1client.friends.invite_heading"), x, y, 0xFFFFFFFF);
		y += 14;
		Component hint = inviteHint();
		if (hint != null) {
			label(hint, x, y, 0xFFFFCC66);
		} else {
			Component status = inviteStatus();
			if (status != null) {
				label(status, x, y, WorldInvites.state() == WorldInvites.State.FAILED ? 0xFFFF6666 : 0xFFAAAAAA);
				y += 14;
			}
			List<FriendsBridge.Friend> friends = FriendsBridge.onlineFriends();
			if (friends.isEmpty()) {
				label(Component.translatable("gui.tntsallin1client.friends.no_online_friends"), x, y, 0xFFAAAAAA);
			}
			for (FriendsBridge.Friend friend : friends) {
				if (y > lastRowY) break;
				heads.add(new Head(friend.uuid(), x, y + 2));
				label(Component.translatable("gui.tntsallin1client.friends.friend_row", friend.name(),
						Component.translatable("gui.tntsallin1client.friends.status." + friend.status())), x + HEAD_TEXT_OFFSET, y + 6, 0xFFE0E0E0);
				if (WorldInvites.isInvited(friend.uuid())) {
					label(resultText(WorldInvites.inviteResult(friend.uuid())), buttonsX - BUTTON_WIDTH - 4, y + 6, 0xFFAAAAAA);
					this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.friends.revoke"), button -> {
								WorldInvites.revoke(this.minecraft, friend.uuid());
								this.rebuildWidgets();
							})
							.bounds(buttonsX, y, BUTTON_WIDTH, ROW_HEIGHT).build());
				} else {
					Button invite = Button.builder(Component.translatable("gui.tntsallin1client.friends.invite"), button -> {
								WorldInvites.invite(this.minecraft, friend.uuid());
								this.rebuildWidgets();
							})
							.bounds(buttonsX, y, BUTTON_WIDTH, ROW_HEIGHT).build();
					invite.active = WorldInvites.state() != WorldInvites.State.FAILED;
					this.addRenderableWidget(invite);
				}
				y += ROW_SPACING;
			}
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds((this.width - 200) / 2, this.height - 30, 200, ROW_HEIGHT)
				.build());
	}

	/** Why inviting isn't possible here, or null if it is. */
	private @Nullable Component inviteHint() {
		if (!this.minecraft.hasSingleplayerServer()) return Component.translatable("gui.tntsallin1client.friends.invite.only_own_world");
		if (!E4mcControl.isAvailable()) return Component.translatable("gui.tntsallin1client.friends.invite.no_e4mc");
		return null;
	}

	private static @Nullable Component inviteStatus() {
		return switch (WorldInvites.state()) {
			case NONE -> null;
			case OPENING -> Component.translatable("gui.tntsallin1client.friends.invite.opening");
			case OPEN -> Component.translatable("gui.tntsallin1client.friends.invite.open");
			case FAILED -> WorldInvites.failure();
		};
	}

	private static Component resultText(@Nullable String result) {
		if (result == null) return Component.translatable("gui.tntsallin1client.friends.invite.pending");
		if (result.equals("ok")) return Component.translatable("gui.tntsallin1client.friends.invite.sent");
		return Component.translatable("gui.tntsallin1client.friends.invite.error");
	}

	private void label(Component text, int x, int y, int color) {
		labels.add(new Label(text, x, y, color));
	}

	@Override
	public void tick() {
		super.tick();
		if (++ticks % REBUILD_TICKS == 0) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		for (Label label : labels) {
			MenuText.text(guiGraphics, this.font, label.text(), label.x(), label.y(), label.color());
		}
		for (Head head : heads) {
			PlayerFaceRenderer.draw(guiGraphics,
					this.minecraft.playerSkinRenderCache().getOrDefault(profile(head.uuid())).playerSkin(), head.x(), head.y(), HEAD_SIZE);
		}
	}

	private static ResolvableProfile profile(String undashedUuid) {
		return PROFILES.computeIfAbsent(undashedUuid, key -> {
			String hex = key.replace("-", "");
			return ResolvableProfile.createUnresolved(UUID.fromString(hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-"
					+ hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20)));
		});
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
