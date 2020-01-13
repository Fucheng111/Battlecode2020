package mbot;

import java.util.*;

import battlecode.common.*;
import mbot.Communication.*;

public class Miner extends RobotPlayer {
	static MapLocation nearestSoup;
	static MapLocation nearestRefinery;
	static MapLocation exploreDest;
	
	static boolean nearbyDesignSchool = false;
	
	static final int EXPLORE_MODE = 1;
	static final int DEFAULT_MODE = 0;
	
	static int mode = 1;
	
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
		
		// Doing actions
		if (mode == EXPLORE_MODE) 
			runExploreMode();
		if (mode == DEFAULT_MODE)
			runDefaultMode();
		
		// If nothing else is broadcast, broadcast the next msg in messageQ
		if (!messageQ.isEmpty() && messageQ.peek().tryBroadcast())
			messageQ.poll();
	}
	
	
	static void runExploreMode() throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		
		// If exploreDest is null, try to find directions, otherwise,
		// ask for directions
		if (exploreDest == null) {
			for (Transaction tx: rc.getBlock(rc.getRoundNum() - 1)) {
				Message msg = new Message(tx);
				
				if (msg.isTeamMessage() && msg.getMessageType() == MessageType.EXPLORE)
					if (msg.getId() == rc.getID())
						exploreDest = msg.getLocation();
			}
			
			// If still null
			if (exploreDest == null) {
				MinerIdleMessage msg = new MinerIdleMessage();
	            
	            if (!msg.setInfo(currLoc.x, currLoc.y, rc.getID()).tryBroadcast())
	            	messageQ.add(msg);
			}
		} else {
			Util.exploreMove(exploreDest);
		}
			
        // Change to mode 1 and update explore if soup is found and move straight to default mode
        if (checkForSoup()) {
        	mode = DEFAULT_MODE;
        	return;
        }
	}
	
	/**
	 * Default mode is mining and refining soup.
	 * 
	 */
	static void runDefaultMode() throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		
		// Update soupLocations and refineryLocations from blockchain
		// Calculate nearest soup and refinery location and update
		// Check for nearby design schools
		if (rc.getRoundNum() > 1)
			updateLocationsFromBlockchain();
		
		// Check nearby for soup
		checkForSoup();
		
		// Try refining
		tryRefining();
			
		// Try mining
		tryMining();
		
		/*
		 * If carrying soup limit, go to nearest refinery (or build one if too far away)
		 * Else, check if there is a nearby design school and build one if there isn't.
		 * Else, go to nearest soup location.
		 * Finally, move randomly until further instructions are received.
		 * 
		 * 
		 */
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			
			// If the nearest refinery is more than 1/6 the map width away, build a nearer refinery
			if ((nearestRefinery == null || currLoc.distanceSquaredTo(nearestRefinery) > 8) && rc.getTeamSoup() > 150) {
				
				buildRefineryAndBroadcast();

			} else {				
				
				Util.goTo(nearestRefinery);		
				
			}
			
		} else if (!nearbyDesignSchool && rc.getTeamSoup() > 500) {
			
			buildDesignSchoolAndBroadcast();
			
		} else if (nearestSoup != null) {
			
			Util.goTo(nearestSoup);
			
		} else {
			
			Util.tryMove(Util.randomDirection());
			
		}
	}
	
	/**
	 * Attempts to refine in all directions. If successful and refinery is not in refineryLocs,
	 * add it and broadcast the location. Also update nearestRefinery.
	 * 
	 * @throws GameActionException
	 */
	static void tryRefining() throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		
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
	}
	
	/**
	 * Attempts to mine in all directions. If successful and soup is not in soupLocs, add it and
	 * broadcast the location. Also update nearestSoup.
	 * 
	 * @throws GameActionException
	 */
	static void tryMining() throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		
		for (Direction dir: directions) {
			if (Util.tryMine(dir)) {
				MapLocation soupLoc = currLoc.add(dir);
				
				if (soupLocations.add(soupLoc)) {
					SoupLocationMessage msg = new SoupLocationMessage();
					if (!msg.setInfo(soupLoc.x, soupLoc.y).tryBroadcast())
						messageQ.add(msg);
				}
				
				if (nearestSoup == null || !nearestSoup.equals(soupLoc)) // Update nearest soup
					nearestSoup = soupLoc;
			}
		}
	}
	
	/**
	 * Builds a refinery in the first available direction and broadcasts it to HQ.
	 * 
	 * @throws GameActionException
	 */
	static void buildRefineryAndBroadcast() throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		
		for (Direction dir : directions)
			if (Util.tryBuild(RobotType.REFINERY, dir)) {
				MapLocation refineLoc = currLoc.add(dir);
				
				RefineryCreatedMessage msg = new RefineryCreatedMessage();
				if (!msg.setInfo(refineLoc.x, refineLoc.y).tryBroadcast())
					messageQ.add(msg);
				
				nearestRefinery = refineLoc;
				
				break;
			}
	}
    
	/**
	 * Builds a refinery in the first available direction and broadcasts it to HQ.
	 * 
	 * @throws GameActionException
	 */
	static void buildDesignSchoolAndBroadcast() throws GameActionException {
		for (Direction dir : directions) {
			if (Util.tryBuild(RobotType.DESIGN_SCHOOL, dir)) {
				MapLocation loc = rc.getLocation().add(dir);
				
				DesignSchoolCreatedMessage msg = new DesignSchoolCreatedMessage();
				msg.setInfo(loc.x, loc.y, rc.getID());
				
				if (!msg.tryBroadcast())
					messageQ.add(msg);
				
				nearbyDesignSchool = true;
			}
		}
	}
	
	/**
     * Loops through the surrounding square and checks for soup within the sensor radius.
     * 
     * @return whether new soup has been found
     * @throws GameActionException
     */
    static boolean checkForSoup() throws GameActionException {
        boolean newSoupFound = false;
        MapLocation currLoc = rc.getLocation();
        int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
        for (int i=-rad; i<=rad; i++) {
            for (int j=-rad; j<=rad; j++) {
                MapLocation loc = currLoc.translate(i,j);
                if (rc.canSenseLocation(loc)) {
                	int soupAtLoc = rc.senseSoup(loc);
                	
                	// If there's soup, add it
                	if (soupAtLoc != 0 && !soupLocations.contains(loc)) { 
                		Util.printAction("try SOUP FOUND message");
                		
                		newSoupFound = true;
                		
                		soupLocations.add(loc);
                		
                		SoupLocationMessage msg = new SoupLocationMessage();
                		msg.setInfo(loc.x, loc.y);
                		
                		if (!msg.tryBroadcast())
                			messageQ.add(msg);
                		
                	// Also check if soup is gone and broadcast 
                	} else if (soupLocations.contains(loc) && soupAtLoc == 0) {
                		
                		if (loc == nearestSoup)
                			nearestSoup = null;
                		
                		soupLocations.remove(loc);
                		
                		SoupGoneMessage msg = new SoupGoneMessage();
                		msg.setInfo(loc.x, loc.y);
                		
                		if (!msg.tryBroadcast())
                			messageQ.add(msg);
                	}
                	
                }
            }
        }
        
        return newSoupFound;
    }
	
	/**
	 * Get latest exploreMessage from the blockchain
	 * @throws GameActionException 
	 */
	static void getInitialExploreFromBlockchain() throws GameActionException {
		for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
			Message msg = new Message(tx);
			MessageType mt = msg.getMessageType();
			
			if (msg.isTeamMessage() && mt == MessageType.EXPLORE)
				exploreDest = msg.getLocation();
		}
	}
	
	/**
	 * Reads the previous round's block from blockchain and updates soupLocs, refineryLocs, and
	 * nearbyDesignSchool. Also recalculates nearestSoup and nearestRefinery.
	 * 
	 * @throws GameActionException
	 */
	private static void updateLocationsFromBlockchain() throws GameActionException {
		// Reading from blockchain
		for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
			Message msg = new Message(tx);
			MessageType mt = msg.getMessageType();
			
			if (msg.isTeamMessage()) {
				
				if (mt == MessageType.SOUP_LOCATION) {
					soupLocations.add(msg.getLocation());
				} else if (mt == MessageType.REFINERY_CREATED) {
					refineryLocations.add(msg.getLocation());
				} else if (mt == MessageType.SOUP_GONE) {
					soupLocations.remove(msg.getLocation());
					
					if (msg.getLocation() == nearestSoup)
						nearestSoup = null;
					
				} else if (mt == MessageType.DESIGN_SCHOOL_CREATED) {
					MapLocation schoolLoc = msg.getLocation();
					
					MapLocation currLoc = rc.getLocation();
					if (currLoc.distanceSquaredTo(schoolLoc) < Math.pow(rc.getMapWidth()/6, 2)) {
						nearbyDesignSchool = true;
					}
					
				}
				
			}
		}
		
		// Calculate new nearest soup and refinery
		MapLocation currLoc = rc.getLocation();
		int distToNearestSoup = nearestSoup != null ? 
				currLoc.distanceSquaredTo(nearestSoup) : Integer.MAX_VALUE;
		int distToNearestRefinery = nearestRefinery != null ?
				currLoc.distanceSquaredTo(nearestRefinery) : Integer.MAX_VALUE;
				
		for (MapLocation loc : soupLocations) {
			int newDist = currLoc.distanceSquaredTo(loc);
			if (newDist < distToNearestSoup) {
				distToNearestSoup = newDist;
				nearestSoup = loc;
			}
		}
		
		for (MapLocation loc : refineryLocations) {
			int newDist = currLoc.distanceSquaredTo(loc);
			if (newDist < distToNearestRefinery) {
				distToNearestRefinery = newDist;
				nearestRefinery = loc;
			}
		}
	}
}
