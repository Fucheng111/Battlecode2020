package mbot;

import java.util.*;

import com.sun.tools.javap.TryBlockWriter;

import battlecode.common.*;
import mbot.Communication.*;
import mbot.RobotPlayer;

public class HQ extends RobotPlayer {
	static HashSet<MapLocation> soupLocations = new HashSet<>();
	static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
	static ArrayList<MapLocation> exploreLocations = new ArrayList<>();
	static int exploreLocationsIndex = 0;

	static int numLandscapers = 0;
	
	static boolean locationInBlockchain = false;
	static final MapLocation hqLoc = rc.getLocation();
	
	/**
	 * TODO blockchain handling
	 * 
	 * 
	 * @throws GameActionException
	 */
    static void run() throws GameActionException {
    	
    	
    	if (exploreLocations.isEmpty())
    		exploreLocations = new ArrayList<>(Arrays.asList(bestMinerLocs(4)));
    	
		broadcastLocationIfNotInBlockchain();
		
		// Listen for communication
		if (rc.getRoundNum() > 1)
			listenForCommunication();
		
		// Create initial miners
		createInitialMiners();
		
		// If nothing else is broadcast, broadcast the next msg in messageQ
		if (!messageQ.isEmpty() && messageQ.peek().tryBroadcast())
			messageQ.poll();
		
    }
    
    /**
     * Listen for incoming communiques from other bots.
     * <br>Listens for MINER_IDLE and broadcasts next location in exploreLocations
     * <br>Listens for SOUP_FOUND and rebroadcasts it to other miners
     * 
     * @throws GameActionException
     */
    private static void listenForCommunication() throws GameActionException {
		
    	// Listen for miner idle communications
    	for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
    		Message msg = new Message(tx);
    		MessageType type = msg.getMessageType();
    		
    		if (msg.isTeamMessage()) {
    			if (type == MessageType.MINER_IDLE) {
    				int minerId = msg.getId();
    				
    				// loop back around initialExploreLocations if needed
    				if (exploreLocationsIndex >= exploreLocations.size())
    					exploreLocationsIndex = 0;
    				
    				MapLocation loc = exploreLocations.get(exploreLocationsIndex++);
    				
    				ExploreMessage message = new ExploreMessage();
    				
    				if (message.setInfo(loc.x, loc.y, minerId).tryBroadcast())
    					messageQ.add(msg);
   
    			} else if (type == MessageType.SOUP_LOCATION) {
    				soupLocations.add(msg.getLocation());
    				
    				if (!msg.tryBroadcast())
    					messageQ.add(msg);
    				
    			} else if (type == MessageType.SOUP_GONE) {
    				soupLocations.remove(msg.getLocation());
    				
    				if (!msg.tryBroadcast())
    					messageQ.add(msg);
    				
    			}
    		}
    	}		
	}

	static void createInitialMiners() throws GameActionException {
    	if (numMiners <= 4) {
			if (Util.tryBuild(RobotType.MINER, Util.randomDirection())) {
				numMiners++;
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
     * A one-time function that broadcasts the HQ location to the blockchain
     * if it is not in there already.
     * 
     * @throws GameActionException
     */
    
    static void broadcastLocationIfNotInBlockchain() throws GameActionException {
    	if (locationInBlockchain) return;
    	
		// Check again if location is in blockchain
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                
            	Message m = new Message(tx);
            	MessageType type = m.getMessageType();
            	
            	if (m.isTeamMessage() && type == MessageType.HQ_FOUND) {
            		locationInBlockchain = true;
            		return;
            	}
            }
        }
        
        // Try to broadcast again
        new HQFoundMessage().setInfo(hqLoc.x, hqLoc.y).tryBroadcast();
    }
}

