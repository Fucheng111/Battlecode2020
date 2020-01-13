package jBot;
import battlecode.common.*;
import java.lang.Math;
import java.util.Arrays;
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
    static boolean nearHQ = false;
    static boolean leftTendency = false;
    static boolean carryingCow = false;
    static int turnCount;
    static int numMiners = 0;
    static int numDrones = 0;
    static int numLandscapers = 0;
    static int unitsQueued = 0;
    static int robotMode = -1;          // Default mode of -1 is the "do nothing" mode
    static int robotNum;
    static Direction destDir;
    static MapLocation hqLoc;
    static MapLocation enemyHQLoc;
    static MapLocation robotDest;
    static MapLocation lastBugPathLoc;
    static PriorityQueue<int[]> messageQ = new PriorityQueue<int[]>();
    static ArrayList<MapLocation> soupLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> waterLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> secondRowLocations = new ArrayList<MapLocation>();

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

        // At the beginning of the game...
        if(rc.getRoundNum() == 1) {
            // Set it's own location as hqLoc
            hqLoc = rc.getLocation();
            // Try to broadcast it's location with 1 soup until it can
            tryBroadcastMessage(1, hqLoc.x, hqLoc.y, 0, 0, 0, 0, 0);
            // Search surrounding square for soup and water
            int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = hqLoc.translate(i,j);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0)
                            soupLocs.add(loc);
                        else if (rc.senseFlooding(loc))
                            waterLocs.add(loc)
                    }
                }
            }
        }

        tryBroadcastQueue();

        // Process transactions past the first round
        if (turnCount != 1) {
            for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
                int[] mess = tx.getMessage();
                if(mess[2]/100 == TEAM_SECRET) {
                    // Enemy HQ Found
                    if (enemyHQLoc == null && mess[2]%100 == 13)
                        enemyHQLoc = new MapLocation(-mess[0], -mess[1]);

                    // Refinery created
                    else if (mess[2]%100 == 1 && !refineryLocs.contains(new MapLocation(-mess[0], -mess[1])))
                        refineryLocs.add(new MapLocation(-mess[0], -mess[1]));
                    // Refinery destroyed
                    else if (mess[2]%100 == 18)
                        refineryLocs.remove(new MapLocation(-mess[0], -mess[1]));
                    // Soup found
                    else if (mess[2]%100 == 8 && !soupLocs.contains(new MapLocation(-mess[0], -mess[1])))
                        soupLocs.add(new MapLocation(-mess[0], -mess[1]));
                    // Soup gone
                    else if (mess[2]%100 == 9)
                        soupLocs.remove(new MapLocation(-mess[0], -mess[1]));
                    // Water found
                    else if (mess[2]%100 == 20 && !waterLocs.contains(new MapLocation(-mess[0], -mess[1])))
                        waterLocs.add(new MapLocation(-mess[0], -mess[1]));
                    // Water gone
                    else if (mess[2]%100 == 21)
                        waterLocs.remove(new MapLocation(-mess[0], -mess[1]));
                }
            }
        }
        
        // If there are a certain number of miners/refineries, build a miner
        if(numMiners <= 4 || (numMiners <= 10 && refineryLocs.size() >= 1)) {
            if(tryBuildAround(RobotType.MINER, randomDirection())) {
                broadcastLocs(soupLocs, 17);
                broadcastLocs(refineryLocs, 12);
                numMiners++;
            }
        }

        // Commission miner movements (go to location or build) based on soupLocs and other stuff

        // Commission buildings movements
    }

    static void runMiner() throws GameActionException {

        // Search surroundings for HQ upon spawn
        if (turnCount == 1) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots)
                if (robot.getType() == RobotType.HQ)
                    hqLoc = robot.getLocation();
        }

        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET) {
                // Receive initial list of refinery/soup/water locations
                if (mess[2]%100 == 12 || mess[2]%100 == 17 || mess[2]%100 == 19) {
                    switch(mess[2]%100) {
                        case 12: decodeLocsMessage(mess, refineryLocs); break;
                        case 17: decodeLocsMessage(mess, soupLocs);     break;
                        case 19: decodeLocsMessage(mess, waterLocs);    break;
                    }
                }

                // Refinery created
                else if (mess[2]%100 == 1 && !refineryLocs.contains(new MapLocation(-mess[0], -mess[1])))
                    refineryLocs.add(new MapLocation(-mess[0], -mess[1]));
                // Refinery destroyed
                else if (mess[2]%100 == 18)
                    refineryLocs.remove(new MapLocation(-mess[0], -mess[1]));
                // Soup found
                else if (mess[2]%100 == 8 && !soupLocs.contains(new MapLocation(-mess[0], -mess[1])))
                    soupLocs.add(new MapLocation(-mess[0], -mess[1]));
                // Soup gone
                else if (mess[2]%100 == 9)
                    soupLocs.remove(new MapLocation(-mess[0], -mess[1]));
                // Water found
                else if (mess[2]%100 == 20 && !waterLocs.contains(new MapLocation(-mess[0], -mess[1])))
                    soupLocs.add(new MapLocation(-mess[0], -mess[1]));
                // Water gone
                else if (mess[2]%100 == 21)
                    soupLocs.remove(new MapLocation(-mess[0], -mess[1]));

                // Move towards location if HQ requests
                else if (mess[2]%100 == 11 && mess[3] == rc.getID()) {
                    robotDest = new MapLocation(-mess[0], -mess[1]);
                    if (mess[4] == -1)
                        robotMode = 1;
                    else if (mess[4] == 0)
                        robotMode = 0;
                    else
                        robotNum = mess[4];
                    waitMode = false;
                }
            }
        }

        checkForEnemyHQ();
        MapLocation currLoc = rc.getLocation();

        // Explore mode
        if (robotMode == 0) {
            if (!exploreMove(robotDest))
                tryBroadcastMessage(3, currLoc.x, currLoc.y, 10, rc.getID(), 0, 0, 0);
            // Change to mode 1 and update robotDest if soup is found
            if (checkForSoup()) {
                robotMode = 1;
                int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
                for (int i=-rad; i<=rad; i++) {
                    for (int j=-rad; j<=rad; j++) {
                        MapLocation loc = currLoc.translate(i,j);
                        if (rc.canSenseLocation(loc) && rc.senseSoup(loc) != 0 && !soupLocs.contains(loc)) {
                            robotDest = loc;
                            tryBroadcastMessage(3, loc.x, loc.y, 8, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }

        // Bug move towards destination mode (0)
        if (robotMode == 1) {
            // Bugmove towards destination until arrived
            if (!bugMove(robotDest)) {
                // Compute direction to destination
                destDir = currLoc.directionTo(robotDest);
                // Change to mine if there's soup
                if (rc.senseSoup(robotDest) != 0)
                    robotMode = 1;
                // Change to refine if there's a refinery
                else if (rc.senseRobotAtLocation(robotDest).getType() == RobotType.REFINERY)
                    robotMode = 2;
                // Change to build if the destination is empty 
                else if (robotNum != 0 && rc.canBuildRobot(spawnedByMiner[robotNum-1], destDir))
                    robotMode = 3;
                // Otherwise, wait a turn
                else
                    minerCantBuild(currLoc);
            }
        }
        
        // Mine soup mode (1)
        else if (robotMode == 2) {
            if (!tryMine(destDir)) {
                // If miner is full of soup, move back to the nearest refinery
                robotMode = 0;
                robotDest = hqLoc;
                int lowestDist = currLoc.distanceSquaredTo(robotDest);
                for (MapLocation loc : refineryLocs) {
                    if (currLoc.distanceSquaredTo(loc) < lowestDist) {
                        robotDest = loc;
                        lowestDist = currLoc.distanceSquaredTo(loc);
                    }
                }
            }
        }
        
        // Refine soup mode (2)
        else if (robotMode == 3) {
            if (tryRefine(destDir)) {
                tryBroadcastMessage(3, currLoc.x, currLoc.y, 10, rc.getID(), 0, 0, 0);
                robotMode = -1;
            }
        }
        
        // Build building mode (4)
        else if (robotMode == 4) {
            if (tryBuild(spawnedByMiner[robotNum-1], destDir)) {
                tryBroadcastMessage(3, robotDest.x, robotDest.y, robotNum, rc.getID(), 0, 0, 0);
                robotMode = -1;
            }
            else
                minerCantBuild(currLoc);
        }

        checkSoupWater();
    }

    static void runRefinery() throws GameActionException {
    }

    static void runVaporator() throws GameActionException {

        // Upon spawn...
        if (turnCount == 1) {
            tryBroadcastMessage(1, rc.getLocation().x, rc.getLocation().y, 2, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();
    }

    static void runDesignSchool() throws GameActionException {

        // Upon spawn...
        if (turnCount == 1) {
            tryBroadcastMessage(1, rc.getLocation().x, rc.getLocation().y, 3, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();

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

        // Upon spawn...
        if (turnCount == 1) {
            tryBroadcastMessage(1, rc.getLocation().x, rc.getLocation().y, 4, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();

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

        // Upon spawning...
        if (hqLoc == null) {
            getHqLocFromBlockchain();
            tryBroadcastMessage(1, rc.getLocation().x, rc.getLocation().y, 14, rc.getID(), 0, 0, 0);
        }

        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/100 == TEAM_SECRET) {
                // Receive commands from HQ
                if (mess[3]%100 == 16)
                    robotMode = mess[4];
                // Enemy HQ Found
                if (enemyHQLoc == null && mess[2]%100 == 13)
                    enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
            }
        }

        checkForEnemyHQ();

        // Two-layer defense mode (0)
        if (robotMode == 0) {
            initializeSecondRowLocations();
            MapLocation currLoc = rc.getLocation();

            // Move if not in position
            if (!nearHQ) {
                bugMove(hqLoc);
                currLoc = rc.getLocation();
                // If adjacent to HQ stop moving, start digging
                if (currLoc.isAdjacentTo(hqLoc))
                    nearHQ = true;
                // If in second row position and first row is in place, also start digging
                else if (currLoc.isWithinDistanceSquared(hqLoc, 5)) {
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2, rc.getTeam());
                    boolean bothScapers = true;
                    for (RobotInfo ri : nearbyRobots)
                        if (ri.getLocation().isAdjacentTo(hqLoc) && ri.getType() != RobotType.LANDSCAPER)
                            bothScapers = false;
                    if (bothScapers)
                        nearHQ = true;
                }
            }

            // Trump Inc.
            else {
                if (rc.getDirtCarrying() == 0)
                    tryDig(currLoc.directionTo(hqLoc).opposite());
                else {
                    // If adj, dump dirt on itself
                    if (currLoc.isAdjacentTo(hqLoc))
                        rc.depositDirt(Direction.CENTER);
                    // Otherwise, alternate between which landscaper deposits directly next
                    // to itself or diagonally next to itself.
                    else {
                        Direction depositDir = null;
                        for (int i = 0; i < secondRowLocations.size(); i++) {
                            if (currLoc.equals(secondRowLocations.get(i))) {
                                if (i % 2 == 0)
                                    depositDir = currLoc.directionTo(hqLoc);
                                else
                                    depositDir = currLoc.directionTo(hqLoc).rotateLeft();
                            }
                        }
                        if (depositDir != null && rc.canDepositDirt(depositDir)) 
                            rc.depositDirt(depositDir);
                    }       
                }
            }
        }

        else if (robotMode == 1) {
            // TODO: add something else for the landscaper to do
        } 

        /* FROM LECTUREPLAYER
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
        */

        // Otherwise try to get to the hq
        else
            bugMove(hqLoc);
    }

    static void runDeliveryDrone() throws GameActionException {

        // Upon spawning...
        if (hqLoc == null) {
            getHqLocFromBlockchain();
            tryBroadcastMessage(1, rc.getLocation().x, rc.getLocation().y, 15, rc.getID(), 0, 0, 0);
        }

        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/100 == TEAM_SECRET) {
                // Process initial list of water locations
                if (mess[2]%100 == 19)
                    decodeLocsMessage(mess, waterLocs);

                // Water found
                else if (mess[2]%100 == 20 && !waterLocs.contains(new MapLocation(-mess[0], -mess[1])))
                    waterLocs.add(new MapLocation(-mess[0], -mess[1]));
                // Water gone
                else if (mess[2]%100 == 21)
                    waterLocs.remove(new MapLocation(-mess[0], -mess[1]));

                // Receive commands from HQ
                else if (mess[2]%100 == 22 && mess[3] == rc.getID())
                    robotMode = mess[4];

                // Enemy HQ Found
                if (enemyHQLoc == null && mess[2]%100 == 13)
                    enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
            }
        }
 
        checkForEnemyHQ();
        checkSoupWater();

        // If holding something
        if (rc.isCurrentlyHoldingUnit()) {
            // Drop off unit if possible
            MapLocation currLoc = rc.getLocation();
            destDir = currLoc.directionTo(robotDest);
            if (currLoc.isAdjacentTo(robotDest) && rc.canDropUnit(destDir)) {
                rc.dropUnit(destDir);
                robotDest = hqLoc;
            }
            // Otherwise, keep moving towards destination
            else
                bugMove(robotDest);
        }

        // If not holding anything, look for enemy units to attack
        Team enemy = rc.getTeam().opponent();
        else {
            // Pick up any enemy units/cows within striking range
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            if (robots.length > 0) {
                RobotInfo robot = robots[0];
                // If a cow is picked up, do our best to path towards enemy HQ
                if (robot.getType() == RobotType.COW) {
                    if (enemyHQLoc != null)
                        robotDest = enemyHQLoc;
                    else
                        robotDest = predictedEnemyHQ();
                }
                // If an enemy unit is picked up
                else {
                    // Path towards closest known water if any has been found
                    if (!waterLocs.empty()) {
                        robotDest = waterLocs.get(0);
                        int lowestDist = currLoc.distanceSquaredTo(robotDest);
                        for (int i=1; i<waterLocs.size(); i++) {
                            MapLocation waterLoc = waterLocs.get(i);
                            if (currLoc.distanceSquaredTo(waterLoc) < lowestDist) {
                                robotDest = waterLoc;
                                lowestDist = currLoc.distanceSquaredTo(waterLoc);
                            }
                        }
                    }  
                    // Path towards nearest corner if no water known (drone can get stuck in corner but oh well)
                    else 
                        robotDest = findNearestCorner();
                }
                rc.pickUpUnit(robot.getID());
            }
            // Otherwise path towards any enemy units sensed
            else {
                robots = rc.senseNearbyRobots(rc.getLocation(), rc.getCurrentSensorRadiusSquared(), enemy);
                if (robots.length > 0)
                    bugMove(robots[0].getLocation());
                else {
                    // Defense mode (0)
                    // Go towards robotDest (most likely hq) if not there already, otherwise wait
                    if (robotMode == 0) {
                        robotDest = hqLoc;
                    }
                    // Attack mode (1) 
                    else if (robotMode == 1) {
                        if (enemyHQLoc == null)
                            robotDest = predictedEnemyHQ();
                        else
                            robotDest = enemyHQLoc;
                    }
                    bugMove(robotDest);
                }
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
     * Move towards a specified location while prioritizing diagonal movements for exploration
     * 
     * @param destination The intended destination
     * @return false if arrived at destination
     * @throws GameActionException
     */
    static boolean exploreMove(MapLocation destination) throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        // Make best diagonal move towards destination
        Direction dir = rc.getLocation().directionTo(destination);
        // If dir is a cardinal direction, find the best diagonal move
        if (Arrays.asList(Direction.cardinalDirections()).contains(dir)) {
            if (dir == Direction.NORTH) {
                if (destination.x < currLoc.x)
                    dir = Direction.NORTHWEST;
                else if (destination.x > currLoc.x)
                    dir = Direction.NORTHEAST;
                else {
                    dir = leftTendency ? dir.rotateLeft() : dir.rotateRight();
                    leftTendency = !leftTendency;
                }
            }
            else if (dir == Direction.SOUTH) {
                if (destination.x < currLoc.x)
                    dir = Direction.SOUTHWEST;
                else if (destination.x > currLoc.x)
                    dir = Direction.SOUTHEAST;
                else {
                    dir = leftTendency ? dir.rotateLeft() : dir.rotateRight();
                    leftTendency = !leftTendency;
                }
            }
            else if (dir == Direction.EAST) {
                if (destination.y < currLoc.y)
                    dir = Direction.SOUTHEAST;
                else if (destination.x > currLoc.x)
                    dir = Direction.NORTHEAST;
                else {
                    dir = leftTendency ? dir.rotateLeft() : dir.rotateRight();
                    leftTendency = !leftTendency;
                }
            }
            else if (dir == Direction.WEST) {
                if (destination.y < currLoc.y)
                    dir = Direction.SOUTHWEST;
                else if (destination.x > currLoc.x)
                    dir = Direction.NORTHWEST;
                else {
                    dir = leftTendency ? dir.rotateLeft() : dir.rotateRight();
                    leftTendency = !leftTendency;
                }
            }
        }
        goTo(destination);
        return !rc.getLocation().isAdjacentTo(destination);
    }

    /**
     * Makes the optimal bug move towards a location
     * 
     * @param destination The intended destination
     * @return false if arrived at destination
     * @throws GameActionException
     */
    static boolean bugMove(MapLocation destination) throws GameActionException {
        // TODO: bugmove (we use goTo as a substitute for now)
        goTo(destination);
        return !rc.getLocation().isAdjacentTo(destination);
    }

    /**
     * Try to path towards a given location
     *
     * @param destination The intended destination
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean goTo(MapLocation destination) throws GameActionException {
        if (rc.getLocation() == destination)
            return false;
        Direction dir = rc.getLocation().directionTo(destination);
        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
        for (Direction d : toTry)
            if(tryMove(d))
                return true;
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
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuildAround(RobotType type, Direction dir) throws GameActionException {
        Direction[] toTry = {
            dir, 
            dir.rotateLeft(),
            dir.rotateRight(),
            dir.rotateLeft().rotateLeft(),
            dir.rotateRight().rotateRight(),
            dir.opposite().rotateLeft(),
            dir.opposite().rotateRight(),
            dir.opposite()
        };
        for (Direction d : toTry) {
            if (rc.canBuildRobot(type, d)) {
                rc.buildRobot(type, dir);
                return true;
            }
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
     * Useful to optimize the spawn direction of units
     * 
     * @param loc the location from which to spawn units
     * @return the direction to spawn units in
     * @throws GameActionException
     */
    static Direction optimalDirection(MapLocation loc) throws GameActionException{
        return randomDirection();
        // Is this really necessary?
    }

    /**
     * Find the best locations to send a certain number of miners to explore the map
     * 
     * @param numMiners the number of miners we have
     * @return an array of MapLocations to send the miners towards
     * @throws GameActionException
     */
    static MapLocation[] bestMinerLocs(int numMiners) throws GameActionException {

        int mapHeight = rc.getMapHeight();
        int mapWidth = rc.getMapWidth();
        
        // Find closest horizontal and vertical distance to an edge
        int closestX = Math.min(hqLoc.x, (mapWidth-1)-hqLoc.x);
        int closestY = Math.min(hqLoc.y, (mapHeight-1)-hqLoc.y);

        // Determine the widest 2 locations based on whether we are at a corner, edge, or center
        // destinations[0] will be on the "right", destinations[1] will be on the "right"
        MapLocation[] destinations = new MapLocation[numMiners];
        // Corner case - parallel with edges
        if (closestX < mapWidth/3 && closestY < mapHeight/3) {
            // Bottom left
            if (hqLoc.x < mapWidth/2 && hqLoc.y < mapHeight/2) {
                destinations[0] = new MapLocation(mapWidth-1, hqLoc.y);                
                destinations[1] = new MapLocation(hqLoc.x, mapHeight-1);
            }
            // Top left
            else if (hqLoc.x < mapWidth/2 && hqLoc.y >= mapHeight/2) {
                destinations[0] = new MapLocation(hqLoc.x, 0);
                destinations[1] = new MapLocation(mapWidth-1, hqLoc.y);
            }
            // Bottom right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y < mapHeight/2) {
                destinations[0] = new MapLocation(hqLoc.x, mapHeight-1);
                destinations[1] = new MapLocation(0, hqLoc.y);
            }
            // Top right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y >= mapHeight/2) {
                destinations[0] = new MapLocation(0, hqLoc.y);
                destinations[1] = new MapLocation(hqLoc.x, 0);
            }
        }
        // Center case - towards a point 5 (sensor radius) units away from the middle 2 distant corners
        else if (closestX >= mapWidth/3 && closestY >= mapHeight/3) {
            // Bottom left
            if (hqLoc.x < mapWidth/2 && hqLoc.y < mapHeight/2) {
                destinations[0] = new MapLocation(mapWidth-1, 5);
                destinations[1] = new MapLocation(5, mapHeight-1);
            }
            // Top left
            else if (hqLoc.x < mapWidth/2 && hqLoc.y >= mapHeight/2) {
                destinations[0] = new MapLocation(5, 0);
                destinations[1] = new MapLocation(mapWidth-1, mapHeight-6);
            }
            // Bottom right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y < mapHeight/2) {
                destinations[0] = new MapLocation(mapWidth-6, mapHeight-1);
                destinations[1] = new MapLocation(0, 5);
            }
            // Top right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y >= mapHeight/2) {
                destinations[0] = new MapLocation(0, mapHeight-6);
                destinations[1] = new MapLocation(mapWidth-6, 0);
            }
        }
        // Edge case (actual map edge not an "edge" case)
        else {
            // Special case for three miners
            // Find offset from middle and shift coordinates proportionally
            if (numMiners <= 3) {
                // Left or right
                int offset = 0;
                if (closestX < mapWidth/3)
                    offset = (hqLoc.y - mapHeight / 2) * mapWidth / mapHeight;
                // Bottom or top
                else
                    offset = (hqLoc.x - mapWidth / 2) * mapHeight / mapWidth;
                // Left edge
                if (hqLoc.x < mapWidth/3) {
                    destinations[0] = new MapLocation(mapWidth/2-offset, 0);
                    destinations[1] = new MapLocation(mapWidth/2+offset, mapHeight-1);
                }
                // Right edge
                else if (mapWidth-hqLoc.x-1 < mapWidth/3) {
                    destinations[0] = new MapLocation(mapWidth/2-offset, mapHeight-1);
                    destinations[1] = new MapLocation(mapWidth/2+offset, 0);
                }
                // Bottom edge
                else if (hqLoc.y < mapHeight/3) {
                    destinations[0] = new MapLocation(mapWidth-1, mapHeight/2+offset);
                    destinations[1] = new MapLocation(0, mapHeight/2-offset);
                }
                // Top edge
                else if (mapHeight-hqLoc.y-1 < mapHeight/3) {
                    destinations[0] = new MapLocation(0, mapHeight/2+offset);
                    destinations[1] = new MapLocation(mapWidth-1, mapHeight/2-offset);
                }
            }
            // Parallel with edge
            else {
                // Left edge
                if (hqLoc.x < mapWidth/3) {
                    destinations[0] = new MapLocation(hqLoc.x, 0);
                    destinations[1] = new MapLocation(hqLoc.x, mapHeight-1);
                }
                // Right edge
                else if (mapWidth-hqLoc.x-1 < mapWidth/3) {
                    destinations[0] = new MapLocation(hqLoc.x, mapHeight-1);
                    destinations[1] = new MapLocation(hqLoc.x, 0);
                }
                // Bottom edge
                else if (hqLoc.y < mapHeight/3) {
                    destinations[0] = new MapLocation(mapWidth-1, hqLoc.y);
                    destinations[1] = new MapLocation(0, hqLoc.y);
                }
                // Top edge
                else if (mapHeight-hqLoc.y-1 < mapHeight/3) {
                    destinations[0] = new MapLocation(0, hqLoc.y);
                    destinations[1] = new MapLocation(mapWidth-1, hqLoc.y);
                }
            }
        }

        // Find the other numMiners-2 locations between the two widest locations
        // Vectors between the 2 widest points and hq
        int v1x = destinations[0].x - hqLoc.x;
        int v1y = destinations[0].y - hqLoc.y;
        int v2x = destinations[1].x - hqLoc.x;
        int v2y = destinations[1].y - hqLoc.y;
        // Dot product angle formula to find widest angle
        double totalAngle = Math.acos((v1x*v2x+v1y*v2y)/Math.sqrt((v1x*v1x+v1y*v1y)*(v2x*v2x+v2y*v2y)));
        // Iterate through each miner in the middle
        double partialAngle = totalAngle/(numMiners-1);
        // Vector found by rotating v1 by rotationAngle radians
        double v3x, v3y;
        // Points found by following v3 from hq until an edge
        double px, py;
        // Shortest time to horizontal and vertical edge and any edge
        double tH = 0;
        double tV = 0;
        for (int i=1; i<=numMiners-2; i++) {
            double rotationAngle = partialAngle*i;
            v3x = Math.cos(rotationAngle)*v1x - Math.sin(rotationAngle)*v1y;
            v3y = Math.sin(rotationAngle)*v1x + Math.cos(rotationAngle)*v1y;
            // Find "time" to each side from following v3 from hq until an edge
            // Find tH
            if (v3x > 0)
                tH = (mapWidth-hqLoc.x-1)/v3x;
            else if (v3x < 0)
                tH = -hqLoc.x/v3x;
            // Find tV
            if (v3y > 0)
                tV = (mapHeight-hqLoc.y-1)/v3y;
            else if (v3y < 0)
                tV = -hqLoc.y/v3y;
            // Compute px and py
            if (v3y == 0 || tH < tV) {
                py = hqLoc.y + tH * v3y;
                // Left edge
                if (v3x < 0)
                    px = 0;
                // Right edge
                else
                    px = mapWidth-1;
            }
            else {
                px = hqLoc.x + tV * v3x;
                // Bottom edge
                if (v3y < 0)
                    py = 0;
                // Top edge
                else
                    py = mapHeight-1;
            }
            destinations[i+1] = new MapLocation((int) Math.round(px), (int) Math.round(py));
        }

        return destinations;
    }

    /**
     * Finds the closest corner to the robot
     * 
     * @return MapLocation of closest corner
     * @throws GameActionException
     */
    static MapLocation findNearestCorner() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int x = (currLoc.x < width/2) ? 0 : width-1;
        int y = (currLoc.y < height/2) ? 0 : height-1;
        return new MapLocation(x, y);
    }

    /**
     * Initialize the locations for the second ring of miners around a location
     * 
     * @throws GameActionException
     */
    static void initializeSecondRowLocations() throws GameActionException {
        int rad = 2;
        MapLocation myLoc = rc.getLocation();
        for (int i = -rad; i < rad; i++) {
            for (int j = -rad; j < rad; j++) {
                MapLocation loc = myLoc.translate(i, j);
                if (myLoc.distanceSquaredTo(loc) == 5)
                    secondRowLocations.add(loc);
            }
        }
    }

    /**
     * The function to execute when a miner can't build in a direction
     * If building/water, then broadcast that it needs a new destination and wait
     * Otherwise, do nothing and wait
     * 
     * @throws GameActionException
     */
    static void minerCantBuild(MapLocation currLoc) throws GameActionException {
        if ((rc.senseFlooding(robotDest) || rc.senseRobotAtLocation(robotDest).getType().isBuilding()) && !waitMode) {
            broadcastMessage(3, currLoc.x, currLoc.y, 10, rc.getID(), 0, 0, 0);
            waitMode = true;
        }
    }

    /**
     * Checks the sensor radius for the enemy HQ if it hasn't been found and broadcast if found
     * 
     * @throws GameActionException
     */
    static void checkForEnemyHQ() throws GameActionException {
        if (enemyHQLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ) {
                    enemyHQLoc = robot.getLocation();
                    tryBroadcastMessage(3, enemyHQLoc.x, enemyHQLoc.y, 13, 0, 0, 0, 0);
                    return;
                }
            }
        }
    }

    /**
     * Loops through the surrounding square and checks for soup within the sensor radius
     * 
     * @return whether new soup has been found
     * @throws GameActionException
     */
    static boolean checkForSoup() throws GameActionException {
        boolean newSoupFound = false;
        MapLocation currLoc = rc.getLocation();
        int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
        for (int i=-rad; i<=rad; i++) {
            for (int j=-rad; j<=rad; j++) {
                MapLocation loc = currLoc.translate(i,j);
                if (rc.canSenseLocation(loc) && rc.senseSoup(loc) != 0 && !soupLocs.contains(loc)) {
                    newSoupFound = true;
                    soupLocs.add(loc);
                    tryBroadcastMessage(1, loc.x, loc.y, 8, 0, 0, 0, 0);
                }
            }
        }
        return newSoupFound;
    }

    /**
     * Checks for new soup, depleted soup ,new water, and disappeared water
     * 
     * @throws GameActionException
     */
    static void checkSoupWater() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
        for (int i=-rad; i<=rad; i++) {
            for (int j=-rad; j<=rad; j++) {
                MapLocation loc = currLoc.translate(i,j);
                if (rc.canSenseLocation(loc)) {
                    // New soup
                    if (!soupLocs.contains(loc) && rc.senseSoup(loc) != 0) {
                        soupLocs.add(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, 8, 0, 0, 0, 0);
                    }
                    // Depleted soup
                    else if (soupLocs.contains(loc) && rc.senseSoup(loc) == 0) {
                        soupLocs.remove(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, 9, 0, 0, 0, 0);
                    }
                    // New water
                    else if (!waterLocs.contains(loc) && rc.senseFlooding(loc)) {
                        waterLocs.add(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, 20, 0, 0, 0, 0);
                    }
                    // Disappeared water
                    else if (waterLocs.contains(loc) && !rc.senseFlooding(loc)) {
                        waterLocs.remove(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, 21, 0, 0, 0, 0)
                    }
                }
            }
        }
    }

    /* COMMUNICATIONS STUFF */

    // List of all the messages and their formats
    static final String[] messageType = {
        "HQ location",                      // 00 - [xLoc, yLoc, code]
        "Refinery created",                 // 01 - [xLoc, yLoc, code]
        "Vaporator created",                // 02 - [xLoc, yLoc, code]
        "Design school created",            // 03 - [xLoc, yLoc, code, robotID]
        "Fulfillment center created",       // 04 - [xLoc, yLoc, code, robotID]
        "Net gun created",                  // 05 - [xLoc, yLoc, code]
        "Make center create drone",         // 06 - [robotID, 0, code]
        "Make school create landscaper",    // 07 - [robotID, 0, code]
        "Soup found",                       // 08 - [xLoc, yLoc, code]
        "Soup gone",                        // 09 - [xLoc, yLoc, code]
        "Miner done need another task",     // 10 - [xLoc, yLoc, code, robotID]
        "Make miner go somewhere",          // 11 - [xLoc, yLoc, code, robotID, (optional) buildingID or -1 for soup]
        "List of refinery locations",       // 12 - [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
        "Enemy HQ found",                   // 13 - [xLoc, yLoc, code]
        "Landscaper spawned",               // 14 - [xLoc, yLoc, code, robotID]
        "Drone spawned",                    // 15 - [xLoc, yLoc, code, robotID]
        "Make landscaper do something",     // 16 - [xLoc, yLoc, code, robotID, activity]
        "List of soup locations",           // 17 - [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
        "Refinery destroyed",               // 18 - [xLoc, yLoc, code]
        "List of water locations",          // 19 - [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
        "Water found",                      // 20 - [xLoc, yLoc, code]
        "Water gone",                       // 21 - [xLoc, yLoc, code]
        "Make drone do something",          // 22 - [xLoc, yLoc, code, robotID, activity]
    };

    /**
     * Generic message broadcasting template
     * See the function for the required message format
     *
     * @return true if a message was broadcasted
     * @throws GameActionException
     */
    static boolean broadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, 
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
    static boolean broadcastMessage(int[] m) throws GameActionException {
        return broadcastMessage(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7]);
    }

    /**
     * Try to broadcast a message; if unsuccessful, save into messageQ
     *
     * @throws GameActionException
     */
    static void tryBroadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, int m6) throws GameActionException {
        if (!broadcastMessage(soupCost, m0, m1, m2, m3, m4, m5, m6)) {
            int[] saveMess = new int[8];
            saveMess[0] = soupCost;
            saveMess[1] = m0;
            saveMess[2] = m1;
            saveMess[3] = m2;
            saveMess[4] = m3;
            saveMess[5] = m4;
            saveMess[6] = m5;
            saveMess[7] = m6;
            messageQ.add(saveMess);
        }
    }

    /**
     * Try to broadcast a message; if unsuccessful, save into messageQ
     * 
     * @param m size 8 array that includes soup cost
     * @throws GameActionException
     */
    static void tryBroadcastMessage(int[] m) throws GameActionException {
        if (!broadcastMessage(m))
            messageQ.add(m);
    }

    /**
     * Try to broadcast each message in the message PriorityQueue
     * 
     * @throws GameActionException
     */
    static void tryBroadcastQueue() throws GameActionException {
        for (int[] message : messageQ)
            if (broadcastMessage(message))
                messageQ.remove(message);
    }

    /**
     * Broadcasts an ArrayList of MapLocations in a series of messages, up to 12 per message
     * 
     * @param locs the array containing the locations
     * @param code the integer representing what is at each location
     * @throws GameActionException
     */
    static void broadcastLocs(ArrayList<MapLocation> locs, int code) throws GameActionException {
        // Handle the array in batches of 12
        for (int i=0; i<(locs.size()+11)/12; i++) {
            int[] message = new int[8];
            message[0] = 1;     // Soup cost
            message[3] = code;  // Message code
            int messagePos = 1;
            for (int j=i*12; j<(i+1)*12 && j<locs.size(); j+=2) {
                // If its the last location and by itself, encode just itself
                if (j == locs.size()-1)
                    message[messagePos] = encodeLoc(locs.get(j));
                // Otherwise, condense itself and the next location in one integer
                else
                    message[messagePos] = encodeLocs(locs.get(j), locs.get(j+1));
                // Go to next position in message, skipping the code
                messagePos++;
                if (messagePos == 3)
                    messagePos++;
            }
            tryBroadcastMessage(message);
        }
    }

    /**
     * Decodes a message containing up to 12 coordinates
     * 
     * @param mess the integer array to decode
     * @param code the integer representing the resource/building
     * @throws GameActionException
     */
    static void decodeLocsMessage(int[] mess, ArrayList<MapLocation> locs) throws GameActionException {
        ArrayList<MapLocation> newLocs = new ArrayList<MapLocation>();
        for (int i=0; i<7 && i!=2; i++) {
            if (mess[i] > 20000) {
                for (MapLocation loc : decodeLocs(mess[i]))
                    newLocs.add(loc);
            }
            else
                newLocs.add(decodeLoc(mess[i]));
        }
        for (MapLocation newLoc : newLocs)
            if (!locs.contains(newLoc))
                locs.add(newLoc);
    }

    /**
     * Encode a MapLocation in the format of 1+xCoord+yCoord for condensation purposes
     * For example, the coordinate (1, 30) becomes 10130
     * 
     * @param loc the location to encode
     * @return the encoded integer
     * @throws GameActionException
     */
    static int encodeLoc(MapLocation loc) throws GameActionException {
        return 10000 + loc.x * 100 + loc.y;
    }

    /**
     * Encodes two MapLocations in the format of 1+x1+y1+x2+y2 for condensation purposes
     * For example, the coordinates (1, 30) and (0, 16) become 101300016
     * 
     * @param loc1 the first location to encode
     * @param loc2 the second location to encode
     * @return the encoded integer
     * @throws GameActionException
     */
    static int encodeLocs(MapLocation loc1, MapLocation loc2) throws GameActionException {
        return 100000000 + loc1.x * 1000000 + loc1.y * 10000 + loc2.x * 100 + loc2.y;
    }

    /**
     * Decode a MapLocation in the format of 1+x+y
     * For example, 10130 becomes (1, 30)
     * 
     * @param mess the integer to decode
     * @return the decoded MapLocation
     * @throws GameActionException
     */
    static MapLocation decodeLoc(int mess) throws GameActionException {
        int coordinates = mess-10000;
        return new MapLocation(coordinates/100,  coordinates%100);
    }

    /**
     * Decodes two MapLocations in the format of 1+x1+y1+x2+y2
     * For example, 101300016 becomes (1, 30) and (0, 16)
     * 
     * @param mess the integer to decode
     * @return an ArrayList containing both decodedMapLocations
     * @throws GameActionException
     */
    static ArrayList<MapLocation> decodeLocs(int mess) throws GameActionException {
        ArrayList<MapLocation> locs = new ArrayList<MapLocation>();
        int coordinates = mess-100000000;
        int coordinates1 = coordinates/10000;
        int coordinates2 = coordinates%10000;
        locs.add(new MapLocation(coordinates1/100, coordinates1%100));
        locs.add(new MapLocation(coordinates2/100, coordinates2%100));
        return locs;
    }

    /**
     * Finds the HQ location from the blockchain
     * Relies on the fact that the HQ location must be broadcasted early on
     * 
     * @throws GameActionException
     */
    static void getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == 0){
                    hqLoc = new MapLocation(mess[0], mess[1]);
                    return;
                }
            }
        }
    }

    /**
     * Guesses the enemy HQ location assuming diagonal symmetry
     * 
     * @return predicted enemy HQ location
     * @throws GameActionException
     */
    static MapLocation predictedEnemyHQ() throws GameActionException {
        return new MapLocation(rc.getMapWidth()-hqLoc.x-1, rc.getMapHeight()-hqLoc.y-1);
    }
}