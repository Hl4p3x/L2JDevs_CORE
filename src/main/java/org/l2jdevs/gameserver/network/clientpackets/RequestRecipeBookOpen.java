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

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.RecipeController;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.network.SystemMessageId;

public final class RequestRecipeBookOpen extends L2GameClientPacket
{
	private static final String _C__B5_REQUESTRECIPEBOOKOPEN = "[C] B5 RequestRecipeBookOpen";
	
	private boolean _isDwarvenCraft;
	
	@Override
	public String getType()
	{
		return _C__B5_REQUESTRECIPEBOOKOPEN;
	}
	
	@Override
	protected void readImpl()
	{
		_isDwarvenCraft = (readD() == 0);
		if (Config.DEBUG)
		{
			_log.info("RequestRecipeBookOpen : " + (_isDwarvenCraft ? "dwarvenCraft" : "commonCraft"));
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
		{
			activeChar.sendPacket(SystemMessageId.NO_RECIPE_BOOK_WHILE_CASTING);
			return;
		}
		
		if (activeChar.getActiveRequester() != null)
		{
			activeChar.sendMessage(LanguageData.getInstance().getMsg(activeChar, "no_recipe_while_trading"));
			return;
		}
		
		RecipeController.getInstance().requestBookOpen(activeChar, _isDwarvenCraft);
	}
}
