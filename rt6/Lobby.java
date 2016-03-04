package org.powerbot.script.rt6;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.StringUtils;

/**
 * Lobby
 */
public class Lobby extends ClientAccessor {
	public Lobby(final ClientContext ctx) {
		super(ctx);
	}

	public boolean opened() {
		return ctx.game.clientState() == Constants.GAME_LOBBY;
	}

	public boolean close() {
		return ctx.game.clientState() == Constants.GAME_LOGIN || ctx.widgets.component(Constants.LOBBY_WIDGET, Constants.LOBBY_CLOSE).component(Constants.LOBBY_CLOSE_SUB).click() && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return ctx.game.clientState() == Constants.GAME_LOGGING;
			}
		});
	}

	public Tab tab() {
		if (!opened()) {
			return Tab.NONE;
		}
		final String s = ctx.widgets.component(Constants.LOBBY_WIDGET, Constants.LOBBY_TABS).component(Constants.LOBBY_TAB_CURRENT).text().replace(' ', '_');
		for (final Tab t : Tab.values()) {
			if (t.name().equalsIgnoreCase(s)) {
				return t;
			}
		}
		return Tab.NONE;
	}

	public boolean tab(final Tab tab) {
		return opened() && (tab() == tab || ctx.widgets.component(Constants.LOBBY_WIDGET, Constants.LOBBY_TABS).component(tab.component()).click() && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return tab() == tab;
			}
		}, 100, 15));
	}

	public List<World> worlds() {
		return worlds(new Filter<World>() {
			@Override
			public boolean accept(final World world) {
				return true;
			}
		});
	}

	public World world(final int number) {
		final World nil = new World(-1, -1, false, -1, "", "", false, -1);
		final List<World> l = worlds(new Filter<World>() {
			@Override
			public boolean accept(final World world) {
				return world.number == number;
			}
		});
		return l.size() == 1 ? l.get(0) : nil;
	}

	public List<World> worlds(final Filter<World> f) {
		final ArrayList<World> list = new ArrayList<World>();
		if (!tab(Tab.WORLD_SELECT)) {
			return list;
		}
		final Widget w = ctx.widgets.widget(Constants.LOBBY_WORLDS);
		final int[] groups = {
				Constants.LOBBY_WORLDS_NUMBER, Constants.LOBBY_WORLDS_FAVOURITES, Constants.LOBBY_WORLDS_PLAYERS,
				Constants.LOBBY_WORLDS_ACTIVITY, Constants.LOBBY_WORLDS_TYPE, Constants.LOBBY_WORLDS_LOOTSHARE,
				Constants.LOBBY_WORLDS_PING
		};
		final Component[] comps = new Component[groups.length];
		int base = -1;
		for (int i = 0; i < groups.length; i++) {
			comps[i] = w.component(groups[i]);
			final int c = comps[i].childrenCount();
			if (base == -1) {
				base = c;
			} else if (base != c) {
				return list;
			}
		}
		for (int i = 0; i < base; i++) {
			final int number = StringUtils.parseInt(comps[0].component(i).text());
			final boolean favorite = comps[1].component(i).textureId() == Constants.LOBBY_TEXTURE_START;
			final int players;
			try {
				players = Integer.parseInt(comps[2].component(i).text());
			} catch (final NumberFormatException ignored) {
				continue;
			}
			final String activity = comps[3].component(i).text(), type = comps[4].component(i).text();
			final boolean lootshare = comps[5].component(i).textureId() == Constants.LOBBY_TEXTURE_LOOTSHARE;
			int ping = -1;
			try {
				ping = Integer.parseInt(comps[6].component(i).text());
			} catch (final NumberFormatException ignored) {
			}

			final World world = new World(i, number, favorite, players, activity, type, lootshare, ping);
			if (f.accept(world)) {
				list.add(world);
			}
		}
		return list;
	}

	public World world() {
		final World nil = new World(-1, -1, false, -1, "", "", false, -1);
		final String cw = ctx.widgets.component(Constants.LOBBY_WIDGET, Constants.LOBBY_CURRENT_WORLD).text();
		final Matcher m = Pattern.compile("^World\\s(\\d*)$").matcher(cw);
		if (m.find()) {
			final int number = StringUtils.parseInt(m.group(1));
			final List<World> worlds = worlds(new Filter<World>() {
				@Override
				public boolean accept(final World world) {
					return world.number() == number;
				}
			});
			return worlds.size() == 1 ? worlds.get(0) : nil;
		}
		return nil;
	}

	public boolean world(final World world) {
		final World c = world(world.number);
		if (c.number == -1) {
			return false;
		}
		final Component bar = ctx.widgets.component(Constants.LOBBY_WORLDS, Constants.LOBBY_WORLDS_BARS).component(c.index);
		final Component viewport = ctx.widgets.component(Constants.LOBBY_WORLDS, Constants.LOBBY_WORLDS_VIEWPORT);
		final Component scrollbar = ctx.widgets.component(Constants.LOBBY_WORLDS, Constants.LOBBY_WORLDS_SCROLL);
		return ctx.widgets.scroll(bar, viewport, scrollbar, true) && bar.click("Select", "World " + c.number) && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return world().number == c.number;
			}
		});
	}

	public boolean enterGame() {
		final Component c = ctx.widgets.component(Constants.LOBBY_WIDGET, Constants.LOBBY_PLAY);
		if (!c.visible()) {
			if (!tab(Tab.PLAYER_INFO) || !Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return c.visible();
				}
			})) {
				return false;
			}
		}
		if (c.click() && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return ctx.game.clientState() != Constants.GAME_LOBBY;
			}
		})) {
			int state;
			while ((state = ctx.game.clientState()) != Constants.GAME_MAP_LOADED) {
				final Component c2 = ctx.widgets.component(Constants.LOBBY_WIDGET, Constants.LOBBY_ERROR);
				if (!Condition.wait(new Condition.Check() {
					@Override
					public boolean poll() {
						final int state = ctx.game.clientState();
						return state == Constants.GAME_MAP_LOADED || state == Constants.GAME_LOBBY || c2.visible();
					}
				}, 600, 50)) {
					break;
				}
				if (state == Constants.GAME_LOBBY || c2.visible() && c2.click()) {
					ctx.properties.put("login.world", "0");
					break;
				}
			}
		}
		return ctx.game.clientState() == Constants.GAME_MAP_LOADED;
	}

	public enum Tab {
		PLAYER_INFO, WORLD_SELECT, FRIENDS, FRIENDS_CHAT, CLAN_CHAT, OPTIONS, NONE;

		public int component() {
			return Constants.LOBBY_TAB_START + Constants.LOBBY_TAB_LENGTH * ordinal();
		}
	}

	public final class World {
		private final int index;
		private final int number;
		private final boolean favorite;
		private final int players;
		private final String activity;
		private final String type;
		private final boolean lootshare;
		private final int ping;

		public World(final int index, final int number, final boolean favorite, final int players, final String activity, final String type, final boolean lootshare, final int ping) {
			this.index = index;
			this.number = number;
			this.favorite = favorite;
			this.players = players;
			this.activity = activity;
			this.type = type;
			this.lootshare = lootshare;
			this.ping = ping;
		}

		public int componentIndex() {
			return index;
		}

		public int number() {
			return number;
		}

		public boolean favorite() {
			return favorite;
		}

		public int players() {
			return players;
		}

		public String activity() {
			return activity;
		}

		public String type() {
			return type;
		}

		public boolean lootshare() {
			return lootshare;
		}

		public int ping() {
			return ping;
		}

		public boolean members() {
			return type.equalsIgnoreCase("Members");
		}

		@Override
		public boolean equals(final Object o) {
			return o instanceof World && ((World) o).number == number;
		}
	}
}
