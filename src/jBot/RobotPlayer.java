package jBot;

import battlecode.common.*;
import java.lang.Math;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

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
    static boolean findingSoup = false;
    static boolean buildingCommissioned = false;
    static boolean isHQDefender = false;
    static boolean builderMiner = false;
    static boolean builderMinerInit = false;
    static boolean isCow = false;
    static boolean expectWater = false;
    static boolean landscaperStart = false;
    static int turnCount;
    static int numMiners = 0;
    static int numDrones = 0;
    static int numLandscapers = 0;
    static int numVaporators = 0;
    static int numNetGuns = 0;
    static int unitsQueued = 0;
    static int robotMode = 0;
    static int buildingNum = 0;
    static int buildingImportance = 0;
    static int landscapersCommissioned = 0;
    static int dronesCommissioned = 0;
    static int cowCooldown = 0;
    static int bugDirectionTendency = 0;    // 0 for none, 1 for left, 2 for right
    static int builderMinerID = -1;
    static int builderMinerCount = 0;       // Increment after building something
    static int[] minerDestOrder;
    static Direction destDir;
    static MapLocation hqLoc;
    static MapLocation enemyHQLoc;
    static MapLocation robotDest;
    static MapLocation defaultUnitDest;
    static MapLocation lastBugPathLoc;
    static MapLocation[] defaultMinerDests;
    static Queue<int[]> messageQ                    = new LinkedList<int[]>();
    static List<Integer> minerIDs                   = new ArrayList<Integer>();
    static List<Integer> designSchoolIDs            = new ArrayList<Integer>();
    static List<Integer> fulfillmentCenterIDs       = new ArrayList<Integer>();
    static Set<MapLocation> soupLocs                = new HashSet<MapLocation>();
    static Set<MapLocation> waterLocs               = new HashSet<MapLocation>();
    static Set<MapLocation> refineryLocs            = new HashSet<MapLocation>();

    // Defensive positions
    static MapLocation defensiveVapLoc;
    static MapLocation defensiveCenterLoc;
    static MapLocation defensiveSchoolLoc;
    static MapLocation[] defensiveGunLocs;
    static MapLocation[] defensiveDroneLocs;
    static MapLocation[] defensiveScaperLocs;
    
    // Communication codes
    static final int TEAM_SECRET = 789;
    static final int HQ_LOC                 = 0;    // [x, y, code]
    static final int REFINERY_CREATED       = 1;    // [x, y, code]
    static final int VAPORATOR_CREATED      = 2;    // [x, y, code]
    static final int DESIGN_SCHOOL_CREATED  = 3;    // [x, y, code, ID]
    static final int FULFILLMENT_CREATED    = 4;    // [x, y, code, ID]
    static final int NET_GUN_CREATED        = 5;    // [x, y, code]
    static final int DESIGN_SCHOOL_TASK     = 6;    // [x, y, code, ID, numLandscapers]
    static final int FULFILLMENT_TASK       = 7;    // [x, y, code, ID, numDrones]
    static final int MINER_INIT_1           = 8;    // [x, y, code, ID]
    static final int MINER_INIT_2           = 9;    // [x, y, code, ID, 5 if builderMiner, numMiners]
    static final int MINER_TASK             = 10;   // [x, y, code, ID, buildingID/-1 for soup/0 for wander, importance]
    static final int LANDSCAPER_SPAWN       = 11;   // [x, y, code, ID]
    static final int LANDSCAPER_TASK        = 12;   // [x, y, code, ID, activity]
    static final int LANDSCAPER_START       = 13;   // [x, y, code, ID] 
    static final int DRONE_SPAWN            = 14;   // [x, y, code, ID]
    static final int DRONE_TASK             = 15;   // [x, y, code, ID, activity]
    static final int INIT_SOUP_LOCS         = 16;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int INIT_WATER_LOCS        = 17;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int INIT_REFINERY_LOCS     = 18;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int SOUP_FOUND             = 19;   // [x, y, code]
    static final int SOUP_GONE              = 20;   // [x, y, code]
    static final int WATER_FOUND            = 21;   // [x, y, code]
    static final int WATER_GONE             = 22;   // [x, y, code]
    static final int REFINERY_DESTROYED     = 23;   // [x, y, code]
    static final int ENEMY_HQ_FOUND         = 24;   // [x, y, code]
    static final int STARTED_TURTLE         = 25;   // [x, y, code]
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        turnCount = 0;

        while (true) {
            turnCount++;
            try {
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
                Clock.yield();
            }
            catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {

        // At the beginning of the game...
        if (turnCount == 1) {
            // Set it's own location as hqLoc and try to broadcast hqLoc until it can
            hqLoc = rc.getLocation();
            tryBroadcastMessage(1, hqLoc.x, hqLoc.y, HQ_LOC, 0, 0, 0, 0);
            // Compute desired miner locations
            initiateMinerDests(11);
            minerDestOrder = new int[]{5, 0, 10, 8, 2, 4, 6, 9, 1, 3, 7};
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
            refineryLocs.add(hqLoc);
            // Initialize defensive locations
            setDefensivePositions();
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
                        minerIDs.add(mess[3]);
                        MapLocation targetDest = defaultMinerDests[minerDestOrder[numMiners%11]];
                        int mode = 0;
                        // Builder miner
                        if (builderMiner) {
                            mode = 5;
                            builderMinerID = mess[3];
                            tryBroadcastMessage(1, 0, 0, STARTED_TURTLE, 0, 0, 0, 0);
                        }
                        numMiners++;
                        tryBroadcastMessage(1, targetDest.x, targetDest.y, MINER_INIT_2, mess[3], mode, numMiners, 0);
                    }
                    // Initialize drone
                    else if (mess[2]%100 == DRONE_SPAWN) {
                        numDrones++;
                        dronesCommissioned--;
                        broadcastAll();
                        MapLocation defaultLoc = defensiveDroneLocs[numDrones%7];
                        tryBroadcastMessage(1, defaultLoc.x, defaultLoc.y, DRONE_TASK, mess[3], 0, 0, 0);
                    }
                    // Initialize landscaper
                    else if (mess[2]%100 == LANDSCAPER_SPAWN) {
                        dronesCommissioned--;
                        MapLocation defaultLoc = defensiveScaperLocs[numLandscapers%16];
                        numLandscapers++;
                        tryBroadcastMessage(1, defaultLoc.x, defaultLoc.y, LANDSCAPER_TASK, mess[3], 0, 0, 0);
                    }
                    // Initialize vaporator
                    else if (mess[2]%100 == VAPORATOR_CREATED) {
                        numVaporators++;
                        buildingCommissioned = false;
                    }
                    // Initialize design school
                    else if (mess[2]%100 == DESIGN_SCHOOL_CREATED) {
                        designSchoolIDs.add(mess[3]);
                        buildingCommissioned = false;
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

        // If the first ring of landscapers are in place, start building
        if (!landscaperStart && rc.senseNearbyRobots(2, rc.getTeam()).length == 8) {
            tryBroadcastMessage(3, 0, 0, LANDSCAPER_START, 0, 0, 0, 0);
            landscaperStart = true;
        }

        // Only commission something if a building/unit isn't being commissioned
        if (!buildingCommissioned && landscapersCommissioned == 0 && dronesCommissioned == 0) {

            // If there are enemies in range, prioritize defense
            RobotInfo[] robots = rc.senseNearbyRobots();
            // Check if there is an enemy nearby
            Team enemyTeam = rc.getTeam().opponent();
            boolean enemyNear = false;
            for (RobotInfo robot : robots) {
                if (robot.getTeam() == enemyTeam) {
                    enemyNear = true;
                    break;
                }
            }
            // If enemy is near, build 7 drones, then 16 landscapers
            if (enemyNear) {
                System.out.println("Check 1");
                if (numDrones < 7) {
                    // Commission drone if possible
                    if (!fulfillmentCenterIDs.isEmpty()) {
                        tryBroadcastMessage(2, 0, 0, FULFILLMENT_TASK, fulfillmentCenterIDs.get(0), 1, 0, 0);
                        dronesCommissioned++;
                    }
                    // Otherwise, look for nearest miner
                    else {
                        int minerID = findNearestMiner(robots, defensiveCenterLoc);
                        // If there is a nearby miner, commission a fulfillment center
                        if (minerID != -1) {
                            tryBroadcastMessage(2, defensiveCenterLoc.x, defensiveCenterLoc.y, MINER_TASK, minerID, 4, 1, 0);
                            buildingCommissioned = true;
                        }
                        // Otherwise try to make a miner
                        else
                            tryMakeMiner();
                    }
                }
                else if (numLandscapers < 16) {
                    // Commission landscaper if possible
                    if (!designSchoolIDs.isEmpty()) {
                        tryBroadcastMessage(2, 0, 0, DESIGN_SCHOOL_TASK, designSchoolIDs.get(0), 1, 0, 0);
                        landscapersCommissioned++;
                    }
                    // Otherwise, look for nearest miner
                    else {
                        int minerID = findNearestMiner(robots, defensiveSchoolLoc);
                        // If there is a nearby miner, commission a design school
                        if (minerID != -1) {
                            tryBroadcastMessage(2, defensiveSchoolLoc.x, defensiveSchoolLoc.y, MINER_TASK, minerID, 3, 1, 0);
                            buildingCommissioned = true;
                        }
                        // Otherwise try to make a miner
                        else
                            tryMakeMiner();
                    }
                }
            }
            
            // Otherwise, focus on building production miners if there is a certain refinery/miner ratio
            else if (numMiners < 11) {
                System.out.println("Check 2");
                if (numMiners < 6*refineryLocs.size())
                    tryMakeMiner();
            }
            
            // When miners are finished, complete the defensive ring
            // Builder miner
            else if (!builderMiner) {
                System.out.println("Check 3");
                if (tryMakeMiner())
                    builderMiner = true;
            }
            // Design School (1)
            else if (designSchoolIDs.isEmpty()) {
                System.out.println("Check 4..." + builderMinerID);
                if (builderMinerID != -1) {
                    tryBroadcastMessage(1, 0, 0, MINER_TASK, builderMinerID, 3, 1, 0);
                    buildingCommissioned = true;
                }
            }
            // Landscapers (16)
            else if (numLandscapers < 16) {
                System.out.println("Check 5");
                int newLandscapers = 16 - numLandscapers;
                tryBroadcastMessage(1, 0, 0, DESIGN_SCHOOL_TASK, designSchoolIDs.get(0), newLandscapers, 0, 0);
                landscapersCommissioned += newLandscapers;
            }
            // Fulfillment Center (1)
            else if (fulfillmentCenterIDs.isEmpty()) {
                System.out.println("Check 6");
                tryBroadcastMessage(1, 0, 0, MINER_TASK, builderMinerID, 4, 1, 0);
                buildingCommissioned = true;
            }
            // Drones (7)
            else if (numDrones < 7) {
                System.out.println("Check 7");
                int newDrones = 7 - numDrones;
                tryBroadcastMessage(1, 0, 0, FULFILLMENT_TASK, fulfillmentCenterIDs.get(0), newDrones, 0, 0);
                dronesCommissioned += newDrones;
            }
            // Net Guns (2)
            else if (numNetGuns < 2) {
                System.out.println("Check 8");
                tryBroadcastMessage(1, 0, 0, MINER_TASK, builderMinerID, 5, 1, 0);
                buildingCommissioned = true;
            }
            // Vaporator (1)
            else if (numVaporators == 0) {
                System.out.println("Check 9");
                tryBroadcastMessage(1, 0, 0, MINER_TASK, builderMinerID, 2, 1, 0);
                buildingCommissioned = true;
            }

            // After the defensive ring is finished...
            else {
                System.out.println("Check 10");
                // Check if vaporator has been built
                RobotInfo potentialVap = rc.senseRobotAtLocation(defensiveVapLoc);
                if (potentialVap != null && potentialVap.getType() == RobotType.VAPORATOR) {
                    // Commission design school towards the center
                    if (designSchoolIDs.size() < 2) {
                        tryBroadcastMessage(1, -1, -1, MINER_TASK, minerIDs.get(0), 3, 0, 0);
                        buildingCommissioned = true;
                    }
                    // Commission fulfillment center towards the center
                    else if (fulfillmentCenterIDs.size() < 2) {
                        tryBroadcastMessage(1, -1, -1, MINER_TASK, minerIDs.get(0), 4, 0, 0);
                        buildingCommissioned = true;
                    }
                    // Finally, randomly commission a miner/drone/landscaper
                    else {
                        double rand = Math.random();
                        // Miner
                        if (rand < .4)
                            tryMakeMiner();
                        // Drone
                        else if (rand < .8) {
                            tryBroadcastMessage(1, 0, 0, FULFILLMENT_TASK, fulfillmentCenterIDs.get(1), 1, 0, 0);
                            dronesCommissioned++;
                            buildingCommissioned = true;
                        }
                        // Landscaper
                        /* Commented out because no other functionality has been added for landscaper besides defense
                        else {
                            tryBroadcastMessage(1, 0, 0, DESIGN_SCHOOL_TASK, designSchoolIDs.get(1), 1, 0, 0);
                            landscapersCommissioned++;
                            buildingCommissioned = true;
                        }
                        */
                    }
                }
            } 
        }
    }

    static void runMiner() throws GameActionException {

        System.out.println("Turn count: " + turnCount);
        System.out.println("Robot mode: " + robotMode);
        System.out.println("Soup carrying: " + rc.getSoupCarrying());
        System.out.println("Num refineries: " + refineryLocs.size());
        
        // Search surroundings for HQ upon spawn
        if (turnCount == 1) {
            builderMinerInit = true;    // Special init only for builder miner
            tryBroadcastMessage(1, 0, 0, MINER_INIT_1, rc.getID(), 0, 0, 0);
            RobotInfo[] robots = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ) {
                    hqLoc = robot.getLocation();
                    break;
                }
            }
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
                else if (mess[2]%100 == MINER_INIT_2) {
                    if (mess[3] == rc.getID()) {
                        defaultUnitDest = new MapLocation(-mess[0], -mess[1]);
                        robotMode = mess[4];
                        numMiners += mess[5];
                    }
                }
                else if (mess[2]%100 == MINER_INIT_1) {
                    if (mess[3] != rc.getID())
                        numMiners++;
                }
                // Move towards location if HQ requests
                else if (mess[2]%100 == MINER_TASK) {
                    if (mess[3] == rc.getID()) {
                        buildingNum = mess[4];
                        buildingImportance = mess[5];
                        if (robotMode != 5) {
                            robotMode = 1;
                            robotDest = new MapLocation(-mess[0], -mess[1]);
                        }
                    }
                }
                // Remove HQ from refineryLocs
                else if (mess[2]%100 == STARTED_TURTLE)
                    refineryLocs.remove(hqLoc);
                updateLocs(mess, 0);    // Update soup locations
                updateLocs(mess, 1);    // Update water locations
                updateLocs(mess, 2);    // Update refinery locations
            }
        }

        checkSoupWater();
        checkForEnemyHQ();
        MapLocation currLoc = rc.getLocation();

        // Explore mode
        if (robotMode == 0) {
            // Go to nearest soup if soup has been found
            if (!soupLocs.isEmpty()) {
                int lsd = 8000;
                for (MapLocation soupLoc : soupLocs) {
                    if (currLoc.distanceSquaredTo(soupLoc) < lsd) {
                        lsd = currLoc.distanceSquaredTo(soupLoc);
                        robotDest = soupLoc;
                    }
                }
                findingSoup = true;
                robotMode = 1;
            }
            // Otherwise explore move to destination until there
            else {
                // robotDest default locations
                if (robotDest == null)
                    if (defaultUnitDest != null)
                        robotDest = defaultUnitDest;
                    else
                        robotDest = new MapLocation(0, 0);
                if (!exploreMove(robotDest))
                    newMinerDest();
            }
        }

        // Bug move towards destination mode (0)
        if (robotMode == 1) {
            // Check if next to refinery
            if (findingRefinery) {
                // If miner senses that refinery isn't there, find a new refinery
                if (rc.canSenseLocation(robotDest)) {
                    RobotInfo robot = rc.senseRobotAtLocation(robotDest);
                    if (robot != null && robot.getTeam() != rc.getTeam() || 
                        (robot.getType() != RobotType.REFINERY && robot.getType() != RobotType.HQ)) {
                        refineryLocs.remove(robotDest);
                        newMinerDest();
                    }
                }
                // Check surrounding square for refineries
                RobotInfo[] robots = rc.senseNearbyRobots(2);
                for (RobotInfo robot : robots) {
                    if (robot.getTeam() == rc.getTeam() && 
                        (robot.getType() == RobotType.REFINERY || robot.getType() == RobotType.HQ)) {
                        findingRefinery = false;
                        robotMode = 3;
                        break;
                    }
                }
            }
            // Check if next to soup
            else if (findingSoup) {
                MapLocation[] nearbySoups = rc.senseNearbySoup(2);
                if (nearbySoups != null && nearbySoups.length != 0) {
                    robotMode = 2;
                    findingSoup = false;
                }
            }
            // Bugmove towards destination until arrived
            if (currLoc.isAdjacentTo(robotDest)) {
                destDir = currLoc.directionTo(robotDest);
                // Change to mine if there's soup
                if (rc.senseSoup(robotDest) != 0) {
                    findingSoup = false;
                    robotMode = 2;
                }
                // Change to build if given building by HQ
                else if (buildingNum != 0)
                    robotMode = 4;
                // Change to refine if there's a refinery or HQ
                else {
                    RobotInfo robot = rc.senseRobotAtLocation(robotDest);
                    if (robot != null && (robot.getType() == RobotType.REFINERY || robot.getType() == RobotType.HQ)) {
                        findingRefinery = false;
                        robotMode = 3;
                    }
                    else
                        newMinerDest();
                }
            }
            else
                bugMoveJ(robotDest);
        }
        
        // Mine soup mode (2)
        else if (robotMode == 2) {
            MapLocation[] nearbySoup = rc.senseNearbySoup(2);
            if (nearbySoup == null || nearbySoup.length == 0 || rc.getSoupCarrying() == 100)
                newMinerDest();
            else {
                MapLocation soupLoc = nearbySoup[0];
                tryMine(currLoc.directionTo(soupLoc));
                if (rc.senseSoup(soupLoc) == 0) {
                    soupLocs.remove(soupLoc);
                    tryBroadcastMessage(1, soupLoc.x, soupLoc.y, SOUP_GONE, 0, 0, 0, 0);
                }
            }
        }
        
        // Refine soup mode (3)
        else if (robotMode == 3) {
            RobotInfo[] robots = rc.senseNearbyRobots(2);
            for (RobotInfo robot : robots) {
                RobotType type = robot.getType();
                if (robot.getTeam() == rc.getTeam() && (type == RobotType.REFINERY || type == RobotType.HQ)) {
                    destDir = currLoc.directionTo(robot.getLocation());
                    break;
                }
            }
            if (robots == null || tryRefine(destDir))
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

        // Special mode for builder miner
        // Wait around HQ and build the defensive formation until it is complete 
        else if (robotMode == 5) {

            // Set defensive positions upon spawn
            if (builderMinerInit) {
                System.out.println("check 1");
                builderMinerInit = false;
                setDefensivePositions();
            }

            // Build design school when commissioned
            if (builderMinerCount == 0) {
                System.out.println("check 2");
                if (buildingNum == 3) {
                    if (currLoc.isAdjacentTo(defensiveSchoolLoc)) {
                        if (tryBuild(RobotType.DESIGN_SCHOOL, currLoc.directionTo(defensiveSchoolLoc)))
                            builderMinerCount++;
                    }
                    else
                        bugMoveJ(defensiveSchoolLoc);
                }
                else {
                    MapLocation waitLoc = defensiveSchoolLoc.subtract(currLoc.directionTo(hqLoc));
                    if (!waitLoc.equals(currLoc))
                        bugMoveJ(waitLoc);
                }
            }

            // Build fulfillment center when commissioned
            else if (builderMinerCount == 1) {
                System.out.println("check 3");
                if (buildingNum == 4) {
                    if (currLoc.isAdjacentTo(defensiveCenterLoc)) {
                        if (tryBuild(RobotType.FULFILLMENT_CENTER, currLoc.directionTo(defensiveCenterLoc)))
                            builderMinerCount++;
                    }
                    else
                        bugMoveJ(defensiveCenterLoc);
                }
                else {
                    MapLocation waitLoc = defensiveCenterLoc.subtract(currLoc.directionTo(hqLoc));
                    if (!waitLoc.equals(currLoc))
                        bugMoveJ(waitLoc);
                }
            }

            // Build net guns when commissioned
            else if (builderMinerCount <= 3) {
                System.out.println("check 4");
                if (buildingNum == 5) {
                    if (currLoc.isAdjacentTo(defensiveGunLocs[builderMinerCount-2])) {
                        if (tryBuild(RobotType.NET_GUN, currLoc.directionTo(defensiveGunLocs[builderMinerCount-2])))
                            builderMinerCount++;
                    }
                    else
                        bugMoveJ(defensiveGunLocs[builderMinerCount-2]);
                }
                else {
                    MapLocation waitLoc = defensiveGunLocs[builderMinerCount-2].subtract(currLoc.directionTo(hqLoc));
                    if (!waitLoc.equals(currLoc))
                        bugMoveJ(waitLoc);
                }
            }

            // Build vaporator when commissioned
            else if (builderMinerCount == 4) {
                System.out.println("check 5");
                if (buildingNum == 2) {
                    if (currLoc.isAdjacentTo(defensiveVapLoc)) {
                        if (tryBuild(RobotType.VAPORATOR, currLoc.directionTo(defensiveVapLoc)))
                            robotMode = 0;
                    }
                    else
                        bugMoveJ(defensiveVapLoc);
                }
                else {
                    MapLocation waitLoc = defensiveVapLoc.subtract(currLoc.directionTo(hqLoc));
                    if (!waitLoc.equals(currLoc))
                        bugMoveJ(waitLoc);
                }
            }   
        }
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
            getHqLocFromBlockchain();
            unitsQueued = 0;
        }
        
        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == DESIGN_SCHOOL_TASK && mess[3] == rc.getID())
                unitsQueued+=mess[4];
        }
        
        // Try to make a landscaper if one is queued
        Direction dir = rc.getLocation().directionTo(hqLoc).opposite();
        if (unitsQueued > 0 && tryBuildAround(RobotType.LANDSCAPER, dir))
            unitsQueued--;
    }

    static void runFulfillmentCenter() throws GameActionException {

        // Upon spawn...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, FULFILLMENT_CREATED, rc.getID(), 0, 0, 0);
            getHqLocFromBlockchain();
            unitsQueued = 0;
        }
        
        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == FULFILLMENT_TASK && mess[0] == rc.getID())
                unitsQueued+=mess[4];
        }
        
        // Try to make a drone if one is queued
        Direction dir = rc.getLocation().directionTo(hqLoc);
        if ((unitsQueued > 0) && tryBuildAround(RobotType.LANDSCAPER, dir))
            unitsQueued--;
    }
        
    static void runNetGun() throws GameActionException {
        
        // Upon spawning...
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, NET_GUN_CREATED, 0, 0, 0, 0);
        }

        tryBroadcastQueue();
        
        // Shoot closest enemy delivery drone
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        MapLocation currLoc = rc.getLocation();
        int targetID = -1;
        int lsd = 8000;
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE && currLoc.distanceSquaredTo(robot.getLocation()) < lsd) {
                lsd = currLoc.distanceSquaredTo(robot.getLocation());
                targetID = robot.getID();
            }
        }
        if (targetID != -1 && rc.canShootUnit(targetID))
            rc.shootUnit(targetID);
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
                if (mess[2]%100 == LANDSCAPER_TASK && mess[3] == rc.getID()) {
                    robotDest = new MapLocation(-mess[0], -mess[1]);
                    robotMode = mess[4];
                    System.out.println("Yo shit my dest is: " + robotDest);
                }
                else if (mess[2]%100 == LANDSCAPER_START)
                    landscaperStart = true;
                // Enemy HQ Found
                else if (enemyHQLoc == null && mess[2]%100 == ENEMY_HQ_FOUND)
                    enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
            }
        }

        // Only start running until it gets a robotDest
        if (robotDest == null)
            return;

        MapLocation currLoc = rc.getLocation();

        // Two-layer defense mode (0)
        if (robotMode == 0) {
            // Move to designated position
            if (!currLoc.equals(robotDest))
                bugMoveJ(robotDest);
            // Trump Inc.
            else if (landscaperStart) {
                // Establish direction to dump dirt when arriving at destination
                if (destDir == null) {
                    // If next to HQ, dump dirt on itself
                    if (currLoc.isAdjacentTo(hqLoc))
                        destDir = Direction.CENTER;
                    // Otherwise, rotate left if not one of (1, -2), (2, 1), (-1, 2), (-2, -1)
                    else {
                        destDir = currLoc.directionTo(hqLoc);
                        int dx = currLoc.x - hqLoc.x;
                        int dy = currLoc.y - hqLoc.y;
                        // Check if (dx, dy) is one of (-1, -2), (-2, 1), (1, 2), (2, -1)
                        if ((dx == -1 && dy == -2) || (dx == -2 && dy == 1) || (dx == 1 && dy == 2) || (dx == 2 && dy == -1))
                            destDir = destDir.rotateLeft();
                    }
                }
                // Dig dirt if not carrying any dirt
                if (rc.getDirtCarrying() == 0)
                    tryDig(currLoc.directionTo(hqLoc).opposite());
                // Otherwise dump dirt
                else if (rc.canDepositDirt(destDir))
                    rc.depositDirt(destDir);
            }
        }

        // Attack mode assuming landscapers are spawning next to/near the enemy HQ (1)
        else if (robotMode == 1) {

            checkForEnemyHQ(); 
            
            currLoc = rc.getLocation();
            
            if(!rc.canMove(currLoc.directionTo(robotDest))) {
            	
            }

            // Move to designated position
            else if (!currLoc.equals(robotDest))
                bugMoveJ(robotDest);
            // Trump Inc.
            else {
                // Establish direction to dump dirt when arriving at destination
                if (destDir == null) {
                    // If next to HQ, dump dirt on itself
                    if (currLoc.isAdjacentTo(enemyHQLoc))
                        destDir = currLoc.directionTo(enemyHQLoc);


                }
                // Dig dirt if not carrying any dirt
                if (rc.getDirtCarrying() == 0)
                    tryDig(currLoc.directionTo(enemyHQLoc).opposite());
                // Otherwise dump dirt
                else if (rc.canDepositDirt(destDir))
                    rc.depositDirt(destDir);
            }
        } 
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
                else if (mess[2]%100 == DRONE_TASK && mess[3] == rc.getID()) {
                    defaultUnitDest = new MapLocation(-mess[0], -mess[1]);
                    robotMode = mess[4];
                }
                // Enemy HQ Found
                if (enemyHQLoc == null && mess[2]%100 == ENEMY_HQ_FOUND)
                    enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
                updateLocs(mess, 0);    // Update soup locations
                updateLocs(mess, 1);    // Update water locations
                updateLocs(mess, 2);    // Update refinery locations
            }
        }
 
        checkForEnemyHQ();
        checkSoupWater();

        if (cowCooldown > 0)
            cowCooldown--;

        MapLocation currLoc = rc.getLocation();

        // If holding something, either drop off or keep moving
        if (rc.isCurrentlyHoldingUnit()) {
            destDir = currLoc.directionTo(robotDest);
            // If you've arrived at the destination
            if (currLoc.isAdjacentTo(robotDest)) {
                // If cow, try to drop off cow anywhere near
                if (isCow) {
                    if (tryDropAround(destDir)) {
                        isCow = false;
                        cowCooldown = 10;
                    }
                }
                // Otherwise, drop off unit if there's water
                else {
                    if (rc.senseFlooding(robotDest))
                        if (tryDrop(destDir))
                            expectWater = false;
                    else {
                        if (expectWater && waterLocs.contains(robotDest)) {
                            expectWater = false;
                            waterLocs.remove(robotDest);
                        }
                        robotDest = findLeftCorner();
                    }
                }
            }

            // Otherwise, keep moving towards destination
            else {
                // Update path towards closest known water if any has been found
                if (!isCow && !waterLocs.isEmpty()) {
                    int lsd = 8000;
                    for (MapLocation waterLoc : waterLocs) {
                        if (currLoc.distanceSquaredTo(waterLoc) < lsd) {
                            robotDest = waterLoc;
                            lsd = currLoc.distanceSquaredTo(waterLoc);
                        }
                    }
                    expectWater = true;
                }
                if (!exploreMove(robotDest))
                    robotDest = findLeftCorner();
            }
        }

        // If not holding anything, look for enemy units to attack
        else {
            Team enemy = rc.getTeam().opponent();
            // Pick up any enemy units/cows within striking range
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            if (robots.length > 0) {
                // Make sure the robot is either an enemy or a non-recent cow
                RobotInfo robot = null;
                for (RobotInfo ri : robots) {
                    if (ri.getType() != RobotType.COW || cowCooldown == 0) {
                        robot = ri;
                        break;
                    }
                }
                if (robot != null) {
                    rc.pickUpUnit(robot.getID());
                    // If a cow is picked up, do our best to path towards enemy HQ
                    if (robot.getType() == RobotType.COW) {
                        if (enemyHQLoc != null)
                            robotDest = enemyHQLoc;
                        else
                            robotDest = predictedEnemyHQ();
                        isCow = true;
                    }
                    // If an enemy unit is picked up
                    else {
                        // Path towards closest known water if any has been found
                        if (!waterLocs.isEmpty()) {
                            int lsd = 8000;
                            for (MapLocation waterLoc : waterLocs) {
                                if (currLoc.distanceSquaredTo(waterLoc) < lsd) {
                                    robotDest = waterLoc;
                                    lsd = currLoc.distanceSquaredTo(waterLoc);
                                }
                            }
                            expectWater = true;
                        }  
                        // Path towards nearest corner if no water known (drone can get stuck in corner but oh well)
                        else 
                            robotDest = findNearestCorner();
                    }
                }
            }
            // Otherwise path towards any enemy units sensed
            else {
                boolean movedTowardsEnemy = false;
                robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemy);
                if (robots.length > 0) {
                    for (RobotInfo robot : robots) {
                        if (robot.getType() != RobotType.COW || cowCooldown == 0) {
                            exploreMove(robot.getLocation());
                            movedTowardsEnemy = true;
                            break;
                        }
                    }  
                }
                if (!movedTowardsEnemy) {
                    // Defense mode (0) - default to defensive position
                    if (robotMode == 0) {
                        robotDest = defaultUnitDest;
                    }
                    // Normal mode (1) - default to center of map
                    else if (robotMode == 1) {
                        robotDest = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
                    }
                    // Attack mode (2) - default to enemy hq
                    else if (robotMode == 2) {
                        if (enemyHQLoc == null)
                            robotDest = predictedEnemyHQ();
                        else
                            robotDest = enemyHQLoc;
                    }
                    if (currLoc != robotDest)
                        exploreMove(robotDest);
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
        if (rc.getLocation().equals(destination))
            return false;
        MapLocation currLoc = rc.getLocation();
        // Make best diagonal move towards destination
        Direction dir = currLoc.directionTo(destination);
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
        Direction[] toTry = {
            dir,
            dir.rotateLeft(),
            dir.rotateLeft().rotateLeft(),
            dir.rotateRight(),
            dir.rotateRight().rotateRight()
        };
        for (Direction d : toTry)
            if(tryMove(d))
                return true;
        return !rc.getLocation().equals(destination);
    }

    /**
     * Makes the optimal bug move towards a location
     * 
     * @param destination The intended destination
     * @throws GameActionException
     */
    static void bugMove(MapLocation destination) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(destination);
        if (dir != null) {
            Direction[] toTry = {
                dir,
                dir.rotateLeft(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateRight(),
                dir.rotateRight().rotateRight()
            };
            for (Direction d : toTry)
                if(tryMove(d))
                    return;
        }
    }

    /**
     * Establish left/right preference based on first time you can't move to destination (left default)
     * Get rid of preference upon being able to directly move towards destination
     * Save last MapLocation and don't go back to it
     * 
     * @param destination The intended destination
     * @throws GameActionException
     */
    static void bugMoveJ(MapLocation destination) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(destination);
        // If able to move directly towards destination, do so and reset tendency
        if (tryMoveNew(dir)) {
            bugDirectionTendency = 0;
            return;
        }
        // If no tendency, find tendency
        if (bugDirectionTendency == 0) {
            if (tryMoveNew(dir.rotateLeft())) {
                bugDirectionTendency = 1;
                return;
            }
            if (tryMoveNew(dir.rotateRight())) {
                bugDirectionTendency = 2;
                return;
            }
            if (tryMoveNew(dir.rotateLeft().rotateLeft())) {
                bugDirectionTendency = 1;
                return;
            }
            if (tryMoveNew(dir.rotateRight().rotateRight())) {
                bugDirectionTendency = 2;
                return;
            }
            if (tryMoveNew(dir.opposite().rotateRight())) {
                bugDirectionTendency = 1;
                return;
            }
            if (tryMoveNew(dir.opposite().rotateLeft())) {
                bugDirectionTendency = 2;
                return;
            }
            if (tryMoveNew(dir.opposite())) {
                bugDirectionTendency = 1;
                return;
            }
        }
        // Left tendency
        if (bugDirectionTendency == 1) {
            Direction[] toTry = {
                dir.rotateLeft(),
                dir.rotateLeft().rotateLeft(),
                dir.opposite().rotateRight(),
                dir.opposite(),
                dir.opposite().rotateLeft(),
                dir.rotateRight().rotateRight(),
                dir.rotateRight()
            };
            for (Direction d : toTry)
                if (tryMoveNew(d))
                    return;
        }
        // Right tendency
        if (bugDirectionTendency == 2) {
            Direction[] toTry = {
                dir.rotateRight(),
                dir.rotateRight().rotateRight(),
                dir.opposite().rotateLeft(),
                dir.opposite(),
                dir.opposite().rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateLeft()
            };
            for (Direction d : toTry)
                if (tryMoveNew(d))
                    return;
        }
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
     * Attempts to move in a given direction, but only if it doens't return to the previous location.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMoveNew(Direction dir) throws GameActionException {
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir) && !rc.senseFlooding(nextLoc) && !nextLoc.equals(lastBugPathLoc)) {
            rc.move(dir);
            lastBugPathLoc = nextLoc;
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
            buildingNum = 0;
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
     * Attempts to drop unit in a given direction.
     *
     * @param dir The intended direction of dropping
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDrop(Direction dir) throws GameActionException {
        if (rc.canDropUnit(dir)) {
            rc.dropUnit(dir);
            return true;
        }
        return false;
    }

    static boolean tryDropAround(Direction dir) throws GameActionException {
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
            if (tryDrop(d))
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
     * Finds the closest "corner" to the robot
     * 
     * @return MapLocation 3x3 away from closest corner
     * @throws GameActionException
     */
    static MapLocation findNearestCorner() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int x = (currLoc.x < width/2) ? 3 : width-4;
        int y = (currLoc.y < height/2) ? 3 : height-4;
        return new MapLocation(x, y);
    }

    /**
     * Finds the "corner" to the "left" of the robot
     * 
     * @return MapLocation near a corner
     * @throws GameActionException
     */
    static MapLocation findLeftCorner() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        if (currLoc.x < width/2 && currLoc.y >= height/2)
            return new MapLocation(3, 3);
        if (currLoc.x >= width/2 && currLoc.y >= height/2)
            return new MapLocation(3, height-4);
        if (currLoc.x < width/2 && currLoc.y < height/2)
            return new MapLocation(width-4, 3);
        // if (currLoc.x >= width/2 && currLoc.y < height/2)
        return new MapLocation(width-4, height-4);
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
     * Searches a list of robots and finds the miner closest to a specified location
     * Returns -1 if no miner found (i.e. nearbyRobots is empty)
     * 
     * @param nearbyRobots list of MapLocations containing robots
     * @param loc location to find closest miner to
     * @return id of closest robot
     * @throws GameActionException
     */
    static int findNearestMiner(RobotInfo[] nearbyRobots, MapLocation loc) {
        int id = -1;
        int lsd = 8000; // Arbitrarily large number greater than 64^2+64^2
        for (RobotInfo robot : nearbyRobots) {
            if (robot.getType() == RobotType.MINER && robot.getTeam() == rc.getTeam() && 
                robot.getLocation().distanceSquaredTo(loc) < lsd) {
                id = robot.getID();
                lsd = robot.getLocation().distanceSquaredTo(loc);
            }
        }
        return id;
    }

    /**
     * Sets the MapLocations/ArrayLists containing the defensive positions for the buildings and drones
     * 
     * @throws GameActionException
     */
    static void setDefensivePositions() throws GameActionException {

        // Initialize arrays
        defensiveGunLocs = new MapLocation[2];
        defensiveDroneLocs = new MapLocation[7];
        defensiveScaperLocs = new MapLocation[16];

        // First do the landscapers
        // First layer
        int count = 0;
        for (Direction d : directions)
            defensiveScaperLocs[count++] = hqLoc.add(d);
        // Second layer
        // Sadly it's more efficient to hardcode this (I think)
        defensiveScaperLocs[8]  = hqLoc.translate(-2,-1);
        defensiveScaperLocs[9]  = hqLoc.translate(-2, 1);
        defensiveScaperLocs[10] = hqLoc.translate(-1,-2);
        defensiveScaperLocs[11] = hqLoc.translate(-1, 2);
        defensiveScaperLocs[12] = hqLoc.translate(1 ,-2);
        defensiveScaperLocs[13] = hqLoc.translate(1 , 2);
        defensiveScaperLocs[14] = hqLoc.translate(2 ,-1);
        defensiveScaperLocs[15] = hqLoc.translate(2 , 1);

        // Now do everything else...

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
                defensiveVapLoc = hqLoc.translate(s*-3, s*3);
                defensiveCenterLoc = hqLoc.translate(s*1, s*3);
                defensiveSchoolLoc = hqLoc.translate(s*-3, s*-1);
                defensiveGunLocs[0] = hqLoc.translate(s*3, s*1);
                defensiveGunLocs[1] = hqLoc.translate(s*-1, s*-3);
                defensiveDroneLocs[0] = hqLoc.translate(s*3, s*3);
                defensiveDroneLocs[1] = hqLoc.translate(s*3, 0);
                defensiveDroneLocs[2] = hqLoc.translate(s*3, s*-1);
                defensiveDroneLocs[3] = hqLoc.translate(s*3, s*-3);
                defensiveDroneLocs[4] = hqLoc.translate(s*1, s*-3);
                defensiveDroneLocs[5] = hqLoc.translate(0, s*-3);
                defensiveDroneLocs[6] = hqLoc.translate(s*-3, s*-3);
            }
            // Top Right/Bottom Left
            else {
                defensiveVapLoc = hqLoc.translate(s*3, s*3);
                defensiveCenterLoc = hqLoc.translate(s*3, s*-1);
                defensiveSchoolLoc = hqLoc.translate(s*-1, s*3);
                defensiveGunLocs[0] = hqLoc.translate(s*1, s*-3);
                defensiveGunLocs[1] = hqLoc.translate(s*-3, s*1);
                defensiveDroneLocs[0] = hqLoc.translate(s*3, s*-3);
                defensiveDroneLocs[1] = hqLoc.translate(0, s*-3);
                defensiveDroneLocs[2] = hqLoc.translate(s*-1, s*-3);
                defensiveDroneLocs[3] = hqLoc.translate(s*-3, s*-3);
                defensiveDroneLocs[4] = hqLoc.translate(s*-3, s*-1);
                defensiveDroneLocs[5] = hqLoc.translate(s*-3, 0);
                defensiveDroneLocs[6] = hqLoc.translate(s*-3, s*3);
            }
        }

        // Edge
        else {
            // Top/Bottom
            if (closestY < mapHeight/3) {
                s = (hqLoc.y >= mapHeight/2) ? 1 : -1;
                defensiveVapLoc = hqLoc.translate(0, s*3);
                defensiveCenterLoc = hqLoc.translate(s*-3, 0);
                defensiveSchoolLoc = hqLoc.translate(s*3, 0);
                defensiveGunLocs[0] = hqLoc.translate(s*3, s*-3);
                defensiveGunLocs[1] = hqLoc.translate(s*-3, s*-3);
                defensiveDroneLocs[0] = hqLoc.translate(s*3, s*1);
                defensiveDroneLocs[1] = hqLoc.translate(s*3, s*-1);
                defensiveDroneLocs[2] = hqLoc.translate(s*1, s*-3);
                defensiveDroneLocs[3] = hqLoc.translate(0, s*-3);
                defensiveDroneLocs[4] = hqLoc.translate(s*-1, s*-3);
                defensiveDroneLocs[5] = hqLoc.translate(s*-3, s*-1);
                defensiveDroneLocs[6] = hqLoc.translate(s*-3, s*1);
            }
            // Left/Right
            else {
                s = (hqLoc.x >= mapWidth/2) ? 1 : -1;
                defensiveVapLoc = hqLoc.translate(s*3, 0);
                defensiveCenterLoc = hqLoc.translate(0, s*3);
                defensiveSchoolLoc = hqLoc.translate(0, s*-3);
                defensiveGunLocs[0] = hqLoc.translate(s*-3, s*-3);
                defensiveGunLocs[1] = hqLoc.translate(s*-3, s*3);
                defensiveDroneLocs[0] = hqLoc.translate(s*1, s*-3);
                defensiveDroneLocs[1] = hqLoc.translate(s*-1, s*-3);
                defensiveDroneLocs[2] = hqLoc.translate(s*-3, s*-1);
                defensiveDroneLocs[3] = hqLoc.translate(s*-3, 0);
                defensiveDroneLocs[4] = hqLoc.translate(s*-3, s*1);
                defensiveDroneLocs[5] = hqLoc.translate(s*-1, s*3);
                defensiveDroneLocs[6] = hqLoc.translate(s*1, s*3);
            }
        }
    }

    /**
     * Find the best locations to send a certain number of miners to explore the map
     * 
     * @param numMiners the number of miners we have
     * @return an array of MapLocations to send the miners towards
     * @throws GameActionException
     */
    static void initiateMinerDests(int numMiners) throws GameActionException {

        defaultMinerDests = new MapLocation[numMiners];

        int mapHeight = rc.getMapHeight();
        int mapWidth = rc.getMapWidth();
        
        // Find closest horizontal and vertical distance to an edge
        int closestX = Math.min(hqLoc.x, (mapWidth-1)-hqLoc.x);
        int closestY = Math.min(hqLoc.y, (mapHeight-1)-hqLoc.y);

        // Determine the widest 2 locations based on whether we are at a corner, edge, or center
        // defaultMinerDests[0] will be on the "right", defaultMinerDests[1] will be on the "right"
        // Corner case - parallel with edges
        if (closestX < mapWidth/3 && closestY < mapHeight/3) {
            // Bottom left
            if (hqLoc.x < mapWidth/2 && hqLoc.y < mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(mapWidth-1, hqLoc.y);                
                defaultMinerDests[1] = new MapLocation(hqLoc.x, mapHeight-1);
            }
            // Top left
            else if (hqLoc.x < mapWidth/2 && hqLoc.y >= mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(hqLoc.x, 0);
                defaultMinerDests[1] = new MapLocation(mapWidth-1, hqLoc.y);
            }
            // Bottom right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y < mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(hqLoc.x, mapHeight-1);
                defaultMinerDests[1] = new MapLocation(0, hqLoc.y);
            }
            // Top right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y >= mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(0, hqLoc.y);
                defaultMinerDests[1] = new MapLocation(hqLoc.x, 0);
            }
        }
        // Center case - towards a point 5 (sensor radius) units away from the middle 2 distant corners
        else if (closestX >= mapWidth/3 && closestY >= mapHeight/3) {
            // Bottom left
            if (hqLoc.x < mapWidth/2 && hqLoc.y < mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(mapWidth-1, 5);
                defaultMinerDests[1] = new MapLocation(5, mapHeight-1);
            }
            // Top left
            else if (hqLoc.x < mapWidth/2 && hqLoc.y >= mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(5, 0);
                defaultMinerDests[1] = new MapLocation(mapWidth-1, mapHeight-6);
            }
            // Bottom right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y < mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(mapWidth-6, mapHeight-1);
                defaultMinerDests[1] = new MapLocation(0, 5);
            }
            // Top right
            else if (hqLoc.x >= mapWidth/2 && hqLoc.y >= mapHeight/2) {
                defaultMinerDests[0] = new MapLocation(0, mapHeight-6);
                defaultMinerDests[1] = new MapLocation(mapWidth-6, 0);
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
                    defaultMinerDests[0] = new MapLocation(mapWidth/2-offset, 0);
                    defaultMinerDests[1] = new MapLocation(mapWidth/2+offset, mapHeight-1);
                }
                // Right edge
                else if (mapWidth-hqLoc.x-1 < mapWidth/3) {
                    defaultMinerDests[0] = new MapLocation(mapWidth/2-offset, mapHeight-1);
                    defaultMinerDests[1] = new MapLocation(mapWidth/2+offset, 0);
                }
                // Bottom edge
                else if (hqLoc.y < mapHeight/3) {
                    defaultMinerDests[0] = new MapLocation(mapWidth-1, mapHeight/2+offset);
                    defaultMinerDests[1] = new MapLocation(0, mapHeight/2-offset);
                }
                // Top edge
                else if (mapHeight-hqLoc.y-1 < mapHeight/3) {
                    defaultMinerDests[0] = new MapLocation(0, mapHeight/2+offset);
                    defaultMinerDests[1] = new MapLocation(mapWidth-1, mapHeight/2-offset);
                }
            }
            // Parallel with edge
            else {
                // Left edge
                if (hqLoc.x < mapWidth/3) {
                    defaultMinerDests[0] = new MapLocation(hqLoc.x, 0);
                    defaultMinerDests[1] = new MapLocation(hqLoc.x, mapHeight-1);
                }
                // Right edge
                else if (mapWidth-hqLoc.x-1 < mapWidth/3) {
                    defaultMinerDests[0] = new MapLocation(hqLoc.x, mapHeight-1);
                    defaultMinerDests[1] = new MapLocation(hqLoc.x, 0);
                }
                // Bottom edge
                else if (hqLoc.y < mapHeight/3) {
                    defaultMinerDests[0] = new MapLocation(mapWidth-1, hqLoc.y);
                    defaultMinerDests[1] = new MapLocation(0, hqLoc.y);
                }
                // Top edge
                else if (mapHeight-hqLoc.y-1 < mapHeight/3) {
                    defaultMinerDests[0] = new MapLocation(0, hqLoc.y);
                    defaultMinerDests[1] = new MapLocation(mapWidth-1, hqLoc.y);
                }
            }
        }

        // Find the other numMiners-2 locations between the two widest locations
        // Vectors between the 2 widest points and hq
        int v1x = defaultMinerDests[0].x - hqLoc.x;
        int v1y = defaultMinerDests[0].y - hqLoc.y;
        int v2x = defaultMinerDests[1].x - hqLoc.x;
        int v2y = defaultMinerDests[1].y - hqLoc.y;
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
            defaultMinerDests[i+1] = new MapLocation((int) Math.round(px), (int) Math.round(py));
        }
    }

    /**
     * Attempt to build a miner (in the "optimal" direction away from the HQ")
     * Broadcast all necessary information upon miner creation
     * 
     * @return whether a miner was created
     * @throws GameActionException
     */
    static boolean tryMakeMiner() throws GameActionException {
        if (rc.getTeamSoup() >= 70 && tryBuildAround(RobotType.MINER, 
            rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2)))) {
            broadcastAll();
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
        // If it's carrying soup, go to the nearest refinery or build a refinery
        if (rc.getSoupCarrying() != 0) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo robot : nearbyRobots) {
                if (robot.getType() == RobotType.REFINERY || robot.getType() == RobotType.HQ) {
                    tryRefine(currLoc.directionTo(robot.getLocation()));
                    return;
                }
            }
            // If not enough refineries per miner, build refinery
            if (numMiners >= 6*refineryLocs.size() && rc.getTeamSoup() >= 200)
                tryBuildAround(RobotType.REFINERY, currLoc.directionTo(hqLoc).opposite());
            // Otherwise build refinery or go to closest refinery 
            else {
                // Find distance to closest refinery
                int lsd = 8000;
                for (MapLocation refineryLoc : refineryLocs) {
                    if (currLoc.distanceSquaredTo(refineryLoc) < lsd) {
                        robotDest = refineryLoc;
                        lsd = currLoc.distanceSquaredTo(refineryLoc);
                    }
                }
                // If closest refinery is too far, build refinery
                if (lsd > 100) {
                    if (rc.getTeamSoup() >= 200)
                        tryBuildAround(RobotType.REFINERY, currLoc.directionTo(hqLoc).opposite());
                }
                // If closest refinery is close enough, just go to refinery
                else {
                    findingRefinery = true;
                    robotMode = 1;
                }
            }
        }
        // Otherwise, if there's known soup, go to the location of the nearest soup
        else if (!soupLocs.isEmpty()) {
            int lsd = 8000;
            for (MapLocation soupLoc : soupLocs) {
                if (currLoc.distanceSquaredTo(soupLoc) < lsd) {
                    lsd = currLoc.distanceSquaredTo(soupLoc);
                    robotDest = soupLoc;
                }
            }
            findingSoup = true;
            robotMode = 1;
        }
        // Otherwise, if it wasn't already exploring, look to HQ for a destination to explore to
        else if (robotMode != 0) {
            robotMode = 0;
            robotDest = defaultUnitDest;
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
            for (RobotInfo robot : rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent())) {
                if (robot.getType() == RobotType.HQ) {
                    enemyHQLoc = robot.getLocation();
                    tryBroadcastMessage(1, enemyHQLoc.x, enemyHQLoc.y, ENEMY_HQ_FOUND, 0, 0, 0, 0);
                    return;
                }
            }
        }
    }

    /**
     * Checks for new soup and new water in the outer two rings of the unit's vision
     * 
     * @throws GameActionException
     */
    static void checkSoupWater() throws GameActionException {
        // New Soup
        MapLocation[] nearbySoups = rc.senseNearbySoup();
        for (MapLocation loc : nearbySoups) {
            if (!soupLocs.contains(loc)) {
                soupLocs.add(loc);
                tryBroadcastMessage(1, loc.x, loc.y, SOUP_FOUND, 0, 0, 0, 0);
            }
        }

        // New water
        MapLocation currLoc = rc.getLocation();
        int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
        for (int i=-rad; i<=rad && Math.abs(i) > rad-2; i++) {
            for (int j=-rad; j<=rad && Math.abs(j) > rad-2; j++) {
                MapLocation loc = currLoc.translate(i,j);
                if (rc.canSenseLocation(loc) && rc.senseFlooding(loc) && !waterLocs.contains(loc)) {
                    waterLocs.add(loc);
                    tryBroadcastMessage(1, loc.x, loc.y, WATER_FOUND, 0, 0, 0, 0);
                }
            }
        }
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
        Set<MapLocation> locs = new HashSet<MapLocation>();
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
        if (mess[2]%100 == createCode)
            locs.add(new MapLocation(-mess[0], -mess[1]));
        else if (mess[2]%100 == destroyCode)
            locs.remove(new MapLocation(-mess[0], -mess[1]));
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
        broadcastLocs(refineryLocs, INIT_REFINERY_LOCS);
        broadcastLocs(waterLocs, INIT_WATER_LOCS);
    }

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
     * @param m int[] holding message to broadcast
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
    static void tryBroadcastMessage(int cost, int m0, int m1, int m2, int m3, int m4, int m5, 
        int m6) throws GameActionException {
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
        while (messageQ.size() != 0) {
            if (broadcastMessage(messageQ.peek()))
                messageQ.poll();
            else
                break;
        }
    }

    /**
     * Broadcasts an ArrayList of MapLocations in a series of messages, up to 12 per message
     * 
     * @param locs the array containing the locations
     * @param code the integer representing what is at each location
     * @throws GameActionException
     */
    static void broadcastLocs(Set<MapLocation> setLocs, int code) throws GameActionException {
        // First convert to ArrayList for indexing
        List<MapLocation> locs = new ArrayList<MapLocation>(setLocs);
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
    static void decodeLocsMessage(int[] mess, Set<MapLocation> locs) throws GameActionException {
        for (int i=0; i<7 && i!=2; i++) {
            if (i < 2)
                mess[i] = -mess[i];
            if (mess[i] > 20000)
                locs.addAll(decodeLocs(mess[i]));
            else if (mess[i] > 0)
                locs.add(decodeLoc(mess[i]));
        }
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
    static Set<MapLocation> decodeLocs(int mess) throws GameActionException {
        Set<MapLocation> locs = new HashSet<MapLocation>();
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
                if(mess[2]/100 == TEAM_SECRET && mess[2]%100 == HQ_LOC){
                    hqLoc = new MapLocation(-mess[0], -mess[1]);
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
