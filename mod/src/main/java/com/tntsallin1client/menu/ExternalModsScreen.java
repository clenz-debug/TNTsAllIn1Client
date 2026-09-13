package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.NativeImage;
import com.tntsallin1client.TNTsAllIn1ClientMod;
import dev.tr7zw.skinlayers.config.ConfigScreenProvider;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Read-only gallery of the bundled third-party mods, requested after the user clarified
 * a first, rejected attempt at this idea (individual toggle rows for Sodium/Continuity
 * mixed into {@link ClientMenuScreen}'s own feature list) - Dawn Client's version instead
 * shows one picture per external mod, the same way vanilla's resourcepack screen shows a
 * pack's icon, except nothing here can be activated/deactivated. The mods that actually
 * have a togglable feature (Continuity's connected/emissive textures, 3D Skin Layers) keep
 * their toggle rows in {@link ClientMenuScreen} itself - this screen exists purely so a
 * mod's own settings screen, where one exists, has one obvious, uncluttered place to reach
 * instead of an "Options" button sitting next to its toggle in the main feature list.
 */
public class ExternalModsScreen extends Screen {
	private static final int ROW_WIDTH = 280;
	private static final int ICON_SIZE = 24;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 32;
	private static final int SETTINGS_BUTTON_WIDTH = 70;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 30;

	/** Icon textures are registered once per mod id with the {@link Minecraft#getTextureManager()} and reused across screen opens. */
	private static final Map<String, Identifier> ICON_CACHE = new HashMap<>();

	private record ModEntry(String modId, String displayName, @Nullable Supplier<Screen> settingsScreenFactory) {
	}

	private final Screen parent;

	public ExternalModsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.external_mods.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		List<ModEntry> entries = List.of(
				new ModEntry("fabric-api", "Fabric API", null),
				new ModEntry("sodium", "Sodium", () -> VideoSettingsScreen.createScreen(this)),
				new ModEntry("lithium", "Lithium", null),
				new ModEntry("continuity", "Continuity", null),
				new ModEntry("skinlayers3d", "3D Skin Layers", () -> ConfigScreenProvider.createConfigScreen(this)));

		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		ModList list = new ModList(this.minecraft, this.width, listHeight, LIST_TOP);
		for (ModEntry entry : entries) {
			list.addEntry(entry);
		}
		this.addRenderableWidget(list);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds((this.width - ROW_WIDTH) / 2, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, BUTTON_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	/**
	 * Loads (and caches) a mod's own icon straight from its {@code fabric.mod.json} declaration,
	 * as a {@link DynamicTexture} registered under a namespaced {@link Identifier} - same
	 * technique vanilla itself uses for server-list favicons ({@code FaviconTexture}). Returns
	 * {@code null} (silently, no icon drawn) if the mod, its icon path, or the image can't be
	 * resolved - this is a purely cosmetic gallery, not worth a hard failure over.
	 */
	private static @Nullable Identifier loadIcon(String modId) {
		if (ICON_CACHE.containsKey(modId)) {
			return ICON_CACHE.get(modId);
		}
		Identifier location = FabricLoader.getInstance().getModContainer(modId)
				.flatMap(container -> container.getMetadata().getIconPath(ICON_SIZE)
						.flatMap(container::findPath)
						.map(path -> readIcon(modId, path)))
				.orElse(null);
		ICON_CACHE.put(modId, location);
		return location;
	}

	private static @Nullable Identifier readIcon(String modId, Path path) {
		try (InputStream stream = Files.newInputStream(path)) {
			NativeImage image = NativeImage.read(stream);
			Identifier location = Identifier.fromNamespaceAndPath(TNTsAllIn1ClientMod.MOD_ID, "external_mod_icon/" + modId);
			Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(() -> "External mod icon " + modId, image));
			return location;
		} catch (Exception e) {
			return null;
		}
	}

	/** Scrollable row list, same {@link ContainerObjectSelectionList} base class as {@link CreditsScreen}. */
	private final class ModList extends ContainerObjectSelectionList<ModList.Row> {
		ModList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		void addEntry(ModEntry entry) {
			Identifier icon = loadIcon(entry.modId());
			Button settingsButton = entry.settingsScreenFactory() != null
					? Button.builder(Component.translatable("gui.tntsallin1client.menu.options_button"),
									b -> ExternalModsScreen.this.minecraft.setScreen(entry.settingsScreenFactory().get()))
							.bounds(0, 0, SETTINGS_BUTTON_WIDTH, BUTTON_HEIGHT)
							.build()
					: null;
			this.addEntry(new Row(icon, entry.displayName(), settingsButton));
		}

		final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final @Nullable Identifier icon;
			private final String name;
			private final @Nullable Button settingsButton;

			Row(@Nullable Identifier icon, String name, @Nullable Button settingsButton) {
				this.icon = icon;
				this.name = name;
				this.settingsButton = settingsButton;
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				int x = this.getContentX();
				int middleY = this.getContentYMiddle();
				if (this.icon != null) {
					guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.icon, x, middleY - ICON_SIZE / 2,
							0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
				}
				guiGraphics.drawString(ExternalModsScreen.this.font, this.name, x + ICON_SIZE + 6, middleY - 4, 0xFFFFFFFF);
				if (this.settingsButton != null) {
					this.settingsButton.setPosition(x + ROW_WIDTH - SETTINGS_BUTTON_WIDTH, middleY - BUTTON_HEIGHT / 2);
					this.settingsButton.render(guiGraphics, mouseX, mouseY, partialTick);
				}
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return this.settingsButton == null ? List.of() : List.of(this.settingsButton);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return this.settingsButton == null ? List.of() : List.of(this.settingsButton);
			}
		}
	}
}
