package comm;

public interface StatusObserver {
	public void status(String s);
	public void error(String s);
}
