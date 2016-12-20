package org.powerbot.script.rt6;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Locatable;
import org.powerbot.script.MenuCommand;
import org.powerbot.script.Random;
import org.powerbot.script.StringUtils;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;

/**
 * Bank
 * Utilities pertaining to the bank.
 */
public class Bank extends ItemQuery<Item> implements Viewable {
	private static final Filter<Interactive> UNREACHABLE_FILTER = new Filter<Interactive>() {
		@Override
		public boolean accept(final Interactive interactive) {
			if (interactive instanceof Locatable) {
				final Tile tile = ((Locatable) interactive).tile();
				for (final Tile bad : Constants.BANK_UNREACHABLES) {
					if (tile.equals(bad)) {
						return false;
					}
				}
			}
			return true;
		}
	};

	public Bank(final ClientContext factory) {
		super(factory);
	}

	private Interactive getBank() {
		final Player p = ctx.players.local();
		final Tile t = p.tile();

		ctx.npcs.select().id(Constants.BANK_NPCS).viewable().select(UNREACHABLE_FILTER).nearest();
		ctx.objects.select().id(Constants.BANK_BOOTHS, Constants.BANK_COUNTERS, Constants.BANK_CHESTS).viewable().select(UNREACHABLE_FILTER).nearest();
		if (!ctx.properties.getProperty("bank.antipattern", "").equals("disable")) {
			final Npc npc = ctx.npcs.poll();
			final GameObject object = ctx.objects.poll();
			return t.distanceTo(npc) < t.distanceTo(object) ? npc : object;
		}
		final double dist = Math.min(t.distanceTo(ctx.npcs.peek()), t.distanceTo(ctx.objects.peek()));
		final double d2 = Math.min(2d, Math.max(0d, dist - 1d));
		final List<Interactive> interactives = new ArrayList<Interactive>();
		ctx.npcs.within(dist + Random.nextInt(2, 5)).within(ctx.npcs.peek(), d2);
		ctx.objects.within(dist + Random.nextInt(2, 5)).within(ctx.objects.peek(), d2);
		ctx.npcs.addTo(interactives);
		ctx.objects.addTo(interactives);
		final int len = interactives.size();
		return len == 0 ? ctx.npcs.nil() : interactives.get(Random.nextInt(0, len));
	}

	/**
	 * Returns the absolute nearest bank for walking purposes. Do not use this to open the bank.
	 *
	 * @return the {@link Locatable} of the nearest bank or {@link Tile#NIL}
	 * @see #open()
	 */
	public Locatable nearest() {
		Locatable nearest = ctx.npcs.select().select(UNREACHABLE_FILTER).id(Constants.BANK_NPCS).nearest().poll();

		final Tile loc = ctx.players.local().tile();
		for (final GameObject object : ctx.objects.select().select(UNREACHABLE_FILTER).
				id(Constants.BANK_BOOTHS, Constants.BANK_COUNTERS, Constants.BANK_CHESTS).nearest().limit(1)) {
			if (loc.distanceTo(object) < loc.distanceTo(nearest)) {
				nearest = object;
			}
		}
		if (nearest.tile() != Tile.NIL) {
			return nearest;
		}
		return Tile.NIL;
	}

	/**
	 * Determines if a bank is present in the loaded region.
	 *
	 * @return <tt>true</tt> if a bank is present; otherwise <tt>false</tt>
	 */
	public boolean present() {
		return nearest() != Tile.NIL;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean inViewport() {
		return getBank().valid();
	}

	/**
	 * Determines if the bank is open.
	 *
	 * @return <tt>true</tt> is the bank is open; otherwise <tt>false</tt>
	 */
	public boolean opened() {
		return ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_ITEMS).valid();
	}

