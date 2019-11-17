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
package org.l2jdevs.gameserver;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.Config;
import org.l2jdevs.Server;
import org.l2jdevs.UPnPService;
import org.l2jdevs.commons.database.pool.impl.ConnectionFactory;
import org.l2jdevs.gameserver.cache.HtmCache;
import org.l2jdevs.gameserver.dao.factory.impl.DAOFactory;
import org.l2jdevs.gameserver.data.json.ExperienceData;
import org.l2jdevs.gameserver.data.sql.impl.AnnouncementsTable;
import org.l2jdevs.gameserver.data.sql.impl.CharNameTable;
import org.l2jdevs.gameserver.data.sql.impl.CharSummonTable;
import org.l2jdevs.gameserver.data.sql.impl.ClanTable;
import org.l2jdevs.gameserver.data.sql.impl.CrestTable;
import org.l2jdevs.gameserver.data.sql.impl.NpcBufferTable;
import org.l2jdevs.gameserver.data.sql.impl.OfflineTradersTable;
import org.l2jdevs.gameserver.data.sql.impl.SummonSkillsTable;
import org.l2jdevs.gameserver.data.sql.impl.TeleportLocationTable;
import org.l2jdevs.gameserver.data.xml.impl.AdminData;
import org.l2jdevs.gameserver.data.xml.impl.ArmorSetsData;
import org.l2jdevs.gameserver.data.xml.impl.BuyListData;
import org.l2jdevs.gameserver.data.xml.impl.CategoryData;
import org.l2jdevs.gameserver.data.xml.impl.ClassListData;
import org.l2jdevs.gameserver.data.xml.impl.DoorData;
import org.l2jdevs.gameserver.data.xml.impl.EnchantItemData;
import org.l2jdevs.gameserver.data.xml.impl.EnchantItemGroupsData;
import org.l2jdevs.gameserver.data.xml.impl.EnchantItemHPBonusData;
import org.l2jdevs.gameserver.data.xml.impl.EnchantItemOptionsData;
import org.l2jdevs.gameserver.data.xml.impl.EnchantSkillGroupsData;
import org.l2jdevs.gameserver.data.xml.impl.FishData;
import org.l2jdevs.gameserver.data.xml.impl.FishingMonstersData;
import org.l2jdevs.gameserver.data.xml.impl.FishingRodsData;
import org.l2jdevs.gameserver.data.xml.impl.HennaData;
import org.l2jdevs.gameserver.data.xml.impl.HitConditionBonusData;
import org.l2jdevs.gameserver.data.xml.impl.InitialEquipmentData;
import org.l2jdevs.gameserver.data.xml.impl.InitialShortcutData;
import org.l2jdevs.gameserver.data.xml.impl.KarmaData;
import org.l2jdevs.gameserver.data.xml.impl.MultisellData;
import org.l2jdevs.gameserver.data.xml.impl.NpcData;
import org.l2jdevs.gameserver.data.xml.impl.OptionData;
import org.l2jdevs.gameserver.data.xml.impl.PetDataTable;
import org.l2jdevs.gameserver.data.xml.impl.PlayerCreationPointData;
import org.l2jdevs.gameserver.data.xml.impl.PlayerTemplateData;
import org.l2jdevs.gameserver.data.xml.impl.PlayerXpPercentLostData;
import org.l2jdevs.gameserver.data.xml.impl.RecipeData;
import org.l2jdevs.gameserver.data.xml.impl.SecondaryAuthData;
import org.l2jdevs.gameserver.data.xml.impl.SiegeScheduleData;
import org.l2jdevs.gameserver.data.xml.impl.SkillLearnData;
import org.l2jdevs.gameserver.data.xml.impl.SkillTreesData;
import org.l2jdevs.gameserver.data.xml.impl.StaticObjectData;
import org.l2jdevs.gameserver.data.xml.impl.TransformData;
import org.l2jdevs.gameserver.data.xml.impl.UIData;
import org.l2jdevs.gameserver.datatables.AugmentationData;
import org.l2jdevs.gameserver.datatables.BotReportTable;
import org.l2jdevs.gameserver.datatables.EventDroplist;
import org.l2jdevs.gameserver.datatables.ItemTable;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.datatables.MerchantPriceConfigTable;
import org.l2jdevs.gameserver.datatables.SkillData;
import org.l2jdevs.gameserver.datatables.SpawnTable;
import org.l2jdevs.gameserver.handler.EffectHandler;
import org.l2jdevs.gameserver.idfactory.IdFactory;
import org.l2jdevs.gameserver.instancemanager.AirShipManager;
import org.l2jdevs.gameserver.instancemanager.AntiFeedManager;
import org.l2jdevs.gameserver.instancemanager.AuctionManager;
import org.l2jdevs.gameserver.instancemanager.BoatManager;
import org.l2jdevs.gameserver.instancemanager.CHSiegeManager;
import org.l2jdevs.gameserver.instancemanager.CastleManager;
import org.l2jdevs.gameserver.instancemanager.CastleManorManager;
import org.l2jdevs.gameserver.instancemanager.ClanHallManager;
import org.l2jdevs.gameserver.instancemanager.CoupleManager;
import org.l2jdevs.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jdevs.gameserver.instancemanager.DayNightSpawnManager;
import org.l2jdevs.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jdevs.gameserver.instancemanager.FortManager;
import org.l2jdevs.gameserver.instancemanager.FortSiegeManager;
import org.l2jdevs.gameserver.instancemanager.FourSepulchersManager;
import org.l2jdevs.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jdevs.gameserver.instancemanager.GraciaSeedsManager;
import org.l2jdevs.gameserver.instancemanager.GrandBossManager;
import org.l2jdevs.gameserver.instancemanager.InstanceManager;
import org.l2jdevs.gameserver.instancemanager.ItemAuctionManager;
import org.l2jdevs.gameserver.instancemanager.ItemsOnGroundManager;
import org.l2jdevs.gameserver.instancemanager.MailManager;
import org.l2jdevs.gameserver.instancemanager.MapRegionManager;
import org.l2jdevs.gameserver.instancemanager.MercTicketManager;
import org.l2jdevs.gameserver.instancemanager.PetitionManager;
import org.l2jdevs.gameserver.instancemanager.PunishmentManager;
import org.l2jdevs.gameserver.instancemanager.QuestManager;
import org.l2jdevs.gameserver.instancemanager.RaidBossPointsManager;
import org.l2jdevs.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2jdevs.gameserver.instancemanager.SiegeManager;
import org.l2jdevs.gameserver.instancemanager.TerritoryWarManager;
import org.l2jdevs.gameserver.instancemanager.WalkingManager;
import org.l2jdevs.gameserver.instancemanager.ZoneManager;
import org.l2jdevs.gameserver.model.AutoSpawnHandler;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.PartyMatchRoomList;
import org.l2jdevs.gameserver.model.PartyMatchWaitingList;
import org.l2jdevs.gameserver.model.entity.Hero;
import org.l2jdevs.gameserver.model.entity.TvTManager;
import org.l2jdevs.gameserver.model.events.EventDispatcher;
import org.l2jdevs.gameserver.model.olympiad.Olympiad;
import org.l2jdevs.gameserver.network.L2GameClient;
import org.l2jdevs.gameserver.network.L2GamePacketHandler;
import org.l2jdevs.gameserver.pathfinding.PathFinding;
import org.l2jdevs.gameserver.script.faenor.FaenorScriptEngine;
import org.l2jdevs.gameserver.scripting.ScriptEngineManager;
import org.l2jdevs.gameserver.taskmanager.KnownListUpdateTaskManager;
import org.l2jdevs.gameserver.taskmanager.TaskManager;
import org.l2jdevs.mmocore.SelectorConfig;
import org.l2jdevs.mmocore.SelectorThread;
import org.l2jdevs.status.Status;
import org.l2jdevs.util.DeadLockDetector;
import org.l2jdevs.util.IPv4Filter;
import org.l2jdevs.util.Util;

