package com.tntsallin1client.menu;

import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Options for the "3D block models" row (own user request: the inventory toggle belongs here, not
 * as a feature of its own). "3D items in inventory" switches the generated flat-inventory-icons
 * add-on pack (launcher/scripts/generate_flat_icons_pack.py), which BundledResourcePacks pins right
 * above Vanilla Tweaks: it shows Vanilla Tweaks' 3D item models flat in the GUI only - 3D stays in
 * hand, on the ground and in item frames. ON means the add-on pack is off. Greyed out when the pack
 * isn't there (game not started via our launcher).
 */
public class BlockModels3dOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;

	private final Screen parent;

	public BlockModels3dOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.block_models_3d_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = (this.width - ROW_WIDTH) / 2;
		int y = 40;

		PackRepository packRepository = this.minecraft.getResourcePackRepository();
		String flatIconsPackId = BundledResourcePacks.flatInventoryIconsPackId(packRepository);
		boolean items3dInInventory = flatIconsPackId == null || !packRepository.getSelectedIds().contains(flatIconsPackId);
		CycleButton<Boolean> items3dButton = this.addRenderableWidget(CycleButton.onOffBuilder(items3dInInventory)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.items_3d_inventory"),
						(button, value) -> {
							if (value) {
								packRepository.removePack(flatIconsPackId);
							} else {
								packRepository.addPack(flatIconsPackId);
							}
							this.minecraft.options.updateResourcePacks(packRepository);
						}));
		items3dButton.active = flatIconsPackId != null;
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
