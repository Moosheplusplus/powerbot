package org.powerbot.script.rt6;

import java.util.Arrays;
import java.util.Iterator;

import org.powerbot.bot.rt6.client.Client;
import org.powerbot.bot.rt6.client.ComponentContainer;
import org.powerbot.script.Identifiable;
import org.powerbot.script.Validatable;

/**
 * Widget
 */
public class Widget extends ClientAccessor implements Identifiable, Validatable, Iterable<Component> {
	private final int index;
	private final Object LOCK;
	private Component[] cache;

	/**
	 * Represents an interactive display window which stores {@link Component}s
	 * and miscellaneous data.
	 * 
	 * @param ctx The {@link ClientContext}
	 * @param index The Widget index
	 */
	public Widget(final ClientContext ctx, final int index) {
		super(ctx);
		this.index = index;
		LOCK = new Object();
		cache = new Component[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int id() {
		return index;
	}
	
	/**
	 * The amount of components nested in a widget.
	 * 
	 * @return The amount represented as an integer
	 */
	public int componentCount() {
		final Object[] internal = getInternalComponents();
		return internal != null ? internal.length : 0;
	}

	/**
	 * An array of the nested components within the widget.
	 * 
	 * @return A {@link Component} array
	 */
	public Component[] components() {
		synchronized (LOCK) {
			final Object[] components = getInternalComponents();
			if (components == null) {
				return cache;
			}
			if (cache.length < components.length) {
				final int len = cache.length;
				cache = Arrays.copyOf(cache, components.length);
				for (int i = len; i < components.length; i++) {
					cache[i] = new Component(ctx, this, i);
				}
			}
			return cache.clone();
		}
	}

	/**
	 * Gets the component at the specified index.
	 * 
	 * @param index The index of the component
	 * @return The component at the specified index, or <code>nil</code> if the
	 * component does not exist.
	 */
	public Component component(final int index) {
		synchronized (LOCK) {
			if (index < cache.length) {
				return cache[index];
			}
			final Object[] components = getInternalComponents();
			final int mod = Math.max(components != null ? components.length : 0, index + 1);
			if (cache.length < mod) {
				final int len = cache.length;
				cache = Arrays.copyOf(cache, mod);
				for (int i = len; i < mod; i++) {
					cache[i] = new Component(ctx, this, i);
				}
			}
			return cache[index];
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean valid() {
		final Client client = ctx.client();
		if (client == null || index < 0) {
			return false;
		}

		final Object[] containers = client.getWidgets();
		return containers.length > 0 && index < containers.length && containers[index] != null && new ComponentContainer(client.reflector, containers[index]).getComponents().length > 0;
	}

	Object[] getInternalComponents() {
		final Client client = ctx.client();
		if (client == null) {
			return null;
		}
		final Object[] containers = client.getWidgets();
		final ComponentContainer container;
		if (containers != null && index >= 0 && index < containers.length && !(container = new ComponentContainer(client.reflector, containers[index])).isNull()) {
			return container.getComponents();
		}
		return null;
	}

	@Override
	public Iterator<Component> iterator() {
		final int count = componentCount();
		return new Iterator<Component>() {
			private int nextId = 0;

			@Override
			public boolean hasNext() {
				return nextId < count;
			}

			@Override
			public Component next() {
				return component(nextId++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + index + "]";
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof Widget)) {
			return false;
		}
		final Widget w = (Widget) o;
		return w.index == index;
	}
}
