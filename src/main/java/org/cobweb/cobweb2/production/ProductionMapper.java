package org.cobweb.cobweb2.production;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cobweb.cobweb2.core.ComplexAgent;
import org.cobweb.cobweb2.core.ComplexEnvironment;
import org.cobweb.cobweb2.core.Environment;
import org.cobweb.cobweb2.core.Location;
import org.cobweb.cobweb2.core.LocationDirection;
import org.cobweb.cobweb2.core.SimulationInternals;
import org.cobweb.cobweb2.interconnect.SpawnMutator;
import org.cobweb.cobweb2.interconnect.StateParameter;
import org.cobweb.cobweb2.interconnect.StatePlugin;
import org.cobweb.util.ArrayUtilities;

public class ProductionMapper implements StatePlugin, SpawnMutator {

	private ComplexEnvironment environment;
	private float[][] vals;
	private float maxValue;
	SimulationInternals simulation;
	private ProductionParams[] initialParams;

	public ProductionMapper(ProductionParams[] productionParams) {
		initialParams = productionParams;
		params = new LinkedList<StateParameter>();
		params.add(new ProductHunt());
	}

	void remProduct(Product p) {
		Location loc = p.loc;
		environment.setDrop(loc, null);
		environment.setFlag(loc, Environment.FLAG_DROP, false);

		updateValues(p, false);
	}

	private void updateValues(Product p, boolean addition) {
		float newMax = 0;
		for (int x = 0; x < vals.length; x++) {
			for (int y = 0; y < vals[x].length; y++) {
				float value = getDifAtLoc(p, new Location(x, y));
				vals[x][y] += addition ? value : - value;

				if (vals[x][y] < 0) {
					vals[x][y] = 0;
				}
				if (vals[x][y] > newMax) {
					newMax = vals[x][y];
				}
			}
		}

		// Accumulation errors could make this a very small number,
		// and we only care about real values
		if (newMax < 1)
			newMax = 1;

		maxValue = newMax;
	}

	private float getDifAtLoc(Product source, Location loc2) {
		float val = source.getValue();
		val /= Math.max(1, simulation.getEnvironment().topology.getDistanceSquared(source.getLocation(), loc2));
		return val;
	}

	/**
	 * @param loc - the location whose "Productivity value" we are querying
	 * @return the total "Productivity value" of the parameter Location.
	 *
	 *         It is most efficient to place products on tiles that have prod.
	 *         vals. that indicate that a sufficient number of products are
	 *         nearby in order to attract agent's business, but not enough are
	 *         around so that there is too much competition. Therefore:
	 *
	 *         -An agent's probability of dropping a product on a tile with low
	 *         (~0) prod. val. should be low. (but not non-existant because then
	 *         initially agents would never drop products) -An agent's
	 *         probability of dropping a product on a tile with a very high
	 *         prod. val. should be infinitesimal. -An agent should have a high
	 *         chance of dropping a product on a tile with a moderate prob. val.
	 *
	 */
	public float getValueAtLocation(Location loc) {
		return vals[loc.x][loc.y];
	}

	public float getValueAtLocation(int x, int y) {
		Location loc = new Location(x, y);
		return getValueAtLocation(loc);
	}

	public float[][] getValues() {
		return vals;
	}

	public float getMax() {
		return maxValue;
	}

	public class ProductHunt implements StateParameter {

		@Override
		public String getName() {
			return "ProdHunt";
		}

		@Override
		public double getValue(ComplexAgent agent) {
			LocationDirection here = agent.getPosition();
			Location ahead = environment.topology.getAdjacent(here);
			if (ahead == null || !environment.topology.isValidLocation(ahead)) {
				return 0;
			}

			float a = getValueAtLocation(here);
			float b = getValueAtLocation(ahead);

			float max = Math.max(a, b);
			if (max == 0)
				return 0;

			return b / max;
		}

	}

	private List<StateParameter> params;

	@Override
	public List<StateParameter> getParameters() {
		return params;
	}

	Set<Product> products = new LinkedHashSet<Product>();

	public void addProduct(float value, ComplexAgent owner) {
		Location loc = owner.getPosition();
		Product prod = new Product(value, owner, loc, this);

		updateValues(prod, true);

		products.add(prod);

		if (environment.testFlag(owner.getPosition(), Environment.FLAG_FOOD)) {
			environment.setFlag(owner.getPosition(), Environment.FLAG_FOOD, false);
		}

		environment.setFlag(owner.getPosition(), Environment.FLAG_DROP, true);
		environment.setDrop(loc, prod);
	}


