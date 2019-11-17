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
package org.l2jdevs.gameserver.model.actor.instance;

import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;

public class L2WarehouseInstance extends L2NpcInstance
{
	/**
	 * Creates a warehouse NPC.
	 * @param template the warehouse NPC template
	 */
	public L2WarehouseInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2WarehouseInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/warehouse/" + pom + ".htm";
	}
	
	@Override
	public boolean isWarehouse()
	{
		return true;
	}
}
