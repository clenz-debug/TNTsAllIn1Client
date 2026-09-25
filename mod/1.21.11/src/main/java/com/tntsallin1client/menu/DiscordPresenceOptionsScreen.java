package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.offline.OfflineProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Options screen for Discord Rich Presence (own user request, see Ideen_für_den_client.md) - which
 * of the available details ("Singleplayer"/"Multiplayer", world name, Minecraft version, elapsed
 * time) actually get shown, plus the one privacy-sensitive one (server name) kept off by default
 * even with everything else on. Same "each toggle independently on/off" shape as every other
 * multi-field options screen here, see {@link ItemCounterOptionsScreen}. The actual Discord
 * Application client id has no control here on purpose - see
 * {@code ClientConfig#discordApplicationClientId}'s own doc comment.
 */
public class DiscordPresenceOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public DiscordPresenceOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.discord_presence_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.discordPresenceEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.discord_presence_options.enabled"),
						(button, value) -> {
							config.discordPresenceEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.discordPresenceShowGameMode)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.discord_presence_options.show_game_mode"),
						(button, value) -> {
							config.discordPresenceShowGameMode = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.discordPresenceShowVersion)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.discord_presence_options.show_version"),
						(button, value) -> {
							config.discordPresenceShowVersion = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.discordPresenceShowWorldName)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.discord_presence_options.show_world_name"),
						(button, value) -> {
							config.discordPresenceShowWorldName = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.discordPresenceShowServerName)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.discord_presence_options.show_server_name"),
						(button, value) -> {
							config.discordPresenceShowServerName = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.discordPresenceShowElapsedTime)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.discord_presence_options.show_elapsed_time"),
						(button, value) -> {
							config.discordPresenceShowElapsedTime = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		if (OfflineProfile.isOfflineLaunch()) {
			// DiscordPresenceManager skips offline launches - say so, the toggles below still read "on".
			MenuText.centered(guiGraphics, this.font, Component.translatable("gui.tntsallin1client.discord_presence_options.offline_hint"),
					this.width / 2, 26, 0xFFFFCC66);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
