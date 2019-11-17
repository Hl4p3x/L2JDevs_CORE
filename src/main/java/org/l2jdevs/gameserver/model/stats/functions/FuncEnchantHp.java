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
package org.l2jdevs.gameserver.model.stats.functions;

import org.l2jdevs.gameserver.data.xml.impl.EnchantItemHPBonusData;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.conditions.Condition;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.stats.Stats;

/**
 * @author Yamaneko
 */
public class FuncEnchantHp extends AbstractFunction
{
	public FuncEnchantHp(Stats stat, int order, Object owner, double value, Condition applayCond)
	{
		super(stat, order, owner, value, applayCond);
	}
	
	@Override
	public double calc(L2Character effector, L2Character effected, Skill skill, double initVal)
	{
		if ((getApplayCond() != null) && !getApplayCond().test(effector, effected, skill))
		{
			return initVal;
		}
		
		final L2ItemInstance item = (L2ItemInstance) getFuncOwner();
		if (item.getEnchantLevel() > 0)
		{
			return initVal + EnchantItemHPBonusData.getInstance().getHPBonus(item);
		}
		return initVal;
	}
}
