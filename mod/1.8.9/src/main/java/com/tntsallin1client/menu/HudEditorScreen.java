package com.tntsallin1client.menu;

import java.util.ArrayList;
import java.util.List;

import com.tntsallin1client.config.ClientConfig;
import com.tntsallin1client.hud.HudElement;
import com.tntsallin1client.hud.HudElements;
import com.tntsallin1client.hud.HudLayout;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

/**
 * Lets HUD elements be dragged to any position and resized by dragging their bottom-right handle,
 * instead of only being toggled on/off. Only elements the player actually has switched on show up
 * here - a box for a feature that never renders ingame would only confuse.
 *
 * <p>In a world the screen has no background of its own: the game keeps drawing its HUD (ours
 * included) underneath an open screen, so the boxes sit on the real thing. Opened from the title
 * screen there is no HUD being drawn, so the elements are drawn here as a preview instead.
 */
public class HudEditorScreen extends ClientScreen {
	private static final int HANDLE_SIZE = 6;
	private static final int PADDING = 2;
	private static final float MIN_SCALE = 0.5f;
	private static final float MAX_SCALE = 4.0f;
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int DONE_BUTTON_ID = 0;
	private static final int RESET_BUTTON_ID = 1;
	private static final int LEFT_MOUSE_BUTTON = 0;
	private static final int HINT_Y = 26;

	private final List<HudElement> elements = new ArrayList<HudElement>();
	private HudElement dragging;
	private boolean resizing;
	/** Where inside the box it was grabbed - the box keeps that point under the cursor while moving. */
	private float grabOffsetX;
	private float grabOffsetY;

	public HudEditorScreen(Screen parent) {
		super(parent, "gui.tntsallin1client.hud_editor.title");
	}

	@Override
	public void init() {
		this.elements.clear();
		ClientConfig config = ClientConfig.get();
		for (HudElement element : HudElements.ALL) {
			if (element.isEditable(config)) {
				this.elements.add(element);
			}
		}

		this.buttons.add(new ButtonWidget(RESET_BUTTON_ID, this.width / 2 - BUTTON_WIDTH - 4, this.height - 28, BUTTON_WIDTH, BUTTON_HEIGHT,
				I18n.translate("gui.tntsallin1client.hud_editor.reset_all")));
		this.buttons.add(new ButtonWidget(DONE_BUTTON_ID, this.width / 2 + 4, this.height - 28, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.translate("gui.done")));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id == DONE_BUTTON_ID) {
			back();
		} else if (button.id == RESET_BUTTON_ID) {
			ClientConfig config = ClientConfig.get();
			for (HudElement element : HudElements.ALL) {
				element.layout(config).reset();
			}
			config.save();
		}
	}

	@Override
	protected void renderScreenBackground() {
		if (this.client.world == null) {
			this.renderBackground();
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		super.render(mouseX, mouseY, tickDelta);
		this.drawCenteredString(this.textRenderer, I18n.translate("gui.tntsallin1client.hud_editor.hint"), this.width / 2, HINT_Y, 0xFFFFFF);

		ClientConfig config = ClientConfig.get();
		for (HudElement element : this.elements) {
			if (this.client.world == null) {
				element.renderInPlace(this.client, config, this.width, this.height);
			}
			Rect bounds = boundsOf(element, config);
			int fillColor = element == this.dragging ? 0x8033CC33 : 0x803388CC;
			fill(bounds.x - PADDING, bounds.y - PADDING, bounds.x + bounds.width + PADDING, bounds.y + bounds.height + PADDING, fillColor);
			fill(bounds.x + bounds.width - HANDLE_SIZE, bounds.y + bounds.height - HANDLE_SIZE,
					bounds.x + bounds.width + PADDING, bounds.y + bounds.height + PADDING, 0xFFFFFFFF);
			this.drawWithShadow(this.textRenderer, I18n.translate(element.labelKey()), bounds.x, bounds.y - this.textRenderer.fontHeight - 2, 0xFFFF55);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == LEFT_MOUSE_BUTTON) {
			ClientConfig config = ClientConfig.get();
			for (HudElement element : this.elements) {
				Rect bounds = boundsOf(element, config);
				boolean onHandle = inRect(mouseX, mouseY,
						bounds.x + bounds.width - HANDLE_SIZE, bounds.y + bounds.height - HANDLE_SIZE,
						bounds.x + bounds.width + PADDING, bounds.y + bounds.height + PADDING);
				boolean onBody = inRect(mouseX, mouseY, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
				if (!onHandle && !onBody) {
					continue;
				}

				// From here on the element has a position of its own, starting where it is right now.
				HudLayout layout = element.layout(config);
				layout.x = bounds.x;
				layout.y = bounds.y;
				layout.customPosition = true;
				this.dragging = element;
				this.resizing = onHandle;
				this.grabOffsetX = mouseX - layout.x;
				this.grabOffsetY = mouseY - layout.y;
				return;
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long timeSinceClick) {
		if (this.dragging == null) {
			return;
		}
		HudLayout layout = this.dragging.layout(ClientConfig.get());
		if (this.resizing) {
			int unscaledWidth = Math.max(1, boxWidth(this.dragging));
			layout.scale = MathHelper.clamp((mouseX - layout.x) / unscaledWidth, MIN_SCALE, MAX_SCALE);
		} else {
			layout.x = mouseX - this.grabOffsetX;
			layout.y = mouseY - this.grabOffsetY;
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		if (this.dragging != null) {
			ClientConfig.get().save();
			this.dragging = null;
			this.resizing = false;
			return;
		}
		super.mouseReleased(mouseX, mouseY, button);
	}

	/** Where the element's box is right now, at its current scale. */
	private Rect boundsOf(HudElement element, ClientConfig config) {
		float scale = element.layout(config).scale;
		return new Rect(
				Math.round(element.x(this.client, config, this.width, this.height)),
				Math.round(element.y(this.client, config, this.width, this.height)),
				Math.round(boxWidth(element) * scale),
				Math.round(boxHeight(element) * scale));
	}

	/**
	 * Unscaled size of the element's box. An element that is switched on but has nothing to show
	 * right now - every part of the coordinates HUD off, or no player yet on the title screen - still
	 * gets a box, the size of its name: the real HUD correctly shows nothing then, but there has to be
	 * something to drag.
	 */
	private int boxWidth(HudElement element) {
		int width = element.width(this.client);
		return width > 0 ? width : this.textRenderer.getStringWidth(I18n.translate(element.labelKey()));
	}

	private int boxHeight(HudElement element) {
		int height = element.height(this.client);
		return height > 0 ? height : this.textRenderer.fontHeight;
	}

	private static boolean inRect(int x, int y, int x1, int y1, int x2, int y2) {
		return x >= x1 && x < x2 && y >= y1 && y < y2;
	}

	private static final class Rect {
		final int x;
		final int y;
		final int width;
		final int height;

		Rect(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}
}
