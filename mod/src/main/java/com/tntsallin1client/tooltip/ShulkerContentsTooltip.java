package com.tntsallin1client.tooltip;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;

/**
 * Phase 5k: while {@link ModKeyBindings#SHULKER_PREVIEW} is held, appends the
 * shulker box's remaining contents to its tooltip. Vanilla's own
 * {@code ItemContainerContents} ({@code TooltipProvider}) already lists the
 * first 5 non-empty slots plus a "and N more" line unconditionally - this
 * deliberately doesn't duplicate or replace that (removing already-appended
 * lines would mean string-matching translatable components, which is fragile)
 * and instead only adds the items past the first 5, so holding the key simply
 * reveals the rest instead of showing a second, redundant list.
 */
public final class ShulkerContentsTooltip {
	private static final int VANILLA_PREVIEW_COUNT = 5;

	private ShulkerContentsTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register(ShulkerContentsTooltip::appendTooltip);
	}

	private static void appendTooltip(ItemStack stack, Item.TooltipContext tooltipContext, TooltipFlag tooltipType, List<Component> lines) {
		if (!ClientConfig.get().shulkerPreviewEnabled || !ModKeyBindings.SHULKER_PREVIEW.isDown()) {
			return;
		}
		if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
			return;
		}

		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null) {
			return;
		}

		int index = 0;
		for (ItemStack contained : contents.nonEmptyItems()) {
			index++;
			if (index <= VANILLA_PREVIEW_COUNT) {
				continue;
			}
			lines.add(Component.translatable("item.container.item_count", contained.getHoverName(), contained.getCount()));
		}
	}
}
