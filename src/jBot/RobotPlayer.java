package jBot;
import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;

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
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
    static final int TEAM_SECRET = 789;

    static int turnCount;
    static int numMiners = 0;
    static int numDesignSchools = 0;
    static int robotMode = 0;
    static MapLocation hqLoc;
    static MapLocation soupLoc;
    static ArrayList<MapLocation> soupLocs = new ArrayList<MapLocation>();

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }
        if(turnCount == 1) {
            sendHqLoc(rc.getLocation());
        }
        if(numMiners < 10) {
            for (Direction dir : directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners++;
                }
        }
    }

    static void runMiner() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }

        // search surroundings for HQ upon spawn
        if (hqLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
        }

        // Check if each soup location is empty; if it is, remove it from soupLocs and broadcast
        for (MapLocation soupLoc : soupLocs) {
            if (rc.canSenseLocation(soupLoc) && rc.senseSoup(soupLoc) == 0) {
                soupLocs.remove(0);
                broadcastMessage(3, soupLoc.x, soupLoc.y, TEAM_SECRET, 9, 0, 0, 0);
            }
        }
        // Wander and find soup mode (0)
        
        // Move towards soup mode (1)
        if (robotMode == 1) {
            
        }
        // Mine soup mode (2)
        else if (robotMode == 1) {
            
        }
        // Find refinery mode (3)
        
        // Refine soup mode (4)
        for (Direction dir : directions)
            if (tryRefine(dir))
                System.out.println("I refined soup! " + rc.getTeamSoup());
        // Build building mode (5)
        
        for (Direction dir : directions)
            if (tryMine(dir)) {
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (!soupLocs.contains(soupLoc)) {
                    broadcastSoupLocation(soupLoc);
                }
            }
        if (numDesignSchools < 3){
            if(tryBuild(RobotType.DESIGN_SCHOOL, randomDirection()))
                System.out.println("created a design school");
        }

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            // time to go back to the HQ
            if(goTo(hqLoc))
                System.out.println("moved towards HQ");
        } else if (soupLocs.size() > 0) {
            goTo(soupLocs.get(0));
        } else if (goTo(randomDirection())) {
            // otherwise, move randomly as usual
            System.out.println("I moved randomly!");
        }
    }

    static void runRefinery() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }
        
    }

    static void runVaporator() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }

    }

    static void runDesignSchool() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }
        if (!broadcastedCreation) {
            broadcastDesignSchoolCreation(rc.getLocation());
        }
        for (Direction dir : directions)
            if(tryBuild(RobotType.LANDSCAPER, dir))
                System.out.println("made a landscaper");
    }

    static void runFulfillmentCenter() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }
        if(rc.getDirtCarrying() == 0){
            tryDig();
        }

        MapLocation bestPlaceToBuildWall = null;
        // find best place to build
        if(hqLoc != null) {
            int lowestElevation = 9999999;
            for (Direction dir : directions) {
                MapLocation tileToCheck = hqLoc.add(dir);
                if(rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                        && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                    if (rc.senseElevation(tileToCheck) < lowestElevation) {
                        lowestElevation = rc.senseElevation(tileToCheck);
                        bestPlaceToBuildWall = tileToCheck;
                    }
                }
            }
        }

        if (Math.random() < 0.4){
            // build the wall
            if (bestPlaceToBuildWall != null) {
                rc.depositDirt(rc.getLocation().directionTo(bestPlaceToBuildWall));
                System.out.println("building a wall");
            }
        }

        // otherwise try to get to the hq
        if(hqLoc != null){
            goTo(hqLoc);
        } else {
            tryMove(randomDirection());
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[3] == 1){
                // Do something
            }
        }
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {
        // Come up for something for this to do besides just shoot things down
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
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

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
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
        }
        return false;
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
        }
        return false;
    }
    
    /**
     * Attempts to dig dirt in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDig(Direction dir) throws GameActionException {
        if(rc.isReady() && rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    /* COMMUNICATIONS STUFF */
    static final String[] messageType = {
        "HQ location",                  // 0
        "Refinery created",             // 1
        "Design school created",        // 2
        "Fulfillment center created",   // 3
        "Net gun created",              // 4
        "Vaporator created",            // 5
        "Delivery Drone spawned",       // 6
        "Landscaper spawned",           // 7
        "Soup location",                // 8
        "Soup gone",                    // 9
    };

    // Broadcasts any message, see below for specific format
    // Returns true iff message was succesfully submitted
    public static boolean broadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, int m6) throws GameActionException {
        int[] message = new int[7];
        message[0] = m0; // xLoc
        message[1] = m1; // yLoc
        message[2] = m2; // teamCode
        message[3] = m3; // messageType
        message[4] = m4; // val1
        message[5] = m5; // val2
        message[6] = m6; // val3
        if (rc.canSubmitTransaction(message, soupCost)) {
            rc.submitTransaction(message, soupCost);
            return true;
        }
        return false;
    }

    public static void sendHqLoc(MapLocation loc) throws GameActionException {
        broadcastMessage(3, loc.x, loc.y, TEAM_SECRET, 0, 0, 0, 0);
    }

    public static boolean broadcastedCreation = false;
    public static void broadcastDesignSchoolCreation(MapLocation loc) throws GameActionException {
        broadcastedCreation = broadcastMessage(3, loc.x, loc.y, TEAM_SECRET, 1, 0, 0, 0);
    }

    public static void broadcastSoupLocation(MapLocation loc) throws GameActionException {
        broadcastMessage(3, loc.x, loc.y, TEAM_SECRET, 2, 0, 0, 0);
    }

    public static void updateSoupLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2] == TEAM_SECRET && mess[1] == 2){
                System.out.println("heard about a tasty new soup location");
                soupLocs.add(new MapLocation(mess[2], mess[3]));
            }
        }
    }
}
