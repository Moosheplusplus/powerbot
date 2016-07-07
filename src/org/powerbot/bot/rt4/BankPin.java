package org.powerbot.bot.rt4;

import org.powerbot.misc.GameAccounts;
import java.util.ArrayList;
import java.util.List;

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
		final String pin = pin();
		if(!ctx.widgets.widget(Constants.BANKPIN_WIDGET).valid() || pin.isEmpty()) {
			if(threshold.contains(this))
				threshold.remove(this);
			return;
		}
		for(final Component c : ctx.widgets.widget(Constants.BANKPIN_WIDGET)
				.components()) {
			final Component child = c.component(1);
			if(child == null || !child.visible())
				continue;
			final String text =  child.text();
			if(c.visible() && c.textColor() == 0 && c.width() == 64 &&
					c.height() == 64 && c.componentCount() == 2 &&
					text.equals(pin.charAt(count % 4)+"")) {
				if(child.click()) {
					count++;
					Condition.wait(new Condition.Check() {
						public boolean poll() {
							return child.text() != text;
						}
					}, 100, 20);
				}
			}
		}
	}
	
	private String pin() {
		final GameAccounts.Account acc = GameAccounts.getInstance().get(
				ctx.properties.getProperty(Login.LOGIN_USER_PROPERTY, ""));
		return (acc == null || acc.getPIN() == null ||
				acc.getPIN().length() != 4) ? "" : acc.getPIN();
	}
}
