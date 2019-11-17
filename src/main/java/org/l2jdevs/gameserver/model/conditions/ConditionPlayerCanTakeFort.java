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

import org.l2jdevs.gameserver.instancemanager.FortManager;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.entity.Fort;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;
import org.l2jdevs.gameserver.util.Util;

/**
 * Player Can Take Fort condition implementation.
 * @author Adry_85
 */
public class ConditionPlayerCanTakeFort extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanTakeFort(boolean val)
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
		boolean canTakeFort = true;
		if (player.isAlikeDead() || player.isCursedWeaponEquipped() || !player.isClanLeader())
		{
			canTakeFort = false;
		}
		
		final Fort fort = FortManager.getInstance().getFort(player);
		final SystemMessage sm;
		if ((fort == null) || (fort.getResidenceId() <= 0) || !fort.getSiege().isInProgress() || (fort.getSiege().getAttackerClan(player.getClan()) == null))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			canTakeFort = false;
		}
		else if (fort.getFlagPole() != effected)
		{
			player.sendPacket(SystemMessageId.INCORRECT_TARGET);
			canTakeFort = false;
		}
		else if (!Util.checkIfInRange(200, player, effected, true))
		{
			player.sendPacket(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
			canTakeFort = false;
		}
		return (_val == canTakeFort);
	}
}