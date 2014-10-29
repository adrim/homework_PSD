package utils;

public class Pair<T, V> {
	private T first		= null;
	private V second	= null;
	
	public Pair() {}
	public Pair(T first, V second) {
		// TODO Auto-generated constructor stub
		this.first	= first;
		this.second	= second;
	}
	public T getFirst() {
		return first;
	}
	public void setFirst(T first) {
		this.first = first;
	}
	public V getSecond() {
		return second;
	}
	public void setSecond(V second) {
		this.second = second;
	}
}
