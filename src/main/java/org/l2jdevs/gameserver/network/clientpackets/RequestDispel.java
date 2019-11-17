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
package org.l2jdevs.gameserver.network.clientpackets;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.datatables.SkillData;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.skills.AbnormalType;
import org.l2jdevs.gameserver.model.skills.Skill;

/**
 * @author KenM
 */
public class RequestDispel extends L2GameClientPacket
{
	private static final String _C_D0_4B_REQUESTDISPEL = "[C] D0:4B RequestDispel";
	
	private int _objectId;
	private int _skillId;
	private int _skillLevel;
	
	@Override
	public String getType()
	{
		return _C_D0_4B_REQUESTDISPEL;
	}
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_skillId = readD();
		_skillLevel = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if ((_skillId <= 0) || (_skillLevel <= 0))
		{
			return;
		}
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		if (skill == null)
		{
			return;
		}
		if (skill.isIrreplaceableBuff() || skill.isStayAfterDeath() || skill.isDebuff())
		{
			return;
		}
		if (skill.getAbnormalType() == AbnormalType.TRANSFORM)
		{
			return;
		}
		if (skill.isDance() && !Config.DANCE_CANCEL_BUFF)
		{
			return;
		}
		if (activeChar.getObjectId() == _objectId)
		{
			activeChar.stopSkillEffects(true, _skillId);
		}
		else
		{
			if (activeChar.hasSummon() && (activeChar.getSummon().getObjectId() == _objectId))
			{
				activeChar.getSummon().stopSkillEffects(true, _skillId);
			}
		}
	}
}
