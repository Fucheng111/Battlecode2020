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
        RobotType.REFINERY,             // (buildingNum) 1
        RobotType.VAPORATOR,            // 2
        RobotType.DESIGN_SCHOOL,        // 3
        RobotType.FULFILLMENT_CENTER,   // 4
        RobotType.NET_GUN               // 5
    };

    static boolean nearHQ = false;
    static boolean leftTendency = false;
    static boolean findingRefinery = false;
    static boolean buildingMiner = false;
    static boolean buildingCommissioned = false;
    static boolean isHQDefender = false;
    static int turnCount;
    static int numMiners = 0;
    static int numDrones = 0;
    static int numLandscapers = 0;
    static int numVaporators = 0;
    static int numNetGuns = 0;
    static int unitsQueued = 0;
    static int robotMode = -1;          // Default mode of -1 is the "do nothing" mode
    static int buildingNum = 0;
    static int buildingImportance = 0;
    static int[] minerDestOrder;
    static Direction destDir;
    static MapLocation hqLoc;
    static MapLocation enemyHQLoc;
    static MapLocation robotDest;
    static MapLocation defaultMinerDest;
    static MapLocation lastBugPathLoc;
    static MapLocation[] desiredMinerDests;
    static PriorityQueue<int[]> messageQ                = new PriorityQueue<int[]>();
    static ArrayList<Integer> designSchoolIDs           = new ArrayList<Integer>();
    static ArrayList<Integer> fulfillmentCenterIDs      = new ArrayList<Integer>();
    static ArrayList<MapLocation> soupLocs              = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> waterLocs             = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocs          = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> secondRowLocations    = new ArrayList<MapLocation>();

    // Defensive positions
    static MapLocation defensiveVapLoc;
    static MapLocation defensiveCenterLoc;
    static MapLocation defensiveSchoolLoc;
    static ArrayList<MapLocation> defensiveGunLocs = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> defensiveDroneLocs = new ArrayList<MapLocation>();
    
    // Communication codes
    static final int TEAM_SECRET = 789;
    static final int HQ_LOC                 = 0;    // [x, y, code]
    static final int REFINERY_CREATED       = 1;    // [x, y, code]
    static final int VAPORATOR_CREATED      = 2;    // [x, y, code]
    static final int DESIGN_SCHOOL_CREATED  = 3;    // [x, y, code, ID]
    static final int FULFILLMENT_CREATED    = 4;    // [x, y, code, ID]
    static final int NET_GUN_CREATED        = 5;    // [x, y, code]
    static final int DESIGN_SCHOOL_TASK     = 6;    // [x, y, code, ID]
    static final int FULFILLMENT_TASK       = 7;    // [x, y, code, ID]
    static final int MINER_INIT_1           = 8;    // [x, y, code, ID]
    static final int MINER_INIT_2           = 9;    // [x, y, code, ID]
    static final int MINER_TASK             = 10;   // [x, y, code, ID, buildingID/-1 for soup/0 for wander, importance]
    static final int LANDSCAPER_SPAWN       = 11;   // [x, y, code, ID]
    static final int LANDSCAPER_TASK        = 12;   // [x, y, code, ID, activity]
    static final int DRONE_SPAWN            = 13;   // [x, y, code, ID]
    static final int DRONE_TASK             = 14;   // [x, y, code, ID, activity]
    static final int INIT_SOUP_LOCS         = 15;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int INIT_WATER_LOCS        = 16;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int INIT_REFINERY_LOCS     = 17;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int SOUP_FOUND             = 18;   // [x, y, code]
    static final int SOUP_GONE              = 19;   // [x, y, code]
    static final int WATER_FOUND            = 20;   // [x, y, code]
    static final int WATER_GONE             = 21;   // [x, y, code]
    static final int REFINERY_DESTROYED     = 22;   // [x, y, code]
    static final int ENEMY_HQ_FOUND         = 23;   // [x, y, code]
    
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
            tryBroadcastMessage(1, hqLoc.x, hqLoc.y, HQ_LOC, 0, 0, 0, 0);
            // Compute desired miner locations
            desiredMinerDests = bestMinerLocs(11);
            minerDestOrder = new int[]{1, 8, 4, 9, 5, 0, 6, 10, 3, 7, 2};
            // Search surrounding square for soup and water
            int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = hqLoc.translate(i,j);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0)
                            soupLocs.add(loc);
                        else if (rc.senseFlooding(loc))
                            waterLocs.add(loc);
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
                    if (enemyHQLoc == null && mess[2]%100 == ENEMY_HQ_FOUND)
                        enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
                    // Initialize miner
                    else if (mess[2]%100 == MINER_INIT_1) {
                        numMiners++;
                        MapLocation targetDest = desiredMinerDests[minerDestOrder[numMiners%11]];
                        tryBroadcastMessage(1, targetDest.x, targetDest.y, MINER_INIT_2, mess[3], 0, 0, 0);
                    }
                    // Initialize drone
                    else if (mess[2]%100 == DRONE_SPAWN) {
                        numDrones++;
                        broadcastAll();
                    }
                    // Initialize vaporator
                    else if (mess[2]%100 == VAPORATOR_CREATED) {
                        numVaporators++;
                        buildingCommissioned = false;
                    }
                    // Initialize design school
                    else if (mess[2]%100 == DESIGN_SCHOOL_CREATED) {
                        designSchoolIDs.add(mess[3]);
                        buildingComissioned = false;
                    }
                    // Initialize fulfillment center
                    else if (mess[2]%100 == FULFILLMENT_CREATED) {
                        fulfillmentCenterIDs.add(mess[3]);
                        buildingCommissioned = false;
                    }
                    // Initialize net gun
                    else if (mess[2]%100 == NET_GUN_CREATED) {
                        numNetGuns++;
                        buildingCommissioned = false;
                    }
                    updateLocs(mess, 0);    // Update soup locations
                    updateLocs(mess, 1);    // Update water locations
                    updateLocs(mess, 2);    // Update refinery locations
                }
            }
        }

        // If there are enemies in range, prioritize defense
        // Build 7 drones, then landscapers
        // TODO: add "if (!buildingCommissioned) {} around everything below
        RobotInfo[] robots = rc.senseNearbyRobots();
        Team enemyTeam = rc.getTeam().opponent();
        // Check if there is an enemy nearby
        boolean enemyNear = false;
        for (RobotInfo robot : robots) {
            if (robot.getTeam() == enemyTeam) {
                enemyNear = true;
                break;
            }
        }
        if (enemyNear) {
            
            //  if (numDrones < 7)
            //      if (fulfillment center exists)  commission drone
            //      find nearest miner
            //      else if (nearest miner exists)  commission fulfillment center
            //      else if (have 70 soup)  try to make a miner
            //  else if (numLandscapers < 16)
            //      if (design school exists)   commission landscaper
            //      find nearest miner
            //      else if (nearest miner exists)  commission design school
            //      else if (have 70 soup)  try to make miner
            //
            // 
            //
            //
            //
            //
            //
            //
            //
            //
            // If we already have a fulfillment center, commission a drone
            if (!fulfillmentCenterIDs.isEmpty() && numDrones < 7) {
                tryBroadcastMessage(2, 0, 0, FULFILLMENT_TASK, fulfillmentCenterIDs.get(0), 0, 0, 0);
                buildingCommissioned = true;
            }
            // Attempt to find closest friendly miner to fulfillment center
            int minerID = -1;
            int lsd = 46;
            int sd;
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.MINER && robot.getTeam() == rc.getTeam()) {
                    sd = robot.getLocation.distanceSquaredTo(defensiveFulfillmentLoc);
                    if (sd < lsd) {
                        minerID = robot.getID();
                        lsd = sd;
                    }
                }
            }
            // Otherwise, try to build a fulfillment center if there's a nearby miner
            else if (fulfillmentCenterIDs.isEmpty() && minerID != -1) {
                tryBroadcastMessage(2, defensiveCenterLoc.x, defensiveCenterLoc.y, MINER_TASK, minerID, spawnedByMiner[4], 1, 0);
                buildingCommissioned = true;
            }
            else if (rc.getTeamSoup() >= 70) {
                tryBuildAround(RobotType.MINER, optimalDirection());
            }
        }
        
        // Otherwise, focus on building miners
        else if (numMiners < 11) {
            tryMakeMiner();
        }
        
        // When miners are finished, complete the defensive ring
        // Builder miner
        else if (!builderMiner) {
            if (tryMakeMiner())
                builderMiner = true;
        }
        
        // Design School (1)
        else if (defensiveSchoolLoc != null) {
            // Commission miner to make thing
            // If not enuf soup or tile occupied, wait
            // Wrong elevation, flooded, not on the map, move on
            if (rc.getTeamSoup() < 150 || )
        }
        // Landscapers (16)
        
        // Fulfillment Center (1)
        
        // Drones (7)
        
        // Net Guns (2)
        
        // Vaporator (1)

        // After the defensive ring is finished, just build a design school and fulfillment center
        // towards the middle and commission a bunch of landscapers/drones 
        
        // If there are a certain number of miners/refineries, build a miner
        
    }

    static void runMiner() throws GameActionException {

        // Search surroundings for HQ upon spawn
        if (turnCount == 1) {
            tryBroadcastMessage(1, 0, 0, MINER_INIT_1, rc.getID(), 0, 0, 0);
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
                // Receive initial list of soup/water/refinery locations
                if (mess[2]%100 == INIT_SOUP_LOCS || mess[2]%100 == INIT_WATER_LOCS || mess[2]%100 == INIT_REFINERY_LOCS) {
                    switch(mess[2]%100) {
                        case INIT_SOUP_LOCS:        decodeLocsMessage(mess, soupLocs);      break;
                        case INIT_WATER_LOCS:       decodeLocsMessage(mess, waterLocs);     break;
                        case INIT_REFINERY_LOCS:    decodeLocsMessage(mess, refineryLocs);  break;
                    }
                }
                // Initialize default definition
                if (mess[2]%100 == MINER_INIT_2 && mess[3]==rc.getID())
                    defaultMinerDest = new MapLocation(-mess[0], -mess[1]);
                // Move towards location if HQ requests
                else if (mess[2]%100 == MINER_TASK && mess[3] == rc.getID()) {
                    robotDest = new MapLocation(-mess[0], -mess[1]);
                    if (mess[4] == -1)
                        robotMode = 1;
                    else {
                        buildingNum = mess[4];
                        buildingImportant = mess[5];
                    }
                }
                updateLocs(mess, 0);    // Update soup locations
                updateLocs(mess, 1);    // Update water locations
                updateLocs(mess, 2);    // Update refinery locations
            }
        }

        checkForEnemyHQ();
        MapLocation currLoc = rc.getLocation();

        // Explore mode
        if (robotMode == 0) {
            if (!exploreMove(robotDest))
                newMinerDest();
            // Change to mode 1 and update robotDest if soup is found
            if (checkForSoup()) {
                robotMode = 1;
                int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
                for (int i=-rad; i<=rad; i++) {
                    for (int j=-rad; j<=rad; j++) {
                        MapLocation loc = currLoc.translate(i,j);
                        if (rc.canSenseLocation(loc) && rc.senseSoup(loc) != 0 && !soupLocs.contains(loc)) {
                            robotDest = loc;
                            tryBroadcastMessage(3, loc.x, loc.y, SOUP_FOUND, 0, 0, 0, 0);
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
                    robotMode = 2;
                // Change to refine if there's a refinery
                else if (rc.senseRobotAtLocation(robotDest).getType() == RobotType.REFINERY)
                    robotMode = 3;
                // Change to build if given building by HQ
                else if (buildingNum != 0)
                    robotMode = 4;
            }
            // If miner senses that refinery isn't there, find a new refinery
            if (findingRefinery && rc.canSenseLocation(robotDest)) {
                RobotInfo robot = rc.senseRobotAtLocation(robotDest);
                if (robot.getTeam() != rc.getTeam() || robot.getType() != RobotType.REFINERY) 
                    newMinerDest();
            }
        }
        
        // Mine soup mode (2)
        else if (robotMode == 2) {
            if (!tryMine(destDir)) 
                newMinerDest();
        }
        
        // Refine soup mode (3)
        else if (robotMode == 3) {
            // Try to refine the soup
            tryRefine(destDir);
            // New destination changes based on if the miner still has soup
            newMinerDest();
        }
        
        // Build building mode (4)
        // In importance 0, try to build all around miner
        // In importance 1, build at a specific location
        // Give the miner a new destination if miner built something
        else if (robotMode == 4) {
            if (buildingImportance == 1 && tryBuild(spawnedByMiner[buildingNum-1], destDir)) 
                newMinerDest();
            else if (buildingImportance == 0 && tryBuildAround(spawnedByMiner[buildingNum-1], destDir))
                newMinerDest();
            // (Implicit) Otherwise, wait
        }

        checkSoupWaterRefineries();
    }

    static void runRefinery() throws GameActionException {
        
        // Upon spawn...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, REFINERY_CREATED, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();
    }

    static void runVaporator() throws GameActionException {

        // Upon spawn...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, VAPORATOR_CREATED, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();
    }

    static void runDesignSchool() throws GameActionException {

        // Upon spawn...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, DESIGN_SCHOOL_CREATED, rc.getID(), 0, 0, 0);
        }
        
        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == DESIGN_SCHOOL_TASK && mess[3] == rc.getID())
                unitsQueued++;
        }
        
        // Try to make a landscaper if one is queued
        if (unitsQueued > 0 && tryBuildAround(RobotType.LANDSCAPER, randomDirection()))
            unitsQueued--;
    }

    static void runFulfillmentCenter() throws GameActionException {

        // Upon spawn...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, FULFILLMENT_CREATED, rc.getID(), 0, 0, 0);
        }
        
        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == FULFILLMENT_TASK && mess[0] == rc.getID())
                unitsQueued++;
        }
        
        // Try to make a drone if one is queued
        if (unitsQueued > 0 && tryBuildAround(RobotType.LANDSCAPER, randomDirection())) {
            
            unitsQueued--;
        }
    }
        
    static void runNetGun() throws GameActionException {
        
        // Upon spawning...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, NET_GUN_CREATED, 0, 0, 0, 0);
        }

        tryBroadcastQueue();
        
        // Get the list of nearby robots, sorted by distance in increasing order
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getLocation(), rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            // Shoot the closest delivery drone
            if (robot.getType() == RobotType.DELIVERY_DRONE) {
                int targetID = robot.getID();
                if (rc.canShootUnit(targetID)) {
                    rc.shootUnit(targetID);
                    return;
                }
            }
        }
    }

    static void runLandscaper() throws GameActionException {

        // Upon spawning...
        if (turnCount == 1) {
            getHqLocFromBlockchain();
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, LANDSCAPER_SPAWN, rc.getID(), 0, 0, 0);
        }

        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/100 == TEAM_SECRET) {
                // Receive commands from HQ
                if (mess[2]%100 == LANDSCAPER_TASK) {
                    robotDest = new MapLocation(-mess[0], -mess[1]);
                    robotMode = mess[4];
                }
                // Enemy HQ Found
                if (enemyHQLoc == null && mess[2]%100 == ENEMY_HQ_FOUND)
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
                    for (RobotInfo robot : nearbyRobots)
                        if (robot.getLocation().isAdjacentTo(hqLoc) && robot.getType() != RobotType.LANDSCAPER)
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
        if (turnCount == 1) {
            getHqLocFromBlockchain();
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, DRONE_SPAWN, rc.getID(), 0, 0, 0);
        }

        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/100 == TEAM_SECRET) {
                // Receive initial list of soup/water/refinery locations
                if (mess[2]%100 == INIT_SOUP_LOCS || mess[2]%100 == INIT_WATER_LOCS || mess[2]%100 == INIT_REFINERY_LOCS) {
                    switch(mess[2]%100) {
                        case INIT_SOUP_LOCS:        decodeLocsMessage(mess, soupLocs);      break;
                        case INIT_WATER_LOCS:       decodeLocsMessage(mess, waterLocs);     break;
                        case INIT_REFINERY_LOCS:    decodeLocsMessage(mess, refineryLocs);  break;
                    }
                }
                // Receive commands from HQ
                else if (mess[2]%100 == DRONE_TASK && mess[3] == rc.getID())
                    robotMode = mess[4];
                // Enemy HQ Found
                if (enemyHQLoc == null && mess[2]%100 == ENEMY_HQ_FOUND)
                    enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
                updateLocs(mess, 0);    // Update soup locations
                updateLocs(mess, 1);    // Update water locations
                updateLocs(mess, 2);    // Update refinery locations
            }
        }
 
        checkForEnemyHQ();
        checkSoupWaterRefineries();

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
        else {
            Team enemy = rc.getTeam().opponent();
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
                    if (!waterLocs.isEmpty()) {
                        MapLocation currLoc = rc.getLocation();
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
     * @return false if adjacent to destination
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
            dir.rotateRight(),
            dir.rotateLeft(),
            dir.rotateRight().rotateRight(),
            dir.rotateLeft().rotateLeft(),
            dir.opposite().rotateLeft(),
            dir.opposite().rotateRight(),
            dir.opposite()
        };
        for (Direction d : toTry)
            if (tryBuild(type, d))
                return true;
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
    static Direction optimalDirection() throws GameActionException{
        MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        return rc.getLocation().directionTo(center);
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
     * Finds the farthest corner to the robot
     * 
     * @return MapLocation of farthest corner
     * @throws GameActionException
     */
    static MapLocation findFarthestCorner() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int x = (currLoc.x >= width/2) ? 0 : width-1;
        int y = (currLoc.y >= height/2) ? 0 : height-1;
        return new MapLocation(x, y);
    }

    /**
     * Sets the ArrayLists containing the defensive positions for the buildings and drones
     * 
     * @throws GameActionException
     */
    static void setDefensivePositions() throws GameActionException {

        int mapHeight = rc.getMapHeight();
        int mapWidth = rc.getMapWidth();
        
        // Find closest horizontal and vertical distance to an edge
        int closestX = Math.min(hqLoc.x, (mapWidth-1)-hqLoc.x);
        int closestY = Math.min(hqLoc.y, (mapHeight-1)-hqLoc.y);

        int s;
        // Corner/Center
        if ((closestX < mapWidth/3 && closestY < mapHeight/3) || (closestX >= mapWidth/3 && closestY >= mapHeight/3)) {
            // Top or bottom switch
            s = (hqLoc.y >= mapHeight/2) ? 1 : -1;
            // Top Left/Bottom Right
            if (hqLoc.x < mapWidth/2) {
                defensiveVapLoc = new MapLocation(currLoc.translate(s*-3, s*3));
                defensiveCenterLoc = new MapLocation(currLoc.translate(s*1, s*3);
                defensiveSchoolLoc = new MapLocation(currLoc.translate(s*-3, s*-1));
                defensiveGunLocs.add(currLoc.translate(s*3, s*1));
                defensiveGunLocs.add(currLoc.translate(s*-1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*3, s*3));
                defensiveDroneLocs.add(currLoc.translate(s*3, 0));
                defensiveDroneLocs.add(currLoc.translate(s*3, s*-1));
                defensiveDroneLocs.add(currLoc.translate(s*3, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(0, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*-3));
            }
            // Top Right/Bottom Left
            else {
                defensiveVapLoc = new MapLocation(currLoc.translate(s*3, s*3));
                defensiveCenterLoc = new MapLocation(currLoc.translate(s*3, s*-1));
                defensiveSchoolLoc = new MapLocation(currLoc.translate(s*-1, s*3));
                defensiveGunLocs.add(currLoc.translate(s*1, s*-3));
                defensiveGunLocs.add(currLoc.translate(s*-3, s*1));
                defensiveDroneLocs.add(currLoc.translate(s*3, s*-3));
                defensiveDroneLocs.add(currLoc.translate(0, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*-1));
                defensiveDroneLocs.add(currLoc.translate(s*-3, 0));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*3));
            }
        }

        // Edge
        else {
            // Top/Bottom
            if (closestY < mapHeight/3) {
                s = (hqLoc.y >= mapHeight/2) ? 1 : -1;
                defensiveVapLoc = new MapLocation(currLoc.translate(0, s*3));
                defensiveCenterLoc = new MapLocation(currLoc.translate(s*-3, 0));
                defensiveSchoolLoc = new MapLocation(currLoc.translate(s*3, 0));
                defensiveGunLocs.add(currLoc.translate(s*3, s*-3));
                defensiveGunLocs.add(currLoc.translate(s*-3, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*3, s*1));
                defensiveDroneLocs.add(currLoc.translate(s*3, s*-1));
                defensiveDroneLocs.add(currLoc.translate(s*1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(0, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*-1));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*1));
            }
            // Left/Right
            else {
                s = (hqLoc.x >= mapWidth/2) ? 1 : -1;
                defensiveVapLoc = new MapLocation(currLoc.translate(s*3, 0));
                defensiveCenterLoc = new MapLocation(currLoc.translate(0, s*3));
                defensiveSchoolLoc = new MapLocation(currLoc.translate(0, s*-3));
                defensiveGunLocs.add(currLoc.translate(s*-3, s*-3));
                defensiveGunLocs.add(currLoc.translate(s*-3, s*3));
                defensiveDroneLocs.add(currLoc.translate(s*1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-1, s*-3));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*-1));
                defensiveDroneLocs.add(currLoc.translate(s*-3, 0));
                defensiveDroneLocs.add(currLoc.translate(s*-3, s*1));
                defensiveDroneLocs.add(currLoc.translate(s*-1, s*3));
                defensiveDroneLocs.add(currLoc.translate(s*1, s*3));
            }
        }
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
     * Attempt to build a miner (in the "optimal" direction away from the HQ"
     * Broadcast all necessary information upon miner creation
     * 
     * @return whether a miner was created
     * @throws GameActionException
     */
    static boolean tryMakeMiner() throws GameActionException {
        if (tryBuildAround(RobotType.MINER, optimalDirection())) {
            broadcastAll()
            return true;
        }
        return false;
    }

    /**
     * Gives a miner a new destination once it finishes refining/building
     * See comments for details on where this function sends the miners
     *
     * 
     * @throws GameActionException
     */
    static void newMinerDest() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        // If it's carrying soup, go to the nearest refinery
        if (rc.getSoupCarrying() != 0) {
            robotMode = 1;
            robotDest = hqLoc;
            int lowestDist = currLoc.distanceSquaredTo(robotDest);
            for (MapLocation loc : refineryLocs) {
                if (currLoc.distanceSquaredTo(loc) < lowestDist) {
                    robotDest = loc;
                    lowestDist = currLoc.distanceSquaredTo(loc);
                }
            }
        }
        // Otherwise, if there's known soup, go to the location of the nearest soup
        else if (soupLocs.size() != 0) {
            robotMode = 1;
            robotDest = soupLocs.get(0);
            int minDist = currLoc.distanceSquaredTo(robotDest);
            int tempDist;
            for (int i=1; i<soupLocs.size(); i++) {
                tempDist = currLoc.distanceSquaredTo(soupLocs.get(i));
                if (tempDist < minDist) {
                    minDist = tempDist;
                    robotDest = soupLocs.get(i);
                }
            }
        }
        // Otherwise, if it wasn't already exploring, look to HQ for a destination to explore to
        else if (robotMode != 0) {
            robotMode = 0;
            robotDest = defaultMinerDest;
        }
        // Finally, if it was already exploring and there's no known soup, just go to the farthest corner
        else
            robotDest = findFarthestCorner();
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
                    tryBroadcastMessage(3, enemyHQLoc.x, enemyHQLoc.y, ENEMY_HQ_FOUND, 0, 0, 0, 0);
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
                    tryBroadcastMessage(1, loc.x, loc.y, SOUP_FOUND, 0, 0, 0, 0);
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
    static void checkSoupWaterRefineries() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
        for (int i=-rad; i<=rad; i++) {
            for (int j=-rad; j<=rad; j++) {
                MapLocation loc = currLoc.translate(i,j);
                if (rc.canSenseLocation(loc)) {
                    // New soup
                    if (!soupLocs.contains(loc) && rc.senseSoup(loc) != 0) {
                        soupLocs.add(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, SOUP_FOUND, 0, 0, 0, 0);
                    }
                    // Depleted soup
                    else if (soupLocs.contains(loc) && rc.senseSoup(loc) == 0) {
                        soupLocs.remove(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, SOUP_GONE, 0, 0, 0, 0);
                    }
                    // New water
                    else if (!waterLocs.contains(loc) && rc.senseFlooding(loc)) {
                        waterLocs.add(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, WATER_FOUND, 0, 0, 0, 0);
                    }
                    // Disappeared water
                    else if (waterLocs.contains(loc) && !rc.senseFlooding(loc)) {
                        waterLocs.remove(loc);
                        tryBroadcastMessage(1, loc.x, loc.y, WATER_GONE, 0, 0, 0, 0);
                    }
                    // Destroyed refinery
                    else if (refineryLocs.contains(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot.getType() != RobotType.REFINERY || robot.getTeam() != rc.getTeam()) {
                            refineryLocs.remove(loc);
                            tryBroadcastMessage(1, loc.x, loc.y, REFINERY_DESTROYED, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    /**
     * Broadcast soup/water/refinery locations to blockchain
     * Typically done upon miner/drone spawn
     * 
     * @param mess the blockchain message to parse
     * @param code the specific resource/building to update
     * @throws GameActionException
     */
    static void broadcastAll() throws GameActionException {
        broadcastLocs(soupLocs, INIT_SOUP_LOCS);
        broadcastLocs(refineryLocs, INIT_WATER_LOCS);
        broadcastLocs(waterLocs, INIT_WATER_LOCS);
    }

    /**
     * Update soup/water/refineryLocs based on blockchain message
     * code: 0 is soup, 1 is water, 2 is refinery
     * 
     * @param mess the blockchain message to parse
     * @param code the specific resource/building to update
     * @throws GameActionException
     */
    static void updateLocs(int[] mess, int code) throws GameActionException {
        int createCode = 0;
        int destroyCode = 0;
        ArrayList<MapLocation> locs = new ArrayList<MapLocation>();
        switch (code) {
            case 0:
                createCode = SOUP_FOUND;
                destroyCode = SOUP_GONE;
                locs = soupLocs;
                break;
            case 1:
                createCode = WATER_FOUND;
                destroyCode = WATER_GONE;
                locs = waterLocs;
                break;
            case 2:
                createCode = REFINERY_CREATED;
                destroyCode = REFINERY_DESTROYED;
                locs = refineryLocs;
                break;
        }
        if (mess[2]%100 == createCode && !locs.contains(new MapLocation(-mess[0], -mess[1])))
            locs.add(new MapLocation(-mess[0], -mess[1]));
        else if (mess[2]%100 == destroyCode)
            locs.remove(new MapLocation(-mess[0], -mess[1]));
    }

    /* COMMUNICATIONS STUFF */

    /**
     * Generic message broadcasting template
     * See the function for the required message format
     *
     * @return true if a message was broadcasted
     * @throws GameActionException
     */
    static boolean broadcastMessage(int cost, int m0, int m1, int m2, int m3, int m4, int m5, 
        int m6) throws GameActionException {
        int[] message = new int[7];
        message[0] = -1 * m0; // -xLoc (negated for encryption)
        message[1] = -1 * m1; // -yLoc (negated for encryption)
        message[2] = TEAM_SECRET * 100 + m2; // teamCode + messageCode (i.e. 78901 is HQ location)
        message[3] = m3; // val1
        message[4] = m4; // val2
        message[5] = m5; // val3
        message[6] = m6; // val4
        if (rc.getTeamSoup() >= cost){
            rc.submitTransaction(message, cost);
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
    static void tryBroadcastMessage(int cost, int m0, int m1, int m2, int m3, int m4, int m5, int m6) throws GameActionException {
        if (!broadcastMessage(cost, m0, m1, m2, m3, m4, m5, m6)) {
            int[] saveMess = new int[8];
            saveMess[0] = cost;
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
