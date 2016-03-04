package org.powerbot.script;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.powerbot.bot.MouseSpline;

/**
 * Input
 * A utility class for generating input to the canvas and retrieving information from the canvas.
 */
public abstract class Input {
	protected final AtomicBoolean blocking;
	private final MouseSpline spline;
	private final AtomicInteger speed;

	protected Input() {
		blocking = new AtomicBoolean(false);
		spline = new MouseSpline();
		speed = new AtomicInteger(100);
	}

	/**
	 * Set the relative speed for mouse movements.
	 * This is a sensitive function and should be used in exceptional circumstances for a short period of time only.
	 *
	 * @param s the new speed as a percentage, i.e. {@code 10} is 10x faster, {@code 25} is 4x as fast
	 *          and {@code 100} is the full speed. Specifying {@code 0} will not change the speed but return the
	 *          current value instead.
	 * @return the speed, which can be different to the value requested
	 */
	public int speed(final int s) {
		speed.set(Math.min(100, Math.max(10, s)));
		return speed.get();
	}

	// TODO: remove boolean return values for input methods

	/**
	 * Whether or not user input is being relayed to the client instance.
	 * 
	 * @return {@link true} if user input is being blocked, otherwise,
	 * {@link false}.
	 */
	public final boolean blocking() {
		return blocking.get();
	}

	/**
	 * Sets whether or not to block user input from being relayed to the
	 * client instance.
	 * 
	 * @param b Will block user input if set to {@link true}.
	 */
	public void blocking(final boolean b) {
		blocking.set(b);
	}

	/**
	 * Simulates the client focusing.
	 * 
	 */
	public abstract void focus();


	/**
	 * Simulates the client de-focusing.
	 * 
	 */
	public abstract void defocus();

	/**
	 * Sends keys to the client, represented in a String. Virtual keys are also
	 * acceptable input, for example, <code>{VK_ENTER}</code>.
	 * 
	 * @see {@link KeyEvent} for the list of Virtual Keys.
	 * @return whether or not the keys were successfully sent.
	 */
	public abstract boolean send(final String s);

	/**
	 * Sends keys to the client, represented in a String, and a new-line at the
	 * end. Virtual keys are also acceptable input, for example,
	 * <code>{VK_ENTER}</code>.
	 * 
	 * @see {@link KeyEvent} for the list of Virtual Keys.
	 * @return whether or not the keys were successfully sent.
	 */
	public final boolean sendln(final String s) {
		return send(s + "\n");
	}

	/**
	 * Gets the location of the simulated mouse.
	 * 
	 * @return The location as a {@link Point}.
	 */
	public abstract Point getLocation();

	/**
	 * Gets the location of where the simulated mouse has pressed last.
	 * 
	 * @return The location as a {@link Point}.
	 */
	public abstract Point getPressLocation();

	/**
	 * Represents when the simulated mouse was pressed.
	 * 
	 * @returns The point of which the mouse was pressed, measured in
	 * milliseconds, between the current time and midnight,
	 * January 1, 1970 UTC.
	 */
	public abstract long getPressWhen();
	
	/**
	 * Simulates the specified mouse button being pressed.
	 * 
	 * @param button The index of the button to press.
	 * @return Whether or not that button press has been successfully
	 * simulated.
	 */
	public abstract boolean press(final int button);


	/**
	 * Simulates the specified mouse button being released.
	 * 
	 * @param button The index of the button to press.
	 * @return Whether or not that button release has been successfully
	 * simulated.
	 */
	public abstract boolean release(final int button);

	protected abstract boolean setLocation(final Point p);

