package org.powerbot.script.rt6;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import org.powerbot.bot.rt6.HashTable;
import org.powerbot.bot.rt6.client.Client;
import org.powerbot.bot.rt6.client.ComponentNode;
import org.powerbot.bot.rt6.client.Widget;
import org.powerbot.script.Calculations;
import org.powerbot.script.Drawable;
import org.powerbot.script.Identifiable;
import org.powerbot.script.StringUtils;

/**
 * Component
 */
public class Component extends Interactive implements Drawable, Displayable, Identifiable {
	static final int RECURSION_DEPTH = 20;
	public static final Color TARGET_FILL_COLOR = new Color(0, 0, 0, 50);
	public static final Color TARGET_STROKE_COLOR = new Color(0, 255, 0, 150);
	private final org.powerbot.script.rt6.Widget widget;
	private final Component parent;
	private final int index;

	public Component(final ClientContext ctx, final org.powerbot.script.rt6.Widget widget, final int index) {
		this(ctx, widget, new Component(ctx, widget, -1), index);
	}

	public Component(final ClientContext ctx, final org.powerbot.script.rt6.Widget widget, final Component parent, final int index) {
		super(ctx);
		this.widget = widget;
		this.parent = parent;
		this.index = index;
	}

	public org.powerbot.script.rt6.Widget widget() {
		return widget;
	}

	public Component parent() {
		return parent;
	}

	public int index() {
		return index;
	}

