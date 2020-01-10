package mbot;

import battlecode.common.*;

public class Miner extends RobotPlayer {
	  /**
	   * TODO implement communication
	   * TODO implement refining
	   * TODO implement mining
	   * TODO implement building
	   * - if HQ is too far, build refinery
	   * 
	   * @throws GameActionException
	   */
	  static void run() throws GameActionException {
	        tryBlockchain();
	        // Try refining
	        for (Direction dir : directions) {
	        	if (tryRefine(dir)) 
	        		printAction("refined soup");
	        }
	        // Try mining
	        for (Direction dir : directions) {
	            if (tryMine(dir))
	            	printAction("mined " + rc.getSoupCarrying() + " soup");
	        }
	        // Try building design school
	        
	        // Return to HQ
	        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
	        	if (goTo(hqLoc))
	        		printAction("moved towards HQ");
	        }
	    }
    
}
