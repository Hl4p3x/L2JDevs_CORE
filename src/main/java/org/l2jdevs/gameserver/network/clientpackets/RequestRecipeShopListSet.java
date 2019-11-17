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
package org.l2jdevs.gameserver.network.clientpackets;

import static org.l2jdevs.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import java.util.Arrays;
import java.util.List;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.data.xml.impl.RecipeData;
import org.l2jdevs.gameserver.enums.PrivateStoreType;
import org.l2jdevs.gameserver.model.L2ManufactureItem;
import org.l2jdevs.gameserver.model.L2RecipeList;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.zone.ZoneId;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.RecipeShopMsg;
import org.l2jdevs.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2jdevs.gameserver.util.Broadcast;
import org.l2jdevs.gameserver.util.Util;

/**
 * RequestRecipeShopListSet client packet class.
 */
public final class RequestRecipeShopListSet extends L2GameClientPacket
{
	private static final String _C__BB_RequestRecipeShopListSet = "[C] BB RequestRecipeShopListSet";
	
	private static final int BATCH_LENGTH = 12;
	
	private L2ManufactureItem[] _items = null;
	
	@Override
	public String getType()
	{
		return _C__BB_RequestRecipeShopListSet;
	}
	
	@Override
	protected void readImpl()
	{
		int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new L2ManufactureItem[count];
		for (int i = 0; i < count; i++)
		{
			int id = readD();
			long cost = readQ();
			if (cost < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new L2ManufactureItem(id, cost);
		}
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (_items == null)
		{
			player.setPrivateStoreType(PrivateStoreType.NONE);
			player.broadcastUserInfo();
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || player.isInDuel())
		{
			player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInsideZone(ZoneId.NO_STORE))
		{
			player.sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		List<L2RecipeList> dwarfRecipes = Arrays.asList(player.getDwarvenRecipeBook());
		List<L2RecipeList> commonRecipes = Arrays.asList(player.getCommonRecipeBook());
		
		player.getManufactureItems().clear();
		
		for (L2ManufactureItem i : _items)
		{
			final L2RecipeList list = RecipeData.getInstance().getRecipeList(i.getRecipeId());
			if (!dwarfRecipes.contains(list) && !commonRecipes.contains(list))
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Player " + player.getName() + " of account " + player.getAccountName() + " tried to set recipe which he dont have.", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (i.getCost() > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to set price more than " + MAX_ADENA + " adena in Private Manufacture.", Config.DEFAULT_PUNISH);
				return;
			}
			
			player.getManufactureItems().put(i.getRecipeId(), i);
		}
		
		player.setStoreName(!player.hasManufactureShop() ? "" : player.getStoreName());
		player.setPrivateStoreType(PrivateStoreType.MANUFACTURE);
		player.sitDown();
		player.broadcastUserInfo();
		Broadcast.toSelfAndKnownPlayers(player, new RecipeShopMsg(player));
	}
}
