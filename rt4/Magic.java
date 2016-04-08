package org.powerbot.script.rt4;

import org.powerbot.script.Condition;

/**
 * Magic
 */
public class Magic extends ClientAccessor {
	public Magic(final ClientContext ctx) {
		super(ctx);
	}

	public Book book() {
		for (final Book b : Book.values()) {
			if (ctx.widgets.widget(b.widget).component(0).valid()) {
				return b;
			}
		}
		return Book.NIL;
	}

	public Spell spell() {
		final Book book = book();
		for (final Spell spell : Spell.values()) {
			if (spell.book != book) {
				continue;
			}
			if (ctx.widgets.component(spell.book.widget, spell.component()).borderThickness() == 2) {
				return spell;
			}
		}
		return Spell.NIL;
	}

	public boolean cast(final Spell spell) {
		if (!ctx.game.tab(Game.Tab.MAGIC)) {
			return false;
		}
		final Spell s = spell();
		if (s != Spell.NIL) {
			if (!ctx.widgets.component(spell.book.widget, s.component()).click("Cast") || !Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return spell() == Spell.NIL;
				}
			}, 10, 30)) {
				return false;
			}
		}
		return ctx.widgets.component(spell.book.widget, spell.component()).click("Cast");
	}

	public enum Spell {
		NIL(Book.NIL, -1, -1),//Selected spell 192,x=bt2

		HOME_TELEPORT(Book.MODERN, 0, 0),
		WIND_STRIKE(Book.MODERN, 1, 1),
		CONFUSE(Book.MODERN, 3, 2),
		ENCHANT_CROSSBOW_BOLT_OPAL(Book.MODERN, 4, 3),
		WATER_STRIKE(Book.MODERN, 5, 4),
		ENCHANT_LEVEL_1_JEWELLERY(Book.MODERN, 7, 5),
		ENCHANT_CROSSBOW_BOLT_SAPPHIRE(Book.MODERN, 7, 3),
		EARTH_STRIKE(Book.MODERN, 9, 6),
		WEAKEN(Book.MODERN, 11, 7),
		FIRE_STRIKE(Book.MODERN, 13, 8),
		ENCHANT_CROSSBOW_BOLT_JADE(Book.MODERN, 14, 3),
		BONES_TO_BANANAS(Book.MODERN, 15, 9),
		WIND_BOLT(Book.MODERN, 17, 10),
		CURSE(Book.MODERN, 19, 11),
		BIND(Book.MODERN, 20, 12),
		LOW_LEVEL_ALCHEMY(Book.MODERN, 21, 13),
		WATER_BOLT(Book.MODERN, 23, 14),
		ENCHANT_CROSSBOW_BOLT_PEARL(Book.MODERN, 24, 3),
		VARROCK_TELEPORT(Book.MODERN, 25, 15),
		ENCHANT_LEVEL_2_JEWELLERY(Book.MODERN, 27, 16),
		ENCHANT_CROSSBOW_BOLT_EMERALD(Book.MODERN, 27, 3),
		EARTH_BOLT(Book.MODERN, 29, 17),
		ENCHANT_CROSSBOW_BOLT_RED_TOPAZ(Book.MODERN, 29, 3),
		LUMBRIDGE_TELEPORT(Book.MODERN, 31, 18),
		TELEKINETIC_GRAB(Book.MODERN, 33, 19),
		FIRE_BOLT(Book.MODERN, 25, 20),
		FALADOR_TELEPORT(Book.MODERN, 37, 21),
		CRUMBLE_UNDEAD(Book.MODERN, 39, 22),
		TELEPORT_TO_HOUSE(Book.MODERN, 40, 23),
		WIND_BLAST(Book.MODERN, 41, 24),
		SUPERHEAT_ITEM(Book.MODERN, 43, 25),
		CAMELOT_TELEPORT(Book.MODERN, 45, 26),
		WATER_BLAST(Book.MODERN, 47, 27),
		ENCHANT_LEVEL_3_JEWELLERY(Book.MODERN, 49, 28),
		ENCHANT_CROSSBOW_BOLT_RUBY(Book.MODERN, 49, 3),
		IBAN_BLAST(Book.MODERN, 50, 29),
		SNARE(Book.MODERN, 50, 30),
		MAGIC_DART(Book.MODERN, 50, 31),
		ARDOUGNE_TELEPORT(Book.MODERN, 51, 32),
		EARTH_BLAST(Book.MODERN, 51, 33),
		HIGH_ALCHEMY(Book.MODERN, 55, 34),
		CHARGE_WATER_ORB(Book.MODERN, 56, 35),
		ENCHANT_LEVEL_4_JEWELLERY(Book.MODERN, 57, 36),
		ENCHANT_CROSSBOW_BOLT_DIAMOND(Book.MODERN, 57, 3),
		WATCHTOWER_TELEPORT(Book.MODERN, 58, 37),
		FIRE_BLAST(Book.MODERN, 59, 38),
		BONES_TO_PEACHES(Book.MODERN, 60, 39),
		CHARGE_EARTH_ORB(Book.MODERN, 60, 40),
		SARADOMIN_STRIKE(Book.MODERN, 60, 41),
		CLAWS_OF_GUTHIX(Book.MODERN, 60, 42),
		FLAMES_OF_ZAMORAK(Book.MODERN, 60, 43),
		TROLLHEIM_TELEPORT(Book.MODERN, 61, 44),
		WIND_WAVE(Book.MODERN, 62, 45),
		CHARGE_FIRE_ORB(Book.MODERN, 63, 46),
		TELEPORT_APE_ATOLL(Book.MODERN, 64, 47),
		WATER_WAVE(Book.MODERN, 65, 48),
		CHARGE_AIR_ORB(Book.MODERN, 66, 49),
		VULNERABILITY(Book.MODERN, 66, 50),
		ENCHANT_LEVEL_5_JEWELLERY(Book.MODERN, 68, 51),
		ENCHANT_CROSSBOW_BOLT_DRAGONSTONE(Book.MODERN, 68, 3),
		TELEPORT_KOUREND(Book.MODERN, 69, 52),
		EARTH_WAVE(Book.MODERN, 70, 53),
		ENFEEBLE(Book.MODERN, 73, 54),
		TELEOTHER_LUMBRIDGE(Book.MODERN, 74, 55),
		FIRE_WAVE(Book.MODERN, 75, 56),
		ENTANGLE(Book.MODERN, 79, 57),
		STUN(Book.MODERN, 80, 58),
		CHARGE(Book.MODERN, 80, 59),
		TELEOTHER_FALADOR(Book.MODERN, 82, 60),
		TELE_BLOCK(Book.MODERN, 85, 61),
		TELEPORT_TO_BOUNTY_TARGET(Book.MODERN, 90, 62),
		ENCHANT_LEVEL_6_JEWELLERY(Book.MODERN, 87, 63),
		ENCHANT_CROSSBOW_BOLT_ONYX(Book.MODERN, 87, 3),
		TELEOTHER_CAMELOT(Book.MODERN, 90, 64);
		private final Book book;
		private final int level, component;

		Spell(final Book book, final int level, final int component) {
			this.book = book;
			this.level = level;
			this.component = component;
		}

		public Book book() {
			return book;
		}

		public int level() {
			return level;
		}

		public int component() {
			return component + book.offset;
		}
	}

	public enum Book {
		MODERN(218, 1), NIL(-1, 0);
		private final int widget;
		private final int offset;

		Book(final int widget, final int offset) {
			this.widget = widget;
			this.offset = offset;
		}

		public int widget() {
			return widget;
		}
	}
}
