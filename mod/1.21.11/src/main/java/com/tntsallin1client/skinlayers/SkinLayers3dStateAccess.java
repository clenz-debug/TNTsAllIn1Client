package com.tntsallin1client.skinlayers;

/**
 * Duck interface implemented by {@code AvatarRenderStateMixin}: which skin layer parts are drawn in 3D
 * for this player this frame, as a bit set of {@link SkinLayerPart#bit}. Decided when the render state
 * is extracted and read when it's drawn - same pattern (and same reason for living outside the mixin
 * package) as {@code ItemPhysicsStateAccess}.
 */
public interface SkinLayers3dStateAccess {
	int tntsallin1client$getSkinLayers3dParts();

	void tntsallin1client$setSkinLayers3dParts(int parts);
}
