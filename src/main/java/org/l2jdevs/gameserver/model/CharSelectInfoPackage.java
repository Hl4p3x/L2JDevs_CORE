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
package org.l2jdevs.gameserver.model;

import org.l2jdevs.gameserver.model.itemcontainer.Inventory;
import org.l2jdevs.gameserver.model.itemcontainer.PcInventory;

/**
 * Used to Store data sent to Client for Character.<br>
 * Selection screen.
 * @version $Revision: 1.2.2.2.2.4 $ $Date: 2005/03/27 15:29:33 $
 */
public class CharSelectInfoPackage
{
	private String _name;
	private int _objectId = 0;
	private long _exp = 0;
	private int _sp = 0;
	private int _clanId = 0;
	private int _race = 0;
	private int _classId = 0;
	private int _baseClassId = 0;
	private long _deleteTimer = 0L;
	private long _lastAccess = 0L;
	private int _face = 0;
	private int _hairStyle = 0;
	private int _hairColor = 0;
	private int _sex = 0;
	private int _level = 1;
	private int _maxHp = 0;
	private double _currentHp = 0;
	private int _maxMp = 0;
	private double _currentMp = 0;
	private final int[][] _paperdoll;
	private int _karma = 0;
	private int _pkKills = 0;
	private int _pvpKills = 0;
	private int _augmentationId = 0;
	private int _x = 0;
	private int _y = 0;
	private int _z = 0;
	private String _htmlPrefix = null;
	private int _vitalityPoints = 0;
	private int _accessLevel = 0;
	
	/**
	 * Constructor for CharSelectInfoPackage.
	 * @param objectId character object Id.
	 * @param name the character's name.
	 */
	public CharSelectInfoPackage(int objectId, String name)
	{
		setObjectId(objectId);
		_name = name;
		_paperdoll = PcInventory.restoreVisibleInventory(objectId);
	}
	
	/**
	 * @return the character's access level.
	 */
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	public int getAugmentationId()
	{
		return _augmentationId;
	}
	
	public int getBaseClassId()
	{
		return _baseClassId;
	}
	
	public int getClanId()
	{
		return _clanId;
	}
	
	public int getClassId()
	{
		return _classId;
	}
	
	public double getCurrentHp()
	{
		return _currentHp;
	}
	
	public double getCurrentMp()
	{
		return _currentMp;
	}
	
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	public int getEnchantEffect()
	{
		if (_paperdoll[Inventory.PAPERDOLL_RHAND][2] > 0)
		{
			return _paperdoll[Inventory.PAPERDOLL_RHAND][2];
		}
		return _paperdoll[Inventory.PAPERDOLL_RHAND][2];
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public int getFace()
	{
		return _face;
	}
	
	public int getHairColor()
	{
		return _hairColor;
	}
	
	public int getHairStyle()
	{
		return _hairStyle;
	}
	
	public String getHtmlPrefix()
	{
		return _htmlPrefix;
	}
	
	public int getKarma()
	{
		return _karma;
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getMaxHp()
	{
		return _maxHp;
	}
	
	public int getMaxMp()
	{
		return _maxMp;
	}
	
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the character object Id.
	 */
	public int getObjectId()
	{
		return _objectId;
	}
	
	public int getPaperdollItemId(int slot)
	{
		return _paperdoll[slot][1];
	}
	
	public int getPaperdollObjectId(int slot)
	{
		return _paperdoll[slot][0];
	}
	
	public int getPkKills()
	{
		return _pkKills;
	}
	
	public int getPvPKills()
	{
		return _pvpKills;
	}
	
	public int getRace()
	{
		return _race;
	}
	
	public int getSex()
	{
		return _sex;
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public int getVitalityPoints()
	{
		return _vitalityPoints;
	}
	
	public int getX()
	{
		return _x;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	/**
	 * @param level the character's access level to be set.
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = level;
	}
	
	public void setAugmentationId(int augmentationId)
	{
		_augmentationId = augmentationId;
	}
	
	public void setBaseClassId(int baseClassId)
	{
		_baseClassId = baseClassId;
	}
	
	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}
	
	public void setClassId(int classId)
	{
		_classId = classId;
	}
	
	public void setCurrentHp(double currentHp)
	{
		_currentHp = currentHp;
	}
	
	public void setCurrentMp(double currentMp)
	{
		_currentMp = currentMp;
	}
	
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	public void setExp(long exp)
	{
		_exp = exp;
	}
	
	public void setFace(int face)
	{
		_face = face;
	}
	
	public void setHairColor(int hairColor)
	{
		_hairColor = hairColor;
	}
	
	public void setHairStyle(int hairStyle)
	{
		_hairStyle = hairStyle;
	}
	
	public void setHtmlPrefix(String s)
	{
		_htmlPrefix = s;
	}
	
	public void setKarma(int k)
	{
		_karma = k;
	}
	
	public void setLastAccess(long lastAccess)
	{
		_lastAccess = lastAccess;
	}
	
	public void setLevel(int level)
	{
		_level = level;
	}
	
	public void setMaxHp(int maxHp)
	{
		_maxHp = maxHp;
	}
	
	public void setMaxMp(int maxMp)
	{
		_maxMp = maxMp;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	public void setPkKills(int PkKills)
	{
		_pkKills = PkKills;
	}
	
	public void setPvPKills(int PvPKills)
	{
		_pvpKills = PvPKills;
	}
	
	public void setRace(int race)
	{
		_race = race;
	}
	
	public void setSex(int sex)
	{
		_sex = sex;
	}
	
	public void setSp(int sp)
	{
		_sp = sp;
	}
	
	public void setVitalityPoints(int points)
	{
		_vitalityPoints = points;
	}
	
	public void setX(int x)
	{
		_x = x;
	}
	
	public void setY(int y)
	{
		_y = y;
	}
	
	public void setZ(int z)
	{
		_z = z;
	}
}
