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
package org.l2jdevs.gameserver.model.events.impl.character.player;

import org.l2jdevs.gameserver.model.actor.L2Npc;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.base.AcquireSkillType;
import org.l2jdevs.gameserver.model.events.EventType;
import org.l2jdevs.gameserver.model.events.impl.IBaseEvent;
import org.l2jdevs.gameserver.model.skills.Skill;

/**
 * @author UnAfraid
 */
public class OnPlayerSkillLearn implements IBaseEvent
{
	private final L2Npc _trainer;
	private final L2PcInstance _activeChar;
	private final Skill _skill;
	private final AcquireSkillType _type;
	
	public OnPlayerSkillLearn(L2Npc trainer, L2PcInstance activeChar, Skill skill, AcquireSkillType type)
	{
		_trainer = trainer;
		_activeChar = activeChar;
		_skill = skill;
		_type = type;
	}
	
	public L2Npc getTrainer()
	{
		return _trainer;
	}
	
	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public AcquireSkillType getAcquireType()
	{
		return _type;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_SKILL_LEARN;
	}
}