	/**
	 * Opens a random in-view bank.
	 * Do not continue execution within the current poll after this method so BankPin may activate.
	 *
	 * @return <tt>true</tt> if the bank was opened; otherwise <tt>false</tt>
	 */
	public boolean open() {
		if (opened()) {
			return true;
		}
		final Interactive interactive = getBank();
		final int id;
		if (interactive.valid()) {
			if (interactive instanceof Npc) {
				id = ((Npc) interactive).id();
			} else if (interactive instanceof GameObject) {
				id = ((GameObject) interactive).id();
			} else {
				id = -1;
			}
		} else {
			id = -1;
		}
		if (id == -1) {
			return false;
		}
		int index = -1;
		final int[][] ids = {Constants.BANK_NPCS, Constants.BANK_BOOTHS, Constants.BANK_CHESTS, Constants.BANK_COUNTERS};
		for (int i = 0; i < ids.length; i++) {
			Arrays.sort(ids[i]);
			if (Arrays.binarySearch(ids[i], id) >= 0) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		}
		final Filter<MenuCommand> f = new Filter<MenuCommand>() {
			@Override
			public boolean accept(final MenuCommand entry) {
				final String s = entry.action;
				return s.equalsIgnoreCase("Use") || s.equalsIgnoreCase("Open") || s.equalsIgnoreCase("Bank");
			}
		};
		final String[] actions = {"Bank", "Bank", null, "Bank"};
		final String[] options = {null, "Bank booth", null, "Counter"};
		if (actions[index] == null) {
			if (interactive.hover()) {
				Condition.wait(new Condition.Check() {
					@Override
					public boolean poll() {
						return ctx.menu.indexOf(f) != -1;
					}
				}, 100, 3);
			}
		}
		final String action = actions[index];
		if (action != null ? interactive.interact(actions[index], options[index]) :
				interactive.interact(f)) {
			do {
				Condition.wait(new Condition.Check() {
					@Override
					public boolean poll() {
						return ctx.widgets.widget(13).component(0).visible() || opened();
					}
				}, 150, 15);
			} while (ctx.players.local().inMotion());

			Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return ctx.widgets.widget(13).component(0).visible() || opened();
				}
			}, 100, 15);
		}
		return opened();
	}

	/**
	 * Closes the bank by the 'X'.
	 *
	 * @return <tt>true</tt> if the bank was closed; otherwise <tt>false</tt>
	 */
	public boolean close() {
		return !opened() || ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_CLOSE).click("Close") && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return !opened();
			}
		}, 150);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Item> get() {
		final Component c = ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_ITEMS);
		if (c == null || !c.valid()) {
			return new ArrayList<Item>();
		}
		final Component[] components = c.components();
		final List<Item> items = new ArrayList<Item>(components.length);
		for (final Component i : components) {
			final int it = i.itemId();
			if (it != -1) {
				items.add(new Item(ctx, it, i.itemStackSize(), i));
			}
		}
		return items;
	}

	/**
	 * Grabs the {@link Item} at the provided index.
	 *
	 * @param index the index of the item to grab
	 * @return the {@link Item} at the specified index; or {@link org.powerbot.script.rt6.Bank#nil()}
	 */
	public Item itemAt(final int index) {
		final Component i = ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_ITEMS).component(index);
		if (i.itemId() != -1) {
			return new Item(ctx, i);
		}
		return nil();
	}

	/**
	 * Returns the first index of the provided item id.
	 *
	 * @param id the id of the item
	 * @return the index of the item; otherwise {@code -1}
	 */
	public int indexOf(final int id) {
		final Component items = ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_ITEMS);
		if (items == null || !items.valid()) {
			return -1;
		}
		final Component[] comps = items.components();
		for (int i = 0; i < comps.length; i++) {
			if (comps[i].itemId() == id) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @return the index of the current bank tab
	 */
	public int currentTab() {
		return ((ctx.varpbits.varpbit(Constants.BANK_STATE) >>> 24) - 136) / 8;
	}

	/**
	 * Changes the current tab to the provided index.
	 *
	 * @param index the index desired
	 * @return <tt>true</tt> if the tab was successfully changed; otherwise <tt>false</tt>
	 */
	public boolean currentTab(final int index) {
		final Component c = ctx.widgets.component(Constants.BANK_WIDGET, 150 + (index * 8));
		return c.click() && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return currentTab() == index;
			}
		}, 100, 8);
	}

	/**
	 * Returns the item in the specified tab if it exists.
	 *
	 * @param index the tab index
	 * @return the {@link Item} displayed in the tab; otherwise {@link org.powerbot.script.rt6.Bank#nil()}
	 */
	public Item tabItem(final int index) {
		final Component c = ctx.widgets.component(Constants.BANK_WIDGET, 150 + (index * 8));
		if (c != null && c.valid()) {
			return new Item(ctx, c);
		}
		return nil();
	}

	/**
	 * Withdraws an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrew, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean withdraw(final int id, final Amount amount) {
		return withdraw(id, amount.getValue());
	}

	/**
	 * Withdraws an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrawn, does not determine if amount was matched; otherwise, <tt>false</tt>
	 */
	public boolean withdraw(final int id, final int amount) {
		return withdraw(select().id(id).poll(), amount);
	}

	/**
	 * Withdraws an item with the provided item and amount.
	 *
	 * @param item   the item instance
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrawn, dos not determine if amount was matched; otherwise, <tt>false</tt>
	 */
	public boolean withdraw(final Item item, final int amount) {
		return withdraw0(item, amount, false);
	}

	/**
	 * Withdraws an item with the provided id and amount to BoB.
	 * Does not guarantee return value means success.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrawn, does not determine if amount was matched; otherwise, <tt>false</tt>
	 */
	public boolean withdrawToBoB(final int id, final int amount) {
		return withdraw(select().id(id).poll(), amount);
	}

	/**
	 * Withdraws an item with the provided item and amount to BoB.
	 * Does not guarantee return value means success.
	 *
	 * @param item
	 * @param amount the amount to withdraw
	 * @return <tt>true</tt> if the item was withdrawn, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean withdrawToBoB(final Item item, final int amount) {
		return withdraw0(item, amount, true);
	}

	boolean withdraw0(final Item item, final int amount, final boolean bob) {//TODO: anti pattern
		final Component component = ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_ITEMS);
		if (!component.valid() || !item.valid()) {
			return false;
		}
		final Component c = item.component();
		if (c.relativePoint().y == 0) {
			if (!currentTab(0) && Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return c.relativePoint().y != 0;
				}
			}, 100, 10)) {
				return false;
			}
		}
		final Rectangle vr = component.viewportRect();
		if (!vr.contains(c.viewportRect()) && !ctx.widgets.scroll(c, ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_SCROLLBAR),
				vr.contains(ctx.input.getLocation()))) {
			return false;
		}

		String action = "Withdraw-" + amount;
		//noinspection StatementWithEmptyBody
		if (amount == 1) {
		} else if (bob) {
			action = "fall";
		} else if (amount == 0 ||
				(item.stackSize() <= amount && amount != 5 && amount != 10)) {
			action = "Withdraw-All";
		} else if (amount == -1 || amount == (item.stackSize() - 1)) {
			action = "Withdraw-All but one";
		}
		final int inv = ctx.backpack.moneyPouchCount() + ctx.backpack.select().count(true);
		if (amount != 0 && !containsAction(c, action)) {
			if (c.interact(bob ? "Withdraw-X to Bob" : "Withdraw-X") && Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return isInputWidgetOpen();
				}
			})) {
				Condition.sleep();
				ctx.input.sendln(amount + "");
			}
		} else {
			if (!c.interact(action)) {
				return false;
			}
		}
		return bob || Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return ctx.backpack.moneyPouchCount() + ctx.backpack.select().count(true) != inv;
			}
		});
	}

	/**
	 * Deposits an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to deposit
	 * @return <tt>true</tt> if the item was deposited, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean deposit(final int id, final Amount amount) {
		return deposit(id, amount.getValue());
	}

	/**
	 * Deposits an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to deposit
	 * @return <tt>true</tt> if the item was deposited, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean deposit(final int id, final int amount) {
		if (!opened() || amount < 0) {
			return false;
		}
		final Item item = ctx.backpack.select().id(id).shuffle().poll();
		if (!item.valid()) {
			return false;
		}
		String action = "Deposit-" + amount;
		final int count = ctx.backpack.select().id(id).count(true);
		if (count == 1) {
			action = "Deposit";
		} else if (amount == 0 || count <= amount) {
			action = "Deposit-All";
		}
		final int cache = ctx.backpack.select().count(true);
		final Component component = item.component();
		if (amount != 0 && !containsAction(component, action)) {
			if (component.interact("Deposit-X") && Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return isInputWidgetOpen();
				}
			})) {
				Condition.sleep();
				ctx.input.sendln(amount + "");
			} else {
				return false;
			}
		} else {
			if (!component.interact(action)) {
				return false;
			}
		}
		return Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return cache != ctx.backpack.select().count(true);
			}
		});
	}

	/**
	 * Deposits the players inventory excluding the specified ids.
	 *
	 * @param ids the ids of the items to ignore when depositing
	 * @return @return <tt>true</tt> if the items were deposited, determines if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean depositAllExcept(final int... ids) {
		return depositAllExcept(new Filter<Item>() {
			@Override
			public boolean accept(final Item item) {
				final int id = item.id();
				for (final int i : ids) {
					if (id == i) {
						return true;
					}
				}
				return false;
			}
		});
	}

	/**
	 * Deposits the players inventory excluding the specified item names.
	 *
	 * @param names the names of the items to ignore when depositing
	 * @return <tt>true</tt> if the items were deposited, determines if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean depositAllExcept(final String... names) {
		return depositAllExcept(new Filter<Item>() {
			@Override
			public boolean accept(final Item item) {
				for (final String s : names) {
					if (s == null) {
						continue;
					}
					if (item.name().toLowerCase().contains(s.toLowerCase())) {
						return true;
					}
				}
				return false;
			}
		});
	}

	/**
	 * Deposits the players inventory excluding the items that match the provided filter.
	 *
	 * @param filter the filter of the items to ignore when depositing
	 * @return <tt>true</tt> if the items were deposited, determines if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean depositAllExcept(final Filter<Item> filter) {
		if (ctx.backpack.select().select(filter).count() == 0) {
			return depositInventory();
		}
		for (final Item i : ctx.backpack.select().shuffle()) {
			if (filter.accept(i)) {
				continue;
			}
			deposit(i.id(), Amount.ALL);
		}

		return ctx.backpack.select().count() == ctx.backpack.select(filter).count();
	}

	/**
	 * Deposits the inventory via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked, not if the inventory is empty; otherwise <tt>false</tt>
	 */
	public boolean depositInventory() {
		return ctx.backpack.get().isEmpty() || ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_DEPOSIT_INVENTORY).click();
	}

	/**
	 * Deposits equipment via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked; otherwise <tt>false</tt>
	 */
	public boolean depositEquipment() {
		return ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_DEPOSIT_EQUIPMENT).click();
	}

	/**
	 * Deposits familiar inventory via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked; otherwise <tt>false</tt>
	 */
	public boolean depositFamiliar() {
		return ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_DEPOSIT_FAMILIAR).click();
	}

	/**
	 * Deposits the money pouch via the button.
	 *
	 * @return <tt>true</tt> if the button was clicked; otherwise <tt>false</tt>
	 */
	public boolean depositMoneyPouch() {
		return ctx.backpack.moneyPouchCount() == 0 || ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_DEPOSIT_MONEY).click();
	}

	public boolean openPresetSetup() {
		return ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_PRESET).click();
	}

	public boolean presetGear1() {
		return presetGear(1);
	}

	public boolean presetGear2() {
		return presetGear(2);
	}

	public boolean presetGear(final int set) {
		return presetGear(set, false);
	}

	public boolean presetGear(final int set, final boolean key) {
		if (!opened()) {
			return false;
		}
		if (key && !isInputWidgetOpen()) {
			ctx.input.send(Integer.toString(set));
			return true;
		}
		switch (set) {
		case 1: {
			return ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_LOAD1).click();
		}
		case 2: {
			return ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_LOAD2).click();
		}
		default:
			return false;
		}
	}

	/**
	 * Changes the withdraw mode.
	 *
	 * @param noted <tt>true</tt> for noted items; otherwise <tt>false</tt>
	 * @return <tt>true</tt> if the withdraw mode was successfully changed; otherwise <tt>false</tt>
	 */
	public boolean withdrawMode(final boolean noted) {
		return withdrawMode() == noted || ctx.widgets.component(Constants.BANK_WIDGET, Constants.BANK_WITHDRAW_MODE).click() && Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return withdrawMode() == noted;
			}
		});
	}

	/**
	 * Determines if the withdraw mode is noted mode.
	 *
	 * @return <tt>true</tt> if withdrawing as notes; otherwise <tt>false</tt>
	 */
	public boolean withdrawMode() {
		return ctx.varpbits.varpbit(Constants.BANK_WITHDRAW_MODE_STATE) == 0x1;
	}

	private boolean containsAction(final Component c, final String action) {
		final String[] actions = c.actions();
		for (final String a : actions) {
			if (a != null && StringUtils.stripHtml(a).trim().equalsIgnoreCase(action)) {
				return true;
			}
		}
		return false;
	}

	private boolean isInputWidgetOpen() {
		return ctx.widgets.component(1469, 2).visible();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Item nil() {
		return new Item(ctx, -1, -1, null);
	}

	/**
	 * An enumeration providing standard bank amount options.
	 */
	public enum Amount {
		ONE(1), FIVE(5), TEN(10), ALL_BUT_ONE(-1), ALL(0);

		private final int value;

		Amount(final int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}
}
