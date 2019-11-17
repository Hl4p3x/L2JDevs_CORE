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
package org.l2jdevs.gameserver.model.stats.functions.formulas;

import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.stats.BaseStats;
import org.l2jdevs.gameserver.model.stats.Stats;
import org.l2jdevs.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncMAtkMod extends AbstractFunction
{
	private static final FuncMAtkMod _fma_instance = new FuncMAtkMod();
	
	private FuncMAtkMod()
	{
		super(Stats.MAGIC_ATTACK, 1, null, 0, null);
	}
	
	public static AbstractFunction getInstance()
	{
		return _fma_instance;
	}
	
	@Override
	public double calc(L2Character effector, L2Character effected, Skill skill, double initVal)
	{
		// Level Modifier^2 * INT Modifier^2
		double lvlMod = effector.isPlayer() ? BaseStats.INT.calcBonus(effector.getActingPlayer()) : BaseStats.INT.calcBonus(effector);
		double intMod = effector.isPlayer() ? effector.getActingPlayer().getLevelMod() : effector.getLevelMod();
		return initVal * Math.pow(lvlMod, 2) * Math.pow(intMod, 2);
	}
}