import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.orbischallenge.pacman.api.common.GhostState;
import com.orbischallenge.pacman.api.common.MoveDir;
import com.orbischallenge.pacman.api.java.Ghost;
import com.orbischallenge.pacman.api.java.JUtil;
import com.orbischallenge.pacman.api.java.Maze;
import com.orbischallenge.pacman.api.java.Pac;
import com.orbischallenge.pacman.api.java.Player;

/**
 * The Player class is the parent class of your AI player. It is just like a
 * template. Your AI player class (called PacPlayer) must implement the
 * following methods.
 * 
 */
public class PacPlayer implements Player {

	private static final int REALLY_SCARED = 2;

	private static final int MAX_PATH_TO_LOOK = 20;

	private static final int NUMBER_OF_CHECKS = 5;

	private static int CHASE_CONSTANT = 32;

	private static int THRESHOLD_TILES = 4;
	
	private static int HOW_DEEP_IN_INTERSECTION = 4;

	private int lives = 3;

	private MazeGraph graph;

	enum Modes {
		EXPLORING, HUNTING, FLEEING
	}

	private static double THRESHOLD_PIXELS = THRESHOLD_TILES * 16;

	enum Quadrants {
		UPPER_LEFT, UPPER_RIGHT, LOWER_RIGHT, LOWER_LEFT
	}

	// States where touching a ghost is bad
	private final static List<GhostState> dangerousStates = Arrays
			.asList(new GhostState[] { GhostState.CHASER, GhostState.SCATTER });

	/**
	 * This is method decides Pacman�s moving direction in the next frame (See
	 * Frame Concept). The parameters represent the maze, ghosts, Pacman, and
	 * score after the execution of last frame. In the next frame, the game will
	 * call this method to set Pacman�s direction and let him move (see Pacman�s
	 * Move).
	 * 
	 * @param maze
	 *            A Maze object representing the current maze.
	 * @param ghosts
	 *            An array of Ghost objects representing the four ghosts.
	 * @param pac
	 *            A Pac object representing Pacman
	 * @param score
	 *            The current score
	 * @return MoveDir
	 */
	public MoveDir calculateDirection(Maze maze, Ghost[] ghosts, Pac pac,
			int score) {

		Modes mode = Modes.EXPLORING;
		List<Ghost> closeActiveGhosts = new ArrayList<Ghost>();
		List<Ghost> closeBlinkingGhosts = new ArrayList<Ghost>();
		for (int i = 0; i < ghosts.length; i++) {
			if (ghosts[i].distanceToPac(pac) < THRESHOLD_PIXELS
					&& (dangerousStates.contains(ghosts[i].getState()))) {
				closeActiveGhosts.add(ghosts[i]);
			} else if (ghosts[i].distanceToPac(pac) < THRESHOLD_PIXELS
					&& ghosts[i].getState().equals(GhostState.FRIGHTEN)) {
				closeBlinkingGhosts.add(ghosts[i]);
			}
		}

		if (!closeActiveGhosts.isEmpty()) {
			mode = Modes.FLEEING;
		} else if (!closeBlinkingGhosts.isEmpty()) {
			mode = Modes.HUNTING;
		}

		MoveDir dir = calculateNext(mode, pac, maze, closeActiveGhosts,
				closeBlinkingGhosts);
		return dir;
	}

	private MoveDir calculateNext(Modes mode, Pac pac, Maze maze,
			List<Ghost> activeGhosts, List<Ghost> blinkingGhosts) {

		// Get the current tile of Pacman
		Point pacTile = pac.getTile();

		List<MoveDir> possibleDirs = pac.getPossibleDirs();
		MoveDir direction = dirToClosestDot(pac.getTile(), possibleDirs,
				new HashSet<Point>());
		switch (mode) {
		case EXPLORING:
			break;
		case FLEEING:
			List<MoveDir> potentialDirs = pac.getPossibleDirs();

			Set<Point> dangerousPoints = new HashSet<Point>();
			for (Ghost ghost : activeGhosts) {
				List<Point> path = graph.getShortestPath(ghost.getTile(),
						pac.getTile(), THRESHOLD_TILES);
				if (!path.isEmpty()) {
					dangerousPoints.addAll(path);
				}
			}
			for (MoveDir potentialDir : possibleDirs) {
				Point point = JUtil.vectorAdd(pacTile,
						JUtil.getVector(potentialDir));
				if (dangerousPoints.contains(point)) {
					potentialDirs.remove(potentialDir);
				}
			}

			if (!potentialDirs.isEmpty()) {
				Set<Point> ignoreList = new HashSet<Point>();
				List<Point> pathOriginal = closestDotPath(pac.getTile(),
						potentialDirs, ignoreList, Integer.MAX_VALUE);
				List<Point> path = pathOriginal;
				int maxAway = 0;
				List<Point> bestPath = pathOriginal;
				for (int i = 0; i < NUMBER_OF_CHECKS; i++) {
					int sum = 0;
					Point dot = path.get(path.size() - 1);
					ignoreList.add(dot);
					for (Ghost ghost : activeGhosts) {
						List<Point> pathForGhost = graph.getShortestPath(
								ghost.getTile(), dot, THRESHOLD_TILES);
						ignoreList.addAll(pathForGhost);
						if (!pathForGhost.isEmpty()) {
							sum += pathForGhost.size();
						} else {
							sum += THRESHOLD_TILES + 1;
						}
					}
					if (sum > maxAway) {
						maxAway = sum;
						bestPath = path;
					}

					List<Point> temp = closestDotPath(pac.getTile(),
							potentialDirs, ignoreList, MAX_PATH_TO_LOOK);
					if (!temp.isEmpty()) {
						path = temp;
					}
				}
				if (maxAway < THRESHOLD_TILES * REALLY_SCARED) {
					direction = smartFlee(pacTile, potentialDirs, activeGhosts);
				}
				direction = getDirFromPath(pacTile, bestPath);
			}

			break;
		case HUNTING:
			List<Point> closestGhost = new ArrayList<Point>();
			int minSize = Integer.MAX_VALUE;
			for (Ghost ghost : blinkingGhosts) {
				List<Point> path = graph.getShortestPath(pac.getTile(),
						ghost.getTile(), ghost.framesTillRecover()
								/ CHASE_CONSTANT);
				if (!path.isEmpty() && path.size() < minSize) {
					closestGhost = path;
					minSize = path.size();
				}
			}
			if (!closestGhost.isEmpty()) {
				if (closestGhost.size() > 1) {
					direction = JUtil.getMoveDir(JUtil.vectorSub(
							closestGhost.get(1), pacTile));
				} else {
					direction = pac.getDir();
				}

			}
			break;
		default:
			throw new IllegalStateException();
		}
		if (graph.warpPoints.contains(pacTile)) {
			direction = pac.getDir();
		}
		return direction;
	}

