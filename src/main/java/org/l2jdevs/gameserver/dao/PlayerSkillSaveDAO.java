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
package org.l2jdevs.gameserver.dao;

import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;

/**
 * Player Skill Save DAO interface.
 * @author Zoey76
 */
public interface PlayerSkillSaveDAO
{
	/**
	 * Deletes the player skills from the database.
	 * @param player the player
	 */
	void delete(L2PcInstance player);
	
	/**
	 * Deletes the player skills from the database for a given class index.
	 * @param player the player
	 * @param classIndex the class index
	 */
	void delete(L2PcInstance player, int classIndex);
	
	/**
	 * Stores the player skills in the database.
	 * @param player the player
	 * @param storeEffects if {@code true} effects will be stored
	 */
	void insert(L2PcInstance player, boolean storeEffects);
	
	/**
	 * Restores the player skills from the database.
	 * @param player the player
	 */
	void load(L2PcInstance player);
}
