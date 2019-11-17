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

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2PetInstance;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.util.Util;

/**
 * This class ...
 * @version $Revision: 1.3.4.4 $ $Date: 2005/03/29 23:15:33 $
 */
public final class RequestGetItemFromPet extends L2GameClientPacket
{
	private static final String _C__2C_REQUESTGETITEMFROMPET = "[C] 2C RequestGetItemFromPet";
	
	private int _objectId;
	private long _amount;
	@SuppressWarnings("unused")
	private int _unknown;
	
	@Override
	public String getType()
	{
		return _C__2C_REQUESTGETITEMFROMPET;
	}
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_amount = readQ();
		_unknown = readD();// = 0 for most trades
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if ((_amount <= 0) || (player == null) || !player.hasPet())
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("getfrompet"))
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "get_item_pet_too_fast"));
			return;
		}
		
		final L2PetInstance pet = (L2PetInstance) player.getSummon();
		if (player.getActiveEnchantItemId() != L2PcInstance.ID_NONE)
		{
			return;
		}
		
		final L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		
		if (_amount > item.getCount())
		{
			Util.handleIllegalPlayerAction(player, getClass().getSimpleName() + ": Character " + player.getName() + " of account " + player.getAccountName() + " tried to get item with oid " + _objectId + " from pet but has invalid count " + _amount + " item count: "
				+ item.getCount(), Config.DEFAULT_PUNISH);
			return;
		}
		
		if (pet.transferItem("Transfer", _objectId, _amount, player.getInventory(), player, pet) == null)
		{
			_log.warning("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
		}
	}
}
