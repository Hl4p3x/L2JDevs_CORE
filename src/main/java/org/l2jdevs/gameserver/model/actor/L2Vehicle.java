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
package org.l2jdevs.gameserver.model.actor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.GameTimeController;
import org.l2jdevs.gameserver.ThreadPoolManager;
import org.l2jdevs.gameserver.ai.CtrlIntention;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.instancemanager.MapRegionManager;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.L2WorldRegion;
import org.l2jdevs.gameserver.model.Location;
import org.l2jdevs.gameserver.model.TeleportWhereType;
import org.l2jdevs.gameserver.model.VehiclePathPoint;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.actor.knownlist.VehicleKnownList;
import org.l2jdevs.gameserver.model.actor.stat.VehicleStat;
import org.l2jdevs.gameserver.model.actor.templates.L2CharTemplate;
import org.l2jdevs.gameserver.model.interfaces.ILocational;
import org.l2jdevs.gameserver.model.items.L2Weapon;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jdevs.gameserver.network.serverpackets.L2GameServerPacket;
import org.l2jdevs.gameserver.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author DS
 */
public abstract class L2Vehicle extends L2Character
{
	private static final Logger LOG = LoggerFactory.getLogger(L2Vehicle.class);
	
	private int _dockId = 0;
	private final List<L2PcInstance> _passengers = new CopyOnWriteArrayList<>();
	private Location _oustLoc = null;
	private Runnable _engine = null;
	
	private VehiclePathPoint[] _currentPath = null;
	private int _runState = 0;
	
