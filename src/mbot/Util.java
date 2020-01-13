package mbot;

import java.util.HashSet;
import java.util.PriorityQueue;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import mbot.Communication.Message;
import mbot.Communication.MessageType;

public class Util extends RobotPlayer {	
    static void tryFindHQ() throws GameActionException {
        // search surroundings for HQ
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                hqLoc = robot.location;
            }
        }
        if(hqLoc == null) {
        	
            getHqLocFromBlockchain();
        }
    }
	
    public static boolean getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++) {
            for(Transaction tx : rc.getBlock(i)) {
                Message msg = new Message(tx);
                if(msg.isTeamMessage() && msg.getMessageType() == MessageType.HQ_FOUND) {
                    hqLoc = msg.getLocation();
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
	
    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
    	
    	// no drowning on my watch
    	boolean flooded = rc.senseFlooding(rc.getLocation().add(dir));
    	
        if (rc.isReady() && rc.canMove(dir) && !flooded) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    /**
     * Attempts to dig dirt in a random direction.
     * 
     * 
     * @return
     * @throws GameActionException
     */
    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to dig dirt in a given direction.
     * 
     * @param dir
     * @return
     * @throws GameActionException
     */
    static boolean tryDig(Direction dir) throws GameActionException {
    	if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to deposit dirt in a given direction.
     * 
     * @param dir
     * @return
     * @throws GameActionException
     */
    static boolean tryDeposit(Direction dir) throws GameActionException {
    	if (rc.canDepositDirt(dir)) {
    		rc.depositDirt(dir);
    		return true;
    	}
    	return false;
    }
    
    /**
     * Move towards a specified location while zigzagging, maximimizing exploration
     * 
     * @param location The intended destination
     * @return false if arrived at destination
     * @throws GameActionException
     */
    static boolean exploreMove(MapLocation location) throws GameActionException {
        // TODO: exploreMove (we use goTo as a substitute for now)
        goTo(location);
        if (rc.getLocation().isAdjacentTo(location))
            return false;
        return true;
    }
    
 // tries to move in the general direction of dir
    static boolean goTo(Direction dir) throws GameActionException {
        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
        for (Direction d : toTry){
            if(tryMove(d))
                return true;
        }
        return false;
    }

    // navigate towards a particular location
    static boolean goTo(MapLocation destination) throws GameActionException {
        return goTo(rc.getLocation().directionTo(destination));
    }
    
    static boolean nearbyRobot(RobotType target) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo r : robots) {
            if(r.getType() == target) {
                return true;
            }
        }
        return false;
    }
    
	/**
	 * Remove duplicates from messageQ. A nonoptimal solution.
	 */
	static void cleanMessageQ() {
		messageQ = new PriorityQueue<Message>(new HashSet<Message>(messageQ));
	}
    
    static void printAction(String action) {
    	int id = rc.getID();
    	RobotType t = rc.getType();
    	MapLocation loc = rc.getLocation();
    	
    	System.out.println(String.format("%s(%d) %s at (%s, %s)", t, id, action, loc.x, loc.y));
    }
}
