package mbot;

import battlecode.common.*;

public class Landscaper extends RobotPlayer {
    static void run() throws GameActionException {
//        if(rc.getDirtCarrying() == 0){
//            tryDig();
//        }
//
//        MapLocation bestPlaceToBuildWall = null;
//        // find best place to build
//        if(hqLoc != null) {
//            int lowestElevation = 9999999;
//            for (Direction dir : directions) {
//                MapLocation tileToCheck = hqLoc.add(dir);
//                if(rc.getLocation().distanceSquaredTo(tileToCheck) < 4
//                        && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
//                    if (rc.senseElevation(tileToCheck) < lowestElevation) {
//                        lowestElevation = rc.senseElevation(tileToCheck);
//                        bestPlaceToBuildWall = tileToCheck;
//                    }
//                }
//            }
//        }
//
//        if (Math.random() < 0.4){
//            // build the wall
//            if (bestPlaceToBuildWall != null) {
//                rc.depositDirt(rc.getLocation().directionTo(bestPlaceToBuildWall));
//                System.out.println("building a wall");
//            }
//        }
//
//        // otherwise try to get to the hq
//        if(hqLoc != null){
//            goTo(hqLoc);
//        } else {
//            tryMove(randomDirection());
//        }
    }
}
