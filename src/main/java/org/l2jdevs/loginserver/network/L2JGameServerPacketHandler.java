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
package org.l2jdevs.loginserver.network;

import java.util.logging.Logger;

import org.l2jdevs.Config;
import org.l2jdevs.loginserver.GameServerThread;
import org.l2jdevs.loginserver.network.gameserverpackets.BlowFishKey;
import org.l2jdevs.loginserver.network.gameserverpackets.ChangeAccessLevel;
import org.l2jdevs.loginserver.network.gameserverpackets.ChangePassword;
import org.l2jdevs.loginserver.network.gameserverpackets.GameServerAuth;
import org.l2jdevs.loginserver.network.gameserverpackets.PlayerAuthRequest;
import org.l2jdevs.loginserver.network.gameserverpackets.PlayerInGame;
import org.l2jdevs.loginserver.network.gameserverpackets.PlayerLogout;
import org.l2jdevs.loginserver.network.gameserverpackets.PlayerTracert;
import org.l2jdevs.loginserver.network.gameserverpackets.ReplyCharacters;
import org.l2jdevs.loginserver.network.gameserverpackets.RequestSendMail;
import org.l2jdevs.loginserver.network.gameserverpackets.RequestTempBan;
import org.l2jdevs.loginserver.network.gameserverpackets.ServerStatus;
import org.l2jdevs.loginserver.network.loginserverpackets.LoginServerFail;
import org.l2jdevs.util.network.BaseRecievePacket;

/**
 * @author mrTJO
 */
public class L2JGameServerPacketHandler
{
	protected static Logger _log = Logger.getLogger(L2JGameServerPacketHandler.class.getName());
	
	public static enum GameServerState
	{
		CONNECTED,
		BF_CONNECTED,
		AUTHED
	}
	
	public static BaseRecievePacket handlePacket(byte[] data, GameServerThread server)
	{
		BaseRecievePacket msg = null;
		int opcode = data[0] & 0xff;
		GameServerState state = server.getLoginConnectionState();
		switch (state)
		{
			case CONNECTED:
				switch (opcode)
				{
					case 0x00:
						msg = new BlowFishKey(data, server);
						break;
					default:
						_log.warning("Unknown Opcode (" + Integer.toHexString(opcode).toUpperCase() + ") in state " + state.name() + " from GameServer, closing connection.");
						server.forceClose(LoginServerFail.NOT_AUTHED);
						break;
				}
				break;
			case BF_CONNECTED:
				switch (opcode)
				{
					case 0x01:
						msg = new GameServerAuth(data, server);
						break;
					default:
						_log.warning("Unknown Opcode (" + Integer.toHexString(opcode).toUpperCase() + ") in state " + state.name() + " from GameServer, closing connection.");
						server.forceClose(LoginServerFail.NOT_AUTHED);
						break;
				}
				break;
			case AUTHED:
				switch (opcode)
				{
					case 0x02:
						msg = new PlayerInGame(data, server);
						break;
					case 0x03:
						msg = new PlayerLogout(data, server);
						break;
					case 0x04:
						msg = new ChangeAccessLevel(data, server);
						break;
					case 0x05:
						msg = new PlayerAuthRequest(data, server);
						break;
					case 0x06:
						msg = new ServerStatus(data, server);
						break;
					case 0x07:
						msg = new PlayerTracert(data);
						break;
					case 0x08:
						msg = new ReplyCharacters(data, server);
						break;
					case 0x09:
						if (Config.EMAIL_SYS_ENABLED)
						{
							msg = new RequestSendMail(data);
						}
						break;
					case 0x0A:
						msg = new RequestTempBan(data);
						break;
					case 0x0B:
						new ChangePassword(data);
						break;
					default:
						_log.warning("Unknown Opcode (" + Integer.toHexString(opcode).toUpperCase() + ") in state " + state.name() + " from GameServer, closing connection.");
						server.forceClose(LoginServerFail.NOT_AUTHED);
						break;
				}
				break;
		}
		return msg;
	}
}
