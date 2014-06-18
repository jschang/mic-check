package com.jonschang.audio;

public interface ValueAccessor<T> {
	public T value();
	public void value(T value);
}
