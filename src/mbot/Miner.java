package mbot;

import java.util.*;

import battlecode.common.*;

public class Miner extends RobotPlayer {
	static MapLocation nearestSoup;
	static MapLocation nearestRefinery;
	
	static final int DEFAULT_MODE = 0;
	
	static int mode = 0;
	
	static HashSet<MapLocation> soupLocations = new HashSet<>();
	static HashSet<MapLocation> refineryLocations = new HashSet<>();
	
	/**
	* TODO implement communication
	* TODO implement refining
	* TODO implement mining
	* TODO implement building
	* TODO a* pathing
	* 
	* @throws GameActionException
	*/
	static void run() throws GameActionException {
		
		// Find HQ
		if (hqLoc == null)
			Util.tryFindHQ();
		
		// Default nearestRefinery to hqLoc once it is found
		if (nearestRefinery == null && hqLoc != null)
			nearestRefinery = hqLoc;
		
		if (mode == DEFAULT_MODE)
			runDefaultMode();
		
	}
	
	/**
	 * Default mode is mining and refining soup.
	 * 
	 */
	static void runDefaultMode() {
		
	}
	
}
