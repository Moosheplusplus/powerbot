package org.powerbot.bot.rt4;

import org.powerbot.misc.GameAccounts;
import org.powerbot.script.Condition;
import org.powerbot.script.PollingScript;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;
import org.powerbot.script.rt4.Constants;

public class BankPin extends PollingScript<ClientContext> {
	public BankPin() {
		priority.set(2);
	}

	private int count = 0;

	@Override
	public void poll() {
		if (!ctx.widgets.widget(Constants.BANKPIN_WIDGET).valid()) {
			if (threshold.contains(this)) {
				threshold.remove(this);
			}
			return;
		}
		if (!threshold.contains(this)) {
			threshold.add(this);
		}

		final String pin = getPin();
		if (pin == null) {
			ctx.controller.stop();
			return;
		}

		for (final Component c : ctx.widgets.widget(Constants.BANKPIN_WIDGET).components()) {
			if (c.textColor() != 0 || c.width() != 64 || c.height() != 64 || c.componentCount() != 2 || !c.visible()) {
				continue;
			}
			final Component child = c.component(1);
			if (!child.visible()) {
				continue;
			}
			//TODO: re-evaluate this to get rid of count; or fail out
			final String text = child.text();
			if (text.equals("" + pin.charAt(count % 4))) {
				if (c.click()) {
					count++;
					Condition.wait(new Condition.Check() {
						public boolean poll() {
							return !child.text().equals(text);
						}
					}, 100, 20);
				}
			}
		}
	}

	private String getPin() {
		final GameAccounts.Account account = GameAccounts.getInstance().get(ctx.properties.getProperty(Login.LOGIN_USER_PROPERTY, ""));
		if (account != null) {
			final String pin = account.getPIN();
			if (pin != null && pin.length() == 4) {
				return pin;
			}
		}
		return null;
	}
}
