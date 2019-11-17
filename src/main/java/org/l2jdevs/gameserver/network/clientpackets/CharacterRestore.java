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

import org.l2jdevs.gameserver.model.CharSelectInfoPackage;
import org.l2jdevs.gameserver.model.events.EventDispatcher;
import org.l2jdevs.gameserver.model.events.impl.character.player.OnPlayerRestore;
import org.l2jdevs.gameserver.network.serverpackets.CharSelectionInfo;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.2 $ $Date: 2005/03/27 15:29:29 $
 */
public final class CharacterRestore extends L2GameClientPacket
{
	private static final String _C__7B_CHARACTERRESTORE = "[C] 7B CharacterRestore";
	
	// cd
	private int _charSlot;
	
	@Override
	public String getType()
	{
		return _C__7B_CHARACTERRESTORE;
	}
	
	@Override
	protected void readImpl()
	{
		_charSlot = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().getCharacterSelect().tryPerformAction("CharacterRestore"))
		{
			return;
		}
		
		getClient().markRestoredChar(_charSlot);
		CharSelectionInfo cl = new CharSelectionInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1, 0);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
		final CharSelectInfoPackage charInfo = getClient().getCharSelection(_charSlot);
		EventDispatcher.getInstance().notifyEvent(new OnPlayerRestore(charInfo.getObjectId(), charInfo.getName(), getClient()));
	}
}