	@Override
	public void bounds(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Component component() {
		return this;
	}

	public Component[] components() {
		final Widget component = getInternalComponent();
		final Object[] interfaces;
		if (component != null && (interfaces = component.getComponents()) != null) {
			final Component[] components = new Component[interfaces.length];
			for (int i = 0; i < interfaces.length; i++) {
				components[i] = new Component(ctx, widget, this, i);
			}
			return components;
		}
		return new Component[0];
	}

	public int childrenCount() {
		final Widget component = getInternalComponent();
		final Object[] interfaces;
		if (component != null && (interfaces = component.getComponents()) != null) {
			return interfaces.length;
		}
		return 0;
	}

	public Component component(final int index) {
		if (index < 0) {
			throw new IndexOutOfBoundsException(index + " < " + 0);
		}
		return new Component(ctx, widget, this, index);
	}

	public String[] actions() {
		final Widget component = getInternalComponent();
		String[] actions = new String[0];
		if (component != null) {
			if ((actions = component.getActions()) == null) {
				actions = new String[0];
			}
		}
		return actions.clone();
	}

	public int textureId() {
		final Widget component = getInternalComponent();
		return component != null ? component.getTextureId() : -1;
	}

	public int borderThickness() {
		final Widget component = getInternalComponent();
		return component != null ? component.getBorderThinkness() : -1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int id() {
		final Widget component = getInternalComponent();
		return component != null ? component.getId() : -1;
	}

	public int itemIndex() {
		final Widget component = getInternalComponent();
		return component != null ? component.getComponentIndex() : -1;
	}

	public String itemName() {
		final Widget component = getInternalComponent();
		String name = "";
		if (component != null && (name = component.getComponentName()) == null) {
			name = "";
		}
		return StringUtils.stripHtml(name);
	}

	public int itemId() {
		final Widget component = getInternalComponent();
		return component != null ? component.getComponentId() : -1;
	}

	public int itemStackSize() {
		final Widget component = getInternalComponent();
		return component != null ? component.getComponentStackSize() : -1;
	}

	public int modelId() {
		final Widget component = getInternalComponent();
		return component != null ? component.getModelId() : -1;
	}

	public int modelType() {
		final Widget component = getInternalComponent();
		return component != null ? component.getModelType() : -1;
	}

	public int modelZoom() {
		final Widget component = getInternalComponent();
		return component != null ? component.getModelZoom() : -1;
	}

	public int parentId() {
		final Client client = ctx.client();
		final Widget component = getInternalComponent();
		if (client == null || component == null) {
			return -1;
		}

		final int pId = component.getParentId();
		if (pId != -1) {
			return pId;
		}


		final int uid = id() >>> 16;
		for (final ComponentNode node : new HashTable<ComponentNode>(client.getWidgetTable(), ComponentNode.class)) {
			if (uid == node.getUid()) {
				return (int) node.getId();
			}
		}
		return -1;
	}

	public Point screenPoint() {
		return _screenPoint(1);
	}

	private Point _screenPoint(final int depth) {
		if (depth > RECURSION_DEPTH) {
			return new Point(-1, -1);
		}
		final Client client = ctx.client();
		final Widget component = getInternalComponent();
		if (client == null || component == null) {
			return new Point(-1, -1);
		}
		final int pId = parentId();
		int x = 0, y = 0;
		if (pId != -1) {
			final Point point = ctx.widgets.component(pId >> 16, pId & 0xffff)._screenPoint(depth + 1);
			x = point.x;
			y = point.y;
		} else {
			final Rectangle[] bounds = client.getWidgetBoundsArray();
			final int index = component.getBoundsArrayIndex();
			if (bounds != null && index > 0 && index < bounds.length && bounds[index] != null) {
				return new Point(bounds[index].x, bounds[index].y);
			}
		}
		if (pId != -1) {
			final Component child = ctx.widgets.component(pId >> 16, pId & 0xffff);
			final int horizontalScrollSize = child.scrollWidthMax(), verticalScrollSize = child.scrollHeightMax();
			if (horizontalScrollSize > 0 || verticalScrollSize > 0) {
				x -= child.scrollX();
				y -= child.scrollY();
			}
		}
		x += component.getX();
		y += component.getY();
		return new Point(x, y);
	}

	public Point relativePoint() {
		final Widget component = getInternalComponent();
		return component != null ? new Point(component.getX(), component.getY()) : new Point(-1, -1);
	}

	public String selectedAction() {
		final Widget component = getInternalComponent();
		String action = "";
		if (component != null && (action = component.getSelectedActionName()) == null) {
			action = "";
		}
		return action;
	}

	public int shadowColor() {
		final Widget component = getInternalComponent();
		return component != null ? component.getShadowColor() : -1;
	}

	public int contentType() {
		final Widget component = getInternalComponent();
		return component != null ? component.getSpecialType() : -1;
	}

	public String text() {
		final Widget component = getInternalComponent();
		String text = "";
		if (component != null && (text = component.getText()) == null) {
			text = "";
		}
		return text;
	}

	public int textColor() {
		final Widget component = getInternalComponent();
		return component != null ? component.getTextColor() : -1;
	}

	public String tooltip() {
		final Widget component = getInternalComponent();
		String tip = "";
		if (component != null && (tip = component.getTooltip()) == null) {
			tip = "";
		}
		return tip;
	}

	public int type() {
		final Widget component = getInternalComponent();
		return component != null ? component.getType() : -1;
	}

	public int width() {
		final Widget component = getInternalComponent();
		return component != null ? component.getWidth() : -1;
	}

	public int height() {
		final Widget component = getInternalComponent();
		return component != null ? component.getHeight() : -1;
	}

	public int rotationX() {
		final Widget component = getInternalComponent();
		return component != null ? component.getXRotation() : -1;
	}

	public int rotationY() {
		final Widget component = getInternalComponent();
		return component != null ? component.getYRotation() : -1;
	}

	public int rotationZ() {
		final Widget component = getInternalComponent();
		return component != null ? component.getZRotation() : -1;
	}

	public boolean flippedVertically() {
		final Widget component = getInternalComponent();
		return component != null && component.isVerticallyFlipped();
	}

	public boolean flippedHorizontally() {
		final Widget component = getInternalComponent();
		return component != null && component.isHorizontallyFlipped();
	}

	public int scrollX() {
		final Widget component = getInternalComponent();
		return component != null ? component.getHorizontalScrollbarPosition() : -1;
	}

	public int scrollWidthMax() {
		final Widget component = getInternalComponent();
		return component != null ? component.getHorizontalScrollbarSize() : -1;
	}

	public int scrollWidth() {
		final Widget component = getInternalComponent();
		return component != null ? component.getHorizontalScrollbarThumbSize() : -1;
	}

	public int scrollY() {
		final Widget component = getInternalComponent();
		return component != null ? component.getVerticalScrollbarPosition() : -1;
	}

	public int scrollHeightMax() {
		final Widget component = getInternalComponent();
		return component != null ? component.getVerticalScrollbarSize() : -1;
	}

	public int scrollHeight() {
		final Widget component = getInternalComponent();
		return component != null ? component.getVerticalScrollbarThumbSize() : -1;
	}

	public boolean inventory() {
		final Widget component = getInternalComponent();
		return component != null && component.isInventoryInterface();
	}

	public boolean visible() {
		return _visible(1);
	}

	private boolean _visible(final int depth) {
		if (depth > RECURSION_DEPTH) {
			System.out.printf("WARNING: Visible operation killed -- beyond depth of %d.%n", RECURSION_DEPTH);
			return false;
		}
		final Widget internal = getInternalComponent();
		int id = 0;
		if (internal != null && valid() && !internal.isHidden()) {
			id = parentId();
		}
		return id == -1 || (id != 0 && ctx.widgets.component(id >> 16, id & 0xffff)._visible(depth + 1));
	}

	public Rectangle boundingRect() {
		final Point absLocation = screenPoint();
		if (absLocation.x == -1 && absLocation.y == -1) {
			return new Rectangle(0, 0, -1, -1);
		}
		return new Rectangle(absLocation.x, absLocation.y,
				width(),
				height()
		);
	}

	public Rectangle viewportRect() {
		final Point absLocation = screenPoint();
		if (absLocation.x == -1 && absLocation.y == -1) {
			return new Rectangle(0, 0, -1, -1);
		}
		return new Rectangle(absLocation.x, absLocation.y,
				scrollWidth(),
				scrollHeight()
		);
	}

	@Override
	public Point nextPoint() {
		final Rectangle interact = getInteractRectangle();
		final int x = interact.x, y = interact.y;
		final int w = interact.width, h = interact.height;
		if (interact.width != -1 && interact.height != -1) {
			return Calculations.nextPoint(interact, new Rectangle(x + w / 2, y + h / 2, w / 4, h / 4));
		}
		return new Point(-1, -1);
	}

	public Point centerPoint() {
		final Rectangle interact = getInteractRectangle();
		return interact.getWidth() != -1 && interact.getHeight() != -1 ? new Point((int) interact.getCenterX(), (int) interact.getCenterY()) : new Point(-1, -1);
	}

	@Override
	public boolean contains(final Point point) {
		return getInteractRectangle().contains(point);
	}

	@Override
	public boolean valid() {
		final Widget internal = getInternalComponent();
		return internal != null && (parent.index() == -1 || parent.visible()) &&
				id() != -1 && internal.getBoundsArrayIndex() != -1;
	}

	@Override
	public void draw(final Graphics render) {
		draw(render, 50);
	}

	@Override
	public void draw(final Graphics render, int alpha) {
		final Rectangle rectangle = getInteractRectangle();
		if (rectangle.getWidth() == -1 || rectangle.getHeight() == -1) {
			return;
		}
		Color c = TARGET_FILL_COLOR;
		int rgb = c.getRGB();
		if (((rgb >> 24) & 0xff) != alpha) {
			c = new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha);
		}
		render.setColor(c);
		render.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
		c = TARGET_STROKE_COLOR;
		rgb = c.getRGB();
		alpha *= 3;
		if (((rgb >> 24) & 0xff) != alpha) {
			c = new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha);
		}
		render.setColor(c);
		render.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}

