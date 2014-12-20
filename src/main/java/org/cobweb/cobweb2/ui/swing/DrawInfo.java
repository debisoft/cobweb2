package org.cobweb.cobweb2.ui.swing;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.cobweb.cobweb2.Simulation;
import org.cobweb.cobweb2.abiotic.TemperatureParams;
import org.cobweb.cobweb2.core.Agent;
import org.cobweb.cobweb2.core.ComplexAgent;
import org.cobweb.cobweb2.core.Environment;
import org.cobweb.cobweb2.core.Location;
import org.cobweb.swingutil.ColorLookup;
import org.cobweb.swingutil.TypeColorEnumeration;
import org.cobweb.util.Point2D;

/**
 * DrawInfo stores the frame data for the draw method to display. DrawInfo's
 * are built by calling newTileColors and newAgent in response to a
 * getDrawInfo call on the Environment or on an Agent. Note that DrawInfo is
 * a private class, and only LocalUIInterface knows of it's existence. For
 * this reason, DrawInfo is local to LocalUIInterface, and LocalUIInterface
 * reads DrawInfo members directly.
 */
class DrawInfo {

	/** Width of the frame info, in tiles */
	int width;

	/** Height of the frame info, in tiles */
	int height;

	/**
	 * width * height array of colors for the tiles; The color for a
	 * specific tile at (x,y) is tileColors[y * width * + x]
	 */
	java.awt.Color[] tileColors;

	/** Linked list of AgentDrawInfo for the display of agents. */
	List<AgentDrawInfo> agents = new LinkedList<AgentDrawInfo>();

	List<PathDrawInfo> paths = new LinkedList<PathDrawInfo>();

	public List<DropDrawInfo> drops = new LinkedList<DropDrawInfo>();

	static final Color COLOR_GRIDLINES = Color.lightGray;

	private ColorLookup colorMap = TypeColorEnumeration.getInstance();

	/**
	 * Construct a DrawInfo width specific width, height and tile colors.
	 * The tiles array is not copied; the caller is assumed to "give" the
	 * array to the drawing info, and not keep any local references around.
	 */
	DrawInfo(Simulation sim, List<ComplexAgent> observedAgents) {
		width = sim.theEnvironment.topology.width;
		height = sim.theEnvironment.topology.height;
		tileColors = new Color[width * height];

		int tileIndex = 0;
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Location currentPos = new Location(x, y);

				if (sim.theEnvironment.testFlag(currentPos, Environment.FLAG_STONE))
					tileColors[tileIndex++] = java.awt.Color.darkGray;

				else if (sim.theEnvironment.testFlag(currentPos, Environment.FLAG_FOOD))
					tileColors[tileIndex++] = colorMap.getColor(sim.theEnvironment.getFoodType(currentPos), 0 /* agentTypeCount */);

				else
					tileColors[tileIndex++] = java.awt.Color.white;
			}
		}


		for (Agent a : sim.theEnvironment.getAgents()) {
			agents.add(new AgentDrawInfo((ComplexAgent) a, colorMap, sim));
		}

		for (ComplexAgent observedAgent: observedAgents) {
			List<Location> path = observedAgent.getInfo().getPathHistory();
			if (path != null)
				paths.add(new PathDrawInfo(path));
		}

		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Location currentPos = new Location(x, y);
				if (sim.theEnvironment.testFlag(currentPos, Environment.FLAG_DROP)){
					drops.add(new DropDrawInfo(new Point2D(x, y), sim.theEnvironment.getDrop(currentPos)));
				}
			}
		}
	}



	/** Draw the tiles and the agents. */
	void draw(java.awt.Graphics g, int tileWidth, int tileHeight) {
		// Tiles
		int tileIndex = 0;
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				g.setColor(tileColors[tileIndex++]);
				g.fillRect(x * tileWidth + 1, y * tileHeight + 1, tileWidth - 1, tileHeight - 1);
			}
		}

		int half =(int)( tileWidth / 2.0f + 0.5f);

		for (DropDrawInfo drop : drops) {
			int x = drop.pos.x;
			int y = drop.pos.y;
			g.setColor(drop.col);
			g.fillRect(x * tileWidth + 0, y * tileHeight + 0, half, half);
			g.fillRect(x * tileWidth + half, y * tileHeight + half, half, half);
		}

		// Grid lines
		g.setColor(DrawInfo.COLOR_GRIDLINES);
		int totalWidth = tileWidth * width;
		for (int y = 0; y <= height; y++) {
			g.drawLine(0, y * tileHeight, totalWidth, y * tileHeight);
		}
		int totalHeight = tileHeight * height;
		for (int x = 0; x <= width; x++) {
			g.drawLine(x * tileWidth, 0, x * tileWidth, totalHeight);
		}

		// Agents
		for (AgentDrawInfo a : agents) {
			a.draw(g, tileWidth, tileHeight);
		}

		// Paths
		for (PathDrawInfo path : paths) {
			path.draw(g, tileWidth, tileHeight);
		}

		int limit = Math.min(TemperatureParams.TEMPERATURE_BANDS, height);
		// Temperature band labels
		for (int y = 0; y < height; y++) {
			int band = y * limit / height;
			g.setColor(colorMap.getColor(band, 5));
			int offset = (limit / 2 - band) * 3 / -2;
			for (int i = 0; i <= band; i++) {
				int x = (i + 2) * -3 + offset;
				g.drawLine(x - 1, y * tileHeight, x, (y + 1) * tileHeight);
			}
		}
	}

}