public final class GameServer
{
	private static final Logger LOG = LoggerFactory.getLogger(GameServer.class);
	private static final String LOG_FOLDER = "log"; // Name of folder for log file
	private static final String LOG_NAME = "./log.cfg"; // Name of log file
	private static final String DATAPACK = "-dp";
	private static final String GEODATA = "-gd";
	
	private final SelectorThread<L2GameClient> _selectorThread;
	private final L2GamePacketHandler _gamePacketHandler;
	private final DeadLockDetector _deadDetectThread;
	public static GameServer gameServer;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();
	
	public GameServer() throws Exception
	{
		long serverLoadStart = System.currentTimeMillis();
		
		LOG.info("{}: Used memory: {}MB.", getClass().getSimpleName(), getUsedMemoryMB());
		
		if (!IdFactory.getInstance().isInitialized())
		{
			LOG.error("{}: Could not read object IDs from database. Please check your configuration.", getClass().getSimpleName());
			throw new Exception("Could not initialize the ID factory!");
		}
		
		ThreadPoolManager.getInstance();
		EventDispatcher.getInstance();
		
		new File("log/game").mkdirs();
		
		ScriptEngineManager.getInstance();
		
		printSection("World");
		// start game time control early
		GameTimeController.init();
		InstanceManager.getInstance();
		L2World.getInstance();
		MapRegionManager.getInstance();
		AnnouncementsTable.getInstance();
		GlobalVariablesManager.getInstance();
		
		printSection("Data");
		CategoryData.getInstance();
		SecondaryAuthData.getInstance();
		
		printSection("Effects");
		EffectHandler.getInstance().executeScript();
		printSection("Enchant Skill Groups");
		EnchantSkillGroupsData.getInstance();
		printSection("Skill Trees");
		SkillTreesData.getInstance();
		printSection("Skills");
		SkillData.getInstance();
		SummonSkillsTable.getInstance();
		
		printSection("Items");
		ItemTable.getInstance();
		EnchantItemGroupsData.getInstance();
		EnchantItemData.getInstance();
		EnchantItemOptionsData.getInstance();
		OptionData.getInstance();
		EnchantItemHPBonusData.getInstance();
		MerchantPriceConfigTable.getInstance().loadInstances();
		BuyListData.getInstance();
		MultisellData.getInstance();
		RecipeData.getInstance();
		ArmorSetsData.getInstance();
		FishData.getInstance();
		FishingMonstersData.getInstance();
		FishingRodsData.getInstance();
		HennaData.getInstance();
		
		printSection("Characters");
		ClassListData.getInstance();
		InitialEquipmentData.getInstance();
		InitialShortcutData.getInstance();
		ExperienceData.getInstance();
		PlayerXpPercentLostData.getInstance();
		KarmaData.getInstance();
		HitConditionBonusData.getInstance();
		PlayerTemplateData.getInstance();
		PlayerCreationPointData.getInstance();
		CharNameTable.getInstance();
		AdminData.getInstance();
		RaidBossPointsManager.getInstance();
		PetDataTable.getInstance();
		CharSummonTable.getInstance().init();
		
		// Multi-Language System
		printSection("Languages");
		LanguageData.getInstance();
		
		printSection("Clans");
		ClanTable.getInstance();
		CHSiegeManager.getInstance();
		ClanHallManager.getInstance();
		AuctionManager.getInstance();
		
		printSection("Geodata");
		GeoData.getInstance();
		
		if (Config.PATHFINDING > 0)
		{
			PathFinding.getInstance();
		}
		
		printSection("NPCs");
		SkillLearnData.getInstance();
		NpcData.getInstance();
		WalkingManager.getInstance();
		StaticObjectData.getInstance();
		ZoneManager.getInstance();
		DoorData.getInstance();
		CastleManager.getInstance().loadInstances();
		NpcBufferTable.getInstance();
		GrandBossManager.getInstance().initZones();
		EventDroplist.getInstance();
		printSection("Auction Manager");
		ItemAuctionManager.getInstance();
		
		printSection("Olympiad");
		Olympiad.getInstance();
		Hero.getInstance();
		
		printSection("Seven Signs");
		SevenSigns.getInstance();
		
		// Call to load caches
		printSection("Cache");
		HtmCache.getInstance();
		CrestTable.getInstance();
		TeleportLocationTable.getInstance();
		UIData.getInstance();
		PartyMatchWaitingList.getInstance();
		PartyMatchRoomList.getInstance();
		PetitionManager.getInstance();
		AugmentationData.getInstance();
		CursedWeaponsManager.getInstance();
		TransformData.getInstance();
		BotReportTable.getInstance();
		
		printSection("Scripts");
		QuestManager.getInstance();
		BoatManager.getInstance();
		AirShipManager.getInstance();
		GraciaSeedsManager.getInstance();
		
		ScriptEngineManager.getInstance().executeScriptList(new File(Config.DATAPACK_ROOT, "data/scripts.cfg"));
		
		SpawnTable.getInstance().load();
		DayNightSpawnManager.getInstance().trim().notifyChangeMode();
		FourSepulchersManager.getInstance().init();
		DimensionalRiftManager.getInstance();
		RaidBossSpawnManager.getInstance();
		
		printSection("Siege");
		SiegeManager.getInstance().getSieges();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().loadInstances();
		FortManager.getInstance().activateInstances();
		FortSiegeManager.getInstance();
		SiegeScheduleData.getInstance();
		
		MerchantPriceConfigTable.getInstance().updateReferences();
		TerritoryWarManager.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();
		
		QuestManager.getInstance().report();
		
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance();
		}
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) || (Config.HERB_AUTO_DESTROY_TIME > 0))
		{
			ItemsAutoDestroy.getInstance();
		}
		
		MonsterRace.getInstance();
		
		SevenSigns.getInstance().spawnSevenSignsNPC();
		SevenSignsFestival.getInstance();
		AutoSpawnHandler.getInstance();
		
		FaenorScriptEngine.getInstance();
		// Init of a cursed weapon manager
		
		LOG.info("AutoSpawnHandler: Loaded {} handlers in total.", AutoSpawnHandler.getInstance().size());
		
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			CoupleManager.getInstance();
		}
		
		TaskManager.getInstance();
		
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.GAME_ID);
		
		if (Config.ALLOW_MAIL)
		{
			MailManager.getInstance();
		}
		
		PunishmentManager.getInstance();
		
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		LOG.info("IdFactory: Free ObjectID's remaining: {}", IdFactory.getInstance().size());
		
		TvTManager.getInstance();
		KnownListUpdateTaskManager.getInstance();
		
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
		{
			OfflineTradersTable.getInstance().restoreOfflineTraders();
		}
		
		if (Config.DEADLOCK_DETECTOR)
		{
			_deadDetectThread = new DeadLockDetector();
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		}
		else
		{
			_deadDetectThread = null;
		}
		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of
		// the current allocation pool, freeMemory the unused memory in the allocation pool
		long freeMem = ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		LOG.info("{}: Started, free memory {} Mb of {} Mb", getClass().getSimpleName(), freeMem, totalMem);
		Toolkit.getDefaultToolkit().beep();
		LoginServerThread.getInstance().start();
		
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		sc.TCP_NODELAY = Config.MMO_TCP_NODELAY;
		
		_gamePacketHandler = new L2GamePacketHandler();
		_selectorThread = new SelectorThread<>(sc, _gamePacketHandler, _gamePacketHandler, _gamePacketHandler, new IPv4Filter());
		
		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch (UnknownHostException e1)
			{
				LOG.error("{}: The GameServer bind address is invalid, using all avaliable IPs!", getClass().getSimpleName(), e1);
			}
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
			_selectorThread.start();
			LOG.info("{}: is now listening on: {}:{}", getClass().getSimpleName(), Config.GAMESERVER_HOSTNAME, Config.PORT_GAME);
		}
		catch (IOException e)
		{
			LOG.error("{}: Failed to open server socket!", getClass().getSimpleName(), e);
			System.exit(1);
		}
		
		LOG.info("{}: Maximum numbers of connected players: {}", getClass().getSimpleName(), Config.MAXIMUM_ONLINE_USERS);
		LOG.info("{}: Server loaded in {} seconds.", getClass().getSimpleName(), TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - serverLoadStart));
		
		printSection("UPnP");
		UPnPService.getInstance();
	}
	
	public static void main(String[] args) throws Exception
	{
		Server.serverMode = Server.MODE_GAMESERVER;
		
		// Initialize configurations.
		Config.load();
		
		final String dp = Util.parseArg(args, DATAPACK, true);
		if (dp != null)
		{
			Config.DATAPACK_ROOT = new File(dp);
		}
		
		final String gd = Util.parseArg(args, GEODATA, true);
		if (gd != null)
		{
			Config.GEODATA_PATH = Paths.get(gd);
		}
		
		// Create log folder
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File(LOG_NAME)))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		
		printSection("Database");
		DAOFactory.getInstance();
		ConnectionFactory.getInstance();
		
		gameServer = new GameServer();
		
		if (Config.IS_TELNET_ENABLED)
		{
			new Status(Server.serverMode).start();
		}
		else
		{
			LOG.info("{}: Telnet server is currently disabled.", GameServer.class.getSimpleName());
		}
	}
	
	public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
	}
	
	public SelectorThread<L2GameClient> getSelectorThread()
	{
		return _selectorThread;
	}
	
	public L2GamePacketHandler getL2GamePacketHandler()
	{
		return _gamePacketHandler;
	}
	
	public DeadLockDetector getDeadLockDetectorThread()
	{
		return _deadDetectThread;
	}
	
	public static void printSection(String s)
	{
		s = "=[ " + s + " ]";
		while (s.length() < 61)
		{
			s = "-" + s;
		}
		LOG.info(s);
	}
}
