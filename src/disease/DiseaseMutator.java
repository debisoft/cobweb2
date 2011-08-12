/**
 *
 */
package disease;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cobweb.ArrayUtilities;
import cobweb.globals;
import cobweb.params.ReflectionUtil;
import cwcore.ComplexAgent;
import cwcore.complexParams.ContactMutator;
import cwcore.complexParams.SpawnMutator;

/**
 * Simulates various diseases that can affect agents.
 */
public class DiseaseMutator implements ContactMutator, SpawnMutator {

	private DiseaseParams[] params;

	private static final Collection<String> blank = new LinkedList<String>();

	private int sickCount[];

	Map<ComplexAgent, Boolean> sick = new HashMap<ComplexAgent, Boolean>();

	/**
	 * DiseaseMutator is an instance of Contanct and Spawn Mutators
	 */
	public DiseaseMutator() {
		sickCount = new int[0];
	}

	public Collection<String> logDataAgent(int agentType) {
		List<String> l = new LinkedList<String>();
		l.add(Integer.toString(sickCount[agentType]));
		return l;
	}

	public Collection<String> logDataTotal() {
		return blank;
	}

	public Collection<String> logHeadersAgent() {
		List<String> header = new LinkedList<String>();
		header.add("Diseased");
		return header;
	}

	public Collection<String> logHeaderTotal() {
		return blank;
	}

	private void makeRandomSick(ComplexAgent agent, float rate) {
		boolean isSick = false;
		if (globals.random.nextFloat() < rate)
			isSick = true;

		if (isSick) {
			DiseaseParams effect = params[agent.type()];
			Field f = effect.param.field;
			if (f != null)
				ReflectionUtil.multiplyField(agent.params, f, effect.factor);

			Color org = agent.getColor();
			Color n = new Color(org.getRed(), org.getGreen(), 255);
			agent.setColor(n);

			sickCount[agent.type()]++;
		}
		sick.put(agent, new Boolean(isSick));
	}

	public void onContact(ComplexAgent bumper, ComplexAgent bumpee) {
		transmitBumpOneWay(bumper, bumpee);
		transmitBumpOneWay(bumpee, bumper);
	}

	public void onDeath(ComplexAgent agent) {
		Boolean b = sick.remove(agent);
		if (b != null && b.booleanValue())
			sickCount[agent.type()]--;
	}

	public void onSpawn(ComplexAgent agent) {
		makeRandomSick(agent, params[agent.type()].initialInfection);
	}

	public void onSpawn(ComplexAgent agent, ComplexAgent parent) {
		if (parent.isAlive() && sick.get(parent).booleanValue())
			makeRandomSick(agent, params[agent.type()].childTransmitRate);
		else
			makeRandomSick(agent, 0);
	}

	public void onSpawn(ComplexAgent agent, ComplexAgent parent1, ComplexAgent parent2) {
		if ((parent1.isAlive() && sick.get(parent1).booleanValue()) || (parent2.isAlive() && sick.get(parent2).booleanValue()))
			makeRandomSick(agent, params[agent.type()].childTransmitRate);
		else
			makeRandomSick(agent, 0);
	}

	public void setParams(DiseaseParams[] diseaseParams, int agentTypes) {
		this.params = diseaseParams;
		sickCount = ArrayUtilities.resizeArray(sickCount, agentTypes);
	}

	private void transmitBumpOneWay(ComplexAgent bumper, ComplexAgent bumpee) {
		int tr = bumper.type();
		int te = bumpee.type();
		if (sick.get(bumper) == null || sick.get(bumpee) == null)
			return;
		if (sick.get(bumper).booleanValue() && params[tr].transmitTo[te] && !sick.get(bumpee).booleanValue()) {
			makeRandomSick(bumpee, params[te].contactTransmitRate);
		}
	}

}
