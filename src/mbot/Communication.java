package mbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Transaction;

public class Communication extends RobotPlayer {
	static final int TEAM_CODE_POS = 2;
	static final int MESSAGE_TYPE_POS = 3;
	
	/**
	 * Message type Enum that maps each type to a unique code value.
	 * 
	 * @author mark jung
	 *
	 */
	public static enum MessageType {
		HQ_FOUND(0, HQFoundMessage.class), REFINERY_CREATED(1, RefineryCreatedMessage.class), VAPORATOR_CREATED(2, VaporatorCreatedMessage.class), 
		DESIGN_SCHOOL_CREATED(3, DesignSchoolCreatedMessage.class), FULFILLMENT_CENTER_CREATED(4, FulfillmentCenterCreatedMessage.class),
		NET_GUN_CREATED(5, NetGunCreatedMessage.class), SOUP_LOCATION(8, SoupLocationMessage.class), SOUP_GONE(9, SoupGoneMessage.class);
		
		int value;
		Class c;
		
		MessageType(int code, Class c) {
			this.value = code;
			this.c = c;
		}
	}
	
	/**
	 * Generic message class that is extended by all other message classes.
	 * 
	 * @author mark jung
	 *
	 */
	public static class Message {
		public int code;
		public int bid;
		public int[] message;
		
		public int cost;
		public boolean teamMessage;
		public MessageType messageType;
		
		public Message() {
			this(defaultBid);
		}
		
		public Message(int bid) {
			this.bid = bid;
			this.message = new int[7];
			this.message[TEAM_CODE_POS] = TEAM_SECRET;
		}
		
		public Message(Transaction tx) {
			message = tx.getMessage();
			cost = tx.getCost();
			
			teamMessage = message[TEAM_CODE_POS] == TEAM_SECRET;
			
			for (MessageType mt : MessageType.values()) {
				if (mt.value == message[MESSAGE_TYPE_POS]) {
					messageType = mt;				}
			}
		}
		
		public boolean tryBroadcast() throws GameActionException {
			if (rc.canSubmitTransaction(message, bid)) {
	            rc.submitTransaction(message, bid);
	            return true;
	        }
			return false;
		}
		
		/**
		 * Return map location for each message.
		 * 
		 * @return
		 */
		public MapLocation getLocation() {
			return new MapLocation(message[0], message[1]);
		}
		
		public boolean isTeamMessage() {
			return teamMessage;
		}
		
		public MessageType getMessageType() {
			return messageType;
		}
	}
	
	public static class HQFoundMessage extends Message {
		public HQFoundMessage() {
			this(defaultBid);
		}
		
		public HQFoundMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.HQ_FOUND.value;
		}
		
		public HQFoundMessage setInfo(int xLoc, int yLoc) {			
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}
	
	public static class RefineryCreatedMessage extends Message {
		public RefineryCreatedMessage() {
			this(defaultBid);
		}
		
		public RefineryCreatedMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.REFINERY_CREATED.value;
		}
		
		public RefineryCreatedMessage setInfo(int xLoc, int yLoc) {
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}
	
	public static class VaporatorCreatedMessage extends Message {
		public VaporatorCreatedMessage() {
			this(defaultBid);
		}
		
		public VaporatorCreatedMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.VAPORATOR_CREATED.value;
		}
		
		public VaporatorCreatedMessage setInfo(int xLoc, int yLoc) {
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}
	
	public static class DesignSchoolCreatedMessage extends Message {
		public DesignSchoolCreatedMessage() {
			this(defaultBid);
		}
		
		public DesignSchoolCreatedMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.DESIGN_SCHOOL_CREATED.value;
		}
		
		public DesignSchoolCreatedMessage setInfo(int xLoc, int yLoc, int minerID) {
			message[0] = xLoc;
			message[1] = yLoc;
			message[4] = minerID;
			
			return this;
		}
	}
	
	public static class FulfillmentCenterCreatedMessage extends Message {
		public FulfillmentCenterCreatedMessage() {
			this(defaultBid);
		}
		
		public FulfillmentCenterCreatedMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.FULFILLMENT_CENTER_CREATED.value;
		}
		
		public FulfillmentCenterCreatedMessage setInfo(int xLoc, int yLoc) {
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}
	
	public static class NetGunCreatedMessage extends Message {
		public NetGunCreatedMessage() {
			this(defaultBid);
		}
		
		public NetGunCreatedMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.NET_GUN_CREATED.value;
		}
		
		public NetGunCreatedMessage setInfo(int xLoc, int yLoc) {
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}
	
	public static class SoupLocationMessage extends Message {
		public SoupLocationMessage() {
			this(defaultBid);
		}
		
		public SoupLocationMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.SOUP_LOCATION.value;
		}
		
		public SoupLocationMessage setInfo(int xLoc, int yLoc) {
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}

	public static class SoupGoneMessage extends Message {
		public SoupGoneMessage() {
			this(defaultBid);
		}
		
		public SoupGoneMessage(int bid) {
			super(bid);
			
			message[MESSAGE_TYPE_POS] = MessageType.SOUP_GONE.value;
		}
		
		public SoupGoneMessage setInfo(int xLoc, int yLoc) {
			message[0] = xLoc;
			message[1] = yLoc;
			
			return this;
		}
	}
	
	
}
