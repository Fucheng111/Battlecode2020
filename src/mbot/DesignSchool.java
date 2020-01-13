package mbot;

import battlecode.common.*;

public class DesignSchool extends RobotPlayer {
	
    static void run() throws GameActionException {
    	
    	if (rc.getTeamSoup() > 300)
	        for (Direction dir : directions)
	            if (Util.tryBuild(RobotType.LANDSCAPER, dir)) {
	            	numLandscapers++;
	            	break;
	            }
    	
    }
}
