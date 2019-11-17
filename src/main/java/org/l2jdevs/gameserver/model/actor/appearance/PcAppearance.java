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
package org.l2jdevs.gameserver.model.actor.appearance;

import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

public class PcAppearance
{
	public static final int DEFAULT_TITLE_COLOR = 0xECF9A2;
	
	private L2PcInstance _owner;
	
	private byte _face;
	
	private byte _hairColor;
	
	private byte _hairStyle;
	
	private boolean _sex; // Female true(1)
	
	/** true if the player is invisible */
	private boolean _ghostmode = false;
	
	/** The current visible name of this player, not necessarily the real one */
	private String _visibleName;
	
	/** The current visible title of this player, not necessarily the real one */
	private String _visibleTitle;
	
	/** The default name color is 0xFFFFFF. */
	private int _nameColor = 0xFFFFFF;
	
	/** The default title color is 0xECF9A2. */
	private int _titleColor = DEFAULT_TITLE_COLOR;
	
	public PcAppearance(byte face, byte hColor, byte hStyle, boolean sex)
	{
		_face = face;
		_hairColor = hColor;
		_hairStyle = hStyle;
		_sex = sex;
	}
	
	public final byte getFace()
	{
		return _face;
	}
	
	public final byte getHairColor()
	{
		return _hairColor;
	}
	
	public final byte getHairStyle()
	{
		return _hairStyle;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	/**
	 * @return Returns the owner.
	 */
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	/**
	 * @return true if char is female
	 */
	public final boolean getSex()
	{
		return _sex;
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	/**
	 * @return Returns the visibleName.
	 */
	public final String getVisibleName()
	{
		if (_visibleName == null)
		{
			return getOwner().getName();
		}
		return _visibleName;
	}
	
	/**
	 * @return Returns the visibleTitle.
	 */
	public final String getVisibleTitle()
	{
		if (_visibleTitle == null)
		{
			return getOwner().getTitle();
		}
		return _visibleTitle;
	}
	
	public boolean isGhost()
	{
		return _ghostmode;
	}
	
	/**
	 * @param value
	 */
	public final void setFace(int value)
	{
		_face = (byte) value;
	}
	
	public void setGhostMode(boolean b)
	{
		_ghostmode = b;
	}
	
	/**
	 * @param value
	 */
	public final void setHairColor(int value)
	{
		_hairColor = (byte) value;
	}
	
	/**
	 * @param value
	 */
	public final void setHairStyle(int value)
	{
		_hairStyle = (byte) value;
	}
	
	public void setNameColor(int nameColor)
	{
		if (nameColor < 0)
		{
			return;
		}
		
		_nameColor = nameColor;
	}
	
	public void setNameColor(int red, int green, int blue)
	{
		_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	/**
	 * @param owner The owner to set.
	 */
	public void setOwner(L2PcInstance owner)
	{
		_owner = owner;
	}
	
	/**
	 * @param isfemale
	 */
	public final void setSex(boolean isfemale)
	{
		_sex = isfemale;
	}
	
	public void setTitleColor(int titleColor)
	{
		if (titleColor < 0)
		{
			return;
		}
		
		_titleColor = titleColor;
	}
	
	public void setTitleColor(int red, int green, int blue)
	{
		_titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	/**
	 * @param visibleName The visibleName to set.
	 */
	public final void setVisibleName(String visibleName)
	{
		_visibleName = visibleName;
	}
	
	/**
	 * @param visibleTitle The visibleTitle to set.
	 */
	public final void setVisibleTitle(String visibleTitle)
	{
		_visibleTitle = visibleTitle;
	}
}
