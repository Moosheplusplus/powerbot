package org.powerbot.script.rt4;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;

/**
 * Combat
 * A utility class with methods for parsing widget and varpbit values.
 */
public class Combat extends ClientAccessor {
	public Combat(final ClientContext ctx) {
		super(ctx);
	}

	public int health() {
		return ctx.skills.level(Constants.SKILLS_HITPOINTS);
	}

	public int specialPercentage() {
		return ctx.varpbits.varpbit(300) / 10;
	}

	public boolean specialAttack() {
		return ctx.varpbits.varpbit(301) == 1;
	}

	public boolean specialAttack(final boolean select) {
		if (specialAttack() == select) {
			return true;
		}

		Component c = null;
		for (final Component comp : ctx.widgets.widget(593).components()) {
			if (comp.text().contains("Special attack:")) {
				c = comp;
				break;
			}
		}

		final int current = specialPercentage();
		return c != null && ctx.game.tab(Game.Tab.ATTACK) && c.visible() && c.click() && Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return specialAttack() == select || specialPercentage() != current;
			}
		}, 300, 6);
	}
}
