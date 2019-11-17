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

import org.l2jdevs.gameserver.SevenSignsFestival;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.model.L2Party;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.network.serverpackets.InventoryUpdate;

/**
 * This class manages all attackable festival NPCs, spawned during the Festival of Darkness.
 * @author Tempy
 */
public class L2FestivalMonsterInstance extends L2MonsterInstance
{
	protected int _bonusMultiplier = 1;
	
	/**
	 * Creates a festival monster.
	 * @param template the festival monster NPC template
	 */
	public L2FestivalMonsterInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2FestivalMonsterInstance);
	}
	
	/**
	 * Actions:
	 * <ul>
	 * <li>Check if the killing object is a player, and then find the party they belong to.</li>
	 * <li>Add a blood offering item to the leader of the party.</li>
	 * <li>Update the party leader's inventory to show the new item addition.</li>
	 * </ul>
	 */
	@Override
	public void doItemDrop(L2Character lastAttacker)
	{
		L2PcInstance killingChar = null;
		
		if (!(lastAttacker instanceof L2PcInstance))
		{
			return;
		}
		
		killingChar = (L2PcInstance) lastAttacker;
		L2Party associatedParty = killingChar.getParty();
		
		if (associatedParty == null)
		{
			return;
		}
		
		L2PcInstance partyLeader = associatedParty.getLeader();
		L2ItemInstance addedOfferings = partyLeader.getInventory().addItem("Sign", SevenSignsFestival.FESTIVAL_OFFERING_ID, _bonusMultiplier, partyLeader, this);
		
		InventoryUpdate iu = new InventoryUpdate();
		
		if (addedOfferings.getCount() != _bonusMultiplier)
		{
			iu.addModifiedItem(addedOfferings);
		}
		else
		{
			iu.addNewItem(addedOfferings);
		}
		
		partyLeader.sendPacket(iu);
		
		super.doItemDrop(lastAttacker); // Normal drop
	}
	
	/**
	 * All mobs in the festival really don't need random animation.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	/**
	 * All mobs in the festival are aggressive, and have high aggro range.
	 */
	@Override
	public boolean isAggressive()
	{
		return true;
	}
	
	/**
	 * Return True if the attacker is not another L2FestivalMonsterInstance.
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker instanceof L2FestivalMonsterInstance)
		{
			return false;
		}
		
		return true;
	}
	
	public void setOfferingBonus(int bonusMultiplier)
	{
		_bonusMultiplier = bonusMultiplier;
	}
}