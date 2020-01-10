package mbot;

import battlecode.common.*;

public class FulfillmentCenter extends RobotPlayer {
    static void run() throws GameActionException {
        for (Direction dir : directions)
            if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
            	numDrones++;
            }
    }
}
