package mbot;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static MapLocation hqLoc;
    
    static int numMiners = 0;
    static int numLandscapers = 0;
    static int numSchools = 0;
    static int numCenters = 0;
    static int numDesignSchools = 0;
    
    static int defaultBid = 1;
    
    static final int TEAM_SECRET = 420;
    
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            try {
//              System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 HQ.run();                break;
                    case MINER:             
                    	findHQ();
                    	Miner.run();             
                    	break;
                    case REFINERY:           Refinery.run();          break;
//                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:
                    	findHQ();
                    	DesignSchool.run();
                    	break;
//                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:
                    	findHQ();
                    	Landscaper.run();
                    	break;
//                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
//                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
	}
	
    static void findHQ() throws GameActionException {
        if (hqLoc == null) {
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

    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
    
    static boolean tryDig(Direction dir) throws GameActionException {
    	if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    public static boolean broadcastMessage(int soupCost, int xLoc, int yLoc, int ts, int messageType, int m4, int m5, int m6) throws GameActionException {
        int[] message = new int[7];
        message[0] = xLoc; // xLoc
        message[1] = yLoc; // yLoc
        message[2] = ts; // teamSecret
        message[3] = messageType; // messageType
        message[4] = m4;
        message[5] = m5; // val2
        message[6] = m6; // val3
        if (rc.canSubmitTransaction(message, soupCost)) {
            rc.submitTransaction(message, soupCost);
            return true;
        }
        return false;
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
    
    public static boolean getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++) {
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[2] == TEAM_SECRET && mess[3] == 4){
                    hqLoc = new MapLocation(mess[0], mess[1]);
                    return true;
                }
            }
        }
        return false;
    }
    
    static void printAction(String action) {
    	int id = rc.getID();
    	RobotType t = rc.getType();
    	MapLocation loc = rc.getLocation();
    	
    	System.out.println(String.format("%s(%d) %s at (%s, %s)", t, id, action, loc.x, loc.y));
    }
}
