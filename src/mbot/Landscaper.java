package mbot;

import java.util.ArrayList;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
	static final int DEFAULT_MODE = 0;
	static boolean nearHQ = false;
	// Deposit locations for second row landscapers
	static ArrayList<MapLocation> secondRowLocations;
	
	static int mode = 0; 
	
    static void run() throws GameActionException {

    	if (mode == DEFAULT_MODE)
    		runDefaultMode();
    	
    }
    
    /**
     * Default mode is two-layer defense. <br>
     * The landscaper starts by trying to find HQ in the blockchain and then
     * initializing the second row positions (first row are not needed). Then it 
     * moves into an unfilled position and starts to build a wall adjacent to HQ.
     * 
     * @throws GameActionException 
     */
    static void runDefaultMode() throws GameActionException {
    	
    	// Find HQ and initialize second row locations
    	if (hqLoc == null) {
    		Util.tryFindHQ();
    	
    		initializeSecondRowLocations();
    	}
    	
    	MapLocation currLoc = rc.getLocation(); 	
    	
    	// Move if not in position
    	if (!nearHQ) {
    		Util.goTo(hqLoc);
    		currLoc = rc.getLocation();
    		
	    	// If adjacent to HQ stop moving, start digging
	    	// Otherwise, if in second row position, also stop moving and start digging
	    	if (currLoc.isAdjacentTo(hqLoc))
	    		nearHQ = true;
	    	else if (currLoc.isWithinDistanceSquared(hqLoc, 5)) {
	    		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2, rc.getTeam());
	    		boolean bothScapers = true;
	    		
	    		for (RobotInfo ri : nearbyRobots) {
	    			if (ri.getLocation().isAdjacentTo(hqLoc) && 
	    				ri.getType() != RobotType.LANDSCAPER)
	    				
	    				bothScapers = false;
	    		}
	    		
	    		if (bothScapers)
	    			nearHQ = true;
	    	}
    	}
    	
    	// Trump Inc.
    	if (nearHQ) {
    		if (rc.getDirtCarrying() == 0)
    			Util.tryDig(currLoc.directionTo(hqLoc).opposite());
    		else {
    			
    			// If adj, dump dirt on itself
    			if (currLoc.isAdjacentTo(hqLoc))
    				rc.depositDirt(Direction.CENTER);
    			
    			// Otherwise, alternate between which landscaper deposits directly next
    			// to itself or diagonally next to itself.
    			else {
    				Direction depositDir = null;
    				
    				for (int i = 0; i < secondRowLocations.size(); i++) {
    					if (currLoc.equals(secondRowLocations.get(i))) {
    						if (i % 2 == 0) {
    							depositDir = currLoc.directionTo(hqLoc);
    						} else {
    							depositDir = currLoc.directionTo(hqLoc).rotateLeft();
    						}
    					}
    				}
    				
    				if (depositDir != null && rc.canDepositDirt(depositDir)) 
    					rc.depositDirt(depositDir);
    			}		
    		}
    	}
    }

	private static void initializeSecondRowLocations() throws GameActionException {
        int rad = 5;
        
        for (int i = -rad; i < rad; i++) {
        	for (int j = -rad; j < rad; j++) {
        		MapLocation loc = hqLoc.translate(i, j);
        		if (hqLoc.distanceSquaredTo(loc) == 5)
        			secondRowLocations.add(loc);
        	}
        }
	}
    
    
}
