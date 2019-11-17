/*
 * Copyright © 2004-2019 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.gameserver.model;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.interfaces.IIdentifiable;
import org.l2jdevs.gameserver.network.serverpackets.AllyCrest;
import org.l2jdevs.gameserver.network.serverpackets.ExPledgeCrestLarge;
import org.l2jdevs.gameserver.network.serverpackets.PledgeCrest;

/**
 * @author NosBit
 */
public final class L2Crest implements IIdentifiable
{
	private final int _id;
	
	private final byte[] _data;
	private final CrestType _type;
	public L2Crest(int id, byte[] data, CrestType type)
	{
		_id = id;
		_data = data;
		_type = type;
	}
	
	/**
	 * Gets the client path to crest for use in html and sends the crest to {@code L2PcInstance}
	 * @param activeChar the @{code L2PcInstance} where html is send to.
	 * @return the client path to crest
	 */
	public String getClientPath(L2PcInstance activeChar)
	{
		String path = null;
		switch (getType())
		{
			case PLEDGE:
			{
				activeChar.sendPacket(new PledgeCrest(getId(), getData()));
				path = "Crest.crest_" + Config.SERVER_ID + "_" + getId();
				break;
			}
			case PLEDGE_LARGE:
			{
				activeChar.sendPacket(new ExPledgeCrestLarge(getId(), getData()));
				path = "Crest.crest_" + Config.SERVER_ID + "_" + getId() + "_l";
				break;
			}
			case ALLY:
			{
				activeChar.sendPacket(new AllyCrest(getId(), getData()));
				path = "Crest.crest_" + Config.SERVER_ID + "_" + getId();
				break;
			}
		}
		return path;
	}
	
	public byte[] getData()
	{
		return _data;
	}
	
	@Override
	public int getId()
	{
		return _id;
	}
	
	public CrestType getType()
	{
		return _type;
	}
	
	public enum CrestType
	{
		PLEDGE(1),
		PLEDGE_LARGE(2),
		ALLY(3);
		
		private final int _id;
		
		private CrestType(int id)
		{
			_id = id;
		}
		
		public static CrestType getById(int id)
		{
			for (CrestType crestType : values())
			{
				if (crestType.getId() == id)
				{
					return crestType;
				}
			}
			return null;
		}
		
		public int getId()
		{
			return _id;
		}
	}
}