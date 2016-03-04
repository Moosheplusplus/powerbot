package org.powerbot.script.rt6;

import java.util.*;
import java.util.concurrent.Callable;

import org.powerbot.script.GeEvent;
import org.powerbot.script.GeListener;
import org.powerbot.script.Condition;
import org.powerbot.script.MessageEvent;
import org.powerbot.script.MessageListener;
import org.powerbot.script.Random;
import org.powerbot.script.rt6.*;
import org.powerbot.script.rt6.Item;
import org.powerbot.script.rt6.Game.Crosshair;

public class GrandExchange extends ClientAccessor implements MessageListener {
	
	public static final String 	GE_CLERK 			= "Grand Exchange clerk";
	public static final int		WIDGET				= 105;
	public static final int		PROGRESS_OFFSET			= 17;
	public static final int		PROGRESS_BAR			= 3;
	public static final int		PROGRESS_TOTAL			= 4;
	public static final int		LABEL_COMPONENT			= 13;
	public static final int		SEARCH_COMPONENT		= 302;
	public static final int		QUERY_COMPONENT			= 53;
	public static final int		QUANTITY_INPUT_COMPONENT	= 290;
	public static final int		QUANTITY_COMPONENT		= 292;
	public static final int		PRICE_INPUT_COMPONENT		= 296;
	public static final int		PRICE_COMPONENT			= 298;
	public static final int		CONFIRM_COMPONENT		= 171;
	public static final int		SELL_TEXTURE			= 1168;
	public static final int		BUY_TEXTURE			= 1170;
	public static final int		BACKPACK_WIDGET			= 107;
	public static final int		BACKPACK_COMPONENT		= 7;
	public static final int		HUD_WIDGET			= 1477;
	public static final int		CLOSE_COMPONENT			= 483;
	public static final int		CLOSE_SUBCOMPONENT		= 1;
	public static final int		COLLECTION_WIDGET		= 651;
	public static final int		COLLECT_BACKPACK_COMPONENT	= 9;
	public static final int		COLLECT_BANK_COMPONENT		= 17;
	
	public GrandExchange(final ClientContext ctx) {
		super(ctx);
		ctx.dispatcher.add(this);
	}
	
	/**
	 * Opens the grand exchange widget. 
	 * 
	 * @return true if it has successfully opened the grand exchange.
	 */
	public boolean open() {
		if(opened())
			return true;
		
		ctx.npcs.select().name(GE_CLERK).nearest();
		if(ctx.npcs.isEmpty())
			return false;
		
		if(!ctx.npcs.peek().inViewport())
			ctx.camera.turnTo(ctx.npcs.peek());
		
		ctx.npcs.peek().interact(true, "Exchange",
				"Grand Exchange clerk");
		return Condition.wait(new Callable<Boolean>() {
			public Boolean call() {
				while(ctx.players.local().inMotion());
				return opened();
			}
		}, 300, 3);
	}
	
	/**
	 * 
	 * @return true if the grand exchange widget is open.
	 */
	public boolean opened() {
		return ctx.widgets.widget(WIDGET).valid();
	}
	
	/**
	 * Closes the grand exchange widget
	 * 
	 * @return true if the grand exchange is no longer opened.
	 */
	public boolean close() {
		if(!opened())
			return true;
		final Component close = ctx.widgets.component(HUD_WIDGET,
				CLOSE_COMPONENT).component(CLOSE_SUBCOMPONENT);
		return Condition.wait(new Callable<Boolean>() {
			public Boolean call() {
				if(ctx.game.crosshair() == Crosshair.NONE && close.valid()
						&& close.visible() && !close.click())
					return false;
				return !opened();
			}
		}, 100, 20);
	}
	
	/**
	 * Buys an item from the grand exchange.
	 * 
	 * @param item The item id to search for
	 * @param amount The amount of the item to buy
	 * @param price The price to buy each item at
	 * @return true if the item has been successfully listed
	 */
	public boolean buy(final int item, final int amount, final int price) {
		if(!opened())
			return false;

		List<Component> avail = getComponentsByTexture(WIDGET, BUY_TEXTURE);
		if(avail.isEmpty())
			return false;
		avail.get(Random.nextInt(0, avail.size())).click();
		if(!Condition.wait(new Callable<Boolean>() {
			public Boolean call() {
				return ctx.widgets.component(WIDGET, SEARCH_COMPONENT)
						.visible();
			}
		}, 100, 25))
			return false;
		
		final CacheItemConfig cic = CacheItemConfig.load(item);
		if (!cic.valid())
			return false;

		ctx.input.send(cic.name.toLowerCase());
		
		if(!Condition.wait(new Callable<Boolean>() {
			public Boolean call() {
				return ctx.widgets.component(WIDGET, QUERY_COMPONENT)
						.childrenCount() > 0;
			}
		}, 100, 30))
			return false;
		
		Component query = ctx.widgets.component(WIDGET, QUERY_COMPONENT);
		if(!query.component(0).text().isEmpty())
			return false;
		
		Component[] results = query.components();

		for (int i = 0, j = results.length; i < j; i++)
			if ((results[i].itemId() == item) && results[i - 1].click())
				break;

		if (!matchesTitle(cic.name))
			return false;
		
		return setQuantity(amount) && setPrice(price) && confirm();
	}
	
