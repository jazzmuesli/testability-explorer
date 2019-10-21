package com.github.jazzmuesli.testability.plugin;

import com.google.common.base.Strings;

public class MyCoolClass {

	private String name = "hello";
	private Object bananas;
	private static final String NAME;
	static {
		NAME = "HELLO";
	}

	private MyCoolClass() {
		this.bananas = new MyStringHolder(Strings.repeat("banana", 30));
	}

	private class MyStringHolder {

		private String s;

		public MyStringHolder(String s) {
			this.s = s;
		}
		@Override
		public String toString() {
			return s;
		}

	}

	MyCoolClass getInstance() {
		return new MyCoolClass();
	}
}
