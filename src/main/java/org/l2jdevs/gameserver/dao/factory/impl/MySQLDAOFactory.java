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
package org.l2jdevs.gameserver.dao.factory.impl;

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
import org.l2jdevs.gameserver.dao.factory.IDAOFactory;
import org.l2jdevs.gameserver.dao.impl.mysql.FriendDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.HennaDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.ItemDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.ItemReuseDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.PetDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.PetSkillSaveDAOMySQL;
import org.l2jdevs.gameserver.dao.impl.mysql.PlayerDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.PlayerSkillSaveDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.PremiumItemDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.RecipeBookDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.RecipeShopListDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.RecommendationBonusDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.ServitorSkillSaveDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.ShortcutDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.SkillDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.SubclassDAOMySQLImpl;
import org.l2jdevs.gameserver.dao.impl.mysql.TeleportBookmarkDAOMySQLImpl;

/**
 * MySQL DAO Factory implementation.
 * @author Zoey76
 */
enum MySQLDAOFactory implements IDAOFactory
{
	INSTANCE;
	
	private final FriendDAO friendDAO = new FriendDAOMySQLImpl();
	private final HennaDAO hennaDAO = new HennaDAOMySQLImpl();
	private final ItemDAO itemDAO = new ItemDAOMySQLImpl();
	private final ItemReuseDAO itemReuseDAO = new ItemReuseDAOMySQLImpl();
	private final PetDAO petDAO = new PetDAOMySQLImpl();
	private final PetSkillSaveDAO petSkillSaveDAO = new PetSkillSaveDAOMySQL();
	private final PlayerDAO playerDAO = new PlayerDAOMySQLImpl();
	private final PlayerSkillSaveDAO playerSkillSaveDAO = new PlayerSkillSaveDAOMySQLImpl();
	private final PremiumItemDAO premiumItemDAO = new PremiumItemDAOMySQLImpl();
	private final RecipeBookDAO recipeBookDAO = new RecipeBookDAOMySQLImpl();
	private final RecipeShopListDAO recipeShopListDAO = new RecipeShopListDAOMySQLImpl();
	private final RecommendationBonusDAO recommendationBonusDAO = new RecommendationBonusDAOMySQLImpl();
	private final ServitorSkillSaveDAO servitorSkillSaveDAO = new ServitorSkillSaveDAOMySQLImpl();
	private final ShortcutDAO shortcutDAO = new ShortcutDAOMySQLImpl();
	private final SkillDAO skillDAO = new SkillDAOMySQLImpl();
	private final SubclassDAO subclassDAO = new SubclassDAOMySQLImpl();
	private final TeleportBookmarkDAO teleportBookmarkDAO = new TeleportBookmarkDAOMySQLImpl();
	
	@Override
	public FriendDAO getFriendDAO()
	{
		return friendDAO;
	}
	
	@Override
	public HennaDAO getHennaDAO()
	{
		return hennaDAO;
	}
	
	@Override
	public ItemDAO getItemDAO()
	{
		return itemDAO;
	}
	
	@Override
	public ItemReuseDAO getItemReuseDAO()
	{
		return itemReuseDAO;
	}
	
	@Override
	public PetDAO getPetDAO()
	{
		return petDAO;
	}
	
	@Override
	public PetSkillSaveDAO getPetSkillSaveDAO()
	{
		return petSkillSaveDAO;
	}
	
	@Override
	public PlayerDAO getPlayerDAO()
	{
		return playerDAO;
	}
	
	@Override
	public PlayerSkillSaveDAO getPlayerSkillSaveDAO()
	{
		return playerSkillSaveDAO;
	}
	
	@Override
	public PremiumItemDAO getPremiumItemDAO()
	{
		return premiumItemDAO;
	}
	
	@Override
	public RecipeBookDAO getRecipeBookDAO()
	{
		return recipeBookDAO;
	}
	
	@Override
	public RecipeShopListDAO getRecipeShopListDAO()
	{
		return recipeShopListDAO;
	}
	
	@Override
	public RecommendationBonusDAO getRecommendationBonusDAO()
	{
		return recommendationBonusDAO;
	}
	
	@Override
	public ServitorSkillSaveDAO getServitorSkillSaveDAO()
	{
		return servitorSkillSaveDAO;
	}
	
	@Override
	public ShortcutDAO getShortcutDAO()
	{
		return shortcutDAO;
	}
	
	@Override
	public SkillDAO getSkillDAO()
	{
		return skillDAO;
	}
	
	@Override
	public SubclassDAO getSubclassDAO()
	{
		return subclassDAO;
	}
	
	@Override
	public TeleportBookmarkDAO getTeleportBookmarkDAO()
	{
		return teleportBookmarkDAO;
	}
}
