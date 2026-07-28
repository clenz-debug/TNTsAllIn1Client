package com.tntsallin1client.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Third-party credits/licenses screen, requested by the user once the number
 * of bundled mods/resourcepacks (Fabric Loader/API, Sodium, Lithium,
 * Continuity, plus the growing Phase 5p/5r resourcepack collection) made this
 * worth doing now instead of deferring to a pre-release checklist item, as
 * the Roadmap originally planned. Each row opens the project's own page via
 * vanilla's {@link ConfirmLinkScreen} (the same "are you sure you want to
 * open this link" confirmation vanilla itself uses for chat links) rather
 * than a bespoke clickable-text implementation - one click target per row,
 * no manual text hit-testing needed.
 *
 * <p>Scoped to what this project actually bundles as of Phase 5s: the five
 * {@code build.gradle.kts}/launcher {@code mods-bundle/} dependencies
 * (Fabric Loader, Fabric API, Sodium, Lithium, Continuity, 3D Skin Layers)
 * plus every resourcepack currently in {@code launcher/resourcepacks-bundle/}.
 * Update this list alongside any future bundle addition/removal - there's no
 * automated way to keep it in sync, same as the Roadmap's own license
 * section.
 */
public class CreditsScreen extends Screen {
	private static final int ROW_WIDTH = 280;
	private static final int ROW_HEIGHT = 20;
	private static final int ITEM_HEIGHT = 24;
	private static final int LIST_TOP = 32;
	private static final int FOOTER_HEIGHT = 30;

	private record CreditEntry(String name, String license, String url) {
	}

	private static final List<CreditEntry> ENTRIES = List.of(
			new CreditEntry("Fabric Loader", "Apache-2.0", "https://github.com/FabricMC/fabric-loader"),
			new CreditEntry("Fabric API", "Apache-2.0", "https://github.com/FabricMC/fabric"),
			new CreditEntry("Sodium", "PolyForm Shield License 1.0.0", "https://github.com/CaffeineMC/sodium"),
			new CreditEntry("Lithium", "LGPL-3.0-only", "https://github.com/CaffeineMC/lithium-fabric"),
			new CreditEntry("Continuity", "LGPL-3.0-only", "https://github.com/PepperCode1/Continuity"),
			new CreditEntry("3D Skin Layers", "tr7zw Protective License", "https://github.com/tr7zw/3d-Skin-Layers"),
			new CreditEntry("Default Dark Mode (resource pack)", "CC-BY-NC-SA-4.0", "https://github.com/nebuIr/Default-Dark-Mode"),
			new CreditEntry("Bushy Vegetation (resource pack)", "BSD-3-Clause", "https://modrinth.com/resourcepack/bushy-vegetation"),
			new CreditEntry("3D Bushy Bushie (resource pack)", "Apache-2.0", "https://modrinth.com/resourcepack/3d-bushy-bushie"),
			new CreditEntry("Mushrooms Plus (resource pack)", "MIT", "https://modrinth.com/resourcepack/mushrooms-plus"),
			new CreditEntry("Vanilla Spinning Stonecutter (3D) (resource pack)", "MIT", "https://modrinth.com/resourcepack/vanilla-spinning-stonecutter-3d"),
			new CreditEntry("Vanilla Tweaks (resource pack selection)", "vanillatweaks.net Terms", "https://vanillatweaks.net/terms/"));

	private final Screen parent;

	public CreditsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.credits.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int listHeight = this.height - LIST_TOP - FOOTER_HEIGHT;
		CreditsList list = new CreditsList(this.minecraft, this.width, listHeight, LIST_TOP);
		for (CreditEntry entry : ENTRIES) {
			list.addEntry(entry, this);
		}
		this.addRenderableWidget(list);

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds((this.width - ROW_WIDTH) / 2, this.height - FOOTER_HEIGHT + 6, ROW_WIDTH, ROW_HEIGHT)
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

	/** Scrollable row list, same {@link ContainerObjectSelectionList} base class as {@link ClientMenuScreen}. */
	private static class CreditsList extends ContainerObjectSelectionList<CreditsList.Row> {
		CreditsList(Minecraft minecraft, int width, int height, int y) {
			super(minecraft, width, height, y, ITEM_HEIGHT);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}

		void addEntry(CreditEntry entry, Screen parentScreen) {
			Component label = Component.literal(entry.name() + " — " + entry.license());
			Button button = Button.builder(label, b -> ConfirmLinkScreen.confirmLinkNow(parentScreen, entry.url()))
					.bounds(0, 0, ROW_WIDTH, ROW_HEIGHT)
					.build();
			this.addEntry(new Row(button));
		}

		static final class Row extends ContainerObjectSelectionList.Entry<Row> {
			private final AbstractWidget widget;

			Row(AbstractWidget widget) {
				this.widget = widget;
			}

			@Override
			public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
				this.widget.setPosition(this.getContentX(), this.getContentY());
				this.widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return List.of(this.widget);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return List.of(this.widget);
			}
		}
	}
}