	/**
	 * Creates an abstract vehicle.
	 * @param template the vehicle template
	 */
	public L2Vehicle(L2CharTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2Vehicle);
		setIsFlying(true);
	}
	
	public boolean addPassenger(L2PcInstance player)
	{
		if ((player == null) || _passengers.contains(player))
		{
			return false;
		}
		
		// already in other vehicle
		if ((player.getVehicle() != null) && (player.getVehicle() != this))
		{
			return false;
		}
		
		_passengers.add(player);
		return true;
	}
	
	public void broadcastToPassengers(L2GameServerPacket sm)
	{
		for (L2PcInstance player : _passengers)
		{
			if (player != null)
			{
				player.sendPacket(sm);
			}
		}
	}
	
	public boolean canBeControlled()
	{
		return _engine == null;
	}
	
	@Override
	public boolean deleteMe()
	{
		_engine = null;
		
		try
		{
			if (isMoving())
			{
				stopMove(null);
			}
		}
		catch (Exception e)
		{
			LOG.warn("Failed stopMove(). {}", e);
		}
		
		try
		{
			oustPlayers();
		}
		catch (Exception e)
		{
			LOG.warn("Failed oustPlayers(). {}", e);
		}
		
		final L2WorldRegion oldRegion = getWorldRegion();
		
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOG.warn("Failed decayMe(). {}", e);
		}
		
		if (oldRegion != null)
		{
			oldRegion.removeFromZones(this);
		}
		
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			LOG.warn("Failed cleaning knownlist. {}", e);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		
		return super.deleteMe();
	}
	
	@Override
	public void detachAI()
	{
	}
	
	public void executePath(VehiclePathPoint[] path)
	{
		_runState = 0;
		_currentPath = path;
		
		if ((_currentPath != null) && (_currentPath.length > 0))
		{
			final VehiclePathPoint point = _currentPath[0];
			if (point.getMoveSpeed() > 0)
			{
				getStat().setMoveSpeed(point.getMoveSpeed());
			}
			if (point.getRotationSpeed() > 0)
			{
				getStat().setRotationSpeed(point.getRotationSpeed());
			}
			
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(point.getX(), point.getY(), point.getZ(), 0));
			return;
		}
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	public int getDockId()
	{
		return _dockId;
	}
	
	@Override
	public int getLevel()
	{
		return 0;
	}
	
	public Location getOustLoc()
	{
		return _oustLoc != null ? _oustLoc : MapRegionManager.getInstance().getTeleToLocation(this, TeleportWhereType.TOWN);
	}
	
	public List<L2PcInstance> getPassengers()
	{
		return _passengers;
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public VehicleStat getStat()
	{
		return (VehicleStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new VehicleStat(this));
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new VehicleKnownList(this));
	}
	
	public boolean isAirShip()
	{
		return false;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	public boolean isBoat()
	{
		return false;
	}
	
	public boolean isEmpty()
	{
		return _passengers.isEmpty();
	}
	
	public boolean isInDock()
	{
		return _dockId > 0;
	}
	
	@Override
	public boolean isVehicle()
	{
		return true;
	}
	
	@Override
	public boolean moveToNextRoutePoint()
	{
		_move = null;
		
		if (_currentPath != null)
		{
			_runState++;
			if (_runState < _currentPath.length)
			{
				final VehiclePathPoint point = _currentPath[_runState];
				if (!isMovementDisabled())
				{
					if (point.getMoveSpeed() == 0)
					{
						point.setHeading(point.getRotationSpeed());
						teleToLocation(point, false);
						_currentPath = null;
					}
					else
					{
						if (point.getMoveSpeed() > 0)
						{
							getStat().setMoveSpeed(point.getMoveSpeed());
						}
						if (point.getRotationSpeed() > 0)
						{
							getStat().setRotationSpeed(point.getRotationSpeed());
						}
						
						MoveData m = new MoveData();
						m.disregardingGeodata = false;
						m.onGeodataPathIndex = -1;
						m._xDestination = point.getX();
						m._yDestination = point.getY();
						m._zDestination = point.getZ();
						m._heading = 0;
						
						final double distance = Math.hypot(point.getX() - getX(), point.getY() - getY());
						if (distance > 1)
						{
							setHeading(Util.calculateHeadingFrom(getX(), getY(), point.getX(), point.getY()));
						}
						
						m._moveStartTime = GameTimeController.getInstance().getGameTicks();
						_move = m;
						
						GameTimeController.getInstance().registerMovingObject(this);
						return true;
					}
				}
			}
			else
			{
				_currentPath = null;
			}
		}
		
		runEngine(10);
		return false;
	}
	
	public void oustPlayer(L2PcInstance player)
	{
		player.setVehicle(null);
		player.setInVehiclePosition(null);
		removePassenger(player);
	}
	
	public void oustPlayers()
	{
		_passengers.forEach(p -> oustPlayer(p));
		_passengers.clear();
	}
	
	/**
	 * Consume ticket(s) and teleport player from boat if no correct ticket
	 * @param itemId Ticket itemId
	 * @param count Ticket count
	 * @param oustX
	 * @param oustY
	 * @param oustZ
	 */
	public void payForRide(int itemId, int count, int oustX, int oustY, int oustZ)
	{
		final Collection<L2PcInstance> passengers = getKnownList().getKnownPlayersInRadius(1000);
		if ((passengers != null) && !passengers.isEmpty())
		{
			L2ItemInstance ticket;
			InventoryUpdate iu;
			for (L2PcInstance player : passengers)
			{
				if (player == null)
				{
					continue;
				}
				if (player.isInBoat() && (player.getBoat() == this))
				{
					if (itemId > 0)
					{
						ticket = player.getInventory().getItemByItemId(itemId);
						if ((ticket == null) || (player.getInventory().destroyItem("Boat", ticket, count, player, this) == null))
						{
							player.sendPacket(SystemMessageId.NOT_CORRECT_BOAT_TICKET);
							player.teleToLocation(new Location(oustX, oustY, oustZ), true);
							continue;
						}
						iu = new InventoryUpdate();
						iu.addModifiedItem(ticket);
						player.sendPacket(iu);
					}
					addPassenger(player);
				}
			}
		}
	}
	
	public void registerEngine(Runnable r)
	{
		_engine = r;
	}
	
	public void removePassenger(L2PcInstance player)
	{
		try
		{
			_passengers.remove(player);
		}
		catch (Exception e)
		{
		}
	}
	
	public void runEngine(int delay)
	{
		if (_engine != null)
		{
			ThreadPoolManager.getInstance().scheduleAi(_engine, delay);
		}
	}
	
	public void setInDock(int d)
	{
		_dockId = d;
	}
	
	public void setOustLoc(Location loc)
	{
		_oustLoc = loc;
	}
	
	@Override
	public void stopMove(Location loc, boolean updateKnownObjects)
	{
		_move = null;
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		
		if (Config.MOVE_BASED_KNOWNLIST && updateKnownObjects)
		{
			getKnownList().findObjects();
		}
	}
	
	@Override
	public void teleToLocation(ILocational loc, boolean allowRandomOffset)
	{
		if (isMoving())
		{
			stopMove(null, false);
		}
		
		setIsTeleporting(true);
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		
		for (L2PcInstance player : _passengers)
		{
			if (player != null)
			{
				player.teleToLocation(loc, false);
			}
		}
		
		decayMe();
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
		
		// temporary fix for heading on teleports
		if (loc.getHeading() != 0)
		{
			setHeading(loc.getHeading());
		}
		
		onTeleported();
		revalidateZone(true);
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	@Override
	public boolean updatePosition()
	{
		final boolean result = super.updatePosition();
		
		for (L2PcInstance player : _passengers)
		{
			if ((player != null) && (player.getVehicle() == this))
			{
				player.setXYZ(getX(), getY(), getZ());
				player.revalidateZone(false);
			}
		}
		
		return result;
	}
}