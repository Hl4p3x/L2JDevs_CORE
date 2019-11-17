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
package org.l2jdevs.gameserver.model.entity;

import java.util.Calendar;
import java.util.List;

import org.l2jdevs.gameserver.model.L2Clan;
import org.l2jdevs.gameserver.model.L2SiegeClan;
import org.l2jdevs.gameserver.model.actor.L2Npc;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author JIV
 */
public interface Siegable
{
	public boolean checkIsAttacker(L2Clan clan);
	
	public boolean checkIsDefender(L2Clan clan);
	
	public void endSiege();
	
	public L2SiegeClan getAttackerClan(int clanId);
	
	public L2SiegeClan getAttackerClan(L2Clan clan);
	
	public List<L2SiegeClan> getAttackerClans();
	
	public List<L2PcInstance> getAttackersInZone();
	
	public L2SiegeClan getDefenderClan(int clanId);
	
	public L2SiegeClan getDefenderClan(L2Clan clan);
	
	public List<L2SiegeClan> getDefenderClans();
	
	public int getFameAmount();
	
	public int getFameFrequency();
	
	public List<L2Npc> getFlag(L2Clan clan);
	
	public Calendar getSiegeDate();
	
	public boolean giveFame();
	
	public void startSiege();
	
	public void updateSiege();
}
