package org.powerbot.script;

public class GeEvent {
	
	public enum Type {
		BUY,
		SELL
	}
	
	public final String item;
	public final double progress;
	
	public GeEvent(final String item, final double progress) {
		this.item = item;
		this.progress = progress;
	}
}
