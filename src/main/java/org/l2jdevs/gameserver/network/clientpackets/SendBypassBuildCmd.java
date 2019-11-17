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
import org.l2jdevs.gameserver.data.xml.impl.AdminData;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.handler.AdminCommandHandler;
import org.l2jdevs.gameserver.handler.IAdminCommandHandler;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.util.GMAudit;

/**
 * This class handles all GM commands triggered by //command
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:29 $
 */
public final class SendBypassBuildCmd extends L2GameClientPacket
{
	private static final String _C__74_SENDBYPASSBUILDCMD = "[C] 74 SendBypassBuildCmd";
	
	public static final int GM_MESSAGE = 9;
	public static final int ANNOUNCEMENT = 10;
	
	private String _command;
	
	@Override
	public String getType()
	{
		return _C__74_SENDBYPASSBUILDCMD;
	}
	
	@Override
	protected void readImpl()
	{
		_command = readS();
		if (_command != null)
		{
			_command = _command.trim();
		}
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		String command = "admin_" + _command.split(" ")[0];
		
		IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
		
		if (ach == null)
		{
			if (activeChar.isGM())
			{
				activeChar.sendMessage(LanguageData.getInstance().getMsg(activeChar, "gm_command_not_exist").replace("%s%", command.substring(6) + ""));
			}
			
			_log.warning("No handler registered for admin command '" + command + "'");
			return;
		}
		
		if (!AdminData.getInstance().hasAccess(command, activeChar.getAccessLevel()))
		{
			activeChar.sendMessage(LanguageData.getInstance().getMsg(activeChar, "gm_command_not_access"));
			_log.warning("Character " + activeChar.getName() + " tryed to use admin command " + command + ", but have no access to it!");
			return;
		}
		
		if (Config.GMAUDIT)
		{
			GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"));
		}
		
		ach.useAdminCommand("admin_" + _command, activeChar);
	}
}
