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
package org.l2jdevs.gameserver.model.conditions;

import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.instancemanager.CastleManager;
import org.l2jdevs.gameserver.instancemanager.FortManager;
import org.l2jdevs.gameserver.instancemanager.TerritoryWarManager;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.entity.Castle;
import org.l2jdevs.gameserver.model.entity.Fort;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.zone.ZoneId;
import org.l2jdevs.gameserver.network.SystemMessageId;

/**
 * Player Can Create Outpost condition implementation.
 * @author Adry_85
 */
public class ConditionPlayerCanCreateOutpost extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanCreateOutpost(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(L2Character effector, L2Character effected, Skill skill, L2Item item)
	{
		if ((effector == null) || !effector.isPlayer())
		{
			return !_val;
		}
		
		final L2PcInstance player = effector.getActingPlayer();
		boolean canCreateOutpost = true;
		if (player.isAlikeDead() || player.isCursedWeaponEquipped() || (player.getClan() == null))
		{
			canCreateOutpost = false;
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(player);
		final Fort fort = FortManager.getInstance().getFort(player);
		if ((castle == null) && (fort == null))
		{
			canCreateOutpost = false;
		}
		
		if (((fort != null) && (fort.getResidenceId() == 0)) || ((castle != null) && (castle.getResidenceId() == 0)))
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "outpost_residence_id"));
			canCreateOutpost = false;
		}
		else if (((fort != null) && !fort.getZone().isActive()) || ((castle != null) && !castle.getZone().isActive()))
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "outpost_siege_active"));
			canCreateOutpost = false;
		}
		else if (!player.isClanLeader())
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "outpost_only_leader"));
			canCreateOutpost = false;
		}
		else if (TerritoryWarManager.getInstance().getHQForClan(player.getClan()) != null)
		{
			player.sendPacket(SystemMessageId.NOT_ANOTHER_HEADQUARTERS);
			canCreateOutpost = false;
		}
		else if (TerritoryWarManager.getInstance().getFlagForClan(player.getClan()) != null)
		{
			player.sendPacket(SystemMessageId.A_FLAG_IS_ALREADY_BEING_DISPLAYED_ANOTHER_FLAG_CANNOT_BE_DISPLAYED);
			canCreateOutpost = false;
		}
		else if (!player.isInsideZone(ZoneId.HQ))
		{
			player.sendPacket(SystemMessageId.NOT_SET_UP_BASE_HERE);
			canCreateOutpost = false;
		}
		return (_val == canCreateOutpost);
	}
}