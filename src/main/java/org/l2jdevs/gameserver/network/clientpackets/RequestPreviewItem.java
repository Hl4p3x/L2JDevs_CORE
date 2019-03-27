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
package org.l2jdevs.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.ThreadPoolManager;
import org.l2jdevs.gameserver.data.xml.impl.BuyListData;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.actor.L2Npc;
import org.l2jdevs.gameserver.model.actor.instance.L2MerchantInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.buylist.L2BuyList;
import org.l2jdevs.gameserver.model.buylist.Product;
import org.l2jdevs.gameserver.model.itemcontainer.Inventory;
import org.l2jdevs.gameserver.model.items.L2Armor;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.items.L2Weapon;
import org.l2jdevs.gameserver.model.items.type.ArmorType;
import org.l2jdevs.gameserver.model.items.type.WeaponType;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.ShopPreviewInfo;
import org.l2jdevs.gameserver.network.serverpackets.UserInfo;
import org.l2jdevs.gameserver.util.Util;

/**
 ** @author Gnacik
 */
public final class RequestPreviewItem extends L2GameClientPacket
{
	private static final String _C__C7_REQUESTPREVIEWITEM = "[C] C7 RequestPreviewItem";
	
	@SuppressWarnings("unused")
	private int _unk;
	private int _listId;
	private int _count;
	private int[] _items;
	
	private class RemoveWearItemsTask implements Runnable
	{
		private final L2PcInstance activeChar;
		
		protected RemoveWearItemsTask(L2PcInstance player)
		{
			activeChar = player;
		}
		
		@Override
		public void run()
		{
			try
			{
				activeChar.sendPacket(SystemMessageId.NO_LONGER_TRYING_ON);
				activeChar.sendPacket(new UserInfo(activeChar));
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	@Override
	protected void readImpl()
	{
		_unk = readD();
		_listId = readD();
		_count = readD();
		
		if (_count < 0)
		{
			_count = 0;
		}
		if (_count > 100)
		{
			return; // prevent too long lists
		}
		
		// Create _items table that will contain all ItemID to Wear
		_items = new int[_count];
		
		// Fill _items table with all ItemID to Wear
		for (int i = 0; i < _count; i++)
		{
			_items[i] = readD();
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
		{
			return;
		}
		
		// Get the current player and return if null
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("buy"))
		{
			activeChar.sendMessage(LanguageData.getInstance().getMsg(activeChar, "buying_too_fast"));
			return;
		}
		
		// If Alternate rule Karma punishment is set to true, forbid Wear to player with Karma
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (activeChar.getKarma() > 0))
		{
			return;
		}
		
		// Check current target of the player and the INTERACTION_DISTANCE
		L2Object target = activeChar.getTarget();
		if (!activeChar.isGM() && ((target == null // No target (i.e. GM Shop)
		) || !((target instanceof L2MerchantInstance)) // Target not a merchant
			|| !activeChar.isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false) // Distance is too far
		))
		{
			return;
		}
		
		if ((_count < 1) || (_listId >= 4000000))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Get the current merchant targeted by the player
		final L2MerchantInstance merchant = (target instanceof L2MerchantInstance) ? (L2MerchantInstance) target : null;
		if (merchant == null)
		{
			_log.warning(getClass().getName() + " Null merchant!");
			return;
		}
		
		final L2BuyList buyList = BuyListData.getInstance().getBuyList(_listId);
		if (buyList == null)
		{
			Util.handleIllegalPlayerAction(activeChar, "Warning!! Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " sent a false BuyList list_id " + _listId, Config.DEFAULT_PUNISH);
			return;
		}
		
		long totalPrice = 0;
		Map<Integer, Integer> itemList = new HashMap<>();
		
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i];
			
			final Product product = buyList.getProductByItemId(itemId);
			if (product == null)
			{
				Util.handleIllegalPlayerAction(activeChar, "Warning!! Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + itemId, Config.DEFAULT_PUNISH);
				return;
			}
			
			L2Item template = product.getItem();
			if (template == null)
			{
				continue;
			}
			
			int slot = Inventory.getPaperdollIndex(template.getBodyPart());
			if (slot < 0)
			{
				continue;
			}
			
			if (template instanceof L2Weapon)
			{
				if (activeChar.getRace().ordinal() == 5)
				{
					if (template.getItemType() == WeaponType.NONE)
					{
						continue;
					}
					else if ((template.getItemType() == WeaponType.RAPIER) || (template.getItemType() == WeaponType.CROSSBOW) || (template.getItemType() == WeaponType.ANCIENTSWORD))
					{
						continue;
					}
				}
			}
			else if (template instanceof L2Armor)
			{
				if (activeChar.getRace().ordinal() == 5)
				{
					if ((template.getItemType() == ArmorType.HEAVY) || (template.getItemType() == ArmorType.MAGIC))
					{
						continue;
					}
				}
			}
			
			if (itemList.containsKey(slot))
			{
				activeChar.sendPacket(SystemMessageId.YOU_CAN_NOT_TRY_THOSE_ITEMS_ON_AT_THE_SAME_TIME);
				return;
			}
			
			itemList.put(slot, itemId);
			totalPrice += Config.WEAR_PRICE;
			if (totalPrice > Inventory.MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(activeChar, "Warning!! Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to purchase over " + Inventory.MAX_ADENA + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
		}
		
		// Charge buyer and add tax to castle treasury if not owned by npc clan because a Try On is not Free
		if ((totalPrice < 0) || !activeChar.reduceAdena("Wear", totalPrice, activeChar.getLastFolkNPC(), true))
		{
			activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}
		
		if (!itemList.isEmpty())
		{
			activeChar.sendPacket(new ShopPreviewInfo(itemList));
			// Schedule task
			ThreadPoolManager.getInstance().scheduleGeneral(new RemoveWearItemsTask(activeChar), Config.WEAR_DELAY * 1000);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__C7_REQUESTPREVIEWITEM;
	}
}
