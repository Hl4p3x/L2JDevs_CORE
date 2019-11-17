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
package org.l2jdevs.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.data.sql.impl.ClanTable;
import org.l2jdevs.gameserver.model.L2Clan;
import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.StatsSet;
import org.l2jdevs.gameserver.model.entity.Auction;
import org.l2jdevs.gameserver.model.entity.ClanHall;
import org.l2jdevs.gameserver.model.entity.clanhall.AuctionableHall;
import org.l2jdevs.gameserver.model.entity.clanhall.SiegableHall;
import org.l2jdevs.gameserver.model.zone.type.L2ClanHallZone;

/**
 * @author Steuf
 */
public final class ClanHallManager
{
	protected static final Logger _log = Logger.getLogger(ClanHallManager.class.getName());
	
	private static Map<Integer, ClanHall> _allClanHalls = new HashMap<>();
	private final Map<Integer, AuctionableHall> _clanHall = new ConcurrentHashMap<>();
	private final Map<Integer, AuctionableHall> _freeClanHall = new ConcurrentHashMap<>();
	private final Map<Integer, AuctionableHall> _allAuctionableClanHalls = new HashMap<>();
	private boolean _loaded = false;
	
	protected ClanHallManager()
	{
		load();
	}
	
	public static final void addClanHall(ClanHall hall)
	{
		_allClanHalls.put(hall.getId(), hall);
	}
	
	public static final Map<Integer, ClanHall> getAllClanHalls()
	{
		return _allClanHalls;
	}
	
	public static ClanHallManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public final ClanHall getAbstractHallByOwner(L2Clan clan)
	{
		// Separate loops to avoid iterating over free clan halls
		for (Map.Entry<Integer, AuctionableHall> ch : _clanHall.entrySet())
		{
			if (clan.getId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		for (Map.Entry<Integer, SiegableHall> ch : CHSiegeManager.getInstance().getConquerableHalls().entrySet())
		{
			if (clan.getId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	/**
	 * @return all ClanHalls
	 */
	public final Map<Integer, AuctionableHall> getAllAuctionableClanHalls()
	{
		return _allAuctionableClanHalls;
	}
	
	public final AuctionableHall getAuctionableHallById(int clanHallId)
	{
		return _allAuctionableClanHalls.get(clanHallId);
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return Clan Hall by x,y,z
	 */
	public final ClanHall getClanHall(int x, int y, int z)
	{
		for (ClanHall temp : _allClanHalls.values())
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		return null;
	}
	
	public final ClanHall getClanHall(L2Object activeObject)
	{
		return getClanHall(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	/**
	 * @param clanHallId
	 * @return Clan Hall by Id
	 */
	public final ClanHall getClanHallById(int clanHallId)
	{
		return _allClanHalls.get(clanHallId);
	}
	
	/**
	 * @param clan
	 * @return Clan Hall by Owner
	 */
	public final AuctionableHall getClanHallByOwner(L2Clan clan)
	{
		for (Map.Entry<Integer, AuctionableHall> ch : _clanHall.entrySet())
		{
			if (clan.getId() == ch.getValue().getOwnerId())
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	/**
	 * @return all ClanHalls that have owner
	 */
	public final Map<Integer, AuctionableHall> getClanHalls()
	{
		return _clanHall;
	}
	
	/**
	 * @return all FreeClanHalls
	 */
	public final Map<Integer, AuctionableHall> getFreeClanHalls()
	{
		return _freeClanHall;
	}
	
	public final ClanHall getNearbyAbstractHall(int x, int y, int maxDist)
	{
		L2ClanHallZone zone = null;
		for (Map.Entry<Integer, ClanHall> ch : _allClanHalls.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	public final AuctionableHall getNearbyClanHall(int x, int y, int maxDist)
	{
		L2ClanHallZone zone = null;
		
		for (Map.Entry<Integer, AuctionableHall> ch : _clanHall.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		for (Map.Entry<Integer, AuctionableHall> ch : _freeClanHall.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		return null;
	}
	
	/**
	 * @param chId
	 * @return true is free ClanHall
	 */
	public final boolean isFree(int chId)
	{
		if (_freeClanHall.containsKey(chId))
		{
			return true;
		}
		return false;
	}
	
	public boolean loaded()
	{
		return _loaded;
	}
	
	/**
	 * Free a ClanHall
	 * @param chId
	 */
	public final synchronized void setFree(int chId)
	{
		_freeClanHall.put(chId, _clanHall.get(chId));
		ClanTable.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()).setHideoutId(0);
		_freeClanHall.get(chId).free();
		_clanHall.remove(chId);
	}
	
	/**
	 * Set ClanHallOwner
	 * @param chId
	 * @param clan
	 */
	public final synchronized void setOwner(int chId, L2Clan clan)
	{
		if (!_clanHall.containsKey(chId))
		{
			_clanHall.put(chId, _freeClanHall.get(chId));
			_freeClanHall.remove(chId);
		}
		else
		{
			_clanHall.get(chId).free();
		}
		ClanTable.getInstance().getClan(clan.getId()).setHideoutId(chId);
		_clanHall.get(chId).setOwner(clan);
	}
	
	/** Load All Clan Hall */
	private final void load()
	{
		try (Connection con = ConnectionFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM clanhall ORDER BY id"))
		{
			int id, ownerId, lease;
			while (rs.next())
			{
				StatsSet set = new StatsSet();
				
				id = rs.getInt("id");
				ownerId = rs.getInt("ownerId");
				lease = rs.getInt("lease");
				
				set.set("id", id);
				set.set("name", rs.getString("name"));
				set.set("ownerId", ownerId);
				set.set("lease", lease);
				set.set("desc", rs.getString("desc"));
				set.set("location", rs.getString("location"));
				set.set("paidUntil", rs.getLong("paidUntil"));
				set.set("grade", rs.getInt("Grade"));
				set.set("paid", rs.getBoolean("paid"));
				AuctionableHall ch = new AuctionableHall(set);
				_allAuctionableClanHalls.put(id, ch);
				addClanHall(ch);
				
				if (ch.getOwnerId() > 0)
				{
					_clanHall.put(id, ch);
					continue;
				}
				_freeClanHall.put(id, ch);
				
				Auction auc = AuctionManager.getInstance().getAuction(id);
				if ((auc == null) && (lease > 0))
				{
					AuctionManager.getInstance().initNPC(id);
				}
			}
			_log.info(getClass().getSimpleName() + ": Loaded: " + getClanHalls().size() + " clan halls");
			_log.info(getClass().getSimpleName() + ": Loaded: " + getFreeClanHalls().size() + " free clan halls");
			_loaded = true;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: ClanHallManager.load(): " + e.getMessage(), e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallManager _instance = new ClanHallManager();
	}
}