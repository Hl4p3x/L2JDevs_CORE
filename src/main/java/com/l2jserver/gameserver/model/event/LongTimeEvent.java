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
package com.l2jserver.gameserver.model.event;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.data.sql.impl.AnnouncementsTable;
import com.l2jserver.gameserver.data.xml.impl.NpcData;
import com.l2jserver.gameserver.datatables.EventDroplist;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.announce.EventAnnouncement;
import com.l2jserver.gameserver.model.drops.DropListScope;
import com.l2jserver.gameserver.model.drops.GeneralDropItem;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.script.DateRange;
import com.l2jserver.gameserver.util.Broadcast;

/**
 * Parent class for long time events.<br>
 * Maintains config reading, spawn of NPC's, adding of event's drop.
 * @author GKR, Sacrifice
 */
public class LongTimeEvent extends Quest
{
	private static final Logger LOG = LoggerFactory.getLogger(LongTimeEvent.class);
	
	// Drop data for event
	private final List<GeneralDropItem> dropList = new ArrayList<>();
	
	// NPC's to spawm and their spawn points
	private final List<NpcSpawn> spawnList = new ArrayList<>();
	
	private DateRange dropEventPeriod;
	private String eventName;
	private DateRange eventPeriod = null;
	
	// Messages
	private String onEnterMsg = "Event is in process";
	protected String endMsg = "Event ends!";
	
	public LongTimeEvent(String name, String descr)
	{
		super(-1, name, descr);
		loadConfig();
		
		if (eventPeriod != null)
		{
			if (eventPeriod.isWithinRange(new Date()))
			{
				startEvent();
				LOG.info("Event {} active till {}", eventName, eventPeriod.getEndDate());
			}
			else if (eventPeriod.getStartDate().after(new Date()))
			{
				final long delay = eventPeriod.getStartDate().getTime() - System.currentTimeMillis();
				ThreadPoolManager.getInstance().scheduleEvent(new ScheduleStart(), delay);
				LOG.info("Event {} will be started at {}", eventName, eventPeriod.getStartDate());
			}
			else
			{
				LOG.info("The event {} has already passed...Ignored", eventName);
			}
		}
	}
	
	/**
	 * @return event period
	 */
	public DateRange getEventPeriod()
	{
		return eventPeriod;
	}
	
	/**
	 * @return {@code true} if now is drop period
	 */
	public boolean isDropPeriod()
	{
		return dropEventPeriod.isWithinRange(new Date());
	}
	
	/**
	 * @return {@code true} if now is event period
	 */
	public boolean isEventPeriod()
	{
		return eventPeriod.isWithinRange(new Date());
	}
	
