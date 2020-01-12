package mbot;

import java.util.*;

import battlecode.common.*;
import mbot.Communication.*;
import mbot.RobotPlayer;

public class HQ extends RobotPlayer {
	static ArrayList<MapLocation> soupLocations = new ArrayList<>();
	static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
	static int enemyHighestCost;
	
	static final MapLocation location = rc.getLocation();
	
	/**
	 * TODO blockchain handling
	 * 
	 * 
	 * @throws GameActionException
	 */
    static void run() throws GameActionException {
    	
		broadcastLocationIfNotInBlockchain();
		
		
    }
    
    static void sendMiners() {
    	int mapWidth = rc.getMapWidth();
    	int mapHeight = rc.getMapHeight();
    	
    	
    }
    
    /**
     * A one-time function that broadcasts the HQ location to the blockchain
     * if it is not in there already.
     * 
     * @throws GameActionException
     */
    static boolean locationInBlockchain = false;
    static void broadcastLocationIfNotInBlockchain() throws GameActionException {
    	if (locationInBlockchain) return;
    	
		// Check again if location is in blockchain
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                
            	Message m = new Message(tx);
            	MessageType type = m.getMessageType();
            	
            	if (m.isTeamMessage() && type == MessageType.HQ_FOUND) {
            		locationInBlockchain = true;
            		return;
            	}
            }
        }
        
        // Try to broadcast again
        new HQFoundMessage().setInfo(location.x, location.y).tryBroadcast();
    }
}

