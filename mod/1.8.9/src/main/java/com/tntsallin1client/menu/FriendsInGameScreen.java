package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.friends.FriendsBridge;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;

/**
 * The "Friends" screen of the pause menu and the title screen: world invitations, and who is
 * online. Laid out like the Fabric versions' screen, with what this version can't do said in
 * words: an invitation needs the Minecraft version of the world it is for, so it can only be
 * declined here, and inviting friends into the own world does not exist (see {@link FriendsBridge}).
 * The friends' faces are left out too - this version's skin lookup isn't within the mod's reach.
 * Data comes from the launcher; rebuilt once a second so changes show up without reopening.
 */
public class FriendsInGameScreen extends ClientScreen {
	private static final String KEY = "gui.tntsallin1client.friends.";
	private static final int ROW_WIDTH = 320;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int LINE_HEIGHT = 14;
	private static final int BUTTON_WIDTH = 80;
	private static final int BACK_BUTTON_WIDTH = 200;
	private static final int REBUILD_TICKS = 20;
	private static final int BACK_BUTTON_ID = 0;
	/** Each plus the invitation's index in {@link #shownInvites}. */
	private static final int FIRST_JOIN_ID = 100;
	private static final int FIRST_DECLINE_ID = 200;
	private static final int HEADING_COLOR = 0xFFFFFF;
	private static final int TEXT_COLOR = 0xE0E0E0;
	private static final int FAINT_COLOR = 0xAAAAAA;
	private static final int HINT_COLOR = 0xFFCC66;

	private final List<Label> labels = new ArrayList<Label>();
	private final List<FriendsBridge.Invite> shownInvites = new ArrayList<FriendsBridge.Invite>();
	private int ticks;

	public FriendsInGameScreen(Screen parent) {
		super(parent, KEY + "title");
	}

	@Override
	public void init() {
		this.labels.clear();
		this.shownInvites.clear();
		int x = (this.width - ROW_WIDTH) / 2;
		int buttonsX = x + ROW_WIDTH - BUTTON_WIDTH;
		int y = 34;
		int lastRowY = this.height - 60;

		List<FriendsBridge.Invite> invites = FriendsBridge.invites();
		if (!invites.isEmpty()) {
			label(I18n.translate(KEY + "invites_heading"), x, y, HEADING_COLOR);
			y += LINE_HEIGHT;
			for (FriendsBridge.Invite invite : invites) {
				if (y > lastRowY) {
					break;
				}
				int index = this.shownInvites.size();
				this.shownInvites.add(invite);
				label(I18n.translate(KEY + "invite_row", invite.fromName, invite.version), x, y + 6, TEXT_COLOR);
				if (invite.isJoinable()) {
					this.buttons.add(new ButtonWidget(FIRST_JOIN_ID + index, buttonsX - BUTTON_WIDTH - 4, y, BUTTON_WIDTH, ROW_HEIGHT, I18n.translate(KEY + "join")));
				}
				this.buttons.add(new ButtonWidget(FIRST_DECLINE_ID + index, buttonsX, y, BUTTON_WIDTH, ROW_HEIGHT, I18n.translate(KEY + "decline")));
				y += ROW_SPACING;
				if (!invite.isJoinable()) {
					label(I18n.translate(KEY + "invite.other_version", invite.version), x, y - 2, HINT_COLOR);
					y += LINE_HEIGHT;
				}
			}
			y += 8;
		}

		label(I18n.translate(KEY + "online_heading"), x, y, HEADING_COLOR);
		y += LINE_HEIGHT;
		List<FriendsBridge.Friend> friends = FriendsBridge.onlineFriends();
		if (friends.isEmpty()) {
			label(I18n.translate(KEY + "no_online_friends"), x, y, FAINT_COLOR);
			y += LINE_HEIGHT;
		}
		for (FriendsBridge.Friend friend : friends) {
			if (y > lastRowY) {
				break;
			}
			label(I18n.translate(KEY + "friend_row", friend.name, I18n.translate(KEY + "status." + friend.status)), x, y + 6, TEXT_COLOR);
			y += ROW_SPACING;
		}
		label(I18n.translate(KEY + "invite.not_in_this_version"), x, y + 6, FAINT_COLOR);

		this.buttons.add(new ButtonWidget(BACK_BUTTON_ID, (this.width - BACK_BUTTON_WIDTH) / 2, this.height - 30, BACK_BUTTON_WIDTH, ROW_HEIGHT, I18n.translate("gui.back")));
	}

	private void label(String text, int x, int y, int color) {
		this.labels.add(new Label(text, x, y, color));
	}

	private void rebuild() {
		this.buttons.clear();
		init();
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == BACK_BUTTON_ID) {
			back();
		} else if (button.id >= FIRST_DECLINE_ID && button.id < FIRST_DECLINE_ID + this.shownInvites.size()) {
			FriendsBridge.declineInvite(this.client, this.shownInvites.get(button.id - FIRST_DECLINE_ID));
			rebuild();
		} else if (button.id >= FIRST_JOIN_ID && button.id < FIRST_JOIN_ID + this.shownInvites.size()) {
			FriendsBridge.acceptInvite(this.client, this.shownInvites.get(button.id - FIRST_JOIN_ID));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (++this.ticks % REBUILD_TICKS == 0) {
			rebuild();
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);
		for (Label label : this.labels) {
			MenuText.text(label.text, label.x, label.y, label.color);
		}
	}

	private static final class Label {
		final String text;
		final int x;
		final int y;
		final int color;

		Label(String text, int x, int y, int color) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}
}
