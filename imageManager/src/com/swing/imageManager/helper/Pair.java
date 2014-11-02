package com.swing.imageManager.helper;

public class Pair {
	private String Key;
	private String Value;

	public Pair() {
	}

	public Pair(String k, String v) {
		Key = k;
		Value = v;
	}

	public String getKey() {
		return Key;
	}

	public void setKey(String key) {
		Key = key;
	}

	public String getValue() {
		return Value;
	}

	public void setValue(String value) {
		Value = value;
	}

	@Override
	public boolean equals(Object arg0) {
		Pair other = (Pair) arg0;
		return (Key.contains(other.Key) && Value.contains(other.Value));
	}

	@Override
	public String toString() {
		return "Pair [Key=" + Key + ", Value=" + Value + "]";
	}

}
