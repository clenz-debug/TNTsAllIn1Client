package com.tntsallin1client.gui;

/**
 * 1.19.3's {@code PackRepository#addPack}/{@code removePack}, put onto 1.18.2's pack repository the
 * same way as {@link WidgetAccess} (injected by Loom, implemented by {@code PackRepositoryAccessMixin}
 * with 1.19.4's code), so the mod's pack switches stay the same code as in the newer versions.
 */
public interface PackRepositoryAccess {
	default boolean addPack(String id) {
		throw new UnsupportedOperationException("implemented by PackRepositoryAccessMixin");
	}

	default boolean removePack(String id) {
		throw new UnsupportedOperationException("implemented by PackRepositoryAccessMixin");
	}
}
