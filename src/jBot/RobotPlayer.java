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
    static boolean nearHQ = false;
    static int turnCount;
    static int numMiners = 0;
    static int numDrones = 0;
    static int numLandscapers = 0;
    static int unitsQueued = 0;
    static int robotMode = -1;          // Default mode of -1 is the "do nothing" mode
    static int robotNum;
    static Direction destDir;
    static MapLocation hqLoc;
    static MapLocation enemyLoc;
    static MapLocation robotDest;
    static MapLocation lastBugPathLoc;
    static PriorityQueue<int[]> messageQ = new PriorityQueue<int[]>();
    static ArrayList<MapLocation> soupLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> vaporatorLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> designSchoolLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> fulfillmentCenterLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> netGunLocs = new ArrayList<MapLocation>();
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
            // Search surrounding square for soup
            int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = hqLoc.translate(i,j);
                    if (rc.canSenseLocation(loc) && rc.senseSoup(loc) != 0 && !soupLocs.contains(loc))
                        soupLocs.add(loc);
                }
            }
        }

        tryBroadcastQueue();

        // Process transactions past the first round
        if (turnCount != 1) {
            for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
                int[] mess = tx.getMessage();
                if(mess[2]/100 == TEAM_SECRET) {
                    // Refinery created
                    if (mess[2]%100 == 1)
                        refineryLocs.add(new MapLocation(-mess[0], -mess[1]));
                    // etc. 
                }
            }
        }
        
        // If there are a certain number of miners/refineries, build a miner
        if(numMiners <= 4 || (numMiners <= 10 && refineryLocs.size() >= 1)) {
            if(tryBuildAround(RobotType.MINER, randomDirection())) {
                // Broadcast all refinery locations (up to 6) if there are any
                if (!refineryLocs.isEmpty())  {
                    int[] message = new int[8];
                    message[0] = 3; // soupCost
                    for (int i=0; i<refineryLocs.size(); i++) {
                        // These conditionals account for the message code at index 2
                        if (i < 2)
                            message[i+1] = encodeLoc(refineryLocs.get(i));
                        else
                            message[i+2] = encodeLoc(refineryLocs.get(i));
                        tryBroadcastMessage(message);
                    }
                }
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
                // Process initial list of refinery locations
                if (mess[2]%100 == 12)
                    for (int i=0; i<6 && i!=2; i++)
                        if (mess[i] != 0) {
                            int coordinates = mess[i] - 10000;
                            refineryLocs.add(new MapLocation(coordinates/100, coordinates%100));
                        }
                // Add refinery to refinery ArrayList
                else if (mess[2]%100 == 1)
                    refineryLocs.add(new MapLocation(-mess[0], -mess[1]));
                // Add soup to soup ArrayList
                else if (mess[2]%100 == 8)
                    soupLocs.add(new MapLocation(-mess[0], -mess[1]));
                // Remove soup from soup ArrayList
                else if (mess[2]%100 == 9)
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

        // Scan surroundings
        checkIfStillHaveSoup();
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
            checkForSoup();
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
    }

    static void runRefinery() throws GameActionException {

        // Don't broadcast location here because we want to broadcast it 10 turns earlier in HQ
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
                if (mess[3]%100 == 16) {
                    if (mess[4] == 0)
                        robotMode = 0;
                    else if (mess[4] == 1) {
                        robotMode = 1;
                    }
                }
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
                if (mess[2]%100 == 1)
                    System.out.println();
                    // Do something
                // etc.
            }
        }
 
        checkForEnemyHQ();

        // Defense mode (0)
        if (robotMode == 0) {

        }

        // Attack mode (1) 
        else if (robotMode == 1) {

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

    /**
     * Makes the optimal bug move towards a location
     * 
     * @param location The intended destination
     * @return false if arrived at destination
     * @throws GameActionException
     */
    static boolean bugMove(MapLocation location) throws GameActionException {
        // TODO: bugmove (we use goTo as a substitute for now)
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
        Direction dir = rc.getLocation().directionTo(destination);
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
        if (enemyLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ) {
                    enemyLoc = robot.getLocation();
                    tryBroadcastMessage(3, enemyLoc.x, enemyLoc.y, 13, 0, 0, 0, 0);
                    break;
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
                    tryBroadcastMessage(3, loc.x, loc.y, 8, 0, 0, 0, 0);
                }
            }
        }
        return newSoupFound;
    }

    /**
     * Checks if there is still soup in each of the locations in soupLoc
     * 
     * @throws GameActionException
     */
    static void checkIfStillHaveSoup() throws GameActionException {
        for (MapLocation soupLoc : soupLocs) {
            if (rc.canSenseLocation(soupLoc) && rc.senseSoup(soupLoc) == 0) {
                tryBroadcastMessage(3, soupLoc.x, soupLoc.y, 9, 0, 0, 0, 0);
                soupLocs.remove(soupLoc);
            }
        }
    }

    /* COMMUNICATIONS STUFF */

    // List of all the messages and their formats
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
        "Make miner go somewhere",          // 11 - [xLoc, yLoc, code, minerID, (optional) buildingID or -1 for soup]
        "List of refinery locations",       // 12 - [1+x1+y1, 1+x2+y2, code, 1+x3+y3, 1+x4+y4, 1+x5+y5, 1+x6+y6] (up to 6)
        "Enemy HQ found",                   // 13 - [xLoc, yLoc, code]
        "Landscaper spawned",               // 14 - [xLoc, yLoc, code, landscaperID]
        "Drone spawned",                    // 15 - [xLoc, yLoc, code, landscaperID]
        "Make landscaper do something",     // 16 - [xLoc, yLoc, code, landscaperID, activity]

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
     * Finds the HQ location from the blockchain
     * Relies on the fact that the HQ location must be broadcasted early on
     * 
     * @throws GameActionException
     */
    static void getHqLocFromBlockchain() throws GameActionException {
        System.out.println("B L O C K C H A I N");
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
}