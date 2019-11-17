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
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.L2Npc;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;

public final class L2ArtefactInstance extends L2Npc
{
	/**
	 * Creates a castle siege artifact.
	 * @param template the artifact NPC template
	 */
	public L2ArtefactInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2ArtefactInstance);
	}
	
	@Override
	public boolean canBeAttacked()
	{
		return false;
	}
	
	/**
	 * Return False.
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		getCastle().registerArtefact(this);
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, Skill skill)
	{
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, Skill skill)
	{
	}
}
