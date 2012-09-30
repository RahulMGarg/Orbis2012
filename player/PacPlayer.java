import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

	private static int CHASE_CONSTANT = 32;

	private static int THRESHOLD_TILES = 5;

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
	private final static List<GhostState> dangerousStates = Arrays.asList(new GhostState[] {GhostState.CHASER, GhostState.SCATTER});

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

		MoveDir dir = calculateNext(mode, pac, maze, closeActiveGhosts, closeBlinkingGhosts);
		return dir;
	}

	private MoveDir calculateNext(Modes mode, Pac pac, Maze maze,
			List<Ghost> activeGhosts, List<Ghost> blinkingGhosts) {

		// Get the current tile of Pacman
		Point pacTile = pac.getTile();

		List<MoveDir> possibleDirs = pac.getPossibleDirs();
		MoveDir direction = dirToClosestDot(pac, possibleDirs);
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
				direction = dirToClosestDot(pac, potentialDirs);
			}

			break;
		case HUNTING:
			List<Point> closestGhost = new ArrayList<Point>();
			int minSize = Integer.MAX_VALUE;
			for (Ghost ghost : blinkingGhosts) {
				List<Point> path = graph.getShortestPath(pac.getTile(),
						ghost.getTile(), ghost.framesTillRecover()/CHASE_CONSTANT);
				if (!path.isEmpty() && path.size()<minSize) {
					closestGhost = path;
					minSize = path.size();
				}
			}
			if(!closestGhost.isEmpty()){
				if(closestGhost.size()>1){
					direction = JUtil.getMoveDir(JUtil.vectorSub(closestGhost.get(1), pacTile));
				}else{
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

	private MoveDir dirToClosestDot(Pac pac, List<MoveDir> potentialDirs) {
		return graph.getClosestDot(pac.getTile(), potentialDirs);
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
		CHASE_CONSTANT = Integer.valueOf(System.getProperty("CHASE_CONSTANT", ((Integer)THRESHOLD_TILES).toString()));
		THRESHOLD_TILES = Integer.valueOf(System.getProperty("THRESHOLD_TILES", ((Integer)THRESHOLD_TILES).toString()));
		THRESHOLD_PIXELS = THRESHOLD_TILES*16;
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