	/**
	 * Simulates the mouse clicking the specified button at the specified
	 * location.
	 * 
	 * @param x      The point which lies on the x-axis of where to click.
	 * @param y      The point which lies on the y-axis of where to click.
	 * @param button The button index to simulate.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean click(final int x, final int y, final int button) {
		return click(new Point(x, y), button);
	}

	/**
	 * Simulates the mouse clicking the specified button at the specified
	 * location.
	 * 
	 * @param x    The point which lies on the x-axis of where to click.
	 * @param y    The point which lies on the y-axis of where to click.
	 * @param left Whether or not to click the left mouse button, otherwise, it
	 * will click the right.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean click(final int x, final int y, final boolean left) {
		return click(new Point(x, y), left);
	}

	/**
	 * Simulates the mouse clicking the specified button at the specified
	 * location.
	 * 
	 * @param point  The point of where to move the mouse and click.
	 * @param button The button index to simulate.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean click(final Point point, final int button) {
		return move(point) && click(button);
	}

	/**
	 * Simulates the mouse clicking the specified button at the specified
	 * location.
	 * 
	 * @param point The point of where to move the mouse and click.
	 * @param left  Whether not to click the left mouse button, otherwise, it
	 * will click the right.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean click(final Point point, final boolean left) {
		return move(point) && click(left);
	}

	/**
	 * Simulates the mouse clicking the specified button.
	 * 
	 * @param left Whether or not to click the left mouse button, otherwise, it
	 * will click the right.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean click(final boolean left) {
		return click(left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON3);
	}

	/**
	 * Simulates the mouse clicking the specified button.
	 * 
	 * @param button The button index to simulate.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean click(final int button) {
		press(button);
		Condition.sleep(spline.getPressDuration());
		release(button);
		Condition.sleep(spline.getPressDuration());
		return true;
	}

	/**
	 * Holds down the left or right button, and drags the mouse to the
	 * specified location.
	 * 
	 * @param p    The point of where to drag the mouse to.
	 * @param left Whether or not to click the left mouse button, otherwise, it
	 * will click the right.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean drag(final Point p, final boolean left) {
		return drag(p, left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON3);
	}


	/**
	 * Holds down the specified button, and drags the mouse to the specified
	 * location.
	 * 
	 * @param p    The point of where to drag the mouse to.
	 * @param left Whether or not to click the left mouse button, otherwise, it
	 * will click the right.
	 * @return Whether or not the mouse click has been successfully simulated.
	 */
	public final boolean drag(final Point p, final int button) {
		press(button);
		final boolean b = move(p);
		release(button);
		return b;
	}

	/**
	 * Sets the mouse position to the specified Point.
	 * 
	 * @param p The point to move the mouse to.
	 * @return {@link true} if the point is within bounds and was
	 * successfully moved.
	 */
	public final boolean hop(final Point p) {
		return setLocation(p);
	}

	/**
	 * Sets the mouse position to the specified {@link Point}.
	 * 
	 * @param x The point on the x-axis to move the mouse to.
	 * @param y The point on the y-axis to move the mouse to.
 	 * @return {@link true} if the point is within bounds and was
	 * successfully moved.
	 */
	public final boolean hop(final int x, final int y) {
		return hop(new Point(x, y));
	}


	/**
	 * Moves the mouse position in a smooth and human-like manner
	 * to the specified point.
	 * 
	 * @param x The point on the x-axis to move the mouse to.
	 * @param y The point on the y-axis to move the mouse to.
 	 * @return {@link true} if the point is within bounds and was
	 * successfully moved.
	 */
	public final boolean move(final int x, final int y) {
		return move(new Point(x, y));
	}

	/**
	 * Moves the mouse position in a smooth and human-like manner
	 * to the specified {@link Point}.
	 * 
	 * @param p The point of where to move the mouse to.
 	 * @return {@link true} if the point is within bounds and was
	 * successfully moved.
	 */
	public final boolean move(final Point p) {
		return apply(
				new Targetable() {
					@Override
					public Point nextPoint() {
						return p;
					}

					@Override
					public boolean contains(final Point point) {
						return p.equals(point);
					}
				},
				new Filter<Point>() {
					@Override
					public boolean accept(final Point point) {
						return p.equals(point);
					}
				}
		);
	}

	public final boolean apply(final Targetable targetable, final Filter<Point> filter) {
		final Point target_point = new Point(-1, -1);
		final int STANDARD_ATTEMPTS = 3;
		for (int i = 0; i < STANDARD_ATTEMPTS; i++) {
			final Point mp = getLocation();
			final Vector3 start = new Vector3(mp.x, mp.y, 255);
			final Point p = targetable.nextPoint();
			if (p.x == -1 || p.y == -1) {
				continue;
			}
			target_point.move(p.x, p.y);
			final Vector3 end = new Vector3(p.x, p.y, 0);
			final Iterable<Vector3> spline = this.spline.getPath(start, end);
			for (final Vector3 v : spline) {
				hop(v.x, v.y);
				Condition.sleep((int) (this.spline.getAbsoluteDelay(v.z) * (speed.get() / 100d) / 1.33e6));
			}
			final Point p2 = getLocation(), ep = end.toPoint2D();
			if (p2.equals(ep) && filter.accept(ep)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Simulates the mouse wheel scrolling down.
	 * 
	 * @return {@link true} if the mouse wheel has been successfully
	 * simulated, otherwise, {@link false}.
	 */
	public final boolean scroll() {
		return scroll(true);
	}

	/**
	 * Simulates the mouse wheel scrolling.
	 * 
	 * @param down Whether or not to scroll up or down.
	 * @return {@link true} if the mouse wheel has been successfully
	 * simulated, otherwise, {@link false}.
	 */
	public abstract boolean scroll(final boolean down);

	public abstract Dimension getComponentSize();
}
