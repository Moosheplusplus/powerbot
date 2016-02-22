import org.powerbot.script;

import java.util.EventListener;

public interface GeListener extends EventListener {

	/**
	 * Invoked when a Grand Exchange message is sent to the client.
	 * 
	 * @param event The {@link GeEvent}.
	 */
	public void onBuy(GeEvent event);

	/**
	 * Invoked when a Grand Exchange message is sent to the client.
	 * 
	 * @param event The {@link GeEvent}.
	 */
	public void onSell(GeEvent event);
}