	private Rectangle getInteractRectangle() {
		final Rectangle r = viewportRect();
		r.grow(-1, -1);
		return r;
	}

	private boolean isInScrollableArea() {
		int pId = parentId();
		if (pId == -1) {
			return false;
		}

		int l = 0;
		Component scrollableArea = ctx.widgets.component(pId >> 16, pId & 0xffff);
		while (scrollableArea.scrollHeightMax() == 0 && (pId = scrollableArea.parentId()) != -1) {
			scrollableArea = ctx.widgets.component(pId >> 16, pId & 0xffff);
			if (++l > RECURSION_DEPTH) {
				break;
			}
		}

		return scrollableArea.scrollHeightMax() != 0;
	}

	private Widget getInternalComponent() {
		final Object[] components;
		if (parent.index() != -1) {
			final Widget parentComponent = parent.getInternalComponent();
			components = parentComponent != null ? parentComponent.getComponents() : null;
		} else {
			components = widget.getInternalComponents();
		}
		final Client c = ctx.client();
		return c != null && components != null && index < components.length ? new Widget(c.reflector, components[index]) : null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + index + (parent.index() != -1 ? "/" + parent : "") + "]@" + widget;
	}

	@Override
	public int hashCode() {
		return widget.id() * 31 + index;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof Component)) {
			return false;
		}
		final Component c = (Component) o;
		return c.widget.equals(widget) && c.index == index &&
				(parent.index() == -1 && c.parent.index() == -1 || (parent.index() != -1 && parent.equals(c.parent)));
	}
}
