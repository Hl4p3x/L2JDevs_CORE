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
package org.l2jdevs.gameserver.model.drops.strategy;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.datatables.ItemTable;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.drops.GeneralDropItem;
import org.l2jdevs.gameserver.model.itemcontainer.Inventory;

/**
 * @author Battlecruiser
 */
public interface IAmountMultiplierStrategy
{
	public static final IAmountMultiplierStrategy DROP = DEFAULT_STRATEGY(Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER);
	public static final IAmountMultiplierStrategy SPOIL = DEFAULT_STRATEGY(Config.RATE_CORPSE_DROP_AMOUNT_MULTIPLIER);
	public static final IAmountMultiplierStrategy STATIC = (item, victim) -> 1;
	
	public static IAmountMultiplierStrategy DEFAULT_STRATEGY(final double defaultMultiplier)
	{
		return (item, victim) ->
		{
			double multiplier;
			if (victim.isChampion())
			{
				multiplier = item.getItemId() != Inventory.ADENA_ID ? Config.L2JMOD_CHAMPION_REWARDS_AMOUNT : Config.L2JMOD_CHAMPION_ADENAS_REWARDS_AMOUNT;
                                if(multiplier > 1)
                                    multiplier = 1 + ((float) victim.getPowerMultiplier(multiplier - 1));
			}
                        else
                            multiplier = victim.getPower();
			if(multiplier < 1) multiplier = 1;
			Float dropAmountMultiplier = Config.RATE_DROP_AMOUNT_MULTIPLIER.get(item.getItemId());
			if (dropAmountMultiplier != null)
			{
				multiplier *= dropAmountMultiplier;
			}
			else if (ItemTable.getInstance().getTemplate(item.getItemId()).hasExImmediateEffect())
			{
				multiplier *= Config.RATE_HERB_DROP_AMOUNT_MULTIPLIER;
			}
			else if (victim.isRaid())
			{
				multiplier *= Config.RATE_RAID_DROP_AMOUNT_MULTIPLIER;
			}
			else
			{
				multiplier *= defaultMultiplier;
			}
			return multiplier;
		};
	}
	
	public double getAmountMultiplier(GeneralDropItem item, L2Character victim);
}
