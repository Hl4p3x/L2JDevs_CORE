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
package org.l2jdevs.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Kerberos, JIV, Sacrifice
 */
public class RaidBossPointsManager
{
	private static final Logger LOG = LoggerFactory.getLogger(RaidBossPointsManager.class);
	
	private static final String SELECT = "SELECT charId, boss_id, points FROM character_raid_points";
	private static final String REPLACE = "REPLACE INTO character_raid_points (charId, boss_id, points) VALUES (?, ?, ?)";
	private static final String DELETE = "DELETE from character_raid_points WHERE charId > 0";
	
	private final Map<Integer, Map<Integer, Integer>> list = new ConcurrentHashMap<>();
	
	public RaidBossPointsManager()
	{
		init();
	}
	
	public static final RaidBossPointsManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public final void addPoints(L2PcInstance player, int bossId, int points)
	{
		final Map<Integer, Integer> tmpPoint = list.computeIfAbsent(player.getObjectId(), k -> new HashMap<>());
		updatePointsInDB(player, bossId, tmpPoint.merge(bossId, points, Integer::sum));
	}
	
	public final int calculateRanking(int playerObjId)
	{
		final Map<Integer, Integer> rank = getRankList();
		if (rank.containsKey(playerObjId))
		{
			return rank.get(playerObjId);
		}
		return 0;
	}
	
	public final void cleanUp()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE))
		{
			ps.executeUpdate();
			list.clear();
			ps.close();
		}
		catch (Exception e)
		{
			LOG.warn("{}: Couldn't clean raid points. {}", getClass().getSimpleName(), e);
		}
	}
	
	public final Map<Integer, Integer> getList(L2PcInstance player)
	{
		return list.get(player.getObjectId());
	}
	
	public final int getPointsByOwnerId(int ownerId)
	{
		final Map<Integer, Integer> tmpPoint = list.get(ownerId);
		int totalPoints = 0;
		
		if ((tmpPoint == null) || tmpPoint.isEmpty())
		{
			return 0;
		}
		
		for (int points : tmpPoint.values())
		{
			totalPoints += points;
		}
		return totalPoints;
	}
	
	public Map<Integer, Integer> getRankList()
	{
		final Map<Integer, Integer> tmpPoints = new HashMap<>();
		for (int ownerId : list.keySet())
		{
			final int totalPoints = getPointsByOwnerId(ownerId);
			if (totalPoints != 0)
			{
				tmpPoints.put(ownerId, totalPoints);
			}
		}
		
		final List<Entry<Integer, Integer>> listRanking = new ArrayList<>(tmpPoints.entrySet());
		listRanking.sort(Comparator.comparing(Entry<Integer, Integer>::getValue).reversed());
		int ranking = 1;
		final Map<Integer, Integer> tmpRanking = new HashMap<>();
		for (Entry<Integer, Integer> entry : listRanking)
		{
			tmpRanking.put(entry.getKey(), ranking++);
		}
		return tmpRanking;
	}
	
	public final void updatePointsInDB(L2PcInstance player, int raidId, int points)
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(REPLACE))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, raidId);
			ps.setInt(3, points);
			ps.executeUpdate();
			ps.close();
		}
		catch (Exception e)
		{
			LOG.warn("{}: Couldn't update char raid points for player: {} {}", getClass().getSimpleName(), player, e);
		}
	}
	
	private final void init()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT);
			ResultSet rset = ps.executeQuery())
		{
			while (rset.next())
			{
				final int charId = rset.getInt("charId");
				final int bossId = rset.getInt("boss_id");
				final int points = rset.getInt("points");
				Map<Integer, Integer> values = list.get(charId);
				
				if (values == null)
				{
					values = new HashMap<>();
				}
				values.put(bossId, points);
				list.put(charId, values);
			}
			LOG.info("{}: Loaded {} Character Raid Points.", getClass().getSimpleName(), list.size());
			rset.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warn("{}: Couldn't load raid points {}", getClass().getSimpleName(), e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossPointsManager _instance = new RaidBossPointsManager();
	}
}