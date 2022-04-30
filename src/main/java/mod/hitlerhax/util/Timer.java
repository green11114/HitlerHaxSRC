package mod.hitlerhax.util;

public class Timer {
	private long time;

	public Timer() {
		this.time = -1;
	}

	public long passed() {
		return System.currentTimeMillis() - this.time;
	}

	public boolean passed(double ms) {
		return System.currentTimeMillis() - this.time >= ms;
	}

	public void reset() {
		this.time = System.currentTimeMillis();
	}

	public void resetTimeSkipTo(long p_MS) {
		this.time = System.currentTimeMillis() + p_MS;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}
