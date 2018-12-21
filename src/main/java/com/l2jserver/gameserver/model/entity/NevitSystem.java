/*
 * Copyright (C) 2004-2018 L2J Server
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
package com.l2jserver.gameserver.model.entity;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.events.EventType;
import com.l2jserver.gameserver.model.events.annotations.RegisterEvent;
import com.l2jserver.gameserver.model.events.impl.character.player.OnPlayerLogin;
import com.l2jserver.gameserver.model.events.impl.character.player.OnPlayerLogout;
import com.l2jserver.gameserver.model.events.listeners.ConsumerEventListener;
import com.l2jserver.gameserver.model.interfaces.IUniqueId;
import com.l2jserver.gameserver.model.skills.AbnormalVisualEffect;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExNevitAdventEffect;
import com.l2jserver.gameserver.network.serverpackets.ExNevitAdventPointInfoPacket;
import com.l2jserver.gameserver.network.serverpackets.ExNevitAdventTimeChange;

/**
 * @author Janiko, IrLex
 */
public class NevitSystem implements IUniqueId
{
	// Settings
	private static final boolean HUNTING_BONUS_ENGINE = Config.HUNTING_BONUS_ENGINE;
	private static final int MAX_POINTS = Config.HUNTING_BONUS_MAX_POINTS;
	private static final int BONUS_EFFECT_TIME = Config.HUNTING_BONUS_EFFECT_TIME;
	protected static final int REFRESH_RATE = Config.HUNTING_BONUS_REFRESH_RATE;
	protected static final int REFRESH_POINTS = Config.HUNTING_BONUS_POINTS_ON_REFRESH;
	private static final boolean EXTRA_POINTS = Config.HUNTING_BONUS_EXTRA_POINTS;
	private static final boolean EXTRA_POINTS_ALL_TIME = Config.HUNTING_BONUS_EXTRA_POINTS_ALL_TIME;
	
	// Nevit Hour
	public static final int ADVENT_TIME = Config.HUNTING_BONUS_MAX_TIME;
	private final L2PcInstance _player;
	
	private volatile ScheduledFuture<?> _adventTask;
	private volatile ScheduledFuture<?> _nevitEffectTask;
	
