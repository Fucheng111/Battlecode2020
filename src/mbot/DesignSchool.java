package mbot;

import battlecode.common.*;

public class DesignSchool extends RobotPlayer {
    static void run() throws GameActionException {
        for (Direction dir : directions)
            if (tryBuild(RobotType.LANDSCAPER, dir)) {
            	numLandscapers++;
            }
    }
}
