package mbot;

import java.util.*;

import battlecode.common.*;
import mbot.RobotPlayer;

public class HQ extends RobotPlayer {
	static ArrayList<MapLocation> soupLocations = new ArrayList<>();
	static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
	static int enemyHighestCost;
	/**
	 * TODO blockchain handling
	 * 
	 * 
	 * @throws GameActionException
	 */
    static void run() throws GameActionException {
    	// Handle incoming Transactions
    	for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            
            // If team transaction
            if (mess[2] == TEAM_SECRET) {
            	if (mess[3] == 1) { // Add new soup location
            		
            	} else if (mess[3] == 2) { // add new refinery location
            		
            	}
            } else {
            	// if econ is good, start trying to outbid enemy
            	int teamSoup = rc.getTeamSoup();
            	int enemyBid = tx.getCost();
            	
            	
            	if (teamSoup > 2000 && enemyBid > defaultBid) {
            		defaultBid = enemyBid + 1;
            	} else if (teamSoup < 2000) {
            		defaultBid = 1;
            	}

            	if (teamSoup > 1000 && turnCount < 200 && enemyBid > defaultBid) {
            		defaultBid = enemyBid + 1;
            	}
            }
            
        }
    	
        if(numMiners < 4 && turnCount < 100) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) {
                	// add one to miner count
                    numMiners++;
                }
        } else if (rc.getTeamSoup() > 500 && numMiners < 10) {
        	for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) {
                	// add one to miner count
                    numMiners++;
                }
        }
        
        // Potential Rush Defense?
        
       	sendLocation();
    }
    
    static void sendLocation() throws GameActionException {
    	MapLocation loc = rc.getLocation();
    	broadcastMessage(defaultBid, loc.x, loc.y, TEAM_SECRET, 4, 0, 0, 0);
    }
    
}

