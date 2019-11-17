/*
 * Copyright © 2004-2019 L2JDevs
 * 
 * This file is part of L2JDevs.
 * 
 * L2JDevs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2JDevs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.gameserver.network.clientpackets;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.instancemanager.MailManager;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.entity.Message;
import org.l2jdevs.gameserver.model.zone.ZoneId;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ExChangePostState;
import org.l2jdevs.gameserver.util.Util;

/**
 * @author Migi, DS
 */
public final class RequestDeleteReceivedPost extends L2GameClientPacket
{
	private static final String _C__D0_68_REQUESTDELETERECEIVEDPOST = "[C] D0:68 RequestDeleteReceivedPost";
	
	private static final int BATCH_LENGTH = 4; // length of the one item
	
	int[] _msgIds = null;
	
	@Override
	public String getType()
	{
		return _C__D0_68_REQUESTDELETERECEIVEDPOST;
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if ((activeChar == null) || (_msgIds == null) || !Config.ALLOW_MAIL)
		{
			return;
		}
		
		if (!activeChar.isInsideZone(ZoneId.PEACE))
		{
			activeChar.sendPacket(SystemMessageId.CANT_USE_MAIL_OUTSIDE_PEACE_ZONE);
			return;
		}
		
		for (int msgId : _msgIds)
		{
			Message msg = MailManager.getInstance().getMessage(msgId);
			if (msg == null)
			{
				continue;
			}
			if (msg.getReceiverId() != activeChar.getObjectId())
			{
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to delete not own post!", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (msg.hasAttachments() || msg.isDeletedByReceiver())
			{
				return;
			}
			
			msg.setDeletedByReceiver();
		}
		activeChar.sendPacket(new ExChangePostState(true, _msgIds, Message.DELETED));
	}
	
	@Override
	protected void readImpl()
	{
		int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_msgIds = new int[count];
		for (int i = 0; i < count; i++)
		{
			_msgIds[i] = readD();
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}