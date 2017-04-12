package org.powerbot.bot.rt4;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.MenuCommand;
import org.powerbot.script.PollingScript;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Npc;

public class RandomEvents extends PollingScript<ClientContext> {
	public RandomEvents() {
		priority.set(3);
	}

	private boolean isValid() {
        	return ctx.input.blocking() && !ctx.properties.getProperty("randomevents.disable", "").equals("true")
			&& !ctx.npcs.select().within(5d).action("Dismiss").select(new Filter<Npc>() {
            		@Override
            		public boolean accept(final Npc npc) {
                		return npc.interacting().equals(ctx.players.local());
            		}
        	}).isEmpty();
    	}

	@Override
	public void poll() {
		if (!isValid()) {
			if (threshold.contains(this)) {
				threshold.remove(this);
			}
			return;
		}
		if (!threshold.contains(this)) {
			threshold.add(this);
		}
		final Npc npc = ctx.npcs.poll();
		if(ctx.input.move(npc.nextPoint())) {
			for(MenuCommand a : ctx.menu.commands()) {
				if(!a.option.contains(" -> "))
					continue;

				ctx.widgets.component(Constants.CHAT_WIDGET, Constants.CHAT_VIEWPORT).click();
			}
		}
		if (npc.interact(false, "Dismiss")) {
			Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return !npc.valid();
				}
			}, 300, 12);
		}
	}
}
