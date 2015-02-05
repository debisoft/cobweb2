package org.cobweb.cobweb2.impl.learning;

import org.cobweb.cobweb2.core.Location;



public abstract class Occurrence implements Queueable {

	public final float detectableDistance;
	public final ComplexAgentLearning target;
	public final Location location;
	public final long time;
	private final String desc;

	private MemorableEvent event;
	private boolean hasOccurred = false;

	public Occurrence(ComplexAgentLearning target, long time, float detectableDistance, String desc) {
		this.target = target;
		this.location = target.getPosition();
		this.time = time;
		this.detectableDistance = detectableDistance;
		this.desc = desc;
		this.target.getEnvironment().allOccurrences.add(this);
	}

	// Effects the agent in whatever way necessary returning a memory of the
	// effect
	public abstract MemorableEvent effect(ComplexAgentLearning concernedAgent);

	// Causes the effect to occur, initializes the event, places the memory
	// in the agent's memory,
	// returns the event
	@Override
	public final void happen() {
		event = effect(target);
		target.remember(event);
		hasOccurred = true;
	}

	public MemorableEvent getEvent() {
		if (!hasOccurred) {
			throw new IllegalStateException("Cannot get event that has not occured");
		}
		return event;
	}

	public boolean hasOccurred() {
		return hasOccurred;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public String getDescription() {
		return desc;
	}
}