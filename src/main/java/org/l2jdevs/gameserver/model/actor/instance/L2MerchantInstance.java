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
package org.l2jdevs.gameserver.model.actor.instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.gameserver.data.xml.impl.BuyListData;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.datatables.MerchantPriceConfigTable;
import org.l2jdevs.gameserver.datatables.MerchantPriceConfigTable.MerchantPriceConfig;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2jdevs.gameserver.model.buylist.L2BuyList;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.BuyList;
import org.l2jdevs.gameserver.network.serverpackets.ExBuySellList;

/**
 * This class ...
 * @version $Revision: 1.10.4.9 $ $Date: 2005/04/11 10:06:08 $
 */
public class L2MerchantInstance extends L2NpcInstance
{
	private static final Logger LOG = LoggerFactory.getLogger(L2MerchantInstance.class);
	private MerchantPriceConfig _mpc;
	
	/**
	 * Creates a merchant,
	 * @param template the merchant NPC template
	 */
	public L2MerchantInstance(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2MerchantInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/merchant/" + pom + ".htm";
	}
	
	/**
	 * @return Returns the mpc.
	 */
	public MerchantPriceConfig getMpc()
	{
		return _mpc;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_mpc = MerchantPriceConfigTable.getInstance().getMerchantPriceConfig(this);
	}
	
	public final void showBuyWindow(L2PcInstance player, int val)
	{
		showBuyWindow(player, val, true);
	}
	
	public final void showBuyWindow(L2PcInstance player, int val, boolean applyTax)
	{
		final L2BuyList buyList = BuyListData.getInstance().getBuyList(val);
		if (buyList == null)
		{
			LOG.warn("BuyList not found! BuyListId: {}", val);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!buyList.isNpcAllowed(getId()))
		{
			LOG.warn("Npc not allowed in BuyList! BuyListId: {} NpcId: {}", val, getId());
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final double taxRate = (applyTax) ? getMpc().getTotalTaxRate() : 0;
		
		player.setInventoryBlockingStatus(true);
		if (player.isGM())
		{
			player.sendMessage(LanguageData.getInstance().getMsg(player, "gm_buy_list_info").replace("%s%", buyList.getListId() + ""));
		}
		player.sendPacket(new BuyList(buyList, player.getAdena(), taxRate));
		player.sendPacket(new ExBuySellList(player, false));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
