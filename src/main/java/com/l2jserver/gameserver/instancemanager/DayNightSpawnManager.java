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
package com.l2jserver.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.datatables.SkillData;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2RaidBossInstance;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * @author godson, Sacrifice
 */
public final class DayNightSpawnManager
{
	private static final Logger LOG = LoggerFactory.getLogger(DayNightSpawnManager.class);
	
	private static final int EILHALDER_VON_HELLMANN = 25328;
	
	private final Map<L2Spawn, L2RaidBossInstance> _bosses = new ConcurrentHashMap<>();
	
	private final List<L2Spawn> _dayCreatures = new ArrayList<>();
	private final List<L2Spawn> _nightCreatures = new ArrayList<>();
	
	// private static int _currentState; // 0 = Day, 1 = Night
	
	protected DayNightSpawnManager()
	{
		// Prevent external initialization.
	}
	
	public void addDayCreature(L2Spawn spawnDat)
	{
		_dayCreatures.add(spawnDat);
	}
	
	public void addNightCreature(L2Spawn spawnDat)
	{
		_nightCreatures.add(spawnDat);
	}
	
	private void applyShadowSense()
	{
		for (L2PcInstance players : L2World.getInstance().getPlayers())
		{
			if ((players != null) && (players.getRace().ordinal() == 2))
			{
				final Skill skill = SkillData.getInstance().getSkill(294, 1); // Shadow Sense level 1
				if ((skill != null) && (players.getSkillLevel(294) == 1))
				{
					if (GameTimeController.getInstance().isNight())
					{
						players.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(skill));
						players.updateAndBroadcastStatus(2);
					}
					else
					{
						players.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.IT_IS_DOWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(skill));
						players.updateAndBroadcastStatus(2);
					}
				}
			}
		}
	}
	
	private void changeMode(int mode)
	{
		if (_nightCreatures.isEmpty() && _dayCreatures.isEmpty() && _bosses.isEmpty())
		{
			return;
		}
		
		switch (mode)
		{
			case 0:
			{
				spawnDayCreatures();
				specialNightBoss(0);
				break;
			}
			case 1:
			{
				spawnNightCreatures();
				specialNightBoss(1);
				break;
			}
			default:
			{
				LOG.warn("{}: Wrong mode sent", getClass().getSimpleName());
				break;
			}
		}
	}
	
	public void cleanUp()
	{
		_nightCreatures.clear();
		_dayCreatures.clear();
		_bosses.clear();
	}
	
	protected L2RaidBossInstance handleBoss(L2Spawn spawnDat)
	{
		if (_bosses.containsKey(spawnDat))
		{
			return _bosses.get(spawnDat);
		}
		
		if (GameTimeController.getInstance().isNight())
		{
			final L2RaidBossInstance raidboss = (L2RaidBossInstance) spawnDat.doSpawn();
			_bosses.put(spawnDat, raidboss);
			return raidboss;
		}
		return null;
	}
	
	private void handleHellmans(L2RaidBossInstance boss, int mode)
	{
		switch (mode)
		{
			case 0:
			{
				boss.deleteMe();
				LOG.info(getClass().getSimpleName() + ": Deleting Hellman raidboss");
				break;
			}
			case 1:
			{
				if (!boss.isVisible())
				{
					boss.spawnMe();
				}
				LOG.info(getClass().getSimpleName() + ": Spawning Hellman raidboss");
				break;
			}
		}
	}
	
	public void notifyChangeMode()
	{
		try
		{
			if (GameTimeController.getInstance().isNight())
			{
				changeMode(1);
			}
			else
			{
				changeMode(0);
			}
		}
		catch (Exception e)
		{
			LOG.warn("{}: Error while notifyChangeMode() {} {}", getClass().getSimpleName(), e.getMessage(), e);
		}
		// Applies Shadow Sense between 00h00m (midnight) and 06h00m (sunrise)
		applyShadowSense();
	}
	
	/**
	 * Manage Spawn/Respawn
	 * @param unSpawnCreatures List with spawns must be unspawned
	 * @param spawnCreatures List with spawns must be spawned
	 * @param UnspawnLogInfo String for log info for unspawned L2NpcInstance
	 * @param SpawnLogInfo String for log info for spawned L2NpcInstance
	 */
	private void spawnCreatures(List<L2Spawn> unSpawnCreatures, List<L2Spawn> spawnCreatures, String UnspawnLogInfo, String SpawnLogInfo)
	{
		try
		{
			if (!unSpawnCreatures.isEmpty())
			{
				int i = 0;
				for (L2Spawn spawn : unSpawnCreatures)
				{
					if (spawn == null)
					{
						continue;
					}
					
					spawn.stopRespawn();
					final L2Npc last = spawn.getLastSpawn();
					if (last != null)
					{
						last.deleteMe();
						i++;
					}
				}
				LOG.info("DayNightSpawnManager: Removed " + i + " " + UnspawnLogInfo + " creatures");
			}
			
			int i = 0;
			for (L2Spawn spawnDat : spawnCreatures)
			{
				if (spawnDat == null)
				{
					continue;
				}
				spawnDat.startRespawn();
				spawnDat.doSpawn();
				i++;
			}
			LOG.info("{}: Spawned {} {} creatures", getClass().getSimpleName(), i, SpawnLogInfo);
		}
		catch (Exception e)
		{
			LOG.warn("{}: Error while spawning creatures: {} {}", getClass().getSimpleName(), e.getMessage(), e);
		}
	}
	
	/**
	 * Spawn Day Creatures, and Unspawn Night Creatures
	 */
	public void spawnDayCreatures()
	{
		spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}
	
	/**
	 * Spawn Night Creatures, and Unspawn Day Creatures
	 */
	public void spawnNightCreatures()
	{
		spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}
	
	private void specialNightBoss(int mode)
	{
		try
		{
			L2RaidBossInstance boss;
			for (L2Spawn spawn : _bosses.keySet())
			{
				boss = _bosses.get(spawn);
				if ((boss == null) && (mode == 1))
				{
					boss = (L2RaidBossInstance) spawn.doSpawn();
					RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					_bosses.put(spawn, boss);
					continue;
				}
				
				if ((boss == null) && (mode == 0))
				{
					continue;
				}
				
				if ((boss != null) && (boss.getId() == EILHALDER_VON_HELLMANN) && boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
				{
					handleHellmans(boss, mode);
				}
				return;
			}
		}
		catch (Exception e)
		{
			LOG.warn("{}: Error while specialNightBoss() {} {} ", getClass().getSimpleName(), e.getMessage(), e);
		}
	}
	
	public DayNightSpawnManager trim()
	{
		((ArrayList<?>) _nightCreatures).trimToSize();
		((ArrayList<?>) _dayCreatures).trimToSize();
		return this;
	}
	
	private static class SingletonHolder
	{
		protected static final DayNightSpawnManager instance = new DayNightSpawnManager();
	}
	
	public static DayNightSpawnManager getInstance()
	{
		return SingletonHolder.instance;
	}
}