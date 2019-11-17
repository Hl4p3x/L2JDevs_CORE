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
package org.l2jdevs.gameserver.dao;

import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.base.SubClass;

/**
 * Subclass DAO interface.
 * @author Zoey76
 */
public interface SubclassDAO
{
	void delete(L2PcInstance player, int classIndex);
	
	/**
	 * Store the basic info about this new subclass.
	 * @param player the player
	 * @param newClass the new subclass
	 * @return {@code true} if the subclass is added to the database
	 */
	boolean insert(L2PcInstance player, SubClass newClass);
	
	/**
	 * Restores the player subclass data.
	 * @param player the player
	 */
	void load(L2PcInstance player);
	
	void update(L2PcInstance player);
}
