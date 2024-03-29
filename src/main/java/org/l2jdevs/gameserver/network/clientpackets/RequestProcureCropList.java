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

import java.util.ArrayList;
import java.util.List;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.datatables.ItemTable;
import org.l2jdevs.gameserver.instancemanager.CastleManorManager;
import org.l2jdevs.gameserver.model.CropProcure;
import org.l2jdevs.gameserver.model.actor.L2Npc;
import org.l2jdevs.gameserver.model.actor.instance.L2MerchantInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.holders.UniqueItemHolder;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

/**
 * @author l3x
 */
public class RequestProcureCropList extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 20; // length of the one item
	
	private List<CropHolder> _items = null;
	
	@Override
	public String getType()
	{
		return "[C] D0:02 RequestProcureCropList";
	}
	
	@Override
	protected final void readImpl()
	{
		final int count = readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != _buf.remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int objId = readD();
			final int itemId = readD();
			final int manorId = readD();
			final long cnt = readQ();
			if ((objId < 1) || (itemId < 1) || (manorId < 0) || (cnt < 0))
			{
				_items = null;
				return;
			}
			_items.add(new CropHolder(objId, itemId, cnt, manorId));
		}
	}
	
	@Override
	protected final void runImpl()
	{
		if (_items == null)
		{
			return;
		}
		
		final L2PcInstance player = getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (manor.isUnderMaintenance())
		{
			sendActionFailed();
			return;
		}
		
		final L2Npc manager = player.getLastFolkNPC();
		if (!(manager instanceof L2MerchantInstance) || !manager.canInteract(player))
		{
			sendActionFailed();
			return;
		}
		
		final int castleId = manager.getCastle().getResidenceId();
		if (manager.getTemplate().getParameters().getInt("manor_id", -1) != castleId)
		{
			sendActionFailed();
			return;
		}
		
		int slots = 0, weight = 0;
		for (CropHolder i : _items)
		{
			final L2ItemInstance item = player.getInventory().getItemByObjectId(i.getObjectId());
			if ((item == null) || (item.getCount() < i.getCount()) || (item.getId() != i.getId()))
			{
				sendActionFailed();
				return;
			}
			
			final CropProcure cp = i.getCropProcure();
			if ((cp == null) || (cp.getAmount() < i.getCount()))
			{
				sendActionFailed();
				return;
			}
			
			final L2Item template = ItemTable.getInstance().getTemplate(i.getRewardId());
			weight += (i.getCount() * template.getWeight());
			
			if (!template.isStackable())
			{
				slots += i.getCount();
			}
			else if (player.getInventory().getItemByItemId(i.getRewardId()) == null)
			{
				slots++;
			}
		}
		
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		else if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		// Used when Config.ALT_MANOR_SAVE_ALL_ACTIONS == true
		final int updateListSize = Config.ALT_MANOR_SAVE_ALL_ACTIONS ? _items.size() : 0;
		final List<CropProcure> updateList = new ArrayList<>(updateListSize);
		
		// Proceed the purchase
		for (CropHolder i : _items)
		{
			final long rewardPrice = ItemTable.getInstance().getTemplate(i.getRewardId()).getReferencePrice();
			if (rewardPrice == 0)
			{
				continue;
			}
			
			final long rewardItemCount = i.getPrice() / rewardPrice;
			if (rewardItemCount < 1)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getId());
				sm.addLong(i.getCount());
				player.sendPacket(sm);
				tradeFailureNotification(player, i);
				continue;
			}
			
			// Fee for selling to other manors
			final long fee = (castleId == i.getManorId()) ? 0 : ((long) (i.getPrice() * 0.05));
			if ((fee != 0) && (player.getAdena() < fee))
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getId());
				sm.addLong(i.getCount());
				player.sendPacket(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				player.sendMessage(String.format("You need %d adena to pay foreign manor trading fee.", fee));
				continue;
			}
			
			final CropProcure cp = i.getCropProcure();
			if (!cp.decreaseAmount(i.getCount()) || ((fee > 0) && !player.reduceAdena("Manor", fee, manager, true)) || !player.destroyItem("Manor", i.getObjectId(), i.getCount(), manager, true))
			{
				continue;
			}
			player.addItem("Manor", i.getRewardId(), rewardItemCount, manager, true);
			
			if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				updateList.add(cp);
			}
		}
		
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			manor.updateCurrentProcure(castleId, updateList);
		}
	}
	
	/**
	 * send message to player about reasons of crop trade failure
	 * @param player message receiver
	 * @param crop crop (holder) was traded
	 */
	private void tradeFailureNotification(L2PcInstance player, CropHolder crop)
	{
		final var rewardName = ItemTable.getInstance().getTemplate(crop.getRewardId()).getName();
		final var cropName = ItemTable.getInstance().getTemplate(crop.getId()).getName();
		final var msg = String.format("Not enough of %s offered to trade for 1 unit of %s.", cropName, rewardName);
		player.sendMessage(msg);
	}
	
	private final class CropHolder extends UniqueItemHolder
	{
		private final int _manorId;
		private CropProcure _cp;
		private int _rewardId = 0;
		
		public CropHolder(int objectId, int id, long count, int manorId)
		{
			super(id, objectId, count);
			_manorId = manorId;
		}
		
		public final CropProcure getCropProcure()
		{
			if (_cp == null)
			{
				_cp = CastleManorManager.getInstance().getCropProcure(_manorId, getId(), false);
			}
			return _cp;
		}
		
		public final int getManorId()
		{
			return _manorId;
		}
		
		public final long getPrice()
		{
			return getCount() * _cp.getPrice();
		}
		
		public final int getRewardId()
		{
			if (_rewardId == 0)
			{
				_rewardId = CastleManorManager.getInstance().getSeedByCrop(_cp.getId()).getReward(_cp.getReward());
			}
			return _rewardId;
		}
	}
}