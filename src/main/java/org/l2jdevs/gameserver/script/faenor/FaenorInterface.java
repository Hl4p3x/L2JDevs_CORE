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
package org.l2jdevs.gameserver.script.faenor;

import java.util.List;
import java.util.logging.Logger;

import org.l2jdevs.gameserver.data.sql.impl.AnnouncementsTable;
import org.l2jdevs.gameserver.datatables.EventDroplist;
import org.l2jdevs.gameserver.model.announce.EventAnnouncement;
import org.l2jdevs.gameserver.script.DateRange;
import org.l2jdevs.gameserver.script.EngineInterface;

/**
 * @author Luis Arias
 */
public class FaenorInterface implements EngineInterface
{
	protected static final Logger _log = Logger.getLogger(FaenorInterface.class.getName());
	
	public static FaenorInterface getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public List<?> getAllPlayers()
	{
		return null;
	}
	
	@Override
	public void addEventDrop(int[] items, int[] count, double chance, DateRange range)
	{
		EventDroplist.getInstance().addGlobalDrop(items, count, (int) (chance * 1000000), range);
	}
	
	@Override
	public void onPlayerLogin(String message, DateRange validDateRange)
	{
		AnnouncementsTable.getInstance().addAnnouncement(new EventAnnouncement(validDateRange, message));
	}
	
	private static class SingletonHolder
	{
		protected static final FaenorInterface _instance = new FaenorInterface();
	}
}
