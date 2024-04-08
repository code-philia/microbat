package microbat.handler.replayexp;

import org.eclipse.core.runtime.jobs.Job;

import microbat.handler.ReplayStats;

public abstract class ReplayJob extends Job {
	public ReplayJob(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	protected ReplayStats stats = new ReplayStats();
	public ReplayStats getReplayStats() {
		return this.stats;
	}
}
