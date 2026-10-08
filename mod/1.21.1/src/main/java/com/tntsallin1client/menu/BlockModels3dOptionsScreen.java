package com.tntsallin1client.menu;

import com.tntsallin1client.blocks3d.Blocks3d;
import com.tntsallin1client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.function.Consumer;

/**
 * Options for the "3D block models" row (own user request: the inventory toggle belongs here, not
 * as a feature of its own). "3D items in inventory & hand" is the mod's own switch in this version
 * (see {@link com.tntsallin1client.resourcepack.Items3d}): off, the GUI and the hand show the vanilla
 * item instead of the pack's 3D item models (own user request) - 3D stays on the ground and in item
 * frames.
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
		ClientConfig config = ClientConfig.get();
		// The mod's own switch in this version (see Items3d) - no pack to add or take away, no reload.
		this.addRenderableWidget(CycleButton.onOffBuilder(config.blockModels3dItems)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.menu.items_3d_inventory"),
						(button, value) -> {
							config.blockModels3dItems = value;
							config.save();
						}));
		y += ROW_SPACING;

		y = addOptionalPackRow(packRepository, x, y, Blocks3d.BUSHES_PACK_ID, "gui.tntsallin1client.menu.bushes_3d",
				config.blockModels3dBushes, value -> config.blockModels3dBushes = value);
		y = addOptionalPackRow(packRepository, x, y, Blocks3d.BUSHY_VEGETATION_PACK_ID, "gui.tntsallin1client.menu.bushy_vegetation",
				config.blockModels3dBushyVegetation, value -> config.blockModels3dBushyVegetation = value);
		y += 6;

		OptionsChrome.add(this, x, y, ROW_WIDTH, this::onClose, this::addRenderableWidget, this::addWidget);
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