	public NevitSystem(L2PcInstance player)
	{
		_player = player;
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> OnPlayerLogout(event), this));
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	private void onPlayerLogin(OnPlayerLogin event)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			final Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 6);
			cal.set(Calendar.MINUTE, 30);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			// Reset Nevit's Blessing
			if ((_player.getLastAccess() < (cal.getTimeInMillis() / 1000L)) && (System.currentTimeMillis() > cal.getTimeInMillis()))
			{
				_player.getNevitSystem().setAdventTime(0); // Refuel-reset hunting bonus time
			}
			
			// Send Packets
			_player.sendPacket(new ExNevitAdventPointInfoPacket(getAdventPoints()));
			_player.sendPacket(new ExNevitAdventTimeChange(getAdventTime(), true));
			
			startNevitEffect(_player.getVariables().getInt("nevit_b", 0));
			
			// Set percent
			int percent = calcPercent(getAdventPoints());
			
			if ((percent >= 45) && (percent < 50))
			{
				_player.sendPacket(SystemMessageId.YOU_ARE_STARTING_TO_FEEL_THE_EFFECTS_OF_NEVITS_ADVENT_BLESSING);
			}
			else if ((percent >= 50) && (percent < 75))
			{
				_player.sendPacket(SystemMessageId.YOU_ARE_FURTHER_INFUSED_WITH_THE_BLESSINGS_OF_NEVIT);
			}
			else if (percent >= 75)
			{
				_player.sendPacket(SystemMessageId.NEVITS_ADVENT_BLESSING_SHINES_STRONGLY_FROM_ABOVE);
			}
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	private void OnPlayerLogout(OnPlayerLogout event)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			stopNevitEffectTask(true);
			stopAdventTask(false);
		}
	}
	
	public void addPoints(int val)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			// setAdventPoints(getEffectTime() > 0 ? 0 : getAdventPoints() + val);
			setAdventPoints(getAdventPoints() + val);
			
			if (getAdventPoints() > MAX_POINTS)
			{
				setAdventPoints(0);
				startNevitEffect(BONUS_EFFECT_TIME);
			}
			
			switch (calcPercent(getAdventPoints()))
			{
				case 45:
				{
					getPlayer().sendPacket(SystemMessageId.YOU_ARE_STARTING_TO_FEEL_THE_EFFECTS_OF_NEVITS_ADVENT_BLESSING);
					break;
				}
				case 50:
				{
					getPlayer().sendPacket(SystemMessageId.YOU_ARE_FURTHER_INFUSED_WITH_THE_BLESSINGS_OF_NEVIT);
					break;
				}
				case 75:
				{
					getPlayer().sendPacket(SystemMessageId.NEVITS_ADVENT_BLESSING_SHINES_STRONGLY_FROM_ABOVE);
					break;
				}
			}
			
			getPlayer().sendPacket(new ExNevitAdventPointInfoPacket(getAdventPoints()));
		}
	}
	
	public void startAdventTask()
	{
		if (HUNTING_BONUS_ENGINE)
		{
			if ((_adventTask == null) && (getAdventTime() < ADVENT_TIME))
			{
				_adventTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AdventTask(), REFRESH_RATE * 1000, REFRESH_RATE * 1000);
				getPlayer().sendPacket(new ExNevitAdventTimeChange(getAdventTime(), false));
			}
		}
	}
	
	protected class AdventTask implements Runnable
	{
		@Override
		public void run()
		{
			setAdventTime(getAdventTime() + REFRESH_RATE);
			if (getAdventTime() >= ADVENT_TIME)
			{
				setAdventTime(ADVENT_TIME);
				stopAdventTask(true);
				return;
			}
			
			addPoints(REFRESH_POINTS);
			if ((getAdventTime() % 60) == 0)
			{
				getPlayer().sendPacket(new ExNevitAdventTimeChange(getAdventTime(), false));
			}
		}
	}
	
	public void stopAdventTask(boolean sendPacket)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			if (_adventTask != null)
			{
				_adventTask.cancel(true);
				_adventTask = null;
			}
			
			if (sendPacket)
			{
				getPlayer().sendPacket(new ExNevitAdventTimeChange(getAdventTime(), true));
			}
		}
	}
	
	private void startNevitEffect(int time)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			if (getEffectTime() > 0)
			{
				stopNevitEffectTask(false);
				time += getEffectTime();
			}
			
			if ((getAdventTime() < ADVENT_TIME) && (time > 0))
			{
				getPlayer().getVariables().set("nevit_b", time);
				getPlayer().sendPacket(new ExNevitAdventEffect(time));
				getPlayer().sendPacket(SystemMessageId.THE_ANGEL_NEVIT_HAS_BLESSED_YOU_FROM_ABOVE);
				getPlayer().startAbnormalVisualEffect(true, AbnormalVisualEffect.NEVIT_ADVENT);
				_nevitEffectTask = ThreadPoolManager.getInstance().scheduleGeneral(new NevitEffectEnd(), time * 1000L);
			}
		}
	}
	
	protected class NevitEffectEnd implements Runnable
	{
		@Override
		public void run()
		{
			getPlayer().getVariables().remove("nevit_b");
			getPlayer().sendPacket(new ExNevitAdventEffect(0));
			getPlayer().sendPacket(new ExNevitAdventPointInfoPacket(getAdventPoints()));
			getPlayer().sendPacket(SystemMessageId.NEVITS_ADVENT_BLESSING_HAS_ENDED);
			getPlayer().stopAbnormalVisualEffect(true, AbnormalVisualEffect.NEVIT_ADVENT);
			stopNevitEffectTask(false);
		}
	}
	
	protected void stopNevitEffectTask(boolean saveTime)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			if (_nevitEffectTask != null)
			{
				if (saveTime)
				{
					int time = getEffectTime();
					if (time > 0)
					{
						getPlayer().getVariables().set("nevit_b", time);
					}
					else
					{
						getPlayer().getVariables().remove("nevit_b");
					}
				}
				_nevitEffectTask.cancel(true);
				_nevitEffectTask = null;
			}
		}
	}
	
	public void checkIfMustGivePoints(long baseExp, L2Attackable l2Attackable)
	{
		if (HUNTING_BONUS_ENGINE)
		{
			if (EXTRA_POINTS)
			{
				if (((_adventTask != null) && EXTRA_POINTS_ALL_TIME) || (_adventTask == null))
				{
					int nevitPoints = Math.round(((baseExp / (l2Attackable.getLevel() * l2Attackable.getLevel())) * 100) / 20);
					addPoints(nevitPoints);
				}
			}
		}
	}
	
	public L2PcInstance getPlayer()
	{
		return _player;
	}
	
	@Override
	public int getObjectId()
	{
		return _player.getObjectId();
	}
	
	private int getEffectTime()
	{
		return _nevitEffectTask == null ? 0 : (int) Math.max(0, _nevitEffectTask.getDelay(TimeUnit.SECONDS));
	}
	
	public boolean isAdventBlessingActive()
	{
		return ((_nevitEffectTask != null) && (_nevitEffectTask.getDelay(TimeUnit.MILLISECONDS) > 0));
	}
	
	public static int calcPercent(int points)
	{
		return (int) ((100.0D / MAX_POINTS) * points);
	}
	
	public void setAdventPoints(int points)
	{
		getPlayer().getVariables().set("hunting_points", points);
	}
	
	public void setAdventTime(int time)
	{
		getPlayer().getVariables().set("hunting_time", time);
	}
	
	public int getAdventPoints()
	{
		return getPlayer().getVariables().getInt("hunting_points", 0);
	}
	
	public int getAdventTime()
	{
		return getPlayer().getVariables().getInt("hunting_time", 0);
	}
}