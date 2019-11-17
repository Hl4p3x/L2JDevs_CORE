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

import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.items.L2Henna;
import org.l2jdevs.gameserver.network.SystemMessageId;

/**
 * @author Zoey76
 */
public final class RequestHennaRemove extends L2GameClientPacket
{
	private static final String _C__72_REQUESTHENNAREMOVE = "[C] 72 RequestHennaRemove";
	private int _symbolId;
	
	@Override
	public String getType()
	{
		return _C__72_REQUESTHENNAREMOVE;
	}
	
	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("HennaRemove"))
		{
			sendActionFailed();
			return;
		}
		
		L2Henna henna;
		boolean found = false;
		for (int i = 1; i <= 3; i++)
		{
			henna = activeChar.getHenna(i);
			if ((henna != null) && (henna.getDyeId() == _symbolId))
			{
				if (activeChar.getAdena() >= henna.getCancelFee())
				{
					activeChar.removeHenna(i);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
					sendActionFailed();
				}
				found = true;
				break;
			}
		}
		// TODO: Test.
		if (!found)
		{
			_log.warning(getClass().getSimpleName() + ": Player " + activeChar + " requested Henna Draw remove without any henna.");
			sendActionFailed();
		}
	}
}
