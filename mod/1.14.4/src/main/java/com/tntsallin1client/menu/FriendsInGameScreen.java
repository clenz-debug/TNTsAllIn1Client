package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.friends.FriendsBridge;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

/**
 * The "Friends" screen of the pause menu and the title screen: world invitations, and who is
 * online. Laid out like the other versions' screen, with what this version can't do said in
 * words: an invitation needs the Minecraft version of the world it is for, so it can only be
 * declined here, and inviting friends into the own world does not exist (see {@link FriendsBridge}).
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
	private static final int HEADING_COLOR = 0xFFFFFF;
	private static final int TEXT_COLOR = 0xE0E0E0;
	private static final int FAINT_COLOR = 0xAAAAAA;
	private static final int HINT_COLOR = 0xFFCC66;

	private final List<Label> labels = new ArrayList<Label>();
	private int ticks;

	public FriendsInGameScreen(Screen parent) {
		super(parent, KEY + "title");
	}

	@Override
	protected void init() {
		this.labels.clear();
		int x = (this.width - ROW_WIDTH) / 2;
		int buttonsX = x + ROW_WIDTH - BUTTON_WIDTH;
		int y = 34;
		int lastRowY = this.height - 60;

		List<FriendsBridge.Invite> invites = FriendsBridge.invites();
		if (!invites.isEmpty()) {
			label(I18n.get(KEY + "invites_heading"), x, y, HEADING_COLOR);
			y += LINE_HEIGHT;
			for (final FriendsBridge.Invite invite : invites) {
				if (y > lastRowY) {
					break;
				}
				label(I18n.get(KEY + "invite_row", invite.fromName, invite.version), x, y + 6, TEXT_COLOR);
				if (invite.isJoinable()) {
					this.addButton(new Button(buttonsX - BUTTON_WIDTH - 4, y, BUTTON_WIDTH, ROW_HEIGHT, I18n.get(KEY + "join"),
							pressed -> FriendsBridge.acceptInvite(this.minecraft, invite)));
				}
				this.addButton(new Button(buttonsX, y, BUTTON_WIDTH, ROW_HEIGHT, I18n.get(KEY + "decline"), pressed -> {
					FriendsBridge.declineInvite(this.minecraft, invite);
					rebuild();
				}));
				y += ROW_SPACING;
				if (!invite.isJoinable()) {
					label(I18n.get(KEY + "invite.other_version", invite.version), x, y - 2, HINT_COLOR);
					y += LINE_HEIGHT;
				}
			}
			y += 8;
		}

		label(I18n.get(KEY + "online_heading"), x, y, HEADING_COLOR);
		y += LINE_HEIGHT;
		List<FriendsBridge.Friend> friends = FriendsBridge.onlineFriends();
		if (friends.isEmpty()) {
			label(I18n.get(KEY + "no_online_friends"), x, y, FAINT_COLOR);
			y += LINE_HEIGHT;
		}
		for (FriendsBridge.Friend friend : friends) {
			if (y > lastRowY) {
				break;
			}
			label(I18n.get(KEY + "friend_row", friend.name, I18n.get(KEY + "status." + friend.status)), x, y + 6, TEXT_COLOR);
			y += ROW_SPACING;
		}
		label(I18n.get(KEY + "invite.not_in_this_version"), x, y + 6, FAINT_COLOR);

		// In the client design "Back" is in the top left corner, like on the options screens.
		this.addButton(TopBar.inUse()
				? TopBar.back(I18n.get("gui.back"), this::back)
				: new Button((this.width - BACK_BUTTON_WIDTH) / 2, this.height - 30, BACK_BUTTON_WIDTH, ROW_HEIGHT, I18n.get("gui.back"), pressed -> back()));
	}

	private void label(String text, int x, int y, int color) {
		this.labels.add(new Label(text, x, y, color));
	}

	@Override
	public void tick() {
		super.tick();
		if (++this.ticks % REBUILD_TICKS == 0) {
			rebuild();
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTick) {
		super.render(mouseX, mouseY, partialTick);
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
