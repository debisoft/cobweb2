package org.cobweb.cobweb2.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

/**
 * The Environment class represents the simulation world; a collection of
 * locations with state, each of which may contain an agent.
 *
 * The Environment class is designed to handle an arbitrary number of
 * dimensions, although the UIInterface is somewhat tied to two dimensions for
 * display purposes.
 *
 * All access to the internal data of the Environment is done through an
 * accessor class, Environment.Location. The practical upshot of this is that
 * the Environment internals may be implemented in C or C++ using JNI, while the
 * Java code still has a nice java flavoured interface to the data.
 *
 * Another advantage of the accessor model is that the internal data need not be
 * in a format that is reasonable for external access. An array of longs where
 * bitfields represent the location states makes sense in this context, because
 * the accessors allow friendly access to this state information.
 *
 * Furthermore, the accessor is designed to be quite general; there should be no
 * need to subclass Environment.Location for a specific Environment
 * implementation. A number of constants should be defined in an Environment
 * implementation to allow agents to interpret the state information of a
 * location, so agents will need to be somewhat aware of the specific
 * environment they are operating in, but all access should be through this
 * interface, using implementation specific access constants.
 */
public abstract class Environment {

	protected SimulationInternals simulation;

	public Environment(SimulationInternals simulation) {
		this.simulation = simulation;
	}

	// Some predefined directions for 2D
	public static final Direction DIRECTION_NORTH = new Direction(0, -1);

	public static final Direction DIRECTION_SOUTH = new Direction(0, +1);

	public static final Direction DIRECTION_WEST =  new Direction(-1, 0);

	public static final Direction DIRECTION_EAST =  new Direction(+1, 0);

	public static final Direction DIRECTION_NORTHEAST = new Direction(+1, -1);

	public static final Direction DIRECTION_SOUTHEAST = new Direction(+1, +1);

	public static final Direction DIRECTION_NORTHWEST = new Direction(-1, -1);

	public static final Direction DIRECTION_SOUTHWEST = new Direction(-1, +1);

	public static final Direction DIRECTION_NONE = new Direction(0, 0);

	/**
	 * The implementation uses a hash table to store agents, as we assume there
	 * are many more locations than agents.
	 */
	protected java.util.Hashtable<Location, Agent> agentTable = new Hashtable<Location, Agent>();

	/**
	 * Adds agent at given position
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param type agent type
	 */
	public void addAgent(int x, int y, int type) {
		// Nothing
	}

	/**
	 * Adds food at given position
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param type agent type
	 */
	public void addFood(int x, int y, int type) {
		// Nothing
	}

	/**
	 * Adds stone at given position
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 */
	public void addStone(int x, int y) {
		// Nothing
	}

	public void clearAgents() {
		for (Agent a : new ArrayList<Agent>(getAgents())) {
			a.die();
		}
		agentTable.clear();
	}

	public void clearFood() {
		// Nothing
	}

	public void clearStones() {
		// Nothing
	}

	public void clearWaste() {
		// Nothing
	}

	Agent getAgent(Location l) {
		return agentTable.get(l);
	}

	public synchronized Collection<Agent> getAgents() {
		return agentTable.values();
	}

	/** @return the dimensionality of this Environment. */
	public abstract int getAxisCount();

	/** @return true if the axis specified wraps. */
	public abstract boolean getAxisWrap(int axis);

	public int getAgentCount() {
		return agentTable.keySet().size();

	}

	public boolean isValidLocation(Location l) {
		return l.x >= 0 && l.x < getWidth()
				&& l.y >= 0 && l.y < getHeight();
	}

	/**
	 * @return Random location.
	 */
	public Location getRandomLocation() {
		Location l;
		do {
			l = new Location(
					simulation.getRandom().nextInt(getWidth()),
					simulation.getRandom().nextInt(getHeight()));
		} while (!isValidLocation(l));
		return l;
	}

	public abstract int getWidth();

	public abstract int getHeight();

	public abstract EnvironmentStats getStatistics();

	public Location getUserDefinedLocation(int x, int y) {
		Location l = new Location(x, y);
		if (!isValidLocation(l))
			throw new IllegalArgumentException("Location not inside environment");

		return l;
	}

	/**
	 * Removes agent at given position
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 */
	public void removeAgent(int x, int y) {
		// Nothing
	}

	/**
	 * Removes food at given position
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 */
	public void removeFood(int x, int y) {
		// Nothing
	}

	/**
	 * Removes stone at given position
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 */
	public void removeStone(int x, int y) {
		// Nothing
	}

	public final void setAgent(Location l, Agent a) {
		if (a != null)
			agentTable.put(l, a);
		else
			agentTable.remove(l);
	}

	/** Core implementation of setFlag; this is what could be accelerated in C++ */
	protected abstract void setFlag(Location l, int flag, boolean state);

	/**
	 * Core implementation of testFlag; this is what could be accelerated in C++
	 */
	protected abstract boolean testFlag(Location l, int flag);

	public abstract boolean hasAgent(int x, int y);

	public abstract Agent getAgent(int x, int y);

	public abstract boolean hasFood(int x, int y);

	public abstract int getFood(int x, int y);

	public abstract boolean hasStone(int x, int y);

	public abstract LocationDirection getAdjacent(LocationDirection location);

	public Location getAdjacent(Location location, Direction direction) {
		return getAdjacent(new LocationDirection(location, direction));
	}

	public abstract double getDistanceSquared(Location from, Location to);

	public double getDistance(Location from, Location to) {
		return Math.sqrt(getDistanceSquared(from, to));
	}

	public LocationDirection getTurnRightPosition(LocationDirection location) {
		return new LocationDirection(location, new Direction(-location.direction.y, location.direction.x));
	}

	public LocationDirection getTurnLeftPosition(LocationDirection location) {
		return new LocationDirection(location, new Direction(location.direction.y, -location.direction.x));
	}

}
