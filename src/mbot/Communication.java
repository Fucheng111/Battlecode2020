package mbot;

import battlecode.common.GameActionException;

public class Communication extends RobotPlayer {
	static final int TEAM_CODE_POS = 2;
	static final int MESSAGE_TYPE_POS = 3;
	
	public enum MessageType {
		HQ_FOUND(0), REFINERY_CREATED(1), VAPORATOR_CREATED(2), DESIGN_SCHOOL_CREATED(3), FULFILLMENT_CENTER_CREATED(4),
		NET_GUN_CREATED(5), SOUP_LOCATION(8), SOUP_GONE(9);
		
		int value;
		
		MessageType(int code) {
			this.value = code;
		}
	}
	
	public class UnknownMessage {
		
	}
	
	public abstract class Message {
		public int code;
		public int bid;
		public int[] message;
		
		public Message() {
			this(defaultBid);
		}
		
		public Message(int bid) {
			this.bid = bid;
			this.message = new int[7];
			this.message[TEAM_CODE_POS] = TEAM_SECRET;
		}
		
		public boolean tryBroadcast() throws GameActionException {
			if (rc.canSubmitTransaction(message, bid)) {
	            rc.submitTransaction(message, bid);
	            return true;
	        }
			return false;
		}
	}
	
	public class HQFoundMessage extends Message {
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
	
	public class RefineryCreatedMessage extends Message {
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
	
	public class VaporatorCreatedMessage extends Message {
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
	
	public class DesignSchoolCreatedMessage extends Message {
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
	
	public class FulfillmentCenterCreatedMessage extends Message {
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
	
	public class NetGunCreatedMessage extends Message {
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
	
	public class SoupLocationMessage extends Message {
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

	public class SoupGoneMessage extends Message {
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
