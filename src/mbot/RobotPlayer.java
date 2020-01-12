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

    // Communication instance for each robot
    
    
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
                    	Miner.run();             
                    	break;
                    case REFINERY:           Refinery.run();          break;
                    case VAPORATOR:          Vaporator.run();         break;
                    case DESIGN_SCHOOL: 	 DesignSchool.run();      break;
                    case FULFILLMENT_CENTER: FulfillmentCenter.run(); break;
                    case LANDSCAPER:   		 Landscaper.run();        break;
                    case DELIVERY_DRONE:     DeliveryDrone.run();     break;
                    case NET_GUN:            NetGun.run();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
	}
}