	private MoveDir smartFlee(Point pacTile, List<MoveDir> potentialDirs, List<Ghost> ghosts) {
		List<Point> ghostPoints = new ArrayList<Point>();
		for (Ghost ghost : ghosts) {
			ghostPoints.add(ghost.getTile());
		}
		int mostIntersections = 0;
		MoveDir bestDir = potentialDirs.get(0);
		for (MoveDir moveDir : potentialDirs) {
			int numberOfIntersections = graph.getNumberOfIntersectionsBeforeGhost(pacTile, moveDir, ghostPoints, HOW_DEEP_IN_INTERSECTION);
			if(numberOfIntersections > mostIntersections){
				mostIntersections = numberOfIntersections;
				bestDir = moveDir;
			}
			
		}
		return bestDir;
	}

	private MoveDir fleeLikeCrazy(List<MoveDir> potentialDirs,
			List<Ghost> ghosts, Point pacTile) {
		int maxDistance = 0;
		MoveDir finalDir = potentialDirs.get(0);
		for (MoveDir dir : potentialDirs) {
			Point newPoint = JUtil.vectorAdd(pacTile, JUtil.getVector(dir));
			int sum = 0;
			for (Ghost ghost : ghosts) {
				sum += ghost.getTile().distanceSq(newPoint);
			}
			if (sum > maxDistance) {
				maxDistance = sum;
				finalDir = dir;
			}
		}
		return finalDir;
	}

	private List<Point> closestDotPath(Point tile, List<MoveDir> potentialDirs,
			Set<Point> ignoreList, int maxLength) {
		return graph.getClosestDot(tile, potentialDirs, ignoreList, maxLength);
	}

	private MoveDir dirToClosestDot(Point point, List<MoveDir> potentialDirs,
			Set<Point> ignoreList) {
		List<Point> path = graph.getClosestDot(point, potentialDirs,
				ignoreList, Integer.MAX_VALUE);
		return getDirFromPath(point, path);
	}

	private MoveDir getDirFromPath(Point point, List<Point> path) {
		if (!path.isEmpty()) {
			return JUtil.getMoveDir(JUtil.vectorSub(path.get(0), point));
		} else {
			throw new RuntimeException();
		}
	}

	/**
	 * This method will be called by the game whenever a new level starts. The
	 * parameters represent the game objects at their initial states. This
	 * method will always be called before calculateDirection.
	 * 
	 * @param maze
	 *            A Maze object representing the current maze.
	 * @param ghosts
	 *            An array of Ghost objects representing the four ghosts.
	 * @param pac
	 *            A Pac object representing Pacman
	 * @param score
	 *            The current score
	 */
	public void onLevelStart(Maze maze, Ghost[] ghosts, Pac pac, int score) {
		System.out.println("Java player start new level!");
		/*
		 * CHASE_CONSTANT = Integer.valueOf(System.getProperty("CHASE_CONSTANT",
		 * ((Integer)THRESHOLD_TILES).toString()));
		 * System.out.println("Chase Constant " + CHASE_CONSTANT);
		 * THRESHOLD_TILES =
		 * Integer.valueOf(System.getProperty("THRESHOLD_TILES",
		 * ((Integer)THRESHOLD_TILES).toString()));
		 * System.out.println("Threshold Tiles " + THRESHOLD_TILES);
		 * THRESHOLD_PIXELS = THRESHOLD_TILES*16;
		 */
		this.graph = new MazeGraph(maze);
	}

	/**
	 * This method will be called by the game whenever Pacman receives a new
	 * life, including the first life. The parameters represent the repositioned
	 * game objects. This method will always be called before calculateDirection
	 * and after onLevelStart.
	 * 
	 * @param maze
	 *            A Maze object representing the current maze.
	 * @param ghosts
	 *            An array of Ghost objects representing the four ghosts.
	 * @param pac
	 *            A Pac object representing Pacman
	 * @param score
	 *            The current score
	 */
	public void onNewLife(Maze maze, Ghost[] ghosts, Pac pac, int score) {
		System.out.println("Hi, I still have " + lives + " lives left.");
		lives--;
	};
}