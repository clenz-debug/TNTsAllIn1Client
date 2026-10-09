package com.tntsallin1client.blocks3d;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.Nullable;

/**
 * 3D chains for hanging signs (own user request: the 3D chains should cover hanging signs too). Up to
 * 26.1 Minecraft draws hanging signs in code, not from a block model, so a resource pack can't change
 * their shape - the mod hides the sign model's own flat chains and draws these links in their place,
 * the same interlocking links as the pack's chain blocks.
 *
 * <p>Drawn with vanilla's own chain block texture (so a pack recoloring chains recolors these too),
 * but mapped so every bar takes a single solid pixel of it: the model's texture size is 256 while the
 * picture is 16 pixels wide, so one picture pixel covers 16 units - more than a bar's whole UV area.
 * The chain picture itself has holes; sampled normally, the bars would too.
 *
 * <p>Model coordinates are the sign model's own: pixels, y pointing down, the board's top edge at y 0
 * and the ceiling six pixels above it.
 */
public final class HangingSignChains {
	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_chain.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int PIXEL = TEXTURE_SIZE / 16;
	/** Solid pixels of the chain picture: a lighter one for the long sides, a darker one for the short ends. */
	private static final int[] SIDE_PIXEL = {1, 1};
	private static final int[] END_PIXEL = {0, 3};

	private static Model.@Nullable Simple sideChains;
	private static Model.@Nullable Simple middleChains;

	private HangingSignChains() {
	}

	/** Draws the chains for a sign drawn in {@code pose} (the sign model's own pose); {@code middle} = the V-shaped middle chains. */
	public static void submit(PoseStack pose, SubmitNodeCollector collector, int light, boolean middle,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		Model.Simple model = middle ? middleChains() : sideChains();
		// The last number is the outline color, not a tint: anything but 0 draws the chains into the
		// glowing-entity outline as well - they turned white whenever a glowing entity was in view
		// (own user report)
		collector.submitModel(model, Unit.INSTANCE, pose, model.renderType(TEXTURE), light, OverlayTexture.NO_OVERLAY, 0, crumbling);
	}

	/** Shows or hides the sign model's own flat chains - hidden while ours are drawn instead. */
	public static void showVanillaChains(Model.Simple signModel, boolean middle, boolean show) {
		ModelPart root = signModel.root();
		String name = middle ? "vChains" : "normalChains";
		if (root.hasChild(name)) {
			root.getChild(name).visible = show;
		}
	}

	private static Model.Simple sideChains() {
		if (sideChains == null) {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			for (float x : new float[] {-5, 5}) {
				CubeListBuilder cubes = CubeListBuilder.create();
				link(cubes, x, -6, 4, true);
				link(cubes, x, -4, 4, false);
				root.addOrReplaceChild(x < 0 ? "left" : "right", cubes, PartPose.ZERO);
			}
			sideChains = bake(mesh);
		}
		return sideChains;
	}

	/** Two legs from the middle of the ceiling down to the board's corners, each a short chain of links. */
	private static Model.Simple middleChains() {
		if (middleChains == null) {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			for (float turn : new float[] {(float) Math.PI / 4, (float) -Math.PI / 4}) {
				CubeListBuilder cubes = CubeListBuilder.create();
				// Each link's end bar sits in the neighbouring link's hole - never on top of another bar.
				// A leg starts half a pixel below the spot both lean around, with a link seen from its
				// narrow side: so the two only touch along an edge. Starting at that spot with links facing
				// the front, they crossed each other - three pixels wide, flickering through one another
				// and sticking up into a chain block above (own user report).
				link(cubes, 0, 0.5f, 4, false);
				link(cubes, 0, 2.5f, 5, true);
				link(cubes, 0, 5.5f, 3.5f, false);
				root.addOrReplaceChild(turn > 0 ? "left" : "right", cubes, PartPose.offsetAndRotation(0, -6, 0, 0, 0, turn));
			}
			// The notch the two legs leave between them under the ceiling: filled by a piece a little
			// thinner than the links, so its faces lie behind theirs and not in the same plane
			CubeListBuilder notch = CubeListBuilder.create();
			texture(notch, SIDE_PIXEL);
			notch.addBox(-0.7f, -6, -1.4f, 1.4f, 0.7f, 2.8f);
			root.addOrReplaceChild("notch", notch, PartPose.ZERO);
			middleChains = bake(mesh);
		}
		return middleChains;
	}

	private static Model.Simple bake(MeshDefinition mesh) {
		ModelPart root = LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE).bakeRoot();
		return new Model.Simple(root, RenderType::entityCutout);
	}

	/**
	 * One link, 3 wide and {@code height} long, starting at {@code top} and hanging down from it, made of
	 * 1-pixel bars around a 1-pixel hole - facing the sign's front ({@code facingFront}) or turned 90 degrees.
	 */
	private static void link(CubeListBuilder cubes, float x, float top, float height, boolean facingFront) {
		for (float offset : new float[] {-1.5f, 0.5f}) {
			texture(cubes, SIDE_PIXEL);
			if (facingFront) {
				cubes.addBox(x + offset, top, -0.5f, 1, height, 1);
			} else {
				cubes.addBox(x - 0.5f, top, offset, 1, height, 1);
			}
		}
		for (float y : new float[] {top, top + height - 1}) {
			texture(cubes, END_PIXEL);
			cubes.addBox(x - 0.5f, y, -0.5f, 1, 1, 1);
		}
	}

	private static void texture(CubeListBuilder cubes, int[] pixel) {
		cubes.texOffs(pixel[0] * PIXEL, pixel[1] * PIXEL);
	}
}
