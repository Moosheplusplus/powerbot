package org.powerbot.bot.rt4;

import org.powerbot.script.Filter;
import org.powerbot.script.PollingScript;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Npc;

public class RandomEvents extends PollingScript<ClientContext> {
	
	private enum State {
		IDLE,
		TALKING
	}
	
	private State state = State.IDLE;
	
	public RandomEvents() {
		priority.set(3);
	}

	private boolean isValid() {
		return state != State.IDLE || !ctx.npcs.select().within(5d).action("Dismiss").select(new Filter<Npc>() {
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
		switch(state) {
			case IDLE:
				boolean dismiss = true;
				for(Npc n : ctx.npcs) {
					if(n.name().equals("Genie") ||
							n.name().equals("Drunken dwarf")) {
						dismiss = false;
						break;
					}
				}
				ctx.npcs.peek().interact(!dismiss,
						(dismiss ? "Dismiss" : "Talk-to"),
						ctx.npcs.peek().name());
				if(!dismiss)
					state = State.TALKING;
				return;
			
			case TALKING:
				if(!ctx.chat.chatting()) {
					state = State.IDLE;
					return;
				}
				ctx.chat.clickContinue(true);
				return;
		}
	}
}