package org.powerbot.script.rt6;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.powerbot.bot.rt6.client.Client;
import org.powerbot.bot.rt6.client.Floor;
import org.powerbot.bot.rt6.client.MapOffset;
import org.powerbot.bot.rt6.client.TransformMatrix;
import org.powerbot.script.Condition;
import org.powerbot.script.Locatable;
import org.powerbot.script.Tile;

/**
 * Game
 */
public class Game extends ClientAccessor {
	public static final int[] SIN_TABLE = new int[16384];
	public static final int[] COS_TABLE = new int[16384];

	static {
		final double d = 0.0003834951969714103d;
		for (int i = 0; i < 16384; i++) {
			SIN_TABLE[i] = (int) (16384d * Math.sin(i * d));
			COS_TABLE[i] = (int) (16384d * Math.cos(i * d));
		}
	}

	private Component viewport_component = null;

	public Game(final ClientContext factory) {
		super(factory);
	}

	/**
	 * Logs out of the game into either the lobby or login screen.
	 *
	 * @param lobby <tt>true</tt> for the lobby; <tt>false</tt> for the login screen
	 * @return <tt>true</tt> if successfully logged out; otherwise <tt>false</tt>
	 */
	public boolean logout(final boolean lobby) {
		if (!ctx.hud.open(Hud.Menu.OPTIONS) && !ctx.input.send("{ESCAPE}")) {
			return false;
		}
		final Widget widget = ctx.widgets.widget(1433);
		if (Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return widget.component(78).visible();
			}
		}, 100, 10)) {
			if (!widget.component(lobby ? 70 : 78).interact("Select")) {
				return false;
			}
		}
		return Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return clientState() == (lobby ? org.powerbot.script.rt6.Constants.GAME_LOBBY : org.powerbot.script.rt6.Constants.GAME_LOGIN);
			}
		});
	}

	/**
	 * Returns the current client state.
	 *
	 * @return the client state
	 * @see org.powerbot.script.rt6.Constants#GAME_LOGIN
	 * @see org.powerbot.script.rt6.Constants#GAME_LOBBY
	 * @see org.powerbot.script.rt6.Constants#GAME_LOGGING
	 * @see org.powerbot.script.rt6.Constants#GAME_MAP_LOADED
	 * @see org.powerbot.script.rt6.Constants#GAME_MAP_LOADING
	 */
	public int clientState() {
		final Client client = ctx.client();
		if (client == null) {
			return -1;
		}
		final int state = client.getClientState();
		if (state == client.reflector.getConstant("V_CLIENT_GAMESTATE_LOGIN_SCREEN")) {
			return Constants.GAME_LOGIN;
		} else if (state == client.reflector.getConstant("V_CLIENT_GAMESTATE_LOBBY_SCREEN")) {
			return Constants.GAME_LOBBY;
		} else if (state == client.reflector.getConstant("V_CLIENT_GAMESTATE_LOGGING_IN")) {
			return Constants.GAME_LOGGING;
		} else if (state == client.reflector.getConstant("V_CLIENT_GAMESTATE_ENVIRONMENT_PLAYABLE")) {
			return Constants.GAME_MAP_LOADED;
		} else if (state == client.reflector.getConstant("V_CLIENT_GAMESTATE_ENVIRONMENT_LOADING")) {
			return Constants.GAME_MAP_LOADING;
		}
		return -1;
	}

	/**
	 * Determines if the player is logged into the game.
	 *
	 * @return <tt>true</tt> if logged in; otherwise <tt>false</tt>
	 */
	public boolean loggedIn() {
		final int state = clientState();
		return state == org.powerbot.script.rt6.Constants.GAME_MAP_LOADED || state == org.powerbot.script.rt6.Constants.GAME_MAP_LOADING;
	}

	/**
	 * Determines the current {@link Crosshair} displayed.
	 *
	 * @return the displayed {@link Crosshair}
	 */
	public Crosshair crosshair() {
		final Client client = ctx.client();
		final int type = client != null ? client.getCrossHairType() : -1;
		if (type < 0 || type > 2) {
			return Crosshair.NONE;
		}
		return Crosshair.values()[type];
	}

	/**
	 * Determines the base of the loaded region.
	 *
	 * @return the {@link Tile} of the base
	 */
	public Tile mapOffset() {
		final Client client = ctx.client();
		if (client == null) {
			return Tile.NIL;
		}
		final MapOffset b = client.getWorld().getMapOffset();
		if (b.isNull()) {
			return Tile.NIL;
		}
		return new Tile(b.getX(), b.getY(), client.getFloor());
	}

	/**
	 * Determines the current floor level.
	 *
	 * @return the current floor level
	 */
	public int floor() {
		final Client client = ctx.client();
		if (client == null) {
			return -1;
		}
		return client.getFloor();
	}

	/**
	 * Determines if a point is in the viewport.
	 *
	 * @param point the point to check
	 * @return <tt>true</tt> if the point is in the viewport; otherwise <tt>false</tt>
	 */
	public boolean inViewport(final Point point) {
		return inViewport(point.x, point.y);
	}

	/**
	 * Determines if a point is in the viewport.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @return <tt>true</tt> if the point is in the viewport; otherwise <tt>false</tt>
	 */
	public boolean inViewport(final int x, final int y) {
		final Viewport v = getViewport();
		if (x > 0 && y > 0) {
			if (loggedIn()) {
				final Rectangle[] rectangles = ctx.hud.bounds();
				for (final Rectangle rectangle : rectangles) {
					if (rectangle.contains(x, y)) {
						return false;
					}
				}
			}
			return x >= v.x && x <= v.mx && y >= v.y && y <= v.my;
		}
		return false;
	}

	/**
	 * Determines the tile height at the provided point in the game region.
	 *
	 * @param rX    the relative x
	 * @param rY    the relative y
	 * @param plane the plane
	 * @return the height at the given point
	 */
	public int tileHeight(final int rX, final int rY, int plane) {
		final Client c = ctx.client();
		if (c == null) {
			return 0;
		}
		if (plane == -1) {
			plane = c.getFloor();
		}
		final int x = rX >> 9, y = rY >> 9;
		final byte[][][] configs = c.getWorld().getFloorSettings().getBytes();
		if (x < 0 || x > 103 || y < 0 || y > 103) {
			return 0;
		}
		if (plane < 3 && (configs[1][x][y] & 2) != 0) {
			++plane;
		}
		final Floor[] landscape = c.getWorld().getLandscape().getFloors();
		if (plane < 0 || plane >= landscape.length) {
			return 0;
		}
		try {
			final int[][] heights = landscape[plane].getHeights();
			final int aX = rX & 0x1ff;
			final int aY = rY & 0x1ff;
			final int start_h = heights[x][y] * (512 - aX) + heights[x + 1][y] * aX >> 9;
			final int end_h = heights[x][1 + y] * (512 - aX) + heights[x + 1][y + 1] * aX >> 9;
			return start_h * (512 - aY) + end_h * aY >> 9;
		} catch (final Exception ignored) {
		}
		return 0;
	}

	/**
	 * Determines an in-view point of the given point in the game region.
	 *
	 * @param x      the relative x position
	 * @param y      the relative y position
	 * @param plane  the plane
	 * @param height the height offset
	 * @return the {@link Point} in game space
	 */
	public Point groundToScreen(final int x, final int y, final int plane, final int height) {
		if (x < 512 || y < 512 || x > 52224 || y > 52224) {
			return new Point(-1, -1);
		}
		final int h = tileHeight(x, y, plane) + height;
		return worldToScreen(x, h, y);
	}

	/**
	 * Transforms the given matrix (3D) into a game screen (2D) point.
	 *
	 * @param x the x
	 * @param y the y
	 * @param z the depth
	 * @return the {@link Point} in game space, otherwise {@code new Point(-1, -1)}
	 */
	public Point worldToScreen(final int x, final int y, final int z) {
		final Viewport viewport = getViewport();
		final float[] product = new float[4];
		try {
			getViewProjMatrix().multiply(x, y, z, product);
		} catch (final Exception ignored) {
			return new Point(-1, -1);
		}
		if (product[2] >= -product[3]) {
			final float w = 1.0f / product[3];
			product[0] = viewport.cx + product[0] * w * viewport.hw;
			product[1] = viewport.cy + product[1] * w * viewport.hh;
			if (viewport.contains(product[0], product[1])) {
				return new Point((int) product[0], (int) product[1]);
			}
		}
		return new Point(-1, -1);
	}

	/**
	 * Calculates a point on the mini-map.
	 *
	 * @param locatable the {@link org.powerbot.script.Locatable} to convert to map point
	 * @return the map {@link Point}
	 */
	public Point tileToMap(final Locatable locatable) {
		final Point bad = new Point(-1, -1);
		final Client client = ctx.client();
		final Tile b = ctx.game.mapOffset();
		final Tile t = locatable.tile().derive(-b.x(), -b.y());
		final int tx = t.x();
		final int ty = t.y();
		if (client == null || tx < 1 || tx > 103 || ty < 1 || ty > 103) {
			return bad;
		}

		final RelativeLocation r = ctx.players.local().relative();
		final float offX = (tx * 4 - r.x() / 128) + 2;
		final float offY = (ty * 4 - r.z() / 128) + 2;
		final int d = (int) Math.round(Math.sqrt(Math.pow(offX, 2) + Math.pow(offY, 2)));

		final Component component = mapComponent();
		final int w = component.scrollWidth();
		final int h = component.scrollHeight();
		final int radius = Math.max(w / 2, h / 2) + 10;
		if (d >= radius /*|| component.contentType() != 1338*/) {
			return bad;
		}

		final boolean f = client.getMinimapSettings() == client.reflector.getConstant("V_MINIMAP_SCALE_ON_VALUE");
		final double a = ctx.camera.rotation() * 16384d / (Math.PI * 2d);
		int i = 0x3fff & (int) a;
		if (!f) {
			i = 0x3fff & client.getMinimapOffset() + (int) a;
		}
		int sin = SIN_TABLE[i], cos = COS_TABLE[i];
		if (!f) {
			final int scale = 256 + client.getMinimapScale();
			sin = 256 * sin / scale;
			cos = 256 * cos / scale;
		}

		int rotX = (int) (cos * offX + sin * offY) >> 14;
		int rotY = (int) (cos * offY - sin * offX) >> 14;
		rotX += w / 2;
		rotY *= -1;
		rotY += h / 2;

		if (rotX > 4 && rotX < w - 4 &&
				rotY > 4 && rotY < h - 4) {
			final Point basePoint = component.screenPoint();
			final int sX = rotX + (int) basePoint.getX();
			final int sY = rotY + (int) basePoint.getY();
			final Point p = new Point(sX, sY);
			if (ctx.hud.legacy()) {
				final Point mid = new Point(basePoint.x + component.width() / 2, basePoint.y + component.height() / 2);
				if (Math.pow(mid.x - p.x, 2) + Math.pow(mid.y - p.y, 2) >= Math.pow(68, 2)) {
					return bad;
				}
			} else {
				final Rectangle rbuffer = new Rectangle(p.x - 6, p.y - 6, 12, 12);//entire tile and a half sized 'buffer' area
				for (final Component blocking : mapBlockingComponents()) {
					if (blocking.viewportRect().intersects(rbuffer)) {
						return bad;
					}
				}
			}
			return p;
		}

		return bad;
	}

	Matrix4f getViewMatrix() {
		final TransformMatrix m = ctx.client().getViewMatrix();
		final Matrix4f store = new Matrix4f();
		store.m00 = m.m00();
		store.m01 = m.m01();
		store.m02 = m.m02();
		store.m03 = m.m03();
		store.m10 = m.m10();
		store.m11 = m.m11();
		store.m12 = m.m12();
		store.m13 = m.m13();
		store.m20 = m.m20();
		store.m21 = m.m21();
		store.m22 = m.m22();
		store.m23 = m.m23();
		//No projection terms.
		store.m30 = 0.0f;
		store.m31 = 0.0f;
		store.m32 = 0.0f;
		store.m33 = 1.0f;
		return store;
	}

	Matrix4f getProjMatrix() {
		return new Matrix4f(ctx.client().getProjMatrix().getMatrix(), false);
	}

	Matrix4f getViewProjMatrix() {
		final Matrix4f viewProjMatrix = new Matrix4f();
		Matrix4f.multiply(getProjMatrix(), getViewMatrix(), viewProjMatrix);
		return viewProjMatrix;
	}

	public Viewport getViewport() {
		final Client client = ctx.client();
		if (client == null) {
			return new Viewport(0, 0, 0, 0);
		}

		if (viewport_component != null &&
				(viewport_component.contentType() == 1337 || viewport_component.contentType() == 1407)) {
			final Rectangle r = viewport_component.viewportRect();
			if (r.width + r.height > 0) {
				return new Viewport(r.x, r.y, r.width, r.height);
			}
		}

		for (int i = 0; i < client.getWidgets().length; i++) {
			for (final Component c : ctx.widgets.widget(i)) {
				if (c.contentType() == 1337 || c.contentType() == 1407) {//TODO 1403?
					if (!c.valid()) {
						continue;
					}
					viewport_component = c;
					final Rectangle r = c.viewportRect();
					return new Viewport(r.x, r.y, r.width, r.height);
				}
			}
		}
		return new Viewport(0, 0, 0, 0);
	}

	public Component mapComponent() {
		final Widget i = ctx.widgets.widget(Constants.MOVEMENT_WIDGET);
		for (final Component c : i.components()) {
			if (c.contentType() == 1338) {
				return c;
			}
		}
		return ctx.widgets.component(Constants.MOVEMENT_WIDGET, Constants.MOVEMENT_MAP);
	}

	private List<Component> mapBlockingComponents() {
		final List<Component> ret = new ArrayList<Component>();
		final Widget widget = ctx.widgets.widget(Constants.MOVEMENT_WIDGET);
		final int[][] bounds = new int[][]{
				{36, 36},
				{44, 44},
				{30, 30},
		};

		for (final Component c : widget.components()) {
			if (!c.visible()) {
				continue;
			}

			final int w = c.width(), h = c.height();
			for (final int[] b : bounds) {
				if (b[0] == w && b[1] == h) {
					ret.add(c);
					break;
				}
			}
		}

		return ret;
	}

	/**
	 * An enumeration of the possible cross-hairs in game.
	 */
	public enum Crosshair implements org.powerbot.script.Crosshair {
		NONE, DEFAULT, ACTION
	}

	static class Matrix4f {
		public float m00, m01, m02, m03;
		public float m10, m11, m12, m13;
		public float m20, m21, m22, m23;
		public float m30, m31, m32, m33;

		public Matrix4f(
				final float m00, final float m01, final float m02, final float m03,
				final float m10, final float m11, final float m12, final float m13,
				final float m20, final float m21, final float m22, final float m23,
				final float m30, final float m31, final float m32, final float m33
		) {
			this.m00 = m00;
			this.m01 = m01;
			this.m02 = m02;
			this.m03 = m03;
			this.m10 = m10;
			this.m11 = m11;
			this.m12 = m12;
			this.m13 = m13;
			this.m20 = m20;
			this.m21 = m21;
			this.m22 = m22;
			this.m23 = m23;
			this.m30 = m30;
			this.m31 = m31;
			this.m32 = m32;
			this.m33 = m33;
		}

		public Matrix4f(final float[] matrix, final boolean rowMajor) {
			if (matrix.length != 16) {
				throw new IllegalArgumentException("Array must be of size 16.");
			}

			if (rowMajor) {
				m00 = matrix[0];
				m01 = matrix[1];
				m02 = matrix[2];
				m03 = matrix[3];
				m10 = matrix[4];
				m11 = matrix[5];
				m12 = matrix[6];
				m13 = matrix[7];
				m20 = matrix[8];
				m21 = matrix[9];
				m22 = matrix[10];
				m23 = matrix[11];
				m30 = matrix[12];
				m31 = matrix[13];
				m32 = matrix[14];
				m33 = matrix[15];
			} else {
				m00 = matrix[0];
				m01 = matrix[4];
				m02 = matrix[8];
				m03 = matrix[12];
				m10 = matrix[1];
				m11 = matrix[5];
				m12 = matrix[9];
				m13 = matrix[13];
				m20 = matrix[2];
				m21 = matrix[6];
				m22 = matrix[10];
				m23 = matrix[14];
				m30 = matrix[3];
				m31 = matrix[7];
				m32 = matrix[11];
				m33 = matrix[15];
			}
		}

		public Matrix4f(final float x, final float y, final float z) {
			m00 = 1.0f;
			m01 = 0.0f;
			m02 = 0.0f;
			m03 = -x;

			m10 = 0.0f;
			m11 = 1.0f;
			m12 = 0.0f;
			m13 = -y;

			m20 = 0.0f;
			m21 = 0.0f;
			m22 = 1.0f;
			m23 = -z;

			m30 = 0.0f;
			m31 = 0.0f;
			m32 = 0.0f;
			m33 = 1.0f;
		}

		public Matrix4f() {
			m01 = m02 = m03 = 0.0f;
			m10 = m12 = m13 = 0.0f;
			m20 = m21 = m23 = 0.0f;
			m30 = m31 = m32 = 0.0f;
			m00 = m11 = m22 = m33 = 1.0f;
		}

		public Matrix4f(final float angle, final float x, final float y, final float z) {
			this();
			Matrix4f.rotate(this, angle, x, y, z, this);
		}

		public static void multiply(final Matrix4f leftSide, final Matrix4f rightSide, final Matrix4f product) {
			final float m00 = leftSide.m00 * rightSide.m00 + leftSide.m01 * rightSide.m10 + leftSide.m02 * rightSide.m20 + leftSide.m03 * rightSide.m30,
					m01 = leftSide.m00 * rightSide.m01 + leftSide.m01 * rightSide.m11 + leftSide.m02 * rightSide.m21 + leftSide.m03 * rightSide.m31,
					m02 = leftSide.m00 * rightSide.m02 + leftSide.m01 * rightSide.m12 + leftSide.m02 * rightSide.m22 + leftSide.m03 * rightSide.m32,
					m03 = leftSide.m00 * rightSide.m03 + leftSide.m01 * rightSide.m13 + leftSide.m02 * rightSide.m23 + leftSide.m03 * rightSide.m33;

			final float m10 = leftSide.m10 * rightSide.m00 + leftSide.m11 * rightSide.m10 + leftSide.m12 * rightSide.m20 + leftSide.m13 * rightSide.m30,
					m11 = leftSide.m10 * rightSide.m01 + leftSide.m11 * rightSide.m11 + leftSide.m12 * rightSide.m21 + leftSide.m13 * rightSide.m31,
					m12 = leftSide.m10 * rightSide.m02 + leftSide.m11 * rightSide.m12 + leftSide.m12 * rightSide.m22 + leftSide.m13 * rightSide.m32,
					m13 = leftSide.m10 * rightSide.m03 + leftSide.m11 * rightSide.m13 + leftSide.m12 * rightSide.m23 + leftSide.m13 * rightSide.m33;

			final float m20 = leftSide.m20 * rightSide.m00 + leftSide.m21 * rightSide.m10 + leftSide.m22 * rightSide.m20 + leftSide.m23 * rightSide.m30,
					m21 = leftSide.m20 * rightSide.m01 + leftSide.m21 * rightSide.m11 + leftSide.m22 * rightSide.m21 + leftSide.m23 * rightSide.m31,
					m22 = leftSide.m20 * rightSide.m02 + leftSide.m21 * rightSide.m12 + leftSide.m22 * rightSide.m22 + leftSide.m23 * rightSide.m32,
					m23 = leftSide.m20 * rightSide.m03 + leftSide.m21 * rightSide.m13 + leftSide.m22 * rightSide.m23 + leftSide.m23 * rightSide.m33;

			final float m30 = leftSide.m30 * rightSide.m00 + leftSide.m31 * rightSide.m10 + leftSide.m32 * rightSide.m20 + leftSide.m33 * rightSide.m30,
					m31 = leftSide.m30 * rightSide.m01 + leftSide.m31 * rightSide.m11 + leftSide.m32 * rightSide.m21 + leftSide.m33 * rightSide.m31,
					m32 = leftSide.m30 * rightSide.m02 + leftSide.m31 * rightSide.m12 + leftSide.m32 * rightSide.m22 + leftSide.m33 * rightSide.m32,
					m33 = leftSide.m30 * rightSide.m03 + leftSide.m31 * rightSide.m13 + leftSide.m32 * rightSide.m23 + leftSide.m33 * rightSide.m33;

			product.m00 = m00;
			product.m01 = m01;
			product.m02 = m02;
			product.m03 = m03;

			product.m10 = m10;
			product.m11 = m11;
			product.m12 = m12;
			product.m13 = m13;

			product.m20 = m20;
			product.m21 = m21;
			product.m22 = m22;
			product.m23 = m23;

			product.m30 = m30;
			product.m31 = m31;
			product.m32 = m32;
			product.m33 = m33;
		}

		public static void inversion(final Matrix4f source, final Matrix4f dest) {
			final float fA0 = source.m00 * source.m11 - source.m01 * source.m10,
					fA1 = source.m00 * source.m12 - source.m02 * source.m10,
					fA2 = source.m00 * source.m13 - source.m03 * source.m10,
					fA3 = source.m01 * source.m12 - source.m02 * source.m11,
					fA4 = source.m01 * source.m13 - source.m03 * source.m11,
					fA5 = source.m02 * source.m13 - source.m03 * source.m12,
					fB0 = source.m20 * source.m31 - source.m21 * source.m30,
					fB1 = source.m20 * source.m32 - source.m22 * source.m30,
					fB2 = source.m20 * source.m33 - source.m23 * source.m30,
					fB3 = source.m21 * source.m32 - source.m22 * source.m31,
					fB4 = source.m21 * source.m33 - source.m23 * source.m31,
					fB5 = source.m22 * source.m33 - source.m23 * source.m32;

			final float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;
			if (Math.abs(fDet) <= 0f) {
				throw new ArithmeticException("This matrix cannot be inverted");
			}

			final float fInvDet = 1.0f / fDet;
			final float m00 = (source.m11 * fB5 - source.m12 * fB4 + source.m13 * fB3) * fInvDet,
					m10 = (-source.m10 * fB5 + source.m12 * fB2 - source.m13 * fB1) * fInvDet,
					m20 = (source.m10 * fB4 - source.m11 * fB2 + source.m13 * fB0) * fInvDet,
					m30 = (-source.m10 * fB3 + source.m11 * fB1 - source.m12 * fB0) * fInvDet;

			final float m01 = (-source.m01 * fB5 + source.m02 * fB4 - source.m03 * fB3) * fInvDet,
					m11 = (source.m00 * fB5 - source.m02 * fB2 + source.m03 * fB1) * fInvDet,
					m21 = (-source.m00 * fB4 + source.m01 * fB2 - source.m03 * fB0) * fInvDet,
					m31 = (source.m00 * fB3 - source.m01 * fB1 + source.m02 * fB0) * fInvDet;

			final float m02 = (source.m31 * fA5 - source.m32 * fA4 + source.m33 * fA3) * fInvDet,
					m12 = (-source.m30 * fA5 + source.m32 * fA2 - source.m33 * fA1) * fInvDet,
					m22 = (source.m30 * fA4 - source.m31 * fA2 + source.m33 * fA0) * fInvDet,
					m32 = (-source.m30 * fA3 + source.m31 * fA1 - source.m32 * fA0) * fInvDet;

			final float m03 = (-source.m21 * fA5 + source.m22 * fA4 - source.m23 * fA3) * fInvDet,
					m13 = (source.m20 * fA5 - source.m22 * fA2 + source.m23 * fA1) * fInvDet,
					m23 = (-source.m20 * fA4 + source.m21 * fA2 - source.m23 * fA0) * fInvDet,
					m33 = (source.m20 * fA3 - source.m21 * fA1 + source.m22 * fA0) * fInvDet;

			dest.m00 = m00;
			dest.m01 = m01;
			dest.m02 = m02;
			dest.m03 = m03;
			dest.m10 = m10;
			dest.m11 = m11;
			dest.m12 = m12;
			dest.m13 = m13;
			dest.m20 = m20;
			dest.m21 = m21;
			dest.m22 = m22;
			dest.m23 = m23;
			dest.m30 = m30;
			dest.m31 = m31;
			dest.m32 = m32;
			dest.m33 = m33;
		}

		public static void rotate(final Matrix4f source, final float angle, final float x, final float y, final float z, final Matrix4f dest) {
			final float fCos = (float) Math.cos(angle), fSin = (float) Math.sin(angle);

			final float fOneMinusCos = 1.0f - fCos;
			final float fXMultYMultFOneMinusCos = x * y * fOneMinusCos,
					fXMultZMultFOneMinusCos = x * z * fOneMinusCos,
					fYMultZMultFOneMinusCos = y * z * fOneMinusCos;
			final float fZMultFSin = z * fSin, fYMultFSin = y * fSin, fXMultFSin = x * fSin;

			final float f3 = x * x * fOneMinusCos + fCos;
			final float f4 = fXMultYMultFOneMinusCos + fZMultFSin,
					f5 = fXMultZMultFOneMinusCos - fYMultFSin,
					f6 = fXMultYMultFOneMinusCos - fZMultFSin;
			final float f7 = y * y * fOneMinusCos + fCos;
			final float f8 = fYMultZMultFOneMinusCos + fXMultFSin,
					f9 = fXMultZMultFOneMinusCos + fYMultFSin,
					f10 = fYMultZMultFOneMinusCos - fXMultFSin;
			final float f11 = z * z * fOneMinusCos + fCos;

			final float f12 = source.m00, f13 = source.m10, f14 = source.m01,
					f15 = source.m11, f16 = source.m02, f17 = source.m12,
					f18 = source.m03, f19 = source.m13;

			dest.m00 = (f12 * f3 + f13 * f6 + source.m20 * f9);
			dest.m10 = (f12 * f4 + f13 * f7 + source.m20 * f10);
			dest.m20 = (f12 * f5 + f13 * f8 + source.m20 * f11);

			dest.m01 = (f14 * f3 + f15 * f6 + source.m21 * f9);
			dest.m11 = (f14 * f4 + f15 * f7 + source.m21 * f10);
			dest.m21 = (f14 * f5 + f15 * f8 + source.m21 * f11);

			dest.m02 = (f16 * f3 + f17 * f6 + source.m22 * f9);
			dest.m12 = (f16 * f4 + f17 * f7 + source.m22 * f10);
			dest.m22 = (f16 * f5 + f17 * f8 + source.m22 * f11);

			dest.m03 = (f18 * f3 + f19 * f6 + source.m23 * f9);
			dest.m13 = (f18 * f4 + f19 * f7 + source.m23 * f10);
			dest.m23 = (f18 * f5 + f19 * f8 + source.m23 * f11);
		}

		public void multiply(final float x, final float y, final float z, final float[] store) {
			store[0] = (m00 * x + m01 * y + m02 * z + m03);
			store[1] = (m10 * x + m11 * y + m12 * z + m13);
			store[2] = (m20 * x + m21 * y + m22 * z + m23);
			if (store.length > 3) {
				store[3] = (m30 * x + m31 * y + m32 * z + m33);
			}
		}

		public float[] getTranslation() {
			return new float[]{this.m03, this.m13, this.m23};
		}
	}

	public static class Viewport {
		public final float x;
		public final float y;
		public final float width;
		public final float height;
		public final float hw;
		public final float hh;
		public final float cx;
		public final float cy;
		public final float mx;
		public final float my;

		public Viewport(final float x, final float y, final float width, final float height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.hw = width / 2f;
			this.hh = height / 2f;
			this.cx = this.hw + this.x;
			this.cy = this.hh + this.y;
			this.mx = this.x + this.width;
			this.my = this.y + this.height;
		}

		public boolean contains(final float x, final float y) {
			return x > this.x && x < mx && y > this.y && y < my;
		}

		@Override
		public String toString() {
			return String.format("Viewport[x=%f,y=%f,w=%f,h=%f]", x, y, width, height);
		}
	}
}
