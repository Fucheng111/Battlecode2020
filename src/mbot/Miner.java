package mbot;

import java.util.*;

import battlecode.common.*;
import mbot.Communication.*;

public class Miner extends RobotPlayer {
	static MapLocation nearestSoup;
	static MapLocation nearestRefinery;
	static Direction initialDirection;
	
	static final int DEFAULT_MODE = 0;
	
	static int mode = 0;
	
	static HashSet<MapLocation> soupLocations = new HashSet<>();
	static HashSet<MapLocation> refineryLocations = new HashSet<>();
	
	/**
	* TODO implement communication
	* TODO implement refining
	* TODO implement mining
	* TODO implement building
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
		
		// Wait for initial direction
		if (initialDirection == null)
			getInitialDirectionFromBlockchain();
		
		// Doing actions
		if (mode == DEFAULT_MODE)
			runDefaultMode();
		
	}
	
	/**
	 * Default mode is mining and refining soup.
	 * 
	 */
	static void runDefaultMode() throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		
		// Try refining
		for (Direction dir: directions)
			if (Util.tryRefine(dir)) {
				MapLocation refineLoc = currLoc.add(dir);
				if (refineryLocations.add(refineLoc)) {
					RefineryCreatedMessage msg = new RefineryCreatedMessage();
					if (!msg.setInfo(refineLoc.x, refineLoc.y).tryBroadcast())
						messageQ.add(msg);
				}
				if (!nearestRefinery.equals(refineLoc)) // Update nearest refinery
					nearestRefinery = refineLoc;
			}
			
		// Try mining
		for (Direction dir: directions) {
			if (Util.tryMine(dir)) {
				MapLocation soupLoc = currLoc.add(dir);
				if (soupLocations.add(soupLoc)) {
					SoupLocationMessage msg = new SoupLocationMessage();
					if (!msg.setInfo(soupLoc.x, soupLoc.y).tryBroadcast())
						messageQ.add(msg);
				}
				if (!nearestSoup.equals(soupLoc)) // Update nearest soup
					nearestSoup = soupLoc;
			}
		}
		
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			if (nearestRefinery != null)
				Util.goTo(nearestRefinery);
			
		} else if (nearestSoup != null) {
			Util.goTo(nearestSoup);
		} else {
			// other pathing?
		}
	}
	
	static void getInitialDirectionFromBlockchain() {
		
	}
	
	/**
	 * Remove duplicates from messageQ. A nonoptimal solution.
	 */
	static void cleanMessageQ() {
		messageQ = new PriorityQueue<Message>(new HashSet<Message>(messageQ));
	}
}
