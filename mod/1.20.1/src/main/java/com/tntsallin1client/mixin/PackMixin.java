package com.tntsallin1client.mixin;

import com.tntsallin1client.resourcepack.BundledResourcePacks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Gives our launcher-bundled resource packs vanilla's "Default"-pack placement (fixed, bottom) -
 * see {@link BundledResourcePacks}. {@code readMetaAndCreate} is the one factory every folder pack
 * (and every built-in pack Fabric registers for a mod) goes through; in this version the placement
 * is two plain arguments of the {@code create} call it ends in, the position and whether it is fixed.
 */
@Mixin(Pack.class)
public class PackMixin {
	private static final String CREATE = "Lnet/minecraft/server/packs/repository/Pack;create(Ljava/lang/String;Lnet/minecraft/network/chat/Component;ZLnet/minecraft/server/packs/repository/Pack$ResourcesSupplier;Lnet/minecraft/server/packs/repository/Pack$Info;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/repository/Pack$Position;ZLnet/minecraft/server/packs/repository/PackSource;)Lnet/minecraft/server/packs/repository/Pack;";

	/** Like "Default", but optional - the user can still turn ours off. */
	@ModifyArg(method = "readMetaAndCreate", at = @At(value = "INVOKE", target = CREATE), index = 2)
	private static boolean tntsallin1client$keepOptional(String id, Component title, boolean required, Pack.ResourcesSupplier resources,
			Pack.Info info, PackType type, Pack.Position position, boolean fixedPosition, PackSource source) {
		return !BundledResourcePacks.pinsAboveDefault(id, source) && required;
	}

	@ModifyArg(method = "readMetaAndCreate", at = @At(value = "INVOKE", target = CREATE), index = 6)
	private static Pack.Position tntsallin1client$pinToBottom(String id, Component title, boolean required, Pack.ResourcesSupplier resources,
			Pack.Info info, PackType type, Pack.Position position, boolean fixedPosition, PackSource source) {
		return BundledResourcePacks.pinsAboveDefault(id, source) ? Pack.Position.BOTTOM : position;
	}

	@ModifyArg(method = "readMetaAndCreate", at = @At(value = "INVOKE", target = CREATE), index = 7)
	private static boolean tntsallin1client$fixPosition(String id, Component title, boolean required, Pack.ResourcesSupplier resources,
			Pack.Info info, PackType type, Pack.Position position, boolean fixedPosition, PackSource source) {
		return BundledResourcePacks.pinsAboveDefault(id, source) || fixedPosition;
	}
}
