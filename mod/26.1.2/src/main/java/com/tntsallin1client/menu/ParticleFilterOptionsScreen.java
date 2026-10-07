package com.tntsallin1client.menu;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.particle.ParticleFilter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Options of the particle filter: one on/off row per kind of particle ({@link ParticleFilter}),
 * below the feature's own switch and a pair of buttons for all of them at once. That is well over
 * a hundred rows, so the screen scrolls - the same pattern as {@link KeystrokesOptionsScreen} - and
 * only builds the rows that are in view.
 */
public class ParticleFilterOptionsScreen extends Screen {
	private static final int ROW_WIDTH = 210;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 24;
	private static final int HALF_GAP = 6;
	private static final int FOOTER_GAP = 6;
	private static final int TOP_MARGIN = 40;
	private static final int BOTTOM_MARGIN = 10;
	private static final int SCROLL_STEP = ROW_SPACING;
	private static final int SCROLLBAR_GAP = 8;
	/** The feature's switch and the "all on / all off" pair. */
	private static final int HEADER_ROWS = 2;

	private final Screen parent;
	private @Nullable List<String> ids;
	private @Nullable ScrollBarHelper scrollBar;
	private int scrollOffset;
	private int maxScroll;

	public ParticleFilterOptionsScreen(Screen parent) {
		super(Component.translatable("gui.tntsallin1client.particle_filter_options.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ClientConfig config = ClientConfig.get();
		if (this.ids == null) {
			this.ids = ParticleFilter.ids();
		}

		int viewportHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
		int contentHeight = (HEADER_ROWS + this.ids.size()) * ROW_SPACING + FOOTER_GAP + OptionsChrome.flowHeight(ParticleFilterOptionsScreen.class);
		this.maxScroll = Math.max(0, contentHeight - viewportHeight);
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

		if (inView(y)) {
			this.addRenderableWidget(CycleButton.onOffBuilder(config.particleFilterEnabled)
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.translatable("gui.tntsallin1client.particle_filter_options.enabled"),
							(button, value) -> {
								config.particleFilterEnabled = value;
								config.save();
							}));
		}
		y += ROW_SPACING;

		if (inView(y)) {
			int halfWidth = (ROW_WIDTH - HALF_GAP) / 2;
			this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.particle_filter_options.all_on"),
							button -> this.setAllShown(true))
					.bounds(x, y, halfWidth, ROW_HEIGHT)
					.build());
			this.addRenderableWidget(Button.builder(Component.translatable("gui.tntsallin1client.particle_filter_options.all_off"),
							button -> this.setAllShown(false))
					.bounds(x + ROW_WIDTH - halfWidth, y, halfWidth, ROW_HEIGHT)
					.build());
		}
		y += ROW_SPACING;

		for (String id : this.ids) {
			if (inView(y)) {
				this.addRenderableWidget(CycleButton.onOffBuilder(ParticleFilter.isShown(id))
						.create(x, y, ROW_WIDTH, ROW_HEIGHT, ParticleFilter.name(id),
								(button, value) -> ParticleFilter.setShown(id, value)));
			}
			y += ROW_SPACING;
		}
		y += FOOTER_GAP;

		OptionsChrome.add(this, x, y, ROW_WIDTH, this::onClose, this::addRenderableWidget, this::addWidget);
	}

	/** Whether a row with its top edge at {@code y} is at least partly inside the scrolling area. */
	private boolean inView(int y) {
		return y + ROW_HEIGHT > TOP_MARGIN && y < this.height - BOTTOM_MARGIN;
	}

	private void setAllShown(boolean shown) {
		ParticleFilter.setAllShown(shown);
		this.rebuild();
	}

	private void rebuild() {
		this.clearWidgets();
		this.init();
	}

	/** Same scissor as {@link KeystrokesOptionsScreen#extractRenderState} - rows scrolled up must not run into the title. */
	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.enableScissor(0, TOP_MARGIN, this.width, this.height - BOTTOM_MARGIN);
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
		guiGraphics.disableScissor();

		this.scrollBar.render(guiGraphics);
		MenuText.centered(guiGraphics, this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.scrollBar.mouseClicked(event)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.scrollBar.mouseDragged(dragY)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.scrollBar.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
			return true;
		}
		if (this.maxScroll <= 0) {
			return false;
		}
		int newOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollDeltaY * SCROLL_STEP), 0, this.maxScroll);
		if (newOffset != this.scrollOffset) {
			this.scrollOffset = newOffset;
			this.rebuild();
		}
		return true;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
