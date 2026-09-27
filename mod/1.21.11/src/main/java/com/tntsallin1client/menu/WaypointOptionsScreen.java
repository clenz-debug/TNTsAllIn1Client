package com.tntsallin1client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.keybind.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Waypoint idea: dedicated options screen for the waypoint system, reached from the mod menu's
 * "Waypoints" toggle row - two key rebinds (same "click, then press a key; Escape unbinds"
 * pattern as {@link ZoomOptionsScreen}/{@link SpawnOverlayOptionsScreen}: one for
 * {@link ModKeyBindings#OPEN_WAYPOINTS}, one for the quick-create follow-up
 * {@link ModKeyBindings#CREATE_WAYPOINT}) plus the display defaults for new waypoints and a button into
 * {@link WaypointListScreen} (see {@link WaypointMenuIntegration} for both keys' gameplay hookup).
 *
 * <p>Scrolls like {@link WaypointEditScreen} when the window is too small (see its class doc) - here
 * too nearly every row is an on/off {@code CycleButton}, so the wheel scrolls the page, never a toggle.
 */
public class WaypointOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = 16;
	private static final int SCROLLBAR_GAP = 8;

	private final Screen parent;
	private @Nullable Button rebindButton;
	private @Nullable Button createRebindButton;
	private @Nullable KeyMapping awaitingKeyMapping;
	private @Nullable ScrollBarHelper scrollBar;
	private int defaultsLabelY;
	private int scrollOffset;
	private int maxScroll;

	public WaypointOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.waypoint_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		this.maxScroll = Math.max(0, computeContentHeight(this.font.lineHeight) - viewportHeight);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll);

		int x = (this.width - ROW_WIDTH) / 2;
		int y = TOP_MARGIN - this.scrollOffset;

		if (this.scrollBar == null) {
			this.scrollBar = new ScrollBarHelper(x + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight,
					() -> this.scrollOffset, () -> this.maxScroll,
					newOffset -> {
						this.scrollOffset = newOffset;
						this.rebuild();
					});
		} else {
			this.scrollBar.reposition(x + ROW_WIDTH + SCROLLBAR_GAP, TOP_MARGIN, viewportHeight);
		}

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointsEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.enabled"),
						(button, value) -> {
							config.waypointsEnabled = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.rebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKeyMapping = ModKeyBindings.OPEN_WAYPOINTS;
					this.updateRebindButtonLabels();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.createRebindButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
					this.awaitingKeyMapping = ModKeyBindings.CREATE_WAYPOINT;
					this.updateRebindButtonLabels();
				})
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		this.updateRebindButtonLabels();
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointConfirmDelete)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.confirm_delete"),
						(button, value) -> {
							config.waypointConfirmDelete = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointOffscreenArrows)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.offscreen_arrows"),
						(button, value) -> {
							config.waypointOffscreenArrows = value;
							config.save();
						}));
		y += ROW_SPACING;

		// The four toggles below only seed newly created waypoints - each waypoint has its own copy.
		this.defaultsLabelY = y + 2;
		y += this.font.lineHeight + 6;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointShowBeam)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.show_beam"),
						(button, value) -> {
							config.waypointShowBeam = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointShowMarker)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.show_marker"),
						(button, value) -> {
							config.waypointShowMarker = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointShowDistance)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.show_distance"),
						(button, value) -> {
							config.waypointShowDistance = value;
							config.save();
						}));
		y += ROW_SPACING;

		this.addRenderableWidget(CycleButton.onOffBuilder(config.waypointFadeNearby)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.waypoint_options.fade_nearby"),
						(button, value) -> {
							config.waypointFadeNearby = value;
							config.save();
						}));
		y += ROW_SPACING + 6;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.waypoint_options.manage_button"),
						button -> this.minecraft.setScreen(new WaypointListScreen(this)))
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
		y += ROW_SPACING;

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
				.bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.build());
	}

	/** Unscrolled content height from {@link #TOP_MARGIN} - mirrors {@link #init}, keep the two in sync. */
	private static int computeContentHeight(int fontLineHeight) {
		// Enabled, both keys, confirm delete, off-screen arrows.
		int height = 5 * ROW_SPACING;
		height += fontLineHeight + 6;
		// Beam, marker, distance, fade.
		height += 4 * ROW_SPACING + 6;
		height += ROW_SPACING;
		height += ROW_HEIGHT;
		return height;
	}

	/**
	 * Full re-layout after the scroll offset changes - see the class doc. {@code clearWidgets} leaves the
	 * removed widget focused, so keystrokes would keep going to an invisible old text field; {@link #init}
	 * adds the same widgets in the same order every time, so focus moves to the new one at the same index.
	 */
	private void rebuild() {
		int focusedIndex = this.children().indexOf(this.getFocused());
		this.clearWidgets();
		this.setFocused(null);
		this.init();
		if (focusedIndex >= 0 && focusedIndex < this.children().size()) {
			this.setFocused(this.children().get(focusedIndex));
		}
	}

	private boolean isInViewport(double mouseY) {
		return mouseY >= TOP_MARGIN && mouseY < this.height - BOTTOM_MARGIN;
	}

	private void updateRebindButtonLabels() {
		updateRebindButtonLabel(this.rebindButton, ModKeyBindings.OPEN_WAYPOINTS, "gui.tntsallin1client.waypoint_options.key");
		updateRebindButtonLabel(this.createRebindButton, ModKeyBindings.CREATE_WAYPOINT, "gui.tntsallin1client.waypoint_options.create_key");
	}

	private void updateRebindButtonLabel(@Nullable Button button, KeyMapping mapping, String labelKey) {
		if (button == null) {
			return;
		}
		Component keyName = mapping.getTranslatedKeyMessage();
		Component label = Component.translatable(labelKey, keyName);
		if (mapping == this.awaitingKeyMapping) {
			label = Component.literal("> ")
					.append(label.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
					.append(" <")
					.withStyle(ChatFormatting.YELLOW);
		}
		button.setMessage(label);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.awaitingKeyMapping != null) {
			this.awaitingKeyMapping.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			finishRebind();
			return true;
		}
		if (this.scrollBar != null && this.scrollBar.mouseClicked(event)) {
			return true;
		}
		// Widgets scrolled out of view are only clipped, not removed - don't let a click on the title
		// or the bottom margin hit one of them.
		if (!this.isInViewport(event.y())) {
			return false;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar != null && this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.scrollBar != null && this.scrollBar.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		if (this.maxScroll <= 0) {
			return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
		}
		int newOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollDeltaY * SCROLL_STEP), 0, this.maxScroll);
		if (newOffset != this.scrollOffset) {
			this.scrollOffset = newOffset;
			this.rebuild();
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (this.awaitingKeyMapping != null) {
			// Matches vanilla's own Controls screen: Escape unbinds rather than cancels.
			this.awaitingKeyMapping.setKey(keyEvent.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(keyEvent));
			finishRebind();
			return true;
		}
		return super.keyPressed(keyEvent);
	}

	private void finishRebind() {
		this.awaitingKeyMapping = null;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.updateRebindButtonLabels();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.drawCenteredString(this.font, Component.translatable("gui.tntsallin1client.waypoint_options.defaults_label"),
				this.width / 2, this.defaultsLabelY, 0xFFAAAAAA);
		guiGraphics.disableScissor();

		if (this.scrollBar != null) {
			this.scrollBar.render(guiGraphics);
		}
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
