package org.powerbot.script.rt4;

import java.util.List;

import org.powerbot.bot.ScriptController;
import org.powerbot.bot.rt4.BankPin;
import org.powerbot.bot.rt4.Bot;
import org.powerbot.bot.rt4.Login;
import org.powerbot.bot.rt4.RandomEvents;
import org.powerbot.bot.rt4.client.Client;
import org.powerbot.script.Script;

/**
 * ClientContext
 * A utility class with references to all major points of the API.
 */
public class ClientContext extends org.powerbot.script.ClientContext<Client> {
	public final Bank bank;
	public final Camera camera;
	public final Chat chat;
	public final Combat combat;
	public final DepositBox depositBox;
	public final Equipment equipment;
	public final Game game;
	public final GrandExchange grandExchange;
	public final GroundItems groundItems;
	public final Inventory inventory;
	public final Magic magic;
	public final Menu menu;
	public final Movement movement;
	public final Npcs npcs;
	public final Objects objects;
	public final Players players;
	public final Prayer prayer;
	public final Skills skills;
	public final Varpbits varpbits;
	public final Widgets widgets;

	private ClientContext(final Bot bot) {
		super(bot);

		if (controller instanceof ScriptController) {
			@SuppressWarnings("unchecked")
			final List<Class<? extends Script>> d = ((ScriptController<ClientContext>) controller).daemons;
			d.add(Login.class);
			d.add(BankPin.class);
			d.add(RandomEvents.class);
		}

		bank = new Bank(this);
		camera = new Camera(this);
		chat = new Chat(this);
		combat = new Combat(this);
		depositBox = new DepositBox(this);
		equipment = new Equipment(this);
		game = new Game(this);
		grandExchange = new GrandExchange(this);
		groundItems = new GroundItems(this);
		inventory = new Inventory(this);
		magic = new Magic(this);
		menu = new Menu(this);
		movement = new Movement(this);
		npcs = new Npcs(this);
		objects = new Objects(this);
		players = new Players(this);
		prayer = new Prayer(this);
		skills = new Skills(this);
		varpbits = new Varpbits(this);
		widgets = new Widgets(this);
	}

	/**
	 * Creates a new chained context.
	 *
	 * @param ctx the parent context
	 */
	public ClientContext(final ClientContext ctx) {
		super(ctx);

		bank = ctx.bank;
		camera = ctx.camera;
		chat = ctx.chat;
		combat = ctx.combat;
		depositBox = ctx.depositBox;
		equipment = ctx.equipment;
		game = ctx.game;
		grandExchange = ctx.grandExchange;
		groundItems = ctx.groundItems;
		inventory = ctx.inventory;
		magic = ctx.magic;
		menu = ctx.menu;
		movement = ctx.movement;
		npcs = ctx.npcs;
		objects = ctx.objects;
		players = ctx.players;
		prayer = ctx.prayer;
		skills = ctx.skills;
		varpbits = ctx.varpbits;
		widgets = ctx.widgets;
	}

	/**
	 * Creates a new context for the given {@link org.powerbot.bot.rt4.Bot}.
	 *
	 * @param bot the bot to associate with
	 * @return a new context
	 */
	public static ClientContext newContext(final Bot bot) {
		return new ClientContext(bot);
	}
}
