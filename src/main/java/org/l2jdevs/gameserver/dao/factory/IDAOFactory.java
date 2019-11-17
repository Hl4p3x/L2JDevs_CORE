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
package org.l2jdevs.gameserver.dao.factory;

import org.l2jdevs.gameserver.dao.FriendDAO;
import org.l2jdevs.gameserver.dao.HennaDAO;
import org.l2jdevs.gameserver.dao.ItemDAO;
import org.l2jdevs.gameserver.dao.ItemReuseDAO;
import org.l2jdevs.gameserver.dao.PetDAO;
import org.l2jdevs.gameserver.dao.PetSkillSaveDAO;
import org.l2jdevs.gameserver.dao.PlayerDAO;
import org.l2jdevs.gameserver.dao.PlayerSkillSaveDAO;
import org.l2jdevs.gameserver.dao.PremiumItemDAO;
import org.l2jdevs.gameserver.dao.RecipeBookDAO;
import org.l2jdevs.gameserver.dao.RecipeShopListDAO;
import org.l2jdevs.gameserver.dao.RecommendationBonusDAO;
import org.l2jdevs.gameserver.dao.ServitorSkillSaveDAO;
import org.l2jdevs.gameserver.dao.ShortcutDAO;
import org.l2jdevs.gameserver.dao.SkillDAO;
import org.l2jdevs.gameserver.dao.SubclassDAO;
import org.l2jdevs.gameserver.dao.TeleportBookmarkDAO;

/**
 * DAO Factory interface.
 * @author Zoey76
 */
public interface IDAOFactory
{
	FriendDAO getFriendDAO();
	
	HennaDAO getHennaDAO();
	
	ItemDAO getItemDAO();
	
	ItemReuseDAO getItemReuseDAO();
	
	PetDAO getPetDAO();
	
	PetSkillSaveDAO getPetSkillSaveDAO();
	
	PlayerDAO getPlayerDAO();
	
	PlayerSkillSaveDAO getPlayerSkillSaveDAO();
	
	PremiumItemDAO getPremiumItemDAO();
	
	RecipeBookDAO getRecipeBookDAO();
	
	RecipeShopListDAO getRecipeShopListDAO();
	
	RecommendationBonusDAO getRecommendationBonusDAO();
	
	ServitorSkillSaveDAO getServitorSkillSaveDAO();
	
	ShortcutDAO getShortcutDAO();
	
	SkillDAO getSkillDAO();
	
	SubclassDAO getSubclassDAO();
	
	TeleportBookmarkDAO getTeleportBookmarkDAO();
}
