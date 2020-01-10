package mbot;

import battlecode.common.*;
import mbot.RobotPlayer;

public class HQ extends RobotPlayer {
	/**
	 * TODO blockchain handling
	 * 
	 * 
	 * @throws GameActionException
	 */
    static void run() throws GameActionException {
        if(numMiners < 10) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) {
                	// add one to miner count
                    numMiners++;
                }
        }
    }
    
}