	/**
	 * Load event configuration file
	 */
	private void loadConfig()
	{
		final File configFile = new File("data/scripts/events/" + getName() + "/config.xml");
		final String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		try
		{
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(configFile);
			if (!doc.getDocumentElement().getNodeName().equalsIgnoreCase("event"))
			{
				throw new NullPointerException("WARNING!!! " + getName() + " event: bad config file!");
			}
			eventName = doc.getDocumentElement().getAttributes().getNamedItem("name").getNodeValue();
			final String period = doc.getDocumentElement().getAttributes().getNamedItem("active").getNodeValue();
			
			if (period.length() == 21) // dd MM yyyy-dd MM yyyy
			{
				eventPeriod = DateRange.parse(period, new SimpleDateFormat("dd MM yyyy", Locale.US));
			}
			else if (period.length() == 11) // dd MM-dd MM
			{
				final String start = period.split("-")[0].concat(" ").concat(currentYear);
				final String end = period.split("-")[1].concat(" ").concat(currentYear);
				final String activePeriod = start.concat("-").concat(end);
				eventPeriod = DateRange.parse(activePeriod, new SimpleDateFormat("dd MM yyyy", Locale.US));
			}
			
			if (doc.getDocumentElement().getAttributes().getNamedItem("dropPeriod") != null)
			{
				final String dropPeriod = doc.getDocumentElement().getAttributes().getNamedItem("dropPeriod").getNodeValue();
				
				if (dropPeriod.length() == 21) // dd MM yyyy-dd MM yyyy
				{
					dropEventPeriod = DateRange.parse(dropPeriod, new SimpleDateFormat("dd MM yyyy", Locale.US));
				}
				else if (dropPeriod.length() == 11) // dd MM-dd MM
				{
					final String start = dropPeriod.split("-")[0].concat(" ").concat(currentYear);
					final String end = dropPeriod.split("-")[1].concat(" ").concat(currentYear);
					final String activeDropPeriod = start.concat("-").concat(end);
					dropEventPeriod = DateRange.parse(activeDropPeriod, new SimpleDateFormat("dd MM yyyy", Locale.US));
				}
				
				// Check if drop period is within range of event period
				if (!eventPeriod.isWithinRange(dropEventPeriod.getStartDate()) || !eventPeriod.isWithinRange(dropEventPeriod.getEndDate()))
				{
					dropEventPeriod = eventPeriod;
				}
			}
			else
			{
				dropEventPeriod = eventPeriod; // Drop period, if not specified, assumes all event period.
			}
			
			if (eventPeriod == null)
			{
				throw new NullPointerException("WARNING!!! " + getName() + " event: illegal event period");
			}
			
			final Date today = new Date();
			if (eventPeriod.getStartDate().after(today) || eventPeriod.isWithinRange(today))
			{
				final Node first = doc.getDocumentElement().getFirstChild();
				for (Node n = first; n != null; n = n.getNextSibling())
				{
					// Loading droplist
					if (n.getNodeName().equalsIgnoreCase("droplist"))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("add"))
							{
								try
								{
									final int itemId = Integer.parseInt(d.getAttributes().getNamedItem("item").getNodeValue());
									final int minCount = Integer.parseInt(d.getAttributes().getNamedItem("min").getNodeValue());
									final int maxCount = Integer.parseInt(d.getAttributes().getNamedItem("max").getNodeValue());
									final String chance = d.getAttributes().getNamedItem("chance").getNodeValue();
									int finalChance = 0;
									
									if (!chance.isEmpty() && chance.endsWith("%"))
									{
										finalChance = Integer.parseInt(chance.substring(0, chance.length() - 1)) * 10000;
									}
									
									if (ItemTable.getInstance().getTemplate(itemId) == null)
									{
										LOG.warn("{} event: {} is wrong item id, item was not added in droplist", getName(), itemId);
										continue;
									}
									
									if (minCount > maxCount)
									{
										LOG.warn("{} event: item {} - min greater than max, item was not added in droplist", getName(), itemId);
										continue;
									}
									
									if ((finalChance < 10000) || (finalChance > 1000000))
									{
										LOG.warn("{} event: item {} - incorrect drop chance, item was not added in droplist", getName(), itemId);
										continue;
									}
									dropList.add((GeneralDropItem) DropListScope.STATIC.newDropItem(itemId, minCount, maxCount, finalChance));
								}
								catch (NumberFormatException nfe)
								{
									LOG.warn("Wrong number format in config.xml droplist block for {} event", getName());
								}
							}
						}
					}
					else if (n.getNodeName().equalsIgnoreCase("spawnlist"))
					{
						// Loading spawnlist
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("add"))
							{
								try
								{
									final int npcId = Integer.parseInt(d.getAttributes().getNamedItem("npc").getNodeValue());
									final int xPos = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
									final int yPos = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
									final int zPos = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
									final int heading = d.getAttributes().getNamedItem("heading").getNodeValue() != null ? Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue()) : 0;
									
									if (NpcData.getInstance().getTemplate(npcId) == null)
									{
										LOG.warn("{} event: {} is wrong NPC id, NPC was not added in spawnlist", getName(), npcId);
										continue;
									}
									spawnList.add(new NpcSpawn(npcId, new Location(xPos, yPos, zPos, heading)));
								}
								catch (NumberFormatException nfe)
								{
									LOG.warn("Wrong number format in config.xml spawnlist block for {} event", getName());
								}
							}
						}
					}
					else if (n.getNodeName().equalsIgnoreCase("messages"))
					{
						// Loading Messages
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if (d.getNodeName().equalsIgnoreCase("add"))
							{
								final String msgType = d.getAttributes().getNamedItem("type").getNodeValue();
								final String msgText = d.getAttributes().getNamedItem("text").getNodeValue();
								if ((msgType != null) && (msgText != null))
								{
									if (msgType.equalsIgnoreCase("onEnd"))
									{
										endMsg = msgText;
									}
									else if (msgType.equalsIgnoreCase("onEnter"))
									{
										onEnterMsg = msgText;
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("{} event: error reading {}! {} {}", getName(), configFile.getAbsolutePath(), e.getMessage(), e);
		}
	}
	
	/**
	 * Maintenance event start - adds global drop, spawns event NPC's, shows start announcement.
	 */
	protected void startEvent()
	{
		final long currentTime = System.currentTimeMillis();
		
		// Add drop
		if (dropList != null)
		{
			if (currentTime < dropEventPeriod.getEndDate().getTime())
			{
				for (GeneralDropItem drop : dropList)
				{
					EventDroplist.getInstance().addGlobalDrop(drop.getItemId(), drop.getMin(), drop.getMax(), (int) drop.getChance(), dropEventPeriod);
				}
			}
		}
		
		// Add spawns
		final long millisToEventEnd = eventPeriod.getEndDate().getTime() - currentTime;
		if (spawnList != null)
		{
			for (NpcSpawn spawn : spawnList)
			{
				addSpawn(spawn.npcId, spawn.loc.getX(), spawn.loc.getY(), spawn.loc.getZ(), spawn.loc.getHeading(), false, millisToEventEnd, false);
			}
		}
		
		// Send message on begin
		Broadcast.toAllOnlinePlayers(onEnterMsg);
		
		// Add announce for entering players
		AnnouncementsTable.getInstance().addAnnouncement(new EventAnnouncement(eventPeriod, onEnterMsg));
		
		// Schedule event end (now only for message sending)
		ThreadPoolManager.getInstance().scheduleEvent(new ScheduleEnd(), millisToEventEnd);
	}
	
	private class NpcSpawn
	{
		protected final Location loc;
		protected final int npcId;
		
		protected NpcSpawn(int pNpcId, Location spawnLoc)
		{
			loc = spawnLoc;
			npcId = pNpcId;
		}
	}
	
	protected class ScheduleEnd implements Runnable
	{
		@Override
		public void run()
		{
			// Send message on end
			Broadcast.toAllOnlinePlayers(endMsg);
		}
	}
	
	protected class ScheduleStart implements Runnable
	{
		@Override
		public void run()
		{
			startEvent();
		}
	}
}