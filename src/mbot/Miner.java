package mbot;

import java.util.*;

import battlecode.common.*;

public class Miner extends RobotPlayer {
	static MapLocation nearestSoup;
	static MapLocation nearestRefinery = hqLoc;
	
	static HashSet<MapLocation> soupLocations = new HashSet<>();
	static HashSet<MapLocation> refineryLocations = new HashSet<>();
	
	/**
	* TODO implement communication
	* TODO implement refining
	* TODO implement mining
	* TODO implement building
	* TODO a* pathing
	* 
	* @throws GameActionException
	*/
	static void run() throws GameActionException {
		
		// Update information from blockchain
		for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[2] == TEAM_SECRET) {
            	if (mess[3] == 1) {
            		soupLocations.add(new MapLocation(mess[0], mess[1]));
            	} else if (mess[3] == 2) {
            		refineryLocations.add(new MapLocation(mess[0], mess[1]));
            	}
            }
		}
		
		// Update soup locations
		detectNearbySoup();
		
		detectNearbyRobots();
		updateNearestSoupLocation();
		updateNearestRefineryLocation();
		
        // Try refining
		for (Direction dir : directions) {
			if (tryRefine(dir)) printAction("refined soup");
		}
		
		// Try mining
		for (Direction dir : directions) {
		    if (tryMine(dir)) {		    	
		    	printAction("mined " + rc.getSoupCarrying() + " soup");
		    	soupLocations.add(rc.getLocation().add(dir));
		    }
		}
		
		// Try building design school
        if (numDesignSchools < 3){
            if(tryBuild(RobotType.DESIGN_SCHOOL, randomDirection())) {
                System.out.println("created a design school");
                numDesignSchools++;
            }
        }
		
		// Head towards refinery (or HQ) if full
		// Otherwise path randomly
		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
			int distToNearestRefinery = rc.getLocation().distanceSquaredTo(nearestRefinery);

			// If too far from nearestRefinery, build a new one
			if (distToNearestRefinery > rc.getMapWidth() / 3) {
				for (Direction dir : directions)
					
					// update nearestRefinery and broadcast for HQ to store
					if (tryBuild(RobotType.REFINERY, dir)) {
						printAction("built refinery");
						nearestRefinery = rc.getLocation().add(dir);
						broadcastMessage(1, nearestRefinery.x, nearestRefinery.y, TEAM_SECRET, 2, 0, 0, 0);
						break;
					}
				
			} else if (goTo(nearestRefinery)) {
				printAction("moved towards nearestRefinery");
			}
		// TODO yeah don't path randomly
		} else if (goTo(randomDirection())) {
			printAction("moved randomly");
		}
	}
	
	static void detectNearbySoup() throws GameActionException {
		int detectRad = (int) Math.sqrt(rc.getCurrentSensorRadiusSquared());
		MapLocation currLoc = rc.getLocation();
		int startX = currLoc.x - detectRad;
		int startY = currLoc.y - detectRad;
		int endX = currLoc.x + detectRad;
		int endY = currLoc.y + detectRad;
		
		for (int i = startX; i < endX; i++) {
			for (int j = startY; j < endY; j++) {
				
				MapLocation loc = new MapLocation(i, j);
				
				// If in detection zone and is soup
				if (rc.canSenseLocation(loc)) {
					if (!soupLocations.contains(loc) && rc.senseSoup(loc) > 0) {
						soupLocations.add(loc);
					} else if (soupLocations.contains(loc) && rc.senseSoup(loc) == 0) {
						soupLocations.remove(loc);
					}
				}
				
				
			}
		}
		
	}
	
	static void detectNearbyRobots() {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		for (RobotInfo ri : nearbyRobots) {
			if (ri.type == RobotType.REFINERY) {
				refineryLocations.add(ri.location);
			}
		}
	}
	
	static void updateNearestSoupLocation() {
		MapLocation currLoc = rc.getLocation();
		int dist = nearestSoup == null ? 
					 Integer.MAX_VALUE : 
					 currLoc.distanceSquaredTo(nearestSoup);
		
		for (MapLocation soupLoc : soupLocations) {
			int newDist = currLoc.distanceSquaredTo(soupLoc);
			
			if (newDist < dist) {
				dist = newDist;
				nearestSoup = soupLoc;
			}
		}
	}
	
	static void updateNearestRefineryLocation() {
		MapLocation currLoc = rc.getLocation();
		int dist = currLoc.distanceSquaredTo(nearestRefinery);
		
		for (MapLocation refLoc : refineryLocations) {
			int newDist = currLoc.distanceSquaredTo(refLoc);
			
			if (newDist < dist) {
				dist = newDist;
				nearestRefinery = refLoc;
			}
		}
	}
	
//	static void pathTowardsTarget() throws GameActionException {
//		MapLocation loc = rc.getLocation();
//		int sensorRad = rc.getCurrentSensorRadiusSquared();
//		
//		Direction toTarget = loc.directionTo(target);
//		boolean moved = false;
//		while (!moved) {
//			if (tryMove(toTarget)) {
//				moved = true;
//			} else {
//				toTarget = toTarget.rotateLeft();
//			}
//		}
//		
//	}
	
//	static class Node {
//		public int f;
//		public int g;
//		public int h;
//		public MapLocation loc;
//		
//		public Node(MapLocation loc, int h) {
//			this.loc = loc;
//			this.h = h;
//			this.f = 0;
//		}
//	}
//	
//	static void aStar() {
//		MapLocation currLoc = rc.getLocation();
//		Node source = new Node(currLoc, 0);
//		source.g = 0;
//		
//		HashSet<MapLocation> visited = new HashSet<>();
//		PriorityQueue<Node> queue = new PriorityQueue<>(new Comparator<Node>() {
//			public int compare(Node a, Node b) {
//				return a.f - b.f;
//			}
//		});
//		
//		queue.add(source);
//		
//		boolean found = false;
//		
//		while (!queue.isEmpty() && !found) {
//			Node curr = queue.poll();
//			
//			visited.add(curr.loc);
//			
//			int adjSize = (int) Math.PI * rc.getCurrentSensorRadiusSquared();
//			MapLocation[] adj = new MapLocation[adjSize];
//			
//			
//			
//		}
//		
//		
//	}
//	
//	
//	static int chebyshevDist(MapLocation a, MapLocation b) {
//	  	return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
//	}
}
