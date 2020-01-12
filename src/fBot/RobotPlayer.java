package fBot;
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

	static int teamCode = 1;

	static int turnCount;
	
	static MapLocation hqLoc;

	static MapLocation hqLocEnemy;


	/*
	 * List of information know by the HQ
	 */
	//soup location keep track by HQ
	static boolean initiation = false;
	static ArrayList<MapLocation> soupLocList;
	//list of Commands; 1 is used to broadcast HQ location
	static int sendMinerToMine = 2;


	/*
	 * List of information know by the miners
	 */
	static MapLocation soupLoc;
	static MapLocation refineryLoc;
	static int wantSoup  = 20;
	static int foundSoup = 21;
	static int removeSoup = 22;


	static int numMiners = 0;

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
	                // if still null, search the blockchain
	            	for (int i = 1; i < rc.getRoundNum(); i++){
	                    for(Transaction tx : rc.getBlock(i)) {
	                        int[] mess = tx.getMessage();
	                        if(mess[2] == teamCode){
	                            System.out.println("found the HQ!");
	                            hqLoc = new MapLocation(mess[0], mess[1]);
	                        }
	                    }
	                }
	            }
	        }

	}

	static void runHQ() throws GameActionException {
		//initiation step
		if(!initiation && rc.getRoundNum()>1) {
			if(broadcastMessage(1,rc.getLocation().x,rc.getLocation().y,teamCode,1,1,1,1)) {
				System.out.println("initiation");
				hqLoc = rc.getLocation();
				soupLocList = new ArrayList<MapLocation>();
				initiation = true;
			}
		}

		if(numMiners < 10) {
			for (Direction dir : directions)
				if(tryBuild(RobotType.MINER, dir)){
					numMiners++;
				}
		}

		if(rc.getRoundNum()>2) {
			Transaction[] transactions = rc.getBlock(rc.getRoundNum()-1);

			for(Transaction transaction: transactions) {
				int[] message = transaction.getMessage();
				if(message[2]==teamCode && message[3] == foundSoup) {
					soupLocList.add(new MapLocation(message[0],message[1]));
				}

				if (message[2]==teamCode && message[3] == wantSoup) {
					if(!soupLocList.isEmpty()) {
						broadcastMessage(1, soupLocList.get(0).x, soupLocList.get(0).y, teamCode, sendMinerToMine, 1, 1, 1);
						
						
					}
				}
				
				if (message[2]==teamCode && message[3] == removeSoup) {
					if(!soupLocList.isEmpty()) {
						soupLocList.remove(new MapLocation(message[0],message[1]));
					}
				}
				
				
			}
		}



	}

	//find soup location
	static boolean askSoupFromHQ() throws GameActionException{

		if (rc.getCooldownTurns() < 3) {
			broadcastMessage(1, 0, 0, teamCode, wantSoup, 0, 0, 0);
		}

		while(soupLoc == null && !rc.isReady()) {
			for(Transaction i : rc.getBlock(rc.getRoundNum()-1)) {
				int[] message_soup = i.getMessage();
				if(message_soup[2]== teamCode && message_soup[3]==sendMinerToMine) {
					soupLoc = new MapLocation(message_soup[0],message_soup[1]);
					
					return true;
				}
			}

			Clock.yield();
		}

		return false;

	}
	

	static void runMiner() throws GameActionException {
		findHQ();
		
		checkIfSoupGone();

		if(soupLoc == null) {
			if(!askSoupFromHQ()) {

				Direction dir = randomDirection();
				while(true) {

					for (Direction dire : directions)
						if (tryRefine(dire))             
							System.out.println("I refined soup! " + rc.getTeamSoup());
					for (Direction dire : directions)
						if (tryMine(dire)) {
							broadcastMessage(1,rc.getLocation().add(dire).x,rc.getLocation().add(dire).y,teamCode,foundSoup,1,1,1);
							soupLoc = rc.getLocation().add(dire);
							return;
						}
					//					 RobotInfo[] listofRobots = rc.senseNearbyRobots(18);
					//					 boolean avoidCow = false;
					//					 for(RobotInfo robot : listofRobots) {
					//						 if(robot.type.equals(RobotType.COW)) {
					//							 avoidCow = true;
					//						 }
					//					 }

					
					if(floodFront(dir)) {
						dir = dir.rotateLeft();
					
					}
					
					if(goTo(dir)) {
						System.out.print("I'm Moving in "+dir.name());
					}
					
					

					Clock.yield();

				}
			}
		}

		
		if(soupLoc.distanceSquaredTo(hqLoc) > 35) {
			RobotInfo[] robots = rc.senseNearbyRobots();
			for (RobotInfo robot: robots) {
				if(robot.type == RobotType.REFINERY) {
					refineryLoc = robot.getLocation();
				}
			}
			
			//build refinery
			if(refineryLoc == null) {
				for (Direction dir : directions)
					if (tryBuild(RobotType.REFINERY,dir))             
						System.out.println("I build a refinery!");
			}
		}

		for (Direction dir : directions)
			if (tryRefine(dir))             
				System.out.println("I refined soup! " + rc.getTeamSoup());
		for (Direction dir : directions)
			if (tryMine(dir)) 
				System.out.println("I mined soup! " + rc.getSoupCarrying());

		
		if (!nearbyRobot(RobotType.DESIGN_SCHOOL) && rc.getLocation().isWithinDistanceSquared(hqLoc, 8)&&rc.getLocation().distanceSquaredTo(hqLoc)>2){
			if(tryBuild(RobotType.DESIGN_SCHOOL, randomDirection()))
				System.out.println("created a design school");
		}
		
		
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			// time to go back to the HQ
			if(refineryLoc != null) {
				goTo(refineryLoc);
			}
			
			if(goTo(hqLoc))
				System.out.println("moved towards HQ");
		}else if(soupLoc != null) {
			if(goTo(soupLoc)) {
				System.out.println("moved towards Soup");
			}
		}else if (goTo(randomDirection())) {
			// otherwise, move randomly as usual
			System.out.println("I moved randomly!");
		}
	}

	static void runRefinery() throws GameActionException {
		// System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
	}

	static void runVaporator() throws GameActionException {

	}

	static void runDesignSchool() throws GameActionException {
		for (Direction dir : directions)
			if(tryBuild(RobotType.LANDSCAPER, dir))
				System.out.println("made a landscaper");
	}

	static void runFulfillmentCenter() throws GameActionException {
		for (Direction dir : directions)
			tryBuild(RobotType.DELIVERY_DRONE, dir);
	}

	static void runLandscaper() throws GameActionException {
		if(hqLoc == null) {
			findHQ();
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

	static boolean tryDig() throws GameActionException {
		Direction dir = randomDirection();
		if(rc.canDigDirt(dir)){
			rc.digDirt(dir);
			return true;
		}
		return false;
	}

	static boolean broadcastMessage(int soupCost, int m0, int m1, int m2, int m3, int m4, int m5, int m6) throws GameActionException {
		int[] message = new int[7];
		message[0] = m0; // xLoc
		message[1] = m1; // yLoc
		message[2] = m2; // teamSecret
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
		if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
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

	static void checkIfSoupGone() throws GameActionException {
		if(soupLoc != null) {
	        if(rc.canSenseLocation(soupLoc)&&rc.senseSoup(soupLoc) ==0) {
	        	soupLoc = null;
	        	broadcastMessage(1, soupLoc.x, soupLoc.y , teamCode, removeSoup, 1, 1, 1);
	        }
		}
    }
	
	static boolean floodFront(Direction dir) throws GameActionException{
		Direction[] front = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};

		for(Direction dire: front) {
			if(rc.senseFlooding(rc.getLocation().add(dire))) {
				return true;
			}
		}
		
		return false;
	}
	

	static void tryBlockchain() throws GameActionException {
		//        if (turnCount > 0) {
		//            int[] message = new int[7];
		//            for (int i = 0; i < 7; i++) {
		//                message[i] = 123;
		//            }
		//            if (rc.canSubmitTransaction(message, 0))
		//                rc.submitTransaction(message, 0);
		//        }
		// System.out.println(rc.getRoundMessages(turnCount-1));
	}
}