	private boolean roll(float chance) {
		return chance > simulation.getRandom().nextFloat();
	}

	private boolean shouldProduce(ComplexAgent agent) {
		if (!agentData.containsKey(agent)) {
			return false;
		}

		ProductionParams params = agentData.get(agent);
		if (!params.productionMode || !roll(params.initProdChance)){
			return false;
		}

		float locationValue = getValueAtLocation(agent.getPosition());

		if (locationValue > params.highDemandCutoff) {
			return false;
		}

		// ADDITIONS:
		// Learning agents should adapt to products

		if (locationValue <= params.lowDemandThreshold) {
			// In an area of low demand
			return roll(params.lowDemandProdChance);
		} else if (locationValue <= params.sweetDemandThreshold) {
			/*
			 * The sweet spot is an inverted parabola, the vertex is 100% probability in the middle of the sweet spot
			 * (between lowDemandThreshold and sweetDemandThreshold)
			 * the tips are sweetDemandStartChance probability at the thresholds.
			 *
			 */

			// parabola shape
			float peak = (params.lowDemandThreshold + params.sweetDemandThreshold) * 0.5f;
			float width = params.sweetDemandThreshold - params.lowDemandThreshold;
			// position along standard parabola
			float x = (locationValue - peak) / (width / 2);
			// parabola value
			float y = x * x  * (1-params.sweetDemandStartChance);

			float chance = params.sweetDemandStartChance + (1 - y);

			// Sweet spot; perfect balance of competition and attraction here;
			// likelihood of producing products here
			// is modelled by a parabola
			return roll(chance);
		}

		// locationValue > 10f; Very high competition in this area!
		// The higher the value the lower the production chances are.

		// Let: d = prodParams.sweetDemandThreshold
		// e = prodParams.highDemandCutoff
		// f = prodParams.highDemandProdChance
		//
		// p1 = (d, f);
		// p2 = (e, 0);
		//
		// rise = f - 0 = f;
		// run = d - e
		//
		// m = f / (d - e)
		//
		// y = mx + b
		//
		// b = y - mx
		// b = 0 - me
		// b = -(f / (d -e))e
		//
		// y = ((f - e) / d)x + e

		float d = params.sweetDemandThreshold;
		float e = params.highDemandCutoff;
		float f = params.highDemandProdChance;

		float rise = f;
		float run = d - e;

		float m = rise / run;

		float b = -1 * m * e;

		// y = mx + b
		float y = (m * locationValue) + b;

		// p1 = (sweetDemandThreshold, prodParams.highDemandProdChance)
		// p2 = (
		// minChance

		return roll(y);
	}

	public void tryProduction(ComplexAgent agent) {
		if (shouldProduce(agent)) {
			// TODO: find a more clean way to create and assign product
			// Healthy agents produce high-value products, and vice-versa
			addProduct(agent.getEnergy() / (float) agent.params.initEnergy, agent);
		}
	}

	private Map<ComplexAgent, ProductionParams> agentData = new HashMap<ComplexAgent, ProductionParams>();

	@Override
	public Collection<String> logDataAgent(int agentType) {
		return NO_DATA;
	}

	@Override
	public Collection<String> logDataTotal() {
		return NO_DATA;
	}

	@Override
	public Collection<String> logHeadersAgent() {
		return NO_DATA;
	}

	@Override
	public Collection<String> logHeaderTotal() {
		return NO_DATA;
	}

	@Override
	public void onDeath(ComplexAgent agent) {
		agentData.remove(agent);
	}

	@Override
	public void onSpawn(ComplexAgent agent) {
		agentData.put(
				agent,
				(ProductionParams) initialParams[agent.getType()].clone()
				);
	}

	@Override
	public void onSpawn(ComplexAgent agent, ComplexAgent parent) {
		agentData.put(
				agent,
				(ProductionParams) agentData.get(parent).clone()
				);
	}

	@Override
	public void onSpawn(ComplexAgent agent, ComplexAgent parent1, ComplexAgent parent2) {
		agentData.put(
				agent,
				(ProductionParams) agentData.get(parent1).clone()
				);
	}

	public void setParams(SimulationInternals sim, ProductionParams[] productionParams, boolean keepOldProducts) {
		simulation = sim;
		environment = (ComplexEnvironment) sim.getEnvironment();

		initialParams = productionParams;

		if (vals == null || !keepOldProducts) {
			vals = new float[environment.topology.width][environment.topology.height];
		} else {
			vals = ArrayUtilities.resizeArray(vals, environment.topology.width, environment.topology.height);
		}
	}


}
