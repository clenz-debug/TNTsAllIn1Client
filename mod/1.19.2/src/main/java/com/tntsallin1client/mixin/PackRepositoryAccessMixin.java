package com.tntsallin1client.mixin;

import com.google.common.collect.Lists;
import com.tntsallin1client.gui.PackRepositoryAccess;
import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

/**
 * {@link PackRepositoryAccess} on 1.19.2's pack repository - 1.19.4's {@code addPack}/{@code removePack},
 * an added pack regrouped right away like the newer versions' {@code PackRepositoryMixin} does.
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryAccessMixin implements PackRepositoryAccess {
	@Shadow
	private Map<String, Pack> available;

	@Shadow
	private List<Pack> selected;

	@Override
	public boolean addPack(String id) {
		Pack pack = this.available.get(id);
		if (pack != null && !this.selected.contains(pack)) {
			List<Pack> list = Lists.newArrayList(this.selected);
			list.add(pack);
			this.selected = BundledResourcePacks.regroup(list);
			return true;
		}
		return false;
	}

	@Override
	public boolean removePack(String id) {
		Pack pack = this.available.get(id);
		if (pack != null && this.selected.contains(pack)) {
			List<Pack> list = Lists.newArrayList(this.selected);
			list.remove(pack);
			this.selected = list;
			return true;
		}
		return false;
	}
}
