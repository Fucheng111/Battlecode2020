package jBot;
import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.PriorityQueue;

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
    static RobotType[] spawnedByMiner = {
        RobotType.REFINERY,             // 1
        RobotType.VAPORATOR,            // 2
        RobotType.DESIGN_SCHOOL,        // 3
        RobotType.FULFILLMENT_CENTER,   // 4
        RobotType.NET_GUN               // 5
    };
    static final int TEAM_SECRET = 789;

    static boolean waitMode = false;    // Ensures that miners don't keep trying to broadcast message 10
    static boolean broadcastedCreation;
    static int turnCount;
    static int numMiners = 0;
    static int numDrones = 0;
    static int numLandscapers = 0;
    static int unitsQueued = 0;
    static int robotMode = 0;
    static int robotNum;
    static Direction destDir;
    static MapLocation hqLoc;
    static MapLocation robotDest;
    static MapLocation lastBugPathLoc;
    static PrioirtyQueue<int[]> messageQ = new PriorityQueue<int[]>();
    static ArrayList<MapLocation> soupLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> vaporatorLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> designSchoolLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> fulfillmentCenterLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> netGunLocs = new ArrayList<MapLocation>();

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

        // Process transactions past the first round
        if (rc.getRoundNum() != 1) {
            for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
                int[] mess = tx.getMessage();
                if(mess[2]/100 == TEAM_SECRET) {
                    // Refinery created
                    if (mess[2]%100 == 1)
                        refineryLocs.add(new Maplocation(-mess[0], -mess[1]));
                    // etc. 
                }
            }
        }
        
        // Send location to landscapers???
        // We can do this at turn 15 or something random like that bc its not like we will be creating landscapers earlier than that
        if(turnCount == 1) {
            sendHqLoc(rc.getLocation());
        }

        // If there are a certain number of miners/refineries, build a miner
        if(numMiners <= 4 || (numMiners <= 10 && refineryLocs.size() >= 1)) {
            if(tryBuild(RobotType.MINER, randomDirection()))
                numMiners++;
        }

        // Commission miner movements

        // Send previous soup locations to miners upon their creation
        // Problem 1: we need to send it 10 turns after their made, so we need to keep track of when they're made
        // Problem 2: we need to condense all the coordinates into 6 integers

        // Commission buildings
    }

    static void runMiner() throws GameActionException {

        // Search surroundings for HQ upon spawn
        if (soupLocs.size() == 0) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ)
                    hqLoc = new MapLocation(robot.getLocation());
            }
        }

        // Attempt to write the messages still in the priority queue
        for (int[] message : messageQ)
            if (broadcastMessage(message))
                messageQ.remove(message);

        // Process transactions from the most recent block in the blockchain
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET) {
                // Add refinery to refinery ArrayList
                if (mess[2]%100 == 1)
                    refineryLocs.add(new MapLocation(-message[0], -message[1]));
                // Add soup to soup ArrayList
                else if (mess[2]%100 == 8)
                    soupLocs.add(new Maplocation(-message[0], -message[1]));
                // Remove soup from soup ArrayList
                else if (mess[2]%100 == 9)
                    soupLocs.remove(new Maplocation(-message[0], -message[1]));
                // Move towards location if HQ requests
                else if (mess[2]%100 == 11 && mess[3] == rc.getID()) {
                    robotDest = new MapLocation(-message[0], -message[1]);
                    robotMode = 0;
                    // If HQ requests a building (message[3] is not 0), then record the building type
                    if (message[4] != 0)
                        robotNum = message[4];
                    waitMode = false;
                }
            }
        }

        // Check nearby soup locations for if they still have soup
        // If a location is empty, remove it from soupLocs and broadcast
        for (MapLocation soupLoc : soupLocs) {
            if (rc.canSenseLocation(soupLoc) && rc.senseSoup(soupLoc) == 0) {
                soupLocs.remove(0);
                tryBroadcastMessage(3, soupLoc.x, soupLoc.y, 9, 0, 0, 0, 0);
            }
        }

        // Record current MapLocation
        MapLocation currLoc = rc.getLocation();

        // Wander towards destination mode (0)
        if (robotMode == 0) {
            // Bugmove towards destination until arrived
            if (!bugMove(robotDest)) {
                // Compute direction to destination
                destDir = currLoc.directionTo(robotDest);
                // Change to mine if there's soup
                if (rc.senseSoup(robotDest) != 0)
                    robotMode = 1;
                // Change to refine if there's a refinery
                else if (refineryLocs.contains(robotDest))
                    robotMode = 2;
                // Change to build if the destination is empty 
                else if (canBuildRobot(destDir) && robotType != null)
                    robotMode = 3;
                // Otherwise, wait a turn
                else
                    minerCantBuild();
            }
            // Search surrounding square (11x11 if no pollution) for soup
            int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = new MapLocation(curLoc.add(i,j));
                    if (rc.onTheMap(loc) && senseSoup(loc) != 0 && !soupLocs.contains(loc))
                        tryBroadcastMessage(3, loc.x, loc.y, 8, 0, 0, 0, 0);
                }
            }
        }
        
        // Mine soup mode (1)
        else if (robotMode == 1) {
            if (!tryMine(destDir)) {
                // If miner is full of soup, move back to the nearest refinery
                robotMode = 0;
                robotDest = hqLoc;
                int lowestDist = currLoc.distanceSquaredTo(robotDest);
                for (Maplocation loc : refineryLocs) {
                    if currLoc.distanceSquaredTo(loc) < lowestDist {
                        robotDest = loc;
                        lowestDist = currLoc.distanceSquaredTo(loc);
                    }
                }
            }
        }
        
        // Refine soup mode (2)
        else if (robotMode == 2) {
            if (tryRefine(destDir)) {
                tryBroadcastMessage(3, currLoc.x, currLoc.y, 10, rc.getID(), 0, 0, 0);
                robotMode == -1;
            }
        }
        
        // Build building mode (3)
        else if (robotMode == 3) {
            if (tryBuild(spawnedByMiner[robotNum-1], destDir)) {
                tryBroadcastMessage(3, robotDest.x, robotDest.y, robotNum, rc.getID(), 0, 0, 0);
                robotMode = -1;
            }
            else
                minerCantBuild();
        }
        // NOTE: -1 is the robotMode for "wait for a command from HQ"
    }

    static void runRefinery() throws GameActionException {

        // Don't broadcast location here because we want to broadcast it 10 turns earlier in HQ
    }

    static void runVaporator() throws GameActionException {

        // Broadcast message on creation
        if (!broadcastedCreation) {
            MapLocation myLoc = rc.getLocation();
            broadcastMessage(3, myLoc.x, myLoc.y, 3, 0, 0, 0, 0);
            broadcastedCreation = true;
        }
    }

    static void runDesignSchool() throws GameActionException {

        // Broadcast message on creation
        if (!broadcastedCreation) {
            MapLocation myLoc = rc.getLocation();
            broadcastMessage(3, myLoc.x, myLoc.y, 3, 0, 0, 0, 0);
            broadcastedCreation = true;
        }

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == 7 && mess[0] == rc.getID())
                unitsQueued++;
        }
        
        // Try to make a landscaper if one is queued
        if (unitsQueued > 0 && tryBuild(RobotType.LANDSCAPER, randomDirection())) {
            unitsQueued--;
        }
    }

    static void runFulfillmentCenter() throws GameActionException {

        // Broadcast message on creation
        if (!broadcastedCreation) {
            MapLocation myLoc = rc.getLocation();
            broadcastMessage(3, myLoc.x, myLoc.y, 4, 0, 0, 0, 0);
            broadcastedCreation = true;
        }

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == 6 && mess[0] == rc.getID())
                unitsQueued++;
        }
        
        // Try to make a drone if one is queued
        if (unitsQueued > 0 && tryBuild(RobotType.LANDSCAPER, randomDirection())) {
            unitsQueued--;
        }
    }

    static void runLandscaper() throws GameActionException {

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/100 == TEAM_SECRET) {
                if (mess[2]%100 == 1])
                    // Do something
                // etc.
            }
        }

        MapLocation bestPlaceToBuildWall = null;
        // find best place to build
        if (hqLoc != null) {
            int lowestElevation = 9999999;
            for (Direction dir : directions) {
                MapLocation tileToCheck = hqLoc.add(dir);
                if (rc.getLocation().distanceSquaredTo(tileToCheck) < 4
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

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/100 == TEAM_SECRET) {
                if (mess[2]%100 == 1])
                    // Do something
                // etc.
            }
        }

        // If not holding anything, look for enemy units to attack
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // Pick up any enemy units/cows within striking range
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            if (robots.length > 0)
                rc.pickUpUnit(robots[0].getID());
            // Otherwise path towards any enemy units/cows sensed
            else {
                robots = rc.senseNearbyRobots(rc.getLocation(), rc.getCurrentSensorRadiusSquared(), enemy);
                if (robots.length > 0)
                    bugMove(robots[0].getLocation());
                else
                    tryMove(randomDirection());
            }
        }
    }

    static void runNetGun() throws GameActionException {
        
        // Get the list of nearby robots, sorted by distance in increasing order
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getLocation(), rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            // Shoot the first delivery drone
            if (robot.getType() == RobotType.DELIVERY_DRONE) {
                int targetID = robot.getID();
                if (rc.canShootUnit(targetID)) {
                    rc.shootUnit(targetID);
                    return;
                }
            }
        }
    }

    /**
     * Makes the optimal bug move towards a location
     * 
     * @param location The intended destination
     * @return false if arrived at destination
     * @throws GameActionException
     */
    static boolean bugMove(MapLocation location) throws GameActionException {
        // TODO: Bugmove (we use goTo as a substitute for now)
        goTo(location);
        if (rc.getLocation().isAdjacentTo(location))
            return false;
        return true;
    }

    /**
     * Try to path towards a given location
     *
     * @param destination The intended destination
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean goTo(MapLocation destination) throws GameActionException {
        dir = rc.getLocation().directionTo(destination);
        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
        for (Direction d : toTry) {
            if(tryMove(d))
                return true;
        }
        return false;
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
        }
        return false;
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
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.canMineSoup(dir)) {
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
        if (rc.canDepositSoup(dir)) {
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
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }
    
    /**
     * Returns a random Direction
     *
     * @return a random Direction
     */
    static Direction randomDirection() throws GameActionException{
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns the "optimal" directon to spawn units facing away from the nearest walls
     * 
     * @param loc the location from which to spawn units
     * @return the direction to spawn units in
     * @throws GameActionException
     */
    static Direction optimalDirection(MapLocation loc) throws GameActionException{

    }

    /**
     * The function to execute when a miner can't build in a direction
     * If building/water, then broadcast that it needs a new destination and wait
     * Otherwise, do nothing and wait
     * 
     * @throws GameActionException
     */
    static void minerCantBuild() throws GameActionException {
        if ((rc.senseFlooding(robotDest) || rc.senseRobotAtLocation(robotDest).getType().isBuilding()) && !waitMode) {
            broadcastMessage(3, currLoc.x, currLoc.y, 10, rc.getID(), 0, 0, 0);
            waitMode = true;
        }
    }

    /* COMMUNICATIONS STUFF */
    static final String[] messageType = {
        "HQ location",                      // 00 - [xLoc, yLoc, code]
        "Refinery created",                 // 01 - [xLoc, yLoc, code]
        "Vaporator created",                // 02 - [xLoc, yLoc, code]
        "Design school created",            // 03 - [xLoc, yLoc, code, minerID]
        "Fulfillment center created",       // 04 - [xLoc, yLoc, code, minerID]
        "Net gun created",                  // 05 - [xLoc, yLoc, code]
        "Make center create drone",         // 06 - [centerID, 0, code]
        "Make school create landscaper",    // 07 - [schoolID, 0, code]
        "Soup location",                    // 08 - [xLoc, yLoc, code]
        "Soup gone",                        // 09 - [xLoc, yLoc, code]
        "Miner done need another task",     // 10 - [xLoc, yLoc, code, minerID]
        "Make miner go somewhere",          // 11 - [xLoc, yLoc, code, minerID, (optional) buildingID]
        "List of soup locations",           // 12 - [uh oh]
    };

    /**
     * Generic message broadcasting template
     * See the function for the required message format
     *
     * @return true if a message was broadcasted
     * @throws GameActionException
     */
    public static boolean broadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, 
        int m6) throws GameActionException {
        int[] message = new int[7];
        message[0] = -1 * m0; // -xLoc (negated for encryption)
        message[1] = -1 * m1; // -yLoc (negated for encryption)
        message[2] = TEAM_SECRET * 100 + m2; // teamCode + messageCode (i.e. 78901 is HQ location)
        message[3] = m3; // val1
        message[4] = m4; // val2
        message[5] = m5; // val3
        message[6] = m6; // val4
        if (rc.canSubmitTransaction(message, soupCost)) {
            rc.submitTransaction(message, soupCost);
            return true;
        }
        return false;
    }

    /**
     * Broadcast a message from an int[] array, specifically for the messageQ
     *
     * @return true if a message was broadcasted
     * @throws GameActionException
     */
    public static boolean broadcastMessage(int[] m) throws GameActionException {
        return broadcastMessage(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7]);
    }

    /**
     * Try to broadcast a message; if unsuccessful, save into messageQ
     *
     * @throws GameActionException
     */
    public static void tryBroadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, 
        int m6) throws GameActionException {
        if (!broadcastMessage(soupCost, m0, m1, m2, m3, m4, m5, m6)) {
            int[] saveMess = new int[8];
            saveMess[0] = soupCost;
            saveMess[1] = m0;
            saveMess[2] = m1
            saveMess[3] = m2;
            saveMess[4] = m3;
            saveMess[5] = m4;
            saveMess[6] = m5;
            saveMess[7] = m6;
            messageQ.add(saveMess);
        }
    }  
}