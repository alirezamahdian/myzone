/*
 * This file is part of JSTUN. 
 * 
 * Copyright (c) 2005 Thomas King <king@t-king.de> - All rights
 * reserved.
 * 
 * This software is licensed under either the GNU Public License (GPL),
 * or the Apache 2.0 license. Copies of both license agreements are
 * included in this distribution.
 */

package utils.peer.stun.attribute;

import utils.peer.stun.attribute.MessageAttributeInterface.MessageAttributeType;

public class UnknownMessageAttributeException extends MessageAttributeParsingException {
	private static final long serialVersionUID = 5375193544145543299L;
	
	private MessageAttributeType type;
	
	public UnknownMessageAttributeException(String mesg, MessageAttributeType type) {
		super(mesg);
		this.type = type;
	}
	
	public MessageAttributeType getType() {
		return type;
	}
}
