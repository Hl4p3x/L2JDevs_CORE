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

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.ThreadPoolManager;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.instancemanager.RaidBossPointsManager;
import org.l2jdevs.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2jdevs.gameserver.model.L2Spawn;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2jdevs.gameserver.model.entity.Hero;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;
import org.l2jdevs.util.Rnd;

/**
 * This class manages all RaidBoss.<br>
 * In a group mob, there are one master called RaidBoss and several slaves called Minions.
 */
public class L2RaidBossInstance extends L2MonsterInstance
{
	private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 30000; // 30 sec
	
	private RaidBossSpawnManager.StatusEnum _raidStatus;
	private boolean _useRaidCurse = true;
	
	/**
	 * Creates a raid boss.
	 * @param template the raid boss template
	 */
	public L2RaidBossInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2RaidBossInstance);
		setIsRaid(true);
		setLethalable(false);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		final L2PcInstance player = killer.getActingPlayer();
		if (player != null)
		{
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
			if (player.getParty() != null)
			{
				for (L2PcInstance member : player.getParty().getMembers())
				{
					RaidBossPointsManager.getInstance().addPoints(member, getId(), (getLevel() / 2) + Rnd.get(-5, 5));
					if (member.isNoble())
					{
						Hero.getInstance().setRBkilled(member.getObjectId(), getId());
					}
				}
			}
			else
			{
				RaidBossPointsManager.getInstance().addPoints(player, getId(), (getLevel() / 2) + Rnd.get(-5, 5));
				if (player.isNoble())
				{
					Hero.getInstance().setRBkilled(player.getObjectId(), getId());
				}
			}
		}
		
		RaidBossSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}
	
	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}
	
	@Override
	public float getVitalityPoints(int damage)
	{
		return -super.getVitalityPoints(damage) / 100;
	}
	
	@Override
	public boolean giveRaidCurse()
	{
		return _useRaidCurse;
	}
	
	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
	}
	
	public void setRaidStatus(RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}
	
	public void setUseRaidCurse(boolean val)
	{
		_useRaidCurse = val;
	}
	
	@Override
	public boolean useVitalityRate()
	{
		return false;
	}
	
	protected void checkAndReturnToSpawn()
	{
		if (isDead() || isMovementDisabled() || !canReturnToSpawnPoint() || isWalker())
		{
			return;
		}
		
		final L2Spawn spawn = getSpawn();
		if (spawn == null)
		{
			return;
		}
		
		final int spawnX = spawn.getX();
		final int spawnY = spawn.getY();
		final int spawnZ = spawn.getZ();
		
		if (!isInCombat() && !isMovementDisabled())
		{
			if (!isInsideRadius(spawnX, spawnY, spawnZ, Math.max(Config.MAX_DRIFT_RANGE, 200), true, false))
			{
				teleToLocation(spawnX, spawnY, spawnZ, false);
			}
		}
	}
	
	@Override
	protected int getMaintenanceInterval()
	{
		return RAIDBOSS_MAINTENANCE_INTERVAL;
	}
	
	/**
	 * Spawn all minions at a regular interval Also if boss is too far from home location at the time of this check, teleport it home.
	 */
	@Override
	protected void startMaintenanceTask()
	{
		_maintenanceTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(() -> checkAndReturnToSpawn(), 60000, getMaintenanceInterval() + Rnd.get(5000));
	}
}