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
package org.l2jdevs.loginserver.network.gameserverpackets;

import java.util.logging.Logger;

import org.l2jdevs.Config;
import org.l2jdevs.loginserver.LoginController;
import org.l2jdevs.util.network.BaseRecievePacket;

/**
 * @author mrTJO
 */
public class PlayerTracert extends BaseRecievePacket
{
	protected static Logger _log = Logger.getLogger(PlayerTracert.class.getName());
	
	/**
	 * @param decrypt
	 */
	public PlayerTracert(byte[] decrypt)
	{
		super(decrypt);
		String account = readS();
		String pcIp = readS();
		String hop1 = readS();
		String hop2 = readS();
		String hop3 = readS();
		String hop4 = readS();
		
		LoginController.getInstance().setAccountLastTracert(account, pcIp, hop1, hop2, hop3, hop4);
		if (Config.DEBUG)
		{
			_log.info("Saved " + account + " last tracert");
		}
	}
}