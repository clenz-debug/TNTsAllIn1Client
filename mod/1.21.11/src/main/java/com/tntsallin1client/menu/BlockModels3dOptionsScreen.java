package com.tntsallin1client.menu;

import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.function.Consumer;

/**
 * Options for the "3D block models" row (own user request: the inventory toggle belongs here, not
 * as a feature of its own). "3D items in inventory & hand" switches the generated flat-inventory-icons
 * add-on pack (launcher/scripts/generate_flat_icons_pack.py), which BundledResourcePacks pins right
 * above the 3D block model packs: it shows the vanilla item instead of their 3D item models in the
 * GUI and in the hand (own user request) - 3D stays on the ground and in item frames. ON means the
 * add-on pack is off. Greyed out when the pack isn't there (game not started via our launcher).
 *
 * Below it one row per optional pack the "3D block models" row switches along (own user request):
 * our 3D bushes and Bushy Vegetation. The choice is remembered in ClientConfig; while the row
 * is on, the pack is switched right away (see Blocks3d).
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
		y += ROW_SPACING;

		ClientConfig config = ClientConfig.get();
		y = addOptionalPackRow(packRepository, x, y, Blocks3d.BUSHES_PACK_ID, "gui.tntsallin1client.menu.bushes_3d",
				config.blockModels3dBushes, value -> config.blockModels3dBushes = value);
		y = addOptionalPackRow(packRepository, x, y, Blocks3d.BUSHY_VEGETATION_PACK_ID, "gui.tntsallin1client.menu.bushy_vegetation",
				config.blockModels3dBushyVegetation, value -> config.blockModels3dBushyVegetation = value);
		y += 6;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	/** Shows the pack's actual state while the row is on, the remembered choice while it's off. */
	private int addOptionalPackRow(PackRepository packRepository, int x, int y, String packId, String labelKey, boolean chosen, Consumer<Boolean> remember) {
		boolean rowOn = packRepository.getSelectedIds().contains(Blocks3d.PACK_ID);
		boolean on = rowOn ? packRepository.getSelectedIds().contains(packId) : chosen;
		CycleButton<Boolean> button = this.addRenderableWidget(CycleButton.onOffBuilder(on)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable(labelKey),
						(cycle, value) -> {
							remember.accept(value);
							ClientConfig.get().save();
							Blocks3d.setOptional(packRepository, packId, value);
							this.minecraft.options.updateResourcePacks(packRepository);
						}));
		button.active = packRepository.getAvailableIds().contains(packId);
		return y + ROW_SPACING;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