	/**
	 * Sells the specified item to the grand exchange.
	 * 
	 * @param item The inventory item to be sold
	 * @param amount The stack size to sell in the grand exchange
	 * @param price The price to list each item at
	 * @return true if it has been successfully listed in the grand exchange
	 */
	public boolean sell(final Item item, final int amount, final int price) {
		if(!opened() || item.id() == -1)
			return false;
		
		Component i = null;
		if(!Condition.wait(new Callable<Boolean>() {
			public Boolean call() {
				return getBackpack().childrenCount() > 0;
			}
		}, 100, 20))
			return false;
		for(Component c : getBackpack().components()) {
			if(c.itemId() == item.id()) {
				i = c;
				break;
			}
		}
		if(i == null || !i.click() || !matchesTitle(item.name()))
			return false;
		
		return setQuantity(amount) && setPrice(price) && confirm();
	}
	
	/**
	 * The amount of available slots within the Grand Exchange. Any
	 * items occupied within a slot will not be counted, or if the slot
	 * is disabled due to lack of membership.
	 * 
	 * @return The amount of vacant slots.
	 */
	public int getAvailableSlots() {
		return getComponentsByTexture(WIDGET, SELL_TEXTURE).size();
	}
	
	/**
	 * Checks whether or not the specified slot is vacant for use.
	 * 
	 * @param slot The slot to check is vacant
	 * @return true if the slot is vacant
	 */
	public boolean isVacant(final int slot) {
		for(Component c : getComponentsByTexture(WIDGET, SELL_TEXTURE))
			if(c.index() == slot)
				return true;
		return false;
	}
	
	/**
	 * Gets the progress from the selected slot.
	 * 
	 * @param slot The slot to check the progress of
	 * @return Will return the progress as a double (0.0 to 1.0). If the
	 * slot is vacant or invalid, it will return -1.0.
	 */
	public double getProgress(final int slot) {
		Component parent = ctx.widgets.component(WIDGET,
				PROGRESS_OFFSET + slot);
		Component progress = parent.component(PROGRESS_TOTAL);
		Component bar = parent.component(PROGRESS_BAR);
		if(!progress.visible() || !bar.visible())
			return -1.0;
		return ((double) progress.width()) / bar.width();
	}
	
	/**
	 * Collects all items within the grand exchange that are available for
	 * collection. All of the items will be deposited into the player's
	 * inventory.
	 * 
	 * @return true if it has successfully collected items to the inventory.
	 */
	public boolean collectToInventory() {
		Component c = ctx.widgets.component(COLLECTION_WIDGET,
				COLLECT_BACKPACK_COMPONENT);
		return opened() && c.valid() && c.visible() && c.click();
	}
	
	/**
	 * Collects all items within the grand exchange that are available for
	 * collection. All of the items will be deposited into the bank.
	 * 
	 * @return true if it has successfully collected items to the bank.
	 */
	public boolean collectToBank() {
		Component c = ctx.widgets.component(COLLECTION_WIDGET,
				COLLECT_BANK_COMPONENT);
		return opened() && c.valid() && c.visible() && c.click();
	}
	
	private boolean setPrice(final int price) {
		return set(""+price, PRICE_COMPONENT, PRICE_INPUT_COMPONENT);
	}
	
	private boolean setQuantity(final int quantity) {
		return set(""+quantity, QUANTITY_COMPONENT, QUANTITY_INPUT_COMPONENT);
	}
		
	private boolean set(final String value,
			final int component, final int input) {
		Component comp = ctx.widgets.component(WIDGET, component);
		if(comp.text().replaceAll("[^\\d]", "").equals(value))
			return true;
		if(!comp.visible() || !comp.click() || !Condition.wait(
			new Callable<Boolean>() {
				public Boolean call() {
					return ctx.widgets.component(WIDGET, input)
							.visible();
				}
			}, 100, 25))
				return false;
		return ctx.input.sendln(value);
	}
	
	private boolean matchesTitle(final String title) {
		return Condition.wait(new Callable<Boolean>() {
			public Boolean call() {
				Component c = ctx.widgets.component(WIDGET, LABEL_COMPONENT);
				return c.valid() && c.visible() && 
						c.text().equalsIgnoreCase(title);
			}
		}, 100, 25);
	}
	
	private boolean confirm() {
		Component confirm = ctx.widgets.component(WIDGET, CONFIRM_COMPONENT);
		return confirm.valid() && confirm.visible() && confirm.click();
	}
	
	private List<Component> getComponentsByTexture(final int widget,
			final int texture) {
		List<Component> available = new ArrayList<Component>();
		for(Component c : ctx.widgets.widget(widget).components())
			if(c.textureId() == texture && c.visible())
				available.add(c);
		return available;
	}

	private Component getBackpack() {
		return ctx.widgets.component(BACKPACK_WIDGET,
				BACKPACK_COMPONENT);
	}

	@Override
	public void messaged(final MessageEvent e) {
		if(e.source().isEmpty() || opened() || !e.text()
				.contains("Grand Exchange"))
			return;
		String[] tokens = e.text().split(" ");
		double progress = 1.0;
		if(tokens[2] != "Finished") {
			String[] prog = tokens[3].split("/");
			progress = Double.valueOf(prog[0]) / Double.valueOf(prog[1]);
		}
		GeEvent.Type type = tokens[2] == "Bought" || tokens[3] == "buying" 
				? GeEvent.Type.BUY : GeEvent.Type.SELL;
		String name = e.text().substring(e.text().indexOf('x') + 2);
		dispatch(new GeEvent(name, progress), type);
	}
	
	private void dispatch(final GeEvent event, final GeEvent.Type type) {
		for(EventListener l : ctx.dispatcher) {
			if(!(l instanceof GeListener))
				continue;
			GeListener listener = (GeListener) l;
			switch(type) {
				case BUY:
					listener.onBuy(event);
					break;
				case SELL:
					listener.onSell(event);
					break;
			}
		}
	}
}
