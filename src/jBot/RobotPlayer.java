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
        RobotType.REFINERY,             // 1 (buildingNum)
        RobotType.VAPORATOR,            // 2
        RobotType.DESIGN_SCHOOL,        // 3
        RobotType.FULFILLMENT_CENTER,   // 4
        RobotType.NET_GUN               // 5
    };
    
    static int turnCount;
    
    // HQ
    // Initialization variables
    static MapLocation[] defaultMinerDests;
    static int[] minerDestOrder;
    static int groundLevel;
    // IDs
    static int lastMinerID = -1;
    static int builderMinerID = -1;
    static int designSchoolID = -1;
    static int fulfillmentCenterID = -1;
    // Number of each robot
    static int numMiners;
    static int numDrones;
    static int numLandscapers;
    static int numVaporators;
    static int numNetGuns;
    // Robots commissioned
    static boolean buildingCommissioned;
    static int landscapersCommissioned;
    static int dronesCommissioned;

    // MULTIPLE UNITS
    static boolean avoidAreas;
    static int robotMode;
    static MapLocation robotDest;
    static MapLocation defaultUnitDest;
    static MapLocation hqLoc;
    static MapLocation enemyHQLoc;
    static Queue<int[]> messageQ = new LinkedList<int[]>();

    // MINERS
    static MapLocation nearestSoup = null;
    static int lsd = 8000;
    static int buildingNum;
    static int buildingImportance;

    // LANDSCAPERS
    static MapLocation closeLoc;
    static Direction dirtDir;
    static Set<Direction> wallDirs;
    static Set<Direction> lastWallDirs;

    // DELIVERY DRONES
    static boolean toRefinery;
    static int unitType;	// 0 for cow, 1 for enemy, 2 for miner
    static int cowCooldown;
    static int minerCooldown;
    static int lastPickupID;
    static List<MapLocation> potentialEnemyHQs;

    // BUILDINGS
    static boolean halt;
    static int unitsQueued;

    // RESOURCES
    static Set<MapLocation> soupLocs     = new HashSet<MapLocation>();
    static Set<MapLocation> waterLocs    = new HashSet<MapLocation>();
    static Set<MapLocation> refineryLocs = new HashSet<MapLocation>();

    // DEFENSIVE POSITIONS
    static MapLocation defensiveVapLoc;
    static MapLocation defensiveCenterLoc;
    static MapLocation defensiveSchoolLoc;
    static MapLocation[] defensiveGunLocs;
    static MapLocation[] defensiveDroneLocs;
    static MapLocation[] defensiveScaperLocs;

    // MOVEMENT
    static MapLocation lastLoc;
    // Variables used in exploreMove
    static boolean leftTendency;
    // Variables used in bugMoveJ
    static int bugDirectionTendency;    // 0 for none, 1 for left, 2 for right
    static MapLocation lastBugPathLoc;
    // Variables used in bugMove2
    static boolean bugLeft = false;
    static MapLocation lastDest = null;
    static int lastDistance;
    static Direction obstacleDir = null;
    static Set<MapLocation> bugMovePath = new HashSet<MapLocation>();
    
    // COMMUNICATION CODES
    // NOTE: (x, y) not used in a lot of these
    static final int TEAM_SECRET            = 142;  // HAS TO BE <= 214
    static final int HQ_LOC                 = 0;    // [x, y, code]
    static final int REFINERY_CREATED       = 1;    // [x, y, code]
    static final int VAPORATOR_CREATED      = 2;    // [x, y, code]
    static final int DESIGN_SCHOOL_CREATED  = 3;    // [x, y, code, ID]
    static final int FULFILLMENT_CREATED    = 4;    // [x, y, code, ID]
    static final int NET_GUN_CREATED        = 5;    // [x, y, code]
    static final int DESIGN_SCHOOL_TASK     = 6;    // [x, y, code, ID, numLandscapers]
    static final int FULFILLMENT_TASK       = 7;    // [x, y, code, ID, numDrones]
    static final int MINER_INIT_1           = 8;    // [x, y, code, ID]
    static final int MINER_INIT_2           = 9;    // [x, y, code, ID, mode, numMiners, designSchoolMade]
    static final int MINER_TASK             = 10;   // [x, y, code, ID, buildingID (implicit 0 for no building), importance]
    static final int LANDSCAPER_SPAWN       = 11;   // [x, y, code, ID]
    static final int LANDSCAPER_TASK        = 12;   // [x, y, code, ID, activity]
    static final int LANDSCAPER_START       = 13;   // [x, y, code, ID]
    static final int DRONE_SPAWN            = 14;   // [x, y, code, ID]
    static final int DRONE_TASK             = 15;   // [x, y, code, ID, activity]
    static final int INIT_SOUP_LOCS         = 16;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code+id, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int INIT_WATER_LOCS        = 17;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code+id, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int INIT_REFINERY_LOCS     = 18;   // [1+x1+y1+x2+y2, 1+x3+y3+x4+y4, code+id, 1+x5+y5+x6+y6, ...] (up to 12 locs)
    static final int SOUP_FOUND             = 19;   // [x, y, code]
    static final int SOUP_GONE              = 20;   // [x, y, code]
    static final int WATER_FOUND            = 21;   // [x, y, code]
    static final int WATER_GONE             = 22;   // [x, y, code]
    static final int REFINERY_DESTROYED     = 23;   // [x, y, code]
    static final int ENEMY_HQ_FOUND         = 24;   // [x, y, code]
    static final int HALT_PRODUCTION        = 25;   // [x, y, code, 0 from miner (wait) or 1 from HQ (stop)]

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
            // Search surrounding square for soup and water
            int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = hqLoc.translate(i,j);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0)
                            soupLocs.add(loc);
                        if (rc.senseFlooding(loc))
                            waterLocs.add(loc);
                    }
                }
            }
            refineryLocs.add(hqLoc);
            // Initialize defensive locations
            setDefensivePositions();
            // Compute desired miner locations
            setMinerDests(11);
            minerDestOrder = new int[]{5, 0, 10, 8, 2, 4, 6, 9, 1, 3, 7};
            // Set ground level
            int hqElevation = rc.senseElevation(hqLoc);
            int viableSquares = 0;
            int tempElevation;
            for (int i=-2; i<=2; i++) {
            	for (int j=-2; j<=2; j++) {
            		tempElevation = rc.senseElevation(hqLoc.translate(i, j));
            		if (Math.abs(tempElevation-hqElevation) <= 3) {
            			groundLevel += tempElevation;
            			viableSquares++;
            		}
            	}
            }
            groundLevel /= viableSquares;
        }

        tryBroadcastQueue();

        // Process transactions past the first round
        if (turnCount != 1) {
            for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
                int[] mess = tx.getMessage();
                if(mess[2]/10000000 == TEAM_SECRET) {
                    switch (mess[2]%100) {
                        case MINER_INIT_1:
                        	halt = false;
                       		lastMinerID = mess[3];
                            robotDest = defaultMinerDests[minerDestOrder[numMiners%11]];
                            numMiners++;
                            int designSchoolMade = 0;
                            if (designSchoolID != -1)
                            	designSchoolMade = 1;
                            tryBroadcastMessage(1, robotDest.x, robotDest.y, MINER_INIT_2, mess[3], 0, numMiners, designSchoolMade);
                            broadcastAll(mess[3]);
                            break;
                        case DRONE_SPAWN:
                        	halt = false;
                            dronesCommissioned--;
                            robotDest = defensiveDroneLocs[numDrones%7];
                            numDrones++;
                            int dmode;
                            // 3 helper drones
                            if (numDrones <= 3)
                            	dmode = -1;
                            // 4 defense drones
                            else if (numDrones <= 7)
                            	dmode = 0;
                            // 5 exploratory drones
                            else if (numDrones <= 12)
                                dmode = 1;
                            // Everyone else is a turtle drone
                            else
                            	dmode = 2;
                            tryBroadcastMessage(1, robotDest.x, robotDest.y, DRONE_TASK, mess[3], dmode, 0, 0);
                            broadcastAll(mess[3]);
                            break;
                        case LANDSCAPER_SPAWN:
                        	halt = false;
                            landscapersCommissioned--;
                            robotDest = defensiveScaperLocs[numLandscapers%16];
                            numLandscapers++;
                            tryBroadcastMessage(1, robotDest.x, robotDest.y, LANDSCAPER_TASK, mess[3], 0, 0, 0);
                            break;
                        case VAPORATOR_CREATED:
                        	halt = false;
                            numVaporators++;
                            buildingCommissioned = false;
                            break;
                        case DESIGN_SCHOOL_CREATED:
                        	halt = false;
                            designSchoolID = mess[3];
                            buildingCommissioned = false;
                            break;
                        case FULFILLMENT_CREATED:
                        	halt = false;
                            fulfillmentCenterID = mess[3];
                            buildingCommissioned = false;
                            break;
                        case NET_GUN_CREATED:
                        	halt = false;
                            numNetGuns++;
                            buildingCommissioned = false;
                            break;
                    }
                    updateLocs(mess, 0);    // Update soup locations
                    updateLocs(mess, 1);    // Update water locations
                    updateLocs(mess, 2);    // Update refinery locations
                }
            }
        }
        
        // If the first ring of landscapers are in place, all other units should avoid the area to not get trapped
        if (!avoidAreas) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2, rc.getTeam());
            if (nearbyRobots.length == 8) {
                avoidAreas = true;
                for (RobotInfo robot : nearbyRobots) {
                    if (robot.getType() != RobotType.LANDSCAPER) {
                        avoidAreas = false;
                        break;
                    }
                }
                if (avoidAreas)
                    tryBroadcastMessage(2, 0, 0, LANDSCAPER_START, 0, 0, 0, 0);
            }
        }

        // Make a new schoolMake sure design school/fulfillment center haven't been destroyed
        if (designSchoolID != -1 && rc.canSenseLocation(defensiveSchoolLoc)) {
        	RobotInfo robot = rc.senseRobotAtLocation(defensiveSchoolLoc);
        	if (robot == null || robot.getType() != RobotType.DESIGN_SCHOOL || robot.getTeam() != rc.getTeam()) {
        		designSchoolID = -1;
        		landscapersCommissioned = 0;
        	}
        }
        if (fulfillmentCenterID != -1 && rc.canSenseLocation(defensiveCenterLoc)) {
        	RobotInfo robot = rc.senseRobotAtLocation(defensiveCenterLoc);
        	if (robot == null || robot.getType() != RobotType.FULFILLMENT_CENTER || robot.getTeam() != rc.getTeam()) {
        		fulfillmentCenterID = -1;
        		dronesCommissioned = 0;
        	}
        }

        // First, if there are any, shoot the closest enemy delivery drone
        Team enemyTeam = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemyTeam);
        boolean groundUnit = false;
        int targetID = -1;
        int lddd = 8000;
        for (RobotInfo enemy : enemies) {
            int ddd = hqLoc.distanceSquaredTo(enemy.getLocation());
            if (enemy.getType() == RobotType.DELIVERY_DRONE) {
            	if (ddd < lddd) {
	                lddd = ddd;
	                targetID = enemy.getID();
               	}
            }
            else
            	groundUnit = true;
        }
        if (targetID != -1 && rc.canShootUnit(targetID))
            rc.shootUnit(targetID);

       	// ROBOT COMMISSIONS: all commission to build a robot will be done in the following block
        // Only commission something if a building/unit isn't being commissioned or if there is an emergency
        // Priority is: enemies, island, defense, enddrones
        if (!buildingCommissioned && landscapersCommissioned == 0 && dronesCommissioned == 0) {

        	RobotInfo[] robots = rc.senseNearbyRobots();
            // Check if there is an enemy nearby
            boolean enemyNear = false;
            for (RobotInfo robot : robots) {
                if (robot.getTeam() == enemyTeam) {
                    enemyNear = true;
                    break;
                }
            }
            // If enemy is near, prioritize defense: build 7 drones, then 16 landscapers
            if (enemyNear) {
                if (numDrones < 7) {
                    // Commission drone if possible
                    if (fulfillmentCenterID != -1) {
                        tryBroadcastMessage(2, 0, 0, FULFILLMENT_TASK, fulfillmentCenterID, 1, 0, 0);
                        dronesCommissioned++;
                    }
                    // Otherwise, commission fulfillment center to the nearest miner
                    else {
                        int minerID = findNearestMiner(robots, defensiveCenterLoc);
                    	defensiveCenterLoc = suitableLoc(defensiveCenterLoc);
                        tryBroadcastMessage(2, defensiveCenterLoc.x, defensiveCenterLoc.y, MINER_TASK, minerID, 4, 1, 0);
                        buildingCommissioned = true;
                    }
                }
                else if (numLandscapers < 16) {
                    // Commission landscaper if possible
                    if (designSchoolID != -1) {
                        tryBroadcastMessage(2, 0, 0, DESIGN_SCHOOL_TASK, designSchoolID, 1, 0, 0);
                        landscapersCommissioned++;
                    }
                    // Otherwise, commission design school to the nearest miner
                    else {
                        int minerID = findNearestMiner(robots, defensiveSchoolLoc);
                    	defensiveSchoolLoc = suitableLoc(defensiveSchoolLoc);
                        tryBroadcastMessage(2, defensiveSchoolLoc.x, defensiveSchoolLoc.y, MINER_TASK, minerID, 3, 1, 0);
                        buildingCommissioned = true;
                    }
                }
            }

            // Otherwise, focus on building production miners if there is a certain refinery/miner ratio
            // Since HQ won't build a miner at 6 miners and 1 refinery, it forces a refinery to be built after 6 miners
            else if (numMiners < 11) {
                if (numMiners < 6*refineryLocs.size())
                    tryMakeMiner();
            }
            
            // When miners are finished, complete the defensive ring
            // Fulfillment Center (1)
            else if (fulfillmentCenterID == -1) {
            	defensiveCenterLoc = suitableLoc(defensiveCenterLoc);
            	int minerID = findNearestMiner(robots, defensiveCenterLoc);
                tryBroadcastMessage(1, defensiveCenterLoc.x, defensiveCenterLoc.y, MINER_TASK, minerID, 4, 1, 0);
                buildingCommissioned = true;
            }
            // Drones (7)
            else if (numDrones < 3) {
                int newDrones = 3 - numDrones;
                tryBroadcastMessage(1, 0, 0, FULFILLMENT_TASK, fulfillmentCenterID, newDrones, 0, 0);
                dronesCommissioned += newDrones;
            }
            // Design School (1)
            else if (designSchoolID == -1) {
            	defensiveSchoolLoc = suitableLoc(defensiveSchoolLoc);
            	int minerID = findNearestMiner(robots, defensiveSchoolLoc);
                tryBroadcastMessage(1, defensiveSchoolLoc.x, defensiveSchoolLoc.y, MINER_TASK, minerID, 3, 1, 0);
                buildingCommissioned = true;
            }
            // Landscapers (16)
            else if (numLandscapers < 16) {
                int newLandscapers = 16 - numLandscapers;
                tryBroadcastMessage(1, 0, 0, DESIGN_SCHOOL_TASK, designSchoolID, newLandscapers, 0, 0);
                landscapersCommissioned += newLandscapers;
            }
            // Net Guns (2)
            else if (numNetGuns < 2) {
            	int numGun = (numNetGuns == 0) ? 0 : 1;
            	MapLocation gunLoc = suitableLoc(defensiveGunLocs[numGun]);
            	int minerID = findNearestMiner(robots, gunLoc);
                tryBroadcastMessage(1, gunLoc.x, gunLoc.y, MINER_TASK, minerID, 5, 1, 0);
                buildingCommissioned = true;
            }
            // Vaporator (1)
            else if (numVaporators == 0) {
            	defensiveVapLoc = suitableLoc(defensiveVapLoc);
            	int minerID = findNearestMiner(robots, defensiveVapLoc);
                tryBroadcastMessage(1, defensiveVapLoc.x, defensiveVapLoc.y, MINER_TASK, minerID, 2, 1, 0);
                buildingCommissioned = true;
            }

            // After the defensive ring is finished, just keep commissioning drones
            else {
                tryBroadcastMessage(1, 0, 0, FULFILLMENT_TASK, fulfillmentCenterID, 1, 0, 0);
                dronesCommissioned++;
            } 
        }
    }

    static void runMiner() throws GameActionException {

        // System.out.println("--------------------");
        // System.out.println("Turn: " + turnCount);
        System.out.println("Mode: " + robotMode);
        // System.out.println("NearestSoup: " + nearestSoup);
        System.out.println("Soup: " + rc.getSoupCarrying());
        // System.out.println("BNum: " + buildingNum);
        // System.out.println("SLocs:" + soupLocs);
        // System.out.println("RLocs: " + refineryLocs);
        // System.out.println("WLocs: " + waterLocs);
        
        // Search surroundings for HQ upon spawn
        if (turnCount == 1) {
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
        MapLocation currLoc = rc.getLocation();

        // Process transactions from the most recent block in the blockchain
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[2]/10000000 == TEAM_SECRET) {
                switch (mess[2]%100) {
                    case INIT_SOUP_LOCS:
                        if (soupLocs.isEmpty()) {
                            decodeLocsMessage(mess, soupLocs);
                            lsd = 8000;
                            int sd;
                            for (MapLocation loc : soupLocs) {
                                sd = currLoc.distanceSquaredTo(loc);
                                if (nearestSoup == null || sd < lsd) {
                                    lsd = sd;
                                    nearestSoup = loc;
                                }
                            }
                        }
                        break;
                    case INIT_WATER_LOCS:
                        decodeLocsMessage(mess, waterLocs);
                        break;
                    case INIT_REFINERY_LOCS:
                        decodeLocsMessage(mess, refineryLocs);
                        break;
                    case MINER_INIT_1:
                        if (mess[3] != rc.getID())
                            numMiners++;
                        break;
                    case MINER_INIT_2:
                        if (mess[3] == rc.getID()) {
                            defaultUnitDest = new MapLocation(-mess[0], -mess[1]);
                            robotMode = mess[4];
                            numMiners += mess[5];
                            if (mess[6] == 1)
                            	avoidAreas = true;
                        }
                        break;
                    case MINER_TASK:
                        if (mess[3] == rc.getID()) {
                        	robotMode = 3;
                        	robotDest = new MapLocation(-mess[0], -mess[1]);
                            buildingNum = mess[4];
                            buildingImportance = mess[5];
                        }
                        break;
                    case DESIGN_SCHOOL_CREATED:
                        refineryLocs.remove(hqLoc);
                        avoidAreas = true;
                        break;
                    case REFINERY_CREATED:
                        halt = false;
                        break;
                    case LANDSCAPER_START:
                        avoidAreas = true;
                        break;
                    case ENEMY_HQ_FOUND:
                        if (enemyHQLoc == null)
                            enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
                        break;
                    case HALT_PRODUCTION:
                    	if (mess[3] == 1 && robotMode == 3)
                    		robotMode = 0;
                    	break;

                }
                updateLocs(mess, 0);    // Update soup locations
                updateLocs(mess, 1);    // Update water locations
                updateLocs(mess, 2);    // Update refinery locations
            }
        }

        // Make sure miner gets any task even if it overloads on bytecode in the first round
        if (turnCount == 4) {
            for (int i=rc.getRoundNum()-5; i<rc.getRoundNum(); i++) {
                for (Transaction tx : rc.getBlock(i)) {
                    int[] mess = tx.getMessage();
                    if (mess[2]/10000000 == TEAM_SECRET && mess[2]%100 == MINER_TASK && mess[3] == rc.getID()) {
                        robotMode = 3;
                    	robotDest = new MapLocation(-mess[0], -mess[1]);
                        buildingNum = mess[4];
                        buildingImportance = mess[5];
                        return;
                    }
                }
            }
            // If it's still null, go into attack mode
            if (defaultUnitDest == null)
                robotMode = 1;
        }

        if (!currLoc.equals(lastLoc)) {
        	lastLoc = currLoc;
        	checkSoupWater();
        }
        if (nearestSoup != null)
            lsd = currLoc.distanceSquaredTo(nearestSoup);
        if (!rc.isReady())
        	return;

        // Explore mode (0)
        if (robotMode == 0) {
            // If there's no known soup, explore
            if (soupLocs.isEmpty()) {
                // If it hasn't gotten its default location yet, make it (0,0) for now
                if (defaultUnitDest == null)
                    defaultUnitDest = new MapLocation(0, 0);
                // If miner has explored all the way to default destination, reassign default  to farthest corner
                if (!exploreMove(defaultUnitDest))
                    defaultUnitDest = findFarthestCorner();
            }
            // If there is supposedly known soup, go to nearest soup
            else 
                robotMode = 1;
        }

        // Finding/Mining soup mode (1)
        if (robotMode == 1) {
            // If no soup exists, go back to exploring
            if (nearestSoup == null) {
                robotMode = 0;
                return;
            }
            // If there's no soup there, remove it from soupLocs and recalculate closest soup
            if (rc.canSenseLocation(nearestSoup) && rc.senseSoup(nearestSoup) == 0 && soupLocs.remove(nearestSoup)) {
                tryBroadcastMessage(1, nearestSoup.x, nearestSoup.y, SOUP_GONE, 0, 0, 0, 0);
                // Recalculate closest soup
                nearestSoup = null;
                lsd = 8000;
                int sd;
                for (MapLocation soupLoc : soupLocs) {
                    sd = currLoc.distanceSquaredTo(soupLoc);
                    if (nearestSoup == null || sd < lsd) {
                        lsd = sd;
                        nearestSoup = soupLoc;
                    }
                }
            }
            // If nearest soup non-adjacent or miner is full, then
            // decide whether to find more soup or go find a refinerygo to nearest refinery, or build a refinery (or wait)
            else if (rc.getSoupCarrying() == 100 || lsd > 2) {
                // If not full of soup yet and the nearest soup is close enough, go there
                // Relies on an exponential function (ae^(bx)+c) that goes through (10,150), (50,50), and (90,8)
                if (rc.getSoupCarrying() == 0 || (rc.getSoupCarrying() <= 95 && 214.135*Math.exp(-.0217*rc.getSoupCarrying())-22.38 > lsd))
                    bugMove2(nearestSoup);
                // If we don't have enough, [wait to] build a refinery or go to nearest refinery
                else {
                    // Look for closest refinery
                    // First check for nearby refineries in case they weren't caught
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                    for (RobotInfo robot : nearbyRobots)
                        if (robot.getType() == RobotType.REFINERY)
                            refineryLocs.add(robot.getLocation());
                    int lrd = 8000;
                    for (MapLocation refineryLoc : refineryLocs)
                        if (currLoc.distanceSquaredTo(refineryLoc) < lrd)
                            lrd = currLoc.distanceSquaredTo(refineryLoc);
                    // If there's 200 soup, we are near soup, and far enough from the nearest refinery, 
                    // build a refinery in the direction of the soup
                    if (rc.getTeamSoup() >= 200 && lrd >= 8 && lsd <= 8)
                        tryBuildRefinery();
                    // If there are no refineries or the nearest one is far, try and wait to build a refinery
                    else if (lrd == 8000 || (lrd > 100 && rc.getTeamSoup() >= 145)) {
                        if (rc.getTeamSoup() >= 200)
                            tryBuildRefinery();
                        else if (!halt) {
                            halt = true;
                            tryBroadcastMessage(1, 0, 0, HALT_PRODUCTION, 0, 0, 0, 0);
                        }
                    }
                    robotMode = 2;
                }
            }
            // If there is adjacent soup and miner isn't full, mine it
            else
                tryMine(currLoc.directionTo(nearestSoup));
        }

        // Finding refinery/refining soup (2)
        // Either waiting for enough soup to build or moving to refinery
        if (robotMode == 2) {
            // If halting, wait for 200 soup
            if (halt) {
                if (rc.getTeamSoup() >= 200)
                    tryBuildRefinery();
            }
            // Otherwise, try to go to nearest refinery
            else {
                // Look for closest refinery
                // First check for nearby refineries in case they weren't caught
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo robot : nearbyRobots)
                    if (robot.getType() == RobotType.REFINERY)
                        refineryLocs.add(robot.getLocation());
                int lrd = 8000;
                for (MapLocation refineryLoc : refineryLocs) {
                    if (currLoc.distanceSquaredTo(refineryLoc) < lrd) {
                        robotDest = refineryLoc;
                        lrd = currLoc.distanceSquaredTo(refineryLoc);
                    }
                }
                // If nearest refinery is too far, try to build refinery or halt
                if (lrd == 8000 || (lrd > 100 && rc.getTeamSoup() >= 145)) {
                    if (rc.getTeamSoup() >= 200)
                        tryBuildRefinery();
                    else if (!halt) {
                        halt = true;
                        tryBroadcastMessage(1, 0, 0, HALT_PRODUCTION, 0, 0, 0, 0);
                    }
                }
                // Otherwise, go to the nearest refinery
                else {
                    // Sense refinery if possible
                    if (rc.canSenseLocation(robotDest)) {
                        RobotInfo robot = rc.senseRobotAtLocation(robotDest);
                        // Make sure refinery is there
                        if (robot == null || robot.getTeam() != rc.getTeam() || 
                            (robot.getType() != RobotType.REFINERY && robot.getType() != RobotType.HQ)) {
                            refineryLocs.remove(robotDest);
                            tryBroadcastMessage(1, robotDest.x, robotDest.y, REFINERY_DESTROYED, 0, 0, 0, 0);
                            return;
                        }
                        // If next to refinery, refine and go
                        else if (currLoc.isAdjacentTo(robotDest) && tryRefine(currLoc.directionTo(robotDest))) {
                            robotMode = 1;
                            return;
                        }
                    }
                    // Otherwise, move there
	                bugMove2(robotDest);
                }
            }
        }

        // Build building mode (3)
        // Drop whatever it's doing, go to a location, and build specified building
        // Only enter this mode if commissioned by HQ to build something
        // In importance 0, try to build all around miner
        // In importance 1, build at a specific location [in defensive formation]
        else if (robotMode == 3) {
            // If on top of destination, take a step towards the center of the map
            if (currLoc.equals(robotDest))
                bugMove2(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
            // If adjacent, build
            else if (currLoc.isAdjacentTo(robotDest)) {
                Direction destDir = currLoc.directionTo(robotDest);
                if ((buildingImportance == 1 && tryBuild(spawnedByMiner[buildingNum-1], destDir)) 
                    || (buildingImportance == 0 && tryBuildAround(spawnedByMiner[buildingNum-1], destDir)))
                    robotMode = 1;
            }
            // Otherwise, move there
            else
                bugMove2(robotDest);
        }
    }

    static void runLandscaper() throws GameActionException {

        // Broadcast existence and get HQ location upon spawn
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, LANDSCAPER_SPAWN, rc.getID(), 0, 0, 0);
            getHqLocFromBlockchain();
        }

        tryBroadcastQueue();
        System.out.println(robotDest);

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/10000000 == TEAM_SECRET) {
                switch (mess[2]%100) {
                    case LANDSCAPER_TASK:
                        if (mess[3] == rc.getID())
                            robotDest = new MapLocation(-mess[0], -mess[1]);
                        break;
                }
            }
        }

        // Only start running until it gets a robotDest
        if (robotDest == null || !rc.isReady())
            return;
        MapLocation currLoc = rc.getLocation();

        // First kill any enemy structures, if not already digging
        if (!currLoc.equals(robotDest)) {
            // Sense nearest enemy
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            MapLocation enemyBuildingLoc = null;
            for (RobotInfo enemy : nearbyEnemies) {
                if (enemy.getType().isBuilding()) {
                    if (enemyBuildingLoc == null || enemy.getLocation().distanceSquaredTo(currLoc) < currLoc.distanceSquaredTo(enemyBuildingLoc)) {
                        enemyBuildingLoc = enemy.getLocation();
                        break;
                    }
                }
            }
            // If nearest enemy exists, kill it
            if (enemyBuildingLoc != null) {
                // Go to it first
                if (!currLoc.isAdjacentTo(enemyBuildingLoc))
                    bugMoveJ(enemyBuildingLoc);
                // Then kill it
                else {
                    Direction enemyDir = currLoc.directionTo(enemyBuildingLoc);
                    if (rc.getDirtCarrying() == 0)
                        tryDigAround(enemyDir.opposite());
                    else if (rc.canDepositDirt(enemyDir))
                        rc.depositDirt(enemyDir);
                }
            }
            // Otherwise, move to location until adjacent, digging if the lanscaper is close but can't move towards it
            else if (!currLoc.isAdjacentTo(robotDest)) {
            	if (currLoc.distanceSquaredTo(robotDest) > 8)
            		bugMoveJ(robotDest);
            	else {
            		// If you've went in a circle back to closeLoc, try to dig to robotDest
            		if (currLoc.equals(closeLoc)) {
            			Direction digDir = currLoc.directionTo(robotDest);
            			Direction[] toTry = {digDir, digDir.rotateLeft(), digDir.rotateRight()};
            			for (Direction dir : toTry) {
            				// First try to move there
            				if (rc.canMove(currLoc.directionTo(robotDest)) && !rc.senseFlooding(robotDest))
                				rc.move(currLoc.directionTo(robotDest));
            				// Make sure it's not too high
            				else if (rc.senseElevation(currLoc.add(dir)) > rc.senseElevation(currLoc) + 3) {
				                if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
				                	if (tryDig(dir))
				                		return;
				                }
				                else {
				                	if (tryDepositAround(currLoc.directionTo(hqLoc).opposite()))
				                		return;
				                }
				            }
				            // Make sure it's not too low
				            else {
				                if (rc.getDirtCarrying() == 0) {
				                	if (tryDigAround(currLoc.directionTo(hqLoc).opposite()))
				                		return;
				                }
				                else {
				                	if (tryDeposit(dir))
				                    	return;
				                }
				            }
            			}
            		}
            		else {
            			// Save "closeLoc" the first time you get within 8 of destination
	            		if (closeLoc == null)
	            			closeLoc = currLoc;
	            		bugMoveJ(robotDest);
            		}
            	}
            }
            // Once adjacent, make sure the spot isn't occupied
            // First try to move there
            else if (rc.canMove(currLoc.directionTo(robotDest)) && !rc.senseFlooding(robotDest))
                rc.move(currLoc.directionTo(robotDest));
            // Then change the elevation until it can move there
            // Make sure it's not too high
            else if (rc.senseElevation(robotDest) > rc.senseElevation(currLoc) + 3) {
                if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit)
                	tryDig(currLoc.directionTo(robotDest));
                else
                	tryDepositAround(currLoc.directionTo(hqLoc).opposite());
            }
            // Make sure it's not too low
            else {
                if (rc.getDirtCarrying() == 0)
                	tryDigAround(currLoc.directionTo(hqLoc).opposite());
                else
                    tryDeposit(currLoc.directionTo(robotDest));
            }
        }

        // We are here, so ... Trump Inc.
        // First layer - will always try to even out dirt
        else if (currLoc.isAdjacentTo(hqLoc)) {
            // Set wall directions
        	if (wallDirs == null || lastWallDirs == null) {
        		wallDirs = new HashSet<Direction>();
        		lastWallDirs = new HashSet<Direction>();
        		wallDirs.add(Direction.CENTER);
        	}
        	// Only allow landscaper to dump onto squares without landscapers, until the dirt differential becomes 60
        	Set<Direction> tempWallDirs = new HashSet<Direction>();
        	for (int i=-1; i<=1; i++) {
                for (int j=-1; j<=1; j++) {
                    MapLocation loc = currLoc.translate(i, j);
                    if (loc.isAdjacentTo(hqLoc) && !loc.equals(hqLoc)) {
                    	RobotInfo robot = rc.senseRobotAtLocation(loc);
                    	if (rc.senseElevation(loc) + 60 <= rc.senseElevation(currLoc)
                    		|| (robot != null && robot.getType() == RobotType.LANDSCAPER && robot.getTeam() == rc.getTeam()))
                    		tempWallDirs.add(currLoc.directionTo(loc));
                    }
                }
            }
            // Make sure there's still a landscaper there
            lastWallDirs.retainAll(tempWallDirs);
            wallDirs.addAll(lastWallDirs);
            lastWallDirs = tempWallDirs;
            // Dig dirt if not carrying any dirt
            if (rc.getDirtCarrying() == 0)
                tryDigAround(currLoc.directionTo(hqLoc).opposite());
            // Otherwise dump dirt onto lowest location adjacent to HQ
            else {
            	int lowestElevation = 10000;
                for (Direction dir : wallDirs) {
                    int elevation = rc.senseElevation(currLoc.add(dir));
                    if (elevation < lowestElevation) {
                        lowestElevation = elevation;
                        dirtDir = dir;
                    }
                }
                tryDeposit(dirtDir);
            }
        }

        // Second layer - always dump in preset direction towards the wall
        else {
            // Set dirt dumping direction
            if (dirtDir == null) {
                dirtDir = currLoc.directionTo(hqLoc);
                int dx = currLoc.x - hqLoc.x;
                int dy = currLoc.y - hqLoc.y;
                // Check if (dx, dy) is one of (-1, -2), (-2, 1), (1, 2), (2, -1)
                if ((dx == -1 && dy == -2) || (dx == -2 && dy == 1) || (dx == 1 && dy == 2) || (dx == 2 && dy == -1))
                    dirtDir = dirtDir.rotateLeft();
            }
            // Dig dirt if not carrying any dirt
            if (rc.getDirtCarrying() == 0)
                tryDigAround(currLoc.directionTo(hqLoc).opposite());
            // Otherwise dump dirt onto itself until 100 dirt, then in preset direction
            else {
                if (rc.senseElevation(currLoc) < 100)
                    tryDeposit(Direction.CENTER);
                else
                    tryDeposit(dirtDir);
            }
        }
    }

    static void runDeliveryDrone() throws GameActionException {

        // Broadcast existence, get HQ location, and set possible HQ locations upon spawn
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, DRONE_SPAWN, rc.getID(), 0, 0, 0);
            getHqLocFromBlockchain();
            // Potential HQ locations is randomized so miners go to different places
            potentialEnemyHQs = new ArrayList<MapLocation>();
            int enemyWidth = rc.getMapWidth()-hqLoc.x-1;
            int enemyHeight = rc.getMapHeight()-hqLoc.y-1;
            if (Math.random() < .5) {
                potentialEnemyHQs.add(new MapLocation(hqLoc.x, enemyHeight));
                potentialEnemyHQs.add(new MapLocation(enemyWidth, enemyHeight));
                potentialEnemyHQs.add(new MapLocation(enemyWidth, hqLoc.y));
            }
            else {
                potentialEnemyHQs.add(new MapLocation(enemyWidth, hqLoc.y));
                potentialEnemyHQs.add(new MapLocation(enemyWidth, enemyHeight));
                potentialEnemyHQs.add(new MapLocation(hqLoc.x, enemyHeight));
            }
        }

        // Make sure delivery drone gets its destination even if it overloads on bytecode in the first round
        if (turnCount == 4 && defaultUnitDest == null) {
            for (int i=rc.getRoundNum()-5; i<rc.getRoundNum(); i++) {
                for (Transaction tx : rc.getBlock(i)) {
                    int[] mess = tx.getMessage();
                    if (mess[2]/10000000 == TEAM_SECRET && mess[2]%100 == DRONE_TASK && mess[3] == rc.getID()) {
                        defaultUnitDest = new MapLocation(-mess[0], -mess[1]);
                        robotMode = mess[4];
                        return;
                    }
                }
            }
            // If it's still null, go into attack mode
            if (defaultUnitDest == null)
                robotMode = 1;
        }

        tryBroadcastQueue();

        // Process transactions from the most recent block in the blockchain
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2]/10000000 == TEAM_SECRET) {
                switch (mess[2]%100) {
                    case INIT_SOUP_LOCS:
                        decodeLocsMessage(mess, soupLocs);
                        break;
                    case INIT_WATER_LOCS:
                        decodeLocsMessage(mess, waterLocs);
                        break;
                    case INIT_REFINERY_LOCS:
                    	decodeLocsMessage(mess, refineryLocs);
                        break;
                    case DRONE_TASK:
                        if (mess[3] == rc.getID()) {
                            defaultUnitDest = new MapLocation(-mess[0], -mess[1]);
                            robotMode = mess[4];
                        }
                        break;
                    case LANDSCAPER_START:
                        avoidAreas = true;
                        break;
                    case ENEMY_HQ_FOUND:
                        if (enemyHQLoc == null)
                            enemyHQLoc = new MapLocation(-mess[0], -mess[1]);
                        break;
                }
                updateLocs(mess, 0);    // Update soup locations
                updateLocs(mess, 1);    // Update water locations
                updateLocs(mess, 2);    // Update refinery locations
            }
        }
 
 		MapLocation currLoc = rc.getLocation();
        if (!currLoc.equals(lastLoc)) {
        	lastLoc = currLoc;
        	checkSoupWater();
        }

        if (cowCooldown > 0 && rc.isReady())
            cowCooldown--;
        if (minerCooldown > 0)
        	minerCooldown--;

        // If able to sense a potential enemy HQ loc, determine if it's the real one
        if (enemyHQLoc == null) {
            for (MapLocation potentialLoc : potentialEnemyHQs) {
                if (rc.canSenseLocation(potentialLoc)) {
                    RobotInfo potentialHQ = rc.senseRobotAtLocation(potentialLoc);
                    if (potentialHQ != null && potentialHQ.getType() == RobotType.HQ && potentialHQ.getTeam() == rc.getTeam().opponent()) {
                        enemyHQLoc = potentialLoc;
                        tryBroadcastMessage(1, enemyHQLoc.x, enemyHQLoc.y, ENEMY_HQ_FOUND, 0, 0, 0, 0);
                    }
                    else {
                        potentialEnemyHQs.remove(potentialLoc);
                        if (potentialEnemyHQs.size() == 1) {
                            enemyHQLoc = potentialEnemyHQs.get(0);
                            tryBroadcastMessage(1, enemyHQLoc.x, enemyHQLoc.y, ENEMY_HQ_FOUND, 0, 0, 0, 0);
                        }
                    }
                    break;
                }
            }
        }

        if (!rc.isReady())
        	return;

        // If holding something, go to a destination that depends on the unit holding
        if (rc.isCurrentlyHoldingUnit()) {
            // If a cow is picked up, do our best to path towards enemy HQ
            if (unitType == 0) {
                if (enemyHQLoc == null)
                    robotDest = potentialEnemyHQs.get(0);
                else
                    robotDest = enemyHQLoc;
                // Drop off when can sense enemy HQ
                if (rc.canSenseLocation(robotDest)) {
                    if (tryDropAround(currLoc.directionTo(robotDest))) {
                        cowCooldown = 10;
                    }
                }
                else
                    exploreMove(robotDest);
            }
            // If an enemy unit is picked up, try to go to water
            else if (unitType == 1) {
                // Path towards closest known water if any has been found
                if (!waterLocs.isEmpty()) {
                    MapLocation water = null;
                    int lwd = 8000;
                    int wd;
                    for (MapLocation waterLoc : waterLocs) {
                        wd = currLoc.distanceSquaredTo(waterLoc);
                        if (wd < lwd) {
                            water = waterLoc;
                            lwd = wd;
                        }
                    }
                    // Check that there's actually water there
                    if (rc.canSenseLocation(water) && !rc.senseFlooding(water)) {
                        waterLocs.remove(water);
                        tryBroadcastMessage(1, water.x, water.y, WATER_GONE, 0, 0, 0, 0);
                    }
                    // Path towards water and drop off there
                    else if (currLoc.equals(water))
                    	bugMove2(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
                    else if (currLoc.isAdjacentTo(water))
                        tryDrop(currLoc.directionTo(water));
                    else
                        bugMoveJ(water);
                }
                // Otherwise, path around the corners
                else 
                    exploreMove(findLeftCorner());
            }
           	// If a miner is picked up, go to either soup or a refinery
           	else {
           		if (toRefinery) {
        			int lrd = 8000;
        			int rd;
        			for (MapLocation refineryLoc : refineryLocs) {
        				rd = currLoc.distanceSquaredTo(refineryLoc);
        				if (rd < lrd) {
        					lrd = rd;
        					robotDest = refineryLoc;
        				}
        			}
        			if (currLoc.isAdjacentTo(robotDest)) {
        				tryDropAround(currLoc.directionTo(robotDest));
        				minerCooldown = 1;
        			}
        			else
        				bugMoveJ(robotDest);
        		}
        		else if (soupLocs.isEmpty())
        			exploreMove(findLeftCorner());
        		else {
        			lsd = 8000;
        			nearestSoup = null;
        			int sd;
        			for (MapLocation soupLoc : soupLocs) {
        				sd = currLoc.distanceSquaredTo(soupLoc);
        				if (sd < lsd) {
        					lsd = sd;
        					nearestSoup = soupLoc;
        				}
        			}
                    if (rc.canSenseLocation(nearestSoup) && rc.senseSoup(nearestSoup) == 0) {
                        soupLocs.remove(nearestSoup);
                        tryBroadcastMessage(1, nearestSoup.x, nearestSoup.y, SOUP_GONE, 0, 0, 0, 0);
                    }
                    else if (currLoc.equals(nearestSoup))
                    	bugMove2(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
                    else if (currLoc.isAdjacentTo(nearestSoup)) {
                        tryDropAround(currLoc.directionTo(nearestSoup));
                        minerCooldown = 16;
                    }
                    else
                        bugMoveJ(nearestSoup);
        		}
           	}
        }

        // If not holding anything, look for enemies, then optionally friendly miners
        else {
        	// Pick up any enemy units/cows within striking range
            Team enemyTeam = rc.getTeam().opponent();
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED);
            for (RobotInfo robot : robots) {
                if (robot.getTeam() == enemyTeam && (robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER)) {
                	unitType = 1;
                    tryPickUp(robot.getID());
                    return;
                }
                else if (robot.getType() == RobotType.COW && cowCooldown == 0) {
                    unitType = 0;
                    tryPickUp(robot.getID());
                    return;
                }
            }
            // Otherwise bee move towards any nearby enemy units sensed
            robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared());
            for (RobotInfo robot : robots) {
                if ((robot.getTeam() == enemyTeam && (robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER)) 
                    || (robot.getType() == RobotType.COW && cowCooldown == 0)) {
                    beeMove(robot.getLocation());
                    return;
                }
            }

            // If the method still hasn't returned, there are no enemy units within range
            // Automatically switch to lategame defense mode at round 1500
            if (rc.getRoundNum() >= 1500)
                robotMode = 2;

            // If drone is in helper mode, look for miners to pick up
			if (robotMode == -1 && minerCooldown == 0) {
    			// Pick up any friendly miners within striking range
        		robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, rc.getTeam());
	            for (RobotInfo robot : robots) {
	            	if (robot.getType() == RobotType.MINER) {
	            		unitType = 2;
	            		tryPickUp(robot.getID());
	            		lastPickupID = robot.getID();
	            		if (rc.senseNearbySoup(8).length > 0)
	            			toRefinery = true;
	            		else
	            			toRefinery = false;
	            		return;
	            	}
	            }
	            // Otherwise bee move towards any nearby miners
	            robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared());
	            for (RobotInfo robot : robots) {
	                if (robot.getType() == RobotType.MINER) {
	                    beeMove(robot.getLocation());
	                    return;
	                }
	            }
			}

			// If the drone isn't in helper mode or didn't find any miners, go to default location based on mode
			// Helper mode (-1) - go back and forth between center of map and defensive position
			if (robotMode == -1) {
				MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
				if (robotDest == null || (!robotDest.equals(center) && !robotDest.equals(defaultUnitDest)))
					robotDest = defaultUnitDest;
				else if (currLoc.equals(defaultUnitDest))
					robotDest = center;
				else if (currLoc.equals(center))
					robotDest = defaultUnitDest;
				bugMoveJ(robotDest);
			}
            // Defense mode (0) - default to defensive position
            else if (robotMode == 0) {
                robotDest = defaultUnitDest;
                if (!currLoc.equals(robotDest))
                    bugMoveJ(robotDest);
            }
            // Attack mode (1) - zigzag around towards potential enemy HQs, looking for enemies
            else if (robotMode == 1) {
                if (enemyHQLoc == null)
                    robotDest = potentialEnemyHQs.get(0);
                else
                    robotDest = enemyHQLoc;
                Direction dir = currLoc.directionTo(robotDest);
                // skrrt around enemy HQ, 13 is net gun range, so >15 is safe
                if (currLoc.add(dir).distanceSquaredTo(robotDest) > 15)
                    exploreMove(robotDest);
                else if (currLoc.add(dir.rotateLeft()).distanceSquaredTo(robotDest) > 15 && !tryMove(dir.rotateLeft()))
                    // 90 degrees left/right is always safe
                    if (!tryMove(dir.rotateLeft().rotateLeft()))
                        tryMove(dir.rotateRight().rotateRight());
            }
            // Lategame defense mode - just circle around HQ to form a drone turtle
            else if (robotMode == 2)
                bugMove2(hqLoc);
        }
    }

    static void runRefinery() throws GameActionException {
        
        // Broadcast existence upon spawn
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, REFINERY_CREATED, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();
    }

    static void runVaporator() throws GameActionException {

        // Broadcast existence upon spawn
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, VAPORATOR_CREATED, 0, 0, 0, 0);
        }
        
        tryBroadcastQueue();
    }

    static void runDesignSchool() throws GameActionException {

        // Broadcast existence and get HQ location upon spawn
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
            if(mess[2]/10000000 == TEAM_SECRET) {
                switch (mess[2]%100) {
                    case DESIGN_SCHOOL_TASK:
                        if (mess[3] == rc.getID()) {
                            unitsQueued += mess[4];
                            halt = false;
                        }
                        break;
                    case HALT_PRODUCTION:
                        halt = true;
                        if (mess[3] == 1)
                        	unitsQueued = 0;
                        break;
                    case REFINERY_CREATED:
                        halt = false;
                        break;
                }
            }
        }

        System.out.println(halt + " " + unitsQueued);
        
        // Try to make a landscaper if one is queued
        if (!halt && (unitsQueued > 0) 
            && tryBuildAround(RobotType.LANDSCAPER, rc.getLocation().directionTo(hqLoc).opposite()))
            unitsQueued--;
    }

    static void runFulfillmentCenter() throws GameActionException {

        // Broadcast existence and get HQ location upon spawn
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
            if(mess[2]/10000000 == TEAM_SECRET) {
                switch (mess[2]%100) {
                    case FULFILLMENT_TASK:
                        if (mess[3] == rc.getID()) {
                            unitsQueued += mess[4];
                            halt = false;
                        }
                        break;
                    case HALT_PRODUCTION:
                        halt = true;
                        if (mess[3] == 1)
                        	unitsQueued = 0;
                        break;
                    case REFINERY_CREATED:
                        halt = false;
                        break;
                }
            }
        }

        System.out.println(halt + " " + unitsQueued);
        
        // Try to make a drone if one is queued
        if (!halt && (unitsQueued > 0) 
            && tryBuildAround(RobotType.DELIVERY_DRONE, rc.getLocation().directionTo(hqLoc).opposite()))
            unitsQueued--;
    }
        
    static void runNetGun() throws GameActionException {
        
        // Broadcast existence upon spawn
        if (turnCount == 1) {
            MapLocation currLoc = rc.getLocation();
            tryBroadcastMessage(1, currLoc.x, currLoc.y, NET_GUN_CREATED, 0, 0, 0, 0);
        }

        tryBroadcastQueue();
        
        // Shoot closest enemy delivery drone
        RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        MapLocation currLoc = rc.getLocation();
        int targetID = -1;
        int lddd = 8000;
        for (RobotInfo robot : robots) {
            int ddd = currLoc.distanceSquaredTo(robot.getLocation());
            if (robot.getType() == RobotType.DELIVERY_DRONE && ddd < lddd) {
                lddd = ddd;
                targetID = robot.getID();
            }
        }
        if (targetID != -1 && rc.canShootUnit(targetID))
            rc.shootUnit(targetID);
    }

    // MOVEMENT

    /**
     * Move towards a specified location while prioritizing diagonal movements for exploration.
     * 
     * @param destination The intended destination
     * @return false if arrived at destination
     * @throws GameActionException
     */
    static boolean exploreMove(MapLocation destination) throws GameActionException {

        // Return false if already there
        MapLocation currLoc = rc.getLocation();
        if (currLoc.equals(destination))
            return false;

        // If adjacent to destination, try to move there and return false
        Direction dir = currLoc.directionTo(destination);
        if (currLoc.isAdjacentTo(destination)) {
            tryMove2(dir);
            return false;
        }

        // Save last destination
    	if (lastDest == null)
    		lastDest = destination;
    	if (!destination.equals(lastDest)) {
    		lastDest = destination;
    		lastBugPathLoc = null;
    	}

        // Make best diagonal move towards destination
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
            if(tryMoveSave(d))
                return true;

        lastBugPathLoc = null;

        return !rc.getLocation().equals(destination);
    }

    /**
     * Makes the optimal bug move towards a location.
     * 
     * @param destination The intended destination
     * @throws GameActionException
     */
    static void bugMove2(MapLocation destination) throws GameActionException {

    	System.out.println(destination);
        bugMoveJ(destination);
        if (true) return;

        // Make sure we aren't already there and we have enough turns
        MapLocation currLoc = rc.getLocation();
        if (currLoc.equals(destination))
            return;

        // If destination changes, start anew
        if (!destination.equals(lastDest))
            bugMovePath.clear();

        // If not following an obstacle, try to move towards destination until it encounters a non-unit obstacle
        if (bugMovePath.isEmpty()) {
            // First, try to move in the direction of the destination
            Direction dir = currLoc.directionTo(destination);
            if (!tryMove2(dir)) {
                RobotInfo robot = rc.senseRobotAtLocation(currLoc.add(dir));
                // If there's a unit obstructing the way, just try to move around it
                if (robot != null) {
                    RobotType type = robot.getType();
                    if (type == RobotType.MINER || type == RobotType.LANDSCAPER || type == RobotType.DELIVERY_DRONE) {
                        Direction[] toTry = {
                            dir.rotateLeft(),
                            dir.rotateLeft().rotateLeft(),
                            dir.opposite().rotateRight(),
                            dir.opposite(),
                            dir.rotateRight(),
                            dir.rotateRight().rotateRight(),
                            dir.opposite().rotateLeft()
                        };
                        for (Direction d : toTry)
                            if (tryMove2(d))
                                return;
                    }
                }
                // Otherwise, it's an obstacle that we have to move around
                // Set the "m-line"
                while (!currLoc.equals(destination)) {
                    bugMovePath.add(currLoc);
                    currLoc = currLoc.add(currLoc.directionTo(destination));
                }
                // Set obstacle dir
                obstacleDir = dir;
                // Set closest distance
                lastDistance = currLoc.distanceSquaredTo(destination);
                // Set last location
                lastDest = destination;
                // Update left/right preference
                if ((destination.x < currLoc.x && destination.y < currLoc.y) 
                    || (destination.x >=currLoc.x && destination.y >= currLoc.y))
                    bugLeft = false;
                else
                    bugLeft = true;
            }
        }

        // Otherwise, if already following an obstacle, keep following until encountering m-line at a closer point
        // Not an "else" because an obstacle could've been set by the previous block
        if (!bugMovePath.isEmpty()) {
            // If obstacle disappeared for some reason, move on
            if (tryMove2(obstacleDir))
                bugMovePath.clear();
            else
                followObstacle();
            if (bugMovePath.contains(currLoc) && currLoc.distanceSquaredTo(destination) < lastDistance)
                bugMovePath.clear();
        }
    }

    /**
     * Helper function for bug move 2.
     * Follows the obstacle based on left/right preference.
     * Assumes that obstacleDir is established and you cannot move in obstacleDir.
     * 
     * @throws GameActionException
     */
    static void followObstacle() throws GameActionException {
        if (bugLeft) {
            if (tryMove2(obstacleDir.rotateLeft()))
                obstacleDir = obstacleDir.rotateRight().rotateRight();
            else if (tryMove2(obstacleDir.rotateLeft().rotateLeft()))
                ;
            else if (tryMove2(obstacleDir.opposite().rotateRight()))
                ;
            else if (tryMove2(obstacleDir.opposite()))
                obstacleDir = obstacleDir.rotateLeft().rotateLeft();
            else if (tryMove2(obstacleDir.opposite().rotateLeft()))
                obstacleDir = obstacleDir.rotateLeft().rotateLeft();
            else if (tryMove2(obstacleDir.rotateRight().rotateRight()))
                obstacleDir = obstacleDir.opposite();
            else if (tryMove2(obstacleDir.rotateRight()))
                obstacleDir = obstacleDir.opposite();
        }
        else {
            if (tryMove2(obstacleDir.rotateRight()))
                obstacleDir = obstacleDir.rotateLeft().rotateLeft();
            else if (tryMove2(obstacleDir.rotateRight().rotateRight()))
                ;
            else if (tryMove2(obstacleDir.opposite().rotateLeft()))
                ;
            else if (tryMove2(obstacleDir.opposite()))
                obstacleDir = obstacleDir.rotateRight().rotateRight();
            else if (tryMove2(obstacleDir.opposite().rotateRight()))
                obstacleDir = obstacleDir.rotateRight().rotateRight();
            else if (tryMove2(obstacleDir.rotateLeft().rotateLeft()))
                obstacleDir = obstacleDir.opposite();
            else if (tryMove2(obstacleDir.rotateLeft()))
                obstacleDir = obstacleDir.opposite();
        }
    }

    /**
     * Personal incomplete moving algorithm.
     * 
     * @param destination The intended destination
     * @throws GameActionException
     */
    static void bugMoveJ(MapLocation destination) throws GameActionException {

    	// Save last destination
    	if (lastDest == null)
    		lastDest = destination;
    	if (!destination.equals(lastDest)) {
    		lastDest = destination;
    		lastBugPathLoc = null;
    	}

        Direction dir = rc.getLocation().directionTo(destination);

        // If able to move directly towards destination, do so and reset tendency
        if (tryMoveSave(dir)) {
            bugDirectionTendency = 0;
            return;
        }

        // If no tendency, find tendency
        if (bugDirectionTendency == 0) {
            if (tryMoveSave(dir.rotateLeft())) {
                bugDirectionTendency = 1;
                return;
            }
            if (tryMoveSave(dir.rotateRight())) {
                bugDirectionTendency = 2;
                return;
            }
            if (tryMoveSave(dir.rotateLeft().rotateLeft())) {
                bugDirectionTendency = 1;
                return;
            }
            if (tryMoveSave(dir.rotateRight().rotateRight())) {
                bugDirectionTendency = 2;
                return;
            }
            if (tryMoveSave(dir.opposite().rotateRight())) {
                bugDirectionTendency = 1;
                return;
            }
            if (tryMoveSave(dir.opposite().rotateLeft())) {
                bugDirectionTendency = 2;
                return;
            }
            if (tryMoveSave(dir.opposite())) {
                bugDirectionTendency = (int) Math.floor(Math.random()) + 1;
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
                if (tryMoveSave(d))
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
                if (tryMoveSave(d))
                    return;
        }

        // If we can't move anywhere, nullify last location
        lastBugPathLoc = null;
    }

    /**
     * Only tries to go in dir and the 4 directions surrounding it
     * Used for drones seeking enemies, prevents drones from moving away from target
     * 
     * @param destination The intended destination
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean beeMove(MapLocation destination) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(destination);
        Direction[] toTry = {
            dir,
            dir.rotateRight(),
            dir.rotateRight().rotateRight(),
            dir.rotateLeft(),
            dir.rotateLeft().rotateLeft()
        };
        for (Direction d : toTry)
            if (tryMove(d))
                return true;
        return false;
    }

    /**
     * Tells the unit if moving in a direction will be a future hole.
     * Only starts returning true when LANDSCAPER_START.
     * Used for landscapers to avoid future holes.
     *
     * @param dir The intended direction of movement
     * @return true if location will be a future hole
     * @throws GameActionException
     */
    static boolean isFutureHole(Direction dir) throws GameActionException {
        if (!avoidAreas)
            return false;
        // If already in a potential hole, don't restrict movement
        MapLocation currLoc = rc.getLocation();
        int cdist = currLoc.distanceSquaredTo(hqLoc);
        if (cdist == 4 || cdist == 8 || cdist == 13)
        	return false;
        int dist = currLoc.add(dir).distanceSquaredTo(hqLoc);
        if (dist == 4 || dist == 8 || dist == 13) {
        	MapLocation futureLoc = currLoc.add(dir);
        	RobotInfo potentialLandscaper = rc.senseRobotAtLocation(futureLoc.add(futureLoc.directionTo(hqLoc)));
        	if (potentialLandscaper != null && potentialLandscaper.getType() == RobotType.LANDSCAPER)
        		return true;
        }
        return false;
    }

    /**
     * Tells the unit if moving in a direction will be a landscaper area.
     * Only starts returning true when LANDSCAPER_START.
     * Used for non-landscapers to avoid landscaper areas.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean isLandscaperArea(Direction dir) throws GameActionException {
        if (!avoidAreas)
            return false;
        // If already close to HQ, dont restrict movement
        MapLocation currLoc = rc.getLocation();
        if (currLoc.distanceSquaredTo(hqLoc) <= 8)
            return false;
        int dist = currLoc.add(dir).distanceSquaredTo(hqLoc);
        if (dist <= 8)
            return true;
        else if (rc.getType() == RobotType.MINER && dist == 13) {
        	MapLocation futureLoc = currLoc.add(dir);
        	RobotInfo potentialLandscaper = rc.senseRobotAtLocation(futureLoc.add(futureLoc.directionTo(hqLoc)));
        	if (potentialLandscaper != null && potentialLandscaper.getType() == RobotType.LANDSCAPER)
        		return true;
        }
        return false;
    }

    // "TRY"

    /**
     * Attempts to move in a given direction, preventing movement into water if not a drone.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir) && (rc.getType().canFly() || !rc.senseFlooding(rc.getLocation().add(dir)))) {
        	lastLoc = rc.getLocation();
            rc.move(dir);
            return true;
        }
        return false;
    }

    /**
     * Attempts to move in a given direction, preventing movement into water if not a drone.
     * If landscaper, avoids holes made by other landscapers
     * Otherwise, upon LANDSCAPER_START, avoids the general landscaper area
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove2(Direction dir) throws GameActionException {
        if (rc.getType() == RobotType.LANDSCAPER) {
            if (rc.canMove(dir) && !isFutureHole(dir) && 
                (rc.getType().canFly() || (!rc.senseFlooding(rc.getLocation().add(dir))))) {
                rc.move(dir);
                return true;
            }
            return false;
        }
        if (rc.canMove(dir) && !isLandscaperArea(dir) && 
            (rc.getType().canFly() || (!rc.senseFlooding(rc.getLocation().add(dir))))) {
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
    static boolean tryMoveSave(Direction dir) throws GameActionException {
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.getType() == RobotType.LANDSCAPER) {
            if (rc.canMove(dir) && !nextLoc.equals(lastBugPathLoc) 
                && (!rc.senseFlooding(nextLoc) || rc.getType().canFly()) && !isFutureHole(dir)) {
                lastBugPathLoc = rc.getLocation();
                rc.move(dir);
                return true;
            }
            return false;
        }
        // System.out.println(dir + " " + rc.canMove(dir) + " " + !nextLoc.equals(lastBugPathLoc) + " " + !isLandscaperArea(dir));
        if (rc.canMove(dir) && !nextLoc.equals(lastBugPathLoc) 
            && (!rc.senseFlooding(nextLoc) || rc.getType().canFly()) && !isLandscaperArea(dir)) {
            lastBugPathLoc = rc.getLocation();
            rc.move(dir);
            return true;
        }
        return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction to build
     * @return true if a building was built
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
     * Attempts to build a given robot around a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction to build
     * @return true if a building was built
     * @throws GameActionException
     */
    static boolean tryBuildAround(RobotType type, Direction dir) throws GameActionException {
        if (type.cost > rc.getTeamSoup())
            return false;
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
     * Attempts to build a refinery in the direction of the nearest soup (or towards the center if none).
     * Automatically builds around the direction since the exact location doesn't matter for refineries.
     * Also makes sure the refinery is far enough from the HQ so it doesn't interfere with defense.
     *
     * @throws GameActionException
     */
    static void tryBuildRefinery() throws GameActionException {
        if (rc.getTeamSoup() < 200)
            return;
        halt = false;
        Direction dir = null;
        MapLocation currLoc = rc.getLocation();
        if (nearestSoup == null)
            dir = currLoc.directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
        else
            dir = currLoc.directionTo(nearestSoup);
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
            if (hqLoc.distanceSquaredTo(currLoc.add(d)) > 18 && tryBuild(RobotType.REFINERY, d))
                return;
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

    /**
     * Attempts to drop unit around a given direction.
     * Used for when the exact drop off point doesn't really matter (such as for cows).
     *
     * @param dir The intended direction of dropping
     * @return true if a move was performed
     * @throws GameActionException
     */
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
     * Only digs if there isn't a friendly unit there.
     *
     * @param dir The intended direction of digging
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDig(Direction dir) throws GameActionException {
        if (rc.canDigDirt(dir)){
            RobotInfo robot = rc.senseRobotAtLocation(rc.getLocation().add(dir));
            if (robot == null || robot.getTeam() != rc.getTeam()) {
                rc.digDirt(dir);
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to dig dirt around a given direction.
     *
     * @param dir The intended direction of digging
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDigAround(Direction dir) throws GameActionException {
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
            if (tryDig(d))
                return true;
        return false;
    }

    /**
     * Attempts to deposit dirt in a given direction.
     *
     * @param dir The intended direction of depositing dirt
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDeposit(Direction dir) throws GameActionException {
        if(rc.canDepositDirt(dir)){
            rc.depositDirt(dir);
            return true;
        }
        return false;
    }

    /**
     * Attempts to deposit dirt around a given direction.
     *
     * @param dir The approximate direction of depositing dirt
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDepositAround(Direction dir) throws GameActionException {
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
            if (tryDeposit(d))
                return true;
        return false;
    }

    /**
     * Attempts to pick up a unit.
     *
     * @param id the ID of the target unit
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryPickUp(int id) throws GameActionException {
        if(rc.canPickUpUnit(id)){
            rc.pickUpUnit(id);
            return true;
        }
        return false;
    }


    // UTILITIES

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
     * Finds the farthest corner to the robot.
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
     * Searches a list of robots and finds the miner closest to a specified location.
     * Returns -1 if no miner found (i.e. nearbyRobots is empty).
     * 
     * @param nearbyRobots list of MapLocations containing robots
     * @param loc location to find closest miner to
     * @return id of closest robot
     * @throws GameActionException
     */
    static int findNearestMiner(RobotInfo[] nearbyRobots, MapLocation loc) {
        int id = lastMinerID;
        int lmd = 8000;
        int md;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.getType() == RobotType.MINER && robot.getTeam() == rc.getTeam()) {
                md = robot.getLocation().distanceSquaredTo(loc);
                if (md < lmd) {
                    lmd = md;
                    id = robot.getID();
                }
            }
        }
        return id;
    }

    /**
     * Sets the MapLocations containing the defensive positions for the buildings and drones.
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
            if ((hqLoc.x < mapWidth/2 && hqLoc.y >= mapHeight/2) || (hqLoc.x >= mapWidth/2 && hqLoc.y < mapHeight/2)) {
                defensiveVapLoc       = hqLoc.translate(-3*s,  3*s);
                defensiveCenterLoc    = hqLoc.translate( 1*s,  3*s);
                defensiveSchoolLoc    = hqLoc.translate(-3*s, -1*s);
                defensiveGunLocs[0]   = hqLoc.translate( 3*s,  1*s);
                defensiveGunLocs[1]   = hqLoc.translate(-1*s, -3*s);
                defensiveDroneLocs[0] = hqLoc.translate( 3*s, -3*s);
                defensiveDroneLocs[1] = hqLoc.translate( 0  , -3*s);
                defensiveDroneLocs[2] = hqLoc.translate( 3*s,  0  );
                defensiveDroneLocs[3] = hqLoc.translate( 3*s, -1*s);
                defensiveDroneLocs[4] = hqLoc.translate( 1*s, -3*s);
                defensiveDroneLocs[5] = hqLoc.translate(-3*s, -3*s);
                defensiveDroneLocs[6] = hqLoc.translate( 3*s,  3*s);
            }
            // Top Right/Bottom Left
            else {
                defensiveVapLoc       = hqLoc.translate( 3*s,  3*s);
                defensiveCenterLoc    = hqLoc.translate( 3*s, -1*s);
                defensiveSchoolLoc    = hqLoc.translate(-1*s,  3*s);
                defensiveGunLocs[0]   = hqLoc.translate( 1*s, -3*s);
                defensiveGunLocs[1]   = hqLoc.translate(-3*s,  1*s);
                defensiveDroneLocs[0] = hqLoc.translate(-3*s, -3*s);
                defensiveDroneLocs[1] = hqLoc.translate( 0  , -3*s);
                defensiveDroneLocs[2] = hqLoc.translate(-3*s,  0  ); 
                defensiveDroneLocs[3] = hqLoc.translate(-3*s, -1*s);
                defensiveDroneLocs[4] = hqLoc.translate(-1*s, -3*s);
                defensiveDroneLocs[5] = hqLoc.translate( 3*s, -3*s);
                defensiveDroneLocs[6] = hqLoc.translate(-3*s,  3*s);   
            }
        }

        // Edge
        else {
            // Top/Bottom
            if (closestY < mapHeight/3) {
                s = (hqLoc.y >= mapHeight/2) ? 1 : -1;
                defensiveVapLoc       = hqLoc.translate( 0  ,  3*s);
                defensiveCenterLoc    = hqLoc.translate(-3*s,  0  );
                defensiveSchoolLoc    = hqLoc.translate( 3*s,  0  );
                defensiveGunLocs[0]   = hqLoc.translate( 3*s, -3*s);
                defensiveGunLocs[1]   = hqLoc.translate(-3*s, -3*s);
                defensiveDroneLocs[0] = hqLoc.translate( 0  , -3*s);
                defensiveDroneLocs[1] = hqLoc.translate(-3*s, -1*s);
                defensiveDroneLocs[2] = hqLoc.translate( 3*s, -1*s);
                defensiveDroneLocs[3] = hqLoc.translate(-1*s, -3*s);
                defensiveDroneLocs[4] = hqLoc.translate( 1*s, -3*s);
                defensiveDroneLocs[5] = hqLoc.translate(-3*s,  1*s);
                defensiveDroneLocs[6] = hqLoc.translate( 3*s,  1*s);
            }
            // Left/Right
            else {
                s = (hqLoc.x >= mapWidth/2) ? 1 : -1;
                defensiveVapLoc       = hqLoc.translate( 3*s,  0  );
                defensiveCenterLoc    = hqLoc.translate( 0  ,  3*s);
                defensiveSchoolLoc    = hqLoc.translate( 0  , -3*s);
                defensiveGunLocs[0]   = hqLoc.translate(-3*s, -3*s);
                defensiveGunLocs[1]   = hqLoc.translate(-3*s,  3*s);
                defensiveDroneLocs[3] = hqLoc.translate(-3*s,  0  );
                defensiveDroneLocs[5] = hqLoc.translate(-1*s,  3*s);
                defensiveDroneLocs[1] = hqLoc.translate(-1*s, -3*s);
                defensiveDroneLocs[2] = hqLoc.translate(-3*s, -1*s);
                defensiveDroneLocs[4] = hqLoc.translate(-3*s,  1*s);
                defensiveDroneLocs[6] = hqLoc.translate( 1*s,  3*s);
                defensiveDroneLocs[0] = hqLoc.translate( 1*s, -3*s); 
            }
        }
    }

    /**
     * Find the best locations to send a certain number of miners to explore the map.
     * 
     * @param numMiners the number of miners we have
     * @return an array of MapLocations to send the miners towards
     * @throws GameActionException
     */
    static void setMinerDests(int numMiners) throws GameActionException {

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
     * Attempt to build a miner (in the "optimal" direction away from the HQ").
     * 
     * @return true if a miner was made
     * @throws GameActionException
     */
    static boolean tryMakeMiner() throws GameActionException {
        if (rc.getTeamSoup() >= 70 && tryBuildAround(RobotType.MINER, 
            rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2))))
            return true;
        return false;
    }

    /**
     * Checks for new soup and new water in the outer rings of the unit's vision.
     * This can be prettier but I think the repetitive code is more efficient.
     * 
     * @throws GameActionException
     */
    static void checkSoupWater() throws GameActionException {

        MapLocation currLoc = rc.getLocation();

        // New Soup
        MapLocation[] nearbySoups = rc.senseNearbySoup();
        for (MapLocation loc : nearbySoups) {
            if (!soupLocs.contains(loc)) {
                soupLocs.add(loc);
                tryBroadcastMessage(1, loc.x, loc.y, SOUP_FOUND, 0, 0, 0, 0);
            }
            if (rc.getType() == RobotType.MINER && (nearestSoup == null || currLoc.distanceSquaredTo(loc) < lsd)) {
                nearestSoup = loc;
                lsd = currLoc.distanceSquaredTo(loc);
            }
        }

        // New water (only sense outer 2 rings for drones and only outer ring for miner)
        int rad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
        // Only sense outer 2 rings for drones until we know of 10 waters
        if (rc.getType() == RobotType.DELIVERY_DRONE && waterLocs.size() < 10) {
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = currLoc.translate(i, j);
                    if (rc.canSenseLocation(loc) && rc.senseFlooding(loc) && !waterLocs.contains(loc)) {
                        // Only add if it's far enough from the other water locs
                        boolean tooClose = false;
                        for (MapLocation water : waterLocs) {
                            if (water.distanceSquaredTo(loc) <= 18) {
                                tooClose = true;
                                break;
                            }
                        }
                        if (!tooClose) {
                            waterLocs.add(loc);
                            tryBroadcastMessage(1, loc.x, loc.y, WATER_FOUND, 0, 0, 0, 0);
                        }
                    }
                    if (Math.abs(i) <= rad-2 && j == -rad+1)
                        j += 2*rad-3;
                }
            }
        }
        // Only sense outer 1 ring for miners (and lategame drones)
        else {
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = currLoc.translate(i, j);
                    if (rc.canSenseLocation(loc) && rc.senseFlooding(loc) && !waterLocs.contains(loc)) {
                        // Only add if it's far enough from the other water locs
                        boolean tooClose = false;
                        for (MapLocation water : waterLocs) {
                            if (water.distanceSquaredTo(loc) <= 18) {
                                tooClose = true;
                                break;
                            }
                        }
                        if (!tooClose) {
                            waterLocs.add(loc);
                            tryBroadcastMessage(1, loc.x, loc.y, WATER_FOUND, 0, 0, 0, 0);
                        }
                    }
                    if (Math.abs(i) <= rad-1 && j == -rad)
                        j += 2*rad-1;
                }
            }
        }
    }

    /**
     * Finds the HQ location from the blockchain.
     * Relies on the fact that the HQ location must be broadcasted early on.
     * 
     * @throws GameActionException
     */
    static void getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[2]/10000000 == TEAM_SECRET && mess[2]%100 == HQ_LOC){
                    hqLoc = new MapLocation(-mess[0], -mess[1]);
                    return;
                }
            }
        }
    }

    /**
     * If the original location is flooded or is a wall, find it a new location.
     * 
     * @param loc the original location
     * @return a new location
     * @throws GameActionException
     */
    static MapLocation suitableLoc(MapLocation oLoc) throws GameActionException {
    	if (rc.canSenseLocation(oLoc) && (rc.senseFlooding(oLoc) || Math.abs(rc.senseElevation(oLoc)-groundLevel) > 3)) {
    		int rad = (int) (Math.sqrt(rc.getCurrentSensorRadiusSquared()) - .5);
            for (int i=-rad; i<=rad; i++) {
                for (int j=-rad; j<=rad; j++) {
                    MapLocation loc = hqLoc.translate(i,j);
                    // Make sure the location is not flooding, not too high, and not occupied
                    if (rc.canSenseLocation(loc) && !rc.senseFlooding(loc) && Math.abs(rc.senseElevation(loc)-groundLevel) <= 3 
                    	&& rc.senseRobotAtLocation(loc) == null)
                        return loc;
                    if (Math.abs(i) <= 2 && j == -3)
                        j += 5;
                }
            }
    	}
    	return oLoc;
    }

    // COMMUNICATION

    /**
     * Update soup/water/refineryLocs based on blockchain message.
     * Code: 0 is soup, 1 is water, 2 is refinery.
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
        MapLocation loc = new MapLocation(-mess[0], -mess[1]);
        if (mess[2]%100 == createCode)
            locs.add(loc);
        else if (mess[2]%100 == destroyCode)
            locs.remove(loc);
        // Special case for miners processing soup
        if (rc.getType() == RobotType.MINER && code == 0) {
            MapLocation currLoc = rc.getLocation();
            // If soup found is closer than nearestSoup, update nearestSoup to soup found
            if (mess[2]%100 == SOUP_FOUND
                && (nearestSoup == null || currLoc.distanceSquaredTo(loc) < lsd)) {
                nearestSoup = loc;
                lsd = currLoc.distanceSquaredTo(loc);
            }
            // If soup gone was the closest soup, find nearest soup again
            else if (mess[2]%100 == SOUP_GONE && (nearestSoup == null || nearestSoup.equals(loc))) {
                nearestSoup = null;
                lsd = 8000;
                int sd;
                for (MapLocation soupLoc : soupLocs) {
                    sd = currLoc.distanceSquaredTo(soupLoc);
                    if (nearestSoup == null || sd < lsd) {
                        lsd = sd;
                        nearestSoup = soupLoc;
                    }
                }
            }
        } 
    }

    /**
     * Broadcast soup/water/refinery locations to blockchain.
     * Typically done upon miner/drone spawn.
     * Broadcasts with a specific ID so only one unit processes the data.
     * 
     * @param mess the blockchain message to parse
     * @param code the specific resource/building to update
     * @throws GameActionException
     */
    static void broadcastAll(int id) throws GameActionException {
        broadcastLocs(soupLocs, id, INIT_SOUP_LOCS);
        broadcastLocs(refineryLocs, id, INIT_REFINERY_LOCS);
        broadcastLocs(waterLocs, id, INIT_WATER_LOCS);
    }

    /**
     * Generic message broadcasting template.
     * See the function for the required message format.
     *
     * @return true if a message was broadcasted
     * @throws GameActionException
     */
    static boolean broadcastMessage(int cost, int m0, int m1, int m2, int m3, int m4, int m5, 
        int m6) throws GameActionException {
        int[] message = new int[7];
        message[0] = -1 * m0; // -xLoc (negated for encryption)
        message[1] = -1 * m1; // -yLoc (negated for encryption)
        message[2] = TEAM_SECRET * 10000000 + m2; // teamCode + messageCode (i.e. 78901 is HQ location)
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
     * Broadcast a message from an int[] array, specifically for the messageQ.
     *
     * @param m int[] holding message to broadcast
     * @return true if a message was broadcasted
     * @throws GameActionException
     */
    static boolean broadcastMessage(int[] m) throws GameActionException {
        return broadcastMessage(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7]);
    }

    /**
     * Try to broadcast a message; if unsuccessful, save into messageQ.
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
     * Try to broadcast a message; if unsuccessful, save into messageQ.
     * 
     * @param m size 8 array that includes soup cost
     * @throws GameActionException
     */
    static void tryBroadcastMessage(int[] m) throws GameActionException {
        if (!broadcastMessage(m))
            messageQ.add(m);
    }

    /**
     * Try to broadcast each message in the message PriorityQueue.
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
     * Broadcasts an ArrayList of MapLocations in a series of messages, up to 12 per message.
     * Broadcasts with one specific ID so only one unit ingests the data.
     * 
     * @param setLocs the set containing the locations
     * @param id the id of the unit intended to receive the messages
     * @param code the integer representing what is at each location
     * @throws GameActionException
     */
    static void broadcastLocs(Set<MapLocation> setLocs, int id, int code) throws GameActionException {
        // First convert to ArrayList for indexing
        List<MapLocation> locs = new ArrayList<MapLocation>(setLocs);
        // Handle the array in batches of 12
        for (int i=0; i<(locs.size()+11)/12; i++) {
            int[] message = new int[8];
            message[0] = 1;     // Soup cost
            message[3] = id * 100 + code;  // Message code
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
     * Decodes a message containing up to 12 coordinates.
     * 
     * @param mess the integer array to decode
     * @param code the integer representing the resource/building
     * @throws GameActionException
     */
    static void decodeLocsMessage(int[] mess, Set<MapLocation> locs) throws GameActionException {
        // Only process if ID matches
        if ((mess[2]-TEAM_SECRET*10000000)/100 != rc.getID())
            return;
        // Process message
        for (int i=0; i<7 && i!=2; i++) {
            if (i < 2)
                mess[i] = -mess[i];
            if (mess[i] > 20000)
                locs.addAll(decodeLocs(mess[i]));
            else if (mess[i] > 0)
                locs.add(decodeLoc(mess[i]));
        }
        // Special case for miners receiving soupLocs
        if (rc.getType() == RobotType.MINER && locs.equals(soupLocs)) {
            MapLocation currLoc = rc.getLocation();
            lsd = 8000;
            int sd;
            for (MapLocation loc : soupLocs) {
                sd = currLoc.distanceSquaredTo(loc);
                if (nearestSoup == null || sd < lsd) {
                    lsd = sd;
                    nearestSoup = loc;
                }
            }
        }
    }

    /**
     * Encode a MapLocation in the format of 1+xCoord+yCoord for condensation purposes.
     * For example, the coordinate (1, 30) becomes 10130.
     * 
     * @param loc the location to encode
     * @return the encoded integer
     * @throws GameActionException
     */
    static int encodeLoc(MapLocation loc) throws GameActionException {
        return 10000 + loc.x * 100 + loc.y;
    }

    /**
     * Encodes two MapLocations in the format of 1+x1+y1+x2+y2 for condensation purposes.
     * For example, the coordinates (1, 30) and (0, 16) become 101300016.
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
     * Decode a MapLocation in the format of 1+x+y.
     * For example, 10130 becomes (1, 30).
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
     * Decodes two MapLocations in the format of 1+x1+y1+x2+y2.
     * For example, 101300016 becomes (1, 30) and (0, 16).
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

}
