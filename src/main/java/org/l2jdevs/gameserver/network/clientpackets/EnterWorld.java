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

import java.util.Base64;
import java.util.Calendar;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.GameTimeController;
import org.l2jdevs.gameserver.LoginServerThread;
import org.l2jdevs.gameserver.SevenSigns;
import org.l2jdevs.gameserver.cache.HtmCache;
import org.l2jdevs.gameserver.data.sql.impl.AnnouncementsTable;
import org.l2jdevs.gameserver.data.xml.impl.AdminData;
import org.l2jdevs.gameserver.data.xml.impl.SkillTreesData;
import org.l2jdevs.gameserver.datatables.LanguageData;
import org.l2jdevs.gameserver.datatables.SkillData;
import org.l2jdevs.gameserver.instancemanager.CHSiegeManager;
import org.l2jdevs.gameserver.instancemanager.CastleManager;
import org.l2jdevs.gameserver.instancemanager.ClanHallManager;
import org.l2jdevs.gameserver.instancemanager.CoupleManager;
import org.l2jdevs.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jdevs.gameserver.instancemanager.DimensionalRiftManager;
import org.l2jdevs.gameserver.instancemanager.FortManager;
import org.l2jdevs.gameserver.instancemanager.FortSiegeManager;
import org.l2jdevs.gameserver.instancemanager.InstanceManager;
import org.l2jdevs.gameserver.instancemanager.MailManager;
import org.l2jdevs.gameserver.instancemanager.PetitionManager;
import org.l2jdevs.gameserver.instancemanager.SiegeManager;
import org.l2jdevs.gameserver.instancemanager.TerritoryWarManager;
import org.l2jdevs.gameserver.model.L2Clan;
import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.L2World;
import org.l2jdevs.gameserver.model.PcCondOverride;
import org.l2jdevs.gameserver.model.TeleportWhereType;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.entity.Couple;
import org.l2jdevs.gameserver.model.entity.Fort;
import org.l2jdevs.gameserver.model.entity.FortSiege;
import org.l2jdevs.gameserver.model.entity.L2Event;
import org.l2jdevs.gameserver.model.entity.Siege;
import org.l2jdevs.gameserver.model.entity.TvTEvent;
import org.l2jdevs.gameserver.model.entity.clanhall.AuctionableHall;
import org.l2jdevs.gameserver.model.entity.clanhall.SiegableHall;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.model.quest.Quest;
import org.l2jdevs.gameserver.model.skills.CommonSkill;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.zone.ZoneId;
import org.l2jdevs.gameserver.network.L2GameClient.GameClientState;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.Die;
import org.l2jdevs.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jdevs.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jdevs.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jdevs.gameserver.network.serverpackets.ExNoticePostArrived;
import org.l2jdevs.gameserver.network.serverpackets.ExNotifyPremiumItem;
import org.l2jdevs.gameserver.network.serverpackets.ExShowContactList;
import org.l2jdevs.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jdevs.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jdevs.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jdevs.gameserver.network.serverpackets.FriendList;
import org.l2jdevs.gameserver.network.serverpackets.HennaInfo;
import org.l2jdevs.gameserver.network.serverpackets.ItemList;
import org.l2jdevs.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jdevs.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jdevs.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jdevs.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jdevs.gameserver.network.serverpackets.PledgeStatusChanged;
import org.l2jdevs.gameserver.network.serverpackets.QuestList;
import org.l2jdevs.gameserver.network.serverpackets.ShortCutInit;
import org.l2jdevs.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev87 bddddbdcccccccccccccccccccc
 * <p>
 */
public class EnterWorld extends L2GameClientPacket
{
	private static final String _C__11_ENTERWORLD = "[C] 11 EnterWorld";
	
	private static final double MIN_HP = 0.5;
	
	private static final int COMBAT_FLAG = 9819;
	
	private final int[][] tracert = new int[5][4];
	
	@Override
	protected void readImpl()
	{
		readB(new byte[32]); // Unknown Byte Array
		readD(); // Unknown Value
		readD(); // Unknown Value
		readD(); // Unknown Value
		readD(); // Unknown Value
		readB(new byte[32]); // Unknown Byte Array
		readD(); // Unknown Value
		for (int i = 0; i < 5; i++)
		{
			for (int o = 0; o < 4; o++)
			{
				tracert[i][o] = readC();
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getActiveChar();
		if (activeChar == null)
		{
			_log.warning("EnterWorld failed! activeChar returned 'null'.");
			getClient().closeNow();
			return;
		}
		
		final String[] address = new String[5];
		for (int i = 0; i < 5; i++)
		{
			address[i] = tracert[i][0] + "." + tracert[i][1] + "." + tracert[i][2] + "." + tracert[i][3];
		}
		
		LoginServerThread.getInstance().sendClientTracert(activeChar.getAccountName(), address);
		
		getClient().setClientTracert(tracert);
		
		// Load Language of player
		final String lang = activeChar.getLang();
		LanguageData.getInstance().setPlayerLang(activeChar, lang);
		
		// Restore to instanced area if enabled
		if (Config.RESTORE_PLAYER_INSTANCE)
		{
			activeChar.setInstanceId(InstanceManager.getInstance().getPlayerInstance(activeChar.getObjectId()));
		}
		else
		{
			int instanceId = InstanceManager.getInstance().getPlayerInstance(activeChar.getObjectId());
			if (instanceId > 0)
			{
				InstanceManager.getInstance().getInstance(instanceId).removePlayer(activeChar.getObjectId());
			}
		}
		
		if (Config.DEBUG)
		{
			if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
			{
				_log.warning("User already exists in Object ID map! User " + activeChar.getName() + " is a character clone.");
			}
		}
		
		// Get client state of player
		getClient().setState(GameClientState.IN_GAME);
		
		// Apply special GM properties to the GM when entering
		if (activeChar.isGM())
		{
			if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel()))
			{
				activeChar.setIsInvul(true);
			}
			
			if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", activeChar.getAccessLevel()))
			{
				activeChar.setInvisible(true);
			}
			
			if (Config.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
			{
				activeChar.setSilenceMode(true);
			}
			
			if (Config.GM_STARTUP_DIET_MODE && AdminData.getInstance().hasAccess("admin_diet", activeChar.getAccessLevel()))
			{
				activeChar.setDietMode(true);
				activeChar.refreshOverloaded();
			}
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmliston", activeChar.getAccessLevel()))
			{
				AdminData.getInstance().addGm(activeChar, false);
			}
			else
			{
				AdminData.getInstance().addGm(activeChar, true);
			}
			
			if (Config.GM_GIVE_SPECIAL_SKILLS)
			{
				SkillTreesData.getInstance().addSkills(activeChar, false);
			}
			
			if (Config.GM_GIVE_SPECIAL_AURA_SKILLS)
			{
				SkillTreesData.getInstance().addSkills(activeChar, true);
			}
		}
		
		// Set dead status if applies
		if (activeChar.getCurrentHp() < MIN_HP)
		{
			activeChar.setIsDead(true);
		}
		
		boolean showClanNotice = false;
		
		// Clan related checks are here
		final L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			activeChar.sendPacket(new PledgeSkillList(clan));
			
			notifyClanMembers(activeChar);
			
			notifySponsorOrApprentice(activeChar);
			
			final AuctionableHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
				{
					activeChar.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				}
			}
			
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(clan))
				{
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(siege.getCastle().getResidenceId());
				}
				else if (siege.checkIsDefender(clan))
				{
					activeChar.setSiegeState((byte) 2);
					activeChar.setSiegeSide(siege.getCastle().getResidenceId());
				}
			}
			
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(clan))
				{
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(siege.getFort().getResidenceId());
				}
				else if (siege.checkIsDefender(clan))
				{
					activeChar.setSiegeState((byte) 2);
					activeChar.setSiegeSide(siege.getFort().getResidenceId());
				}
			}
			
			for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
			{
				if (!hall.isInSiege())
				{
					continue;
				}
				
				if (hall.isRegistered(clan))
				{
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(hall.getId());
					activeChar.setIsInHideoutSiege(true);
				}
			}
			
			sendPacket(new PledgeShowMemberListAll(clan, activeChar));
			sendPacket(new PledgeStatusChanged(clan));
			
			// Residential skills support
			if (clan.getCastleId() > 0)
			{
				CastleManager.getInstance().getCastleByOwner(clan).giveResidentialSkills(activeChar);
			}
			
			if (clan.getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(clan).giveResidentialSkills(activeChar);
			}
			
			showClanNotice = clan.isNoticeEnabled();
		}
		
		if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(activeChar) > 0)
		{
			if (TerritoryWarManager.getInstance().isTWInProgress())
			{
				activeChar.setSiegeState((byte) 1);
			}
			activeChar.setSiegeSide(TerritoryWarManager.getInstance().getRegisteredTerritoryId(activeChar));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL))
		{
			int cabal = SevenSigns.getInstance().getPlayerCabal(activeChar.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL)
			{
				if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				{
					activeChar.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
				}
				else
				{
					activeChar.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
				}
			}
		}
		else
		{
			activeChar.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
			activeChar.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}
		
		if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT)
		{
			float points = (Config.RATE_RECOVERY_ON_RECONNECT * (System.currentTimeMillis() - activeChar.getLastAccess())) / 60000;
			if (points > 0)
			{
				activeChar.updateVitalityPoints(points, false, true);
			}
		}
		
		activeChar.checkRecoBonusTask();
		
		activeChar.broadcastUserInfo();
		
		// Send Macro List
		activeChar.getMacros().sendUpdate();
		
		// Send Item List
		sendPacket(new ItemList(activeChar, false));
		
		// Send GG check
		activeChar.queryGameGuard();
		
		// Send Teleport Bookmark List
		sendPacket(new ExGetBookMarkInfoPacket(activeChar));
		
		// Send Shortcuts
		sendPacket(new ShortCutInit(activeChar));
		
		// Send Action list
		activeChar.sendPacket(ExBasicActionList.STATIC_PACKET);
		
		// Send Skill list
		activeChar.sendSkillList();
		
		// Apply Dye
		activeChar.recalcHennaStats();
		
		// Send Dye Information
		activeChar.sendPacket(new HennaInfo(activeChar));
		
		Quest.playerEnter(activeChar);
		
		activeChar.sendPacket(new QuestList());
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			activeChar.setProtection(true);
		}
		
		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		
		activeChar.getInventory().applyItemSkills();
		
		if (L2Event.isParticipant(activeChar))
		{
			L2Event.restorePlayerEventStatus(activeChar);
		}
		
		// Wedding Checks
		if (Config.L2JMOD_ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
		}
		
		if (activeChar.isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().getCursedWeapon(activeChar.getCursedWeaponEquippedId()).cursedOnLogin();
		}
		
		activeChar.updateEffectIcons();
		
		activeChar.sendPacket(new EtcStatusUpdate(activeChar));
		
		// Expand Skill
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));
		
		sendPacket(new FriendList(activeChar));
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
		sm.addCharName(activeChar);
		
		if (activeChar.hasFriends())
		{
			for (int id : activeChar.getFriends())
			{
				final L2Object obj = L2World.getInstance().findObject(id);
				if (obj != null)
				{
					obj.sendPacket(sm);
				}
			}
		}
		
		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		
		if (Config.FOUNDERS_AND_TEAM_LICENSE_MESSAGE_ENABLED)
		{
			final int actualYear = Calendar.getInstance().get(Calendar.YEAR);
			final int totalYears = actualYear - 2004;
			activeChar.sendMessage(getText("VGhpcyBTZXJ2ZXIgdXNlcyBMMkpEZXZzLCBhIFByb2plY3QgZm91bmRlZCBieSBTd2FybG9n"));
			activeChar.sendMessage(getText("YW5kIGRldmVsb3BlZCBieSBMMkpEZXZzIFRlYW0gYXQgd3d3LmwyamRldnMuY29t"));
			activeChar.sendMessage(getText("Q29weXJpZ2h0IDIwMDQt") + actualYear);
			activeChar.sendMessage(getText("VGhhbmsgeW91IGZvciA=") + totalYears + getText("IHllYXJzIQ=="));
		}
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		AnnouncementsTable.getInstance().showAnnouncements(activeChar);
		
		if (showClanNotice)
		{
			final NpcHtmlMessage notice = new NpcHtmlMessage();
			notice.setFile(activeChar.getHtmlPrefix(), "data/html/clanNotice.htm");
			notice.replace("%clan_name%", activeChar.getClan().getName());
			notice.replace("%notice_text%", activeChar.getClan().getNotice());
			notice.disableValidation();
			sendPacket(notice);
		}
		else if (Config.SERVER_NEWS)
		{
			String serverNews = HtmCache.getInstance().getHtm(activeChar.getHtmlPrefix(), "data/html/servnews.htm");
			if (serverNews != null)
			{
				sendPacket(new NpcHtmlMessage(serverNews));
			}
		}
		
		if (Config.PETITIONING_ALLOWED)
		{
			PetitionManager.getInstance().checkPetitionMessages(activeChar);
		}
		
		if (activeChar.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to others
			sendPacket(new Die(activeChar));
		}
		
		activeChar.onPlayerEnter();
		
		sendPacket(new SkillCoolTime(activeChar));
		sendPacket(new ExVoteSystemInfo(activeChar));
		sendPacket(new ExShowContactList(activeChar));
		
		for (L2ItemInstance item : activeChar.getInventory().getItems())
		{
			if (item.isTimeLimitedItem())
			{
				item.scheduleLifeTimeTask();
			}
			if (item.isShadowItem() && item.isEquipped())
			{
				item.decreaseMana(false);
			}
		}
		
		for (L2ItemInstance whItem : activeChar.getWarehouse().getItems())
		{
			if (whItem.isTimeLimitedItem())
			{
				whItem.scheduleLifeTimeTask();
			}
		}
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
		}
		
		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		}
		
		// remove combat flag before teleporting
		final L2ItemInstance combatFlag = activeChar.getInventory().getItemByItemId(COMBAT_FLAG);
		if (combatFlag != null)
		{
			final Fort fort = FortManager.getInstance().getFort(activeChar);
			if (fort != null)
			{
				FortSiegeManager.getInstance().dropCombatFlag(activeChar, fort.getResidenceId());
			}
			else
			{
				final int slot = activeChar.getInventory().getSlotFromItem(combatFlag);
				activeChar.getInventory().unEquipItemInBodySlot(slot);
				activeChar.destroyItem("CombatFlag", combatFlag, null, true);
			}
		}
		
		// Attacker or spectator logging in to a siege zone.
		// Actually should be checked for inside castle only?
		if (!activeChar.canOverrideCond(PcCondOverride.ZONE_CONDITIONS) && activeChar.isInsideZone(ZoneId.SIEGE) && (!activeChar.isInSiege() || (activeChar.getSiegeState() < 2)))
		{
			activeChar.teleToLocation(TeleportWhereType.TOWN);
		}
		
		if (Config.ALLOW_MAIL)
		{
			if (MailManager.getInstance().hasUnreadPost(activeChar))
			{
				sendPacket(ExNoticePostArrived.valueOf(false));
			}
		}
		
		TvTEvent.onLogin(activeChar);
		
		if (Config.WELCOME_MESSAGE_ENABLED)
		{
			activeChar.sendPacket(new ExShowScreenMessage(Config.WELCOME_MESSAGE_TEXT, Config.WELCOME_MESSAGE_TIME));
		}
		
		final int birthday = activeChar.checkBirthDay();
		if (birthday == 0)
		{
			activeChar.sendPacket(SystemMessageId.YOUR_BIRTHDAY_GIFT_HAS_ARRIVED);
			// activeChar.sendPacket(new ExBirthdayPopup()); Removed in H5?
		}
		else if (birthday != -1)
		{
			final SystemMessage sm1 = SystemMessage.getSystemMessage(SystemMessageId.THERE_ARE_S1_DAYS_UNTIL_YOUR_CHARACTERS_BIRTHDAY);
			sm1.addInt(birthday);
			activeChar.sendPacket(sm1);
		}
		
		if (!activeChar.getPremiumItemList().isEmpty())
		{
			activeChar.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}
		
		if (activeChar.getRace().ordinal() == 2)
		{
			final Skill skill = SkillData.getInstance().getSkill(294, 1); // Shadow Sense level 1
			if ((skill != null) && (activeChar.getSkillLevel(294) == 1))
			{
				if (GameTimeController.getInstance().isNight())
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(skill));
					activeChar.updateAndBroadcastStatus(2);
				}
				else
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.IT_IS_DOWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(skill));
					activeChar.updateAndBroadcastStatus(2);
				}
			}
		}
		
		// Unstuck players that had client open when server crashed.
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void engage(L2PcInstance cha)
	{
		final int chaId = cha.getObjectId();
		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if ((cl.getPlayer1Id() == chaId) || (cl.getPlayer2Id() == chaId))
			{
				if (cl.getMaried())
				{
					cha.setMarried(true);
				}
				
				cha.setCoupleId(cl.getId());
				
				if (cl.getPlayer1Id() == chaId)
				{
					cha.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					cha.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}
	
	/**
	 * @param cha
	 * @param partnerId
	 */
	private void notifyPartner(L2PcInstance cha, int partnerId)
	{
		final L2PcInstance partner = L2World.getInstance().getPlayer(cha.getPartnerId());
		if (partner != null)
		{
			partner.sendMessage(LanguageData.getInstance().getMsg(partner, "enter_with_partner"));
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void notifyClanMembers(L2PcInstance activeChar)
	{
		final L2Clan clan = activeChar.getClan();
		if (clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			
			final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
			msg.addString(activeChar.getName());
			clan.broadcastToOtherOnlineMembers(msg, activeChar);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}
	
	/**
	 * @param activeChar
	 */
	private void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if (activeChar.getSponsor() != 0)
		{
			final L2PcInstance sponsor = L2World.getInstance().getPlayer(activeChar.getSponsor());
			if (sponsor != null)
			{
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				sponsor.sendPacket(msg);
			}
		}
		else if (activeChar.getApprentice() != 0)
		{
			final L2PcInstance apprentice = L2World.getInstance().getPlayer(activeChar.getApprentice());
			if (apprentice != null)
			{
				final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				apprentice.sendPacket(msg);
			}
		}
	}
	
	/**
	 * @param string
	 * @return
	 */
	private String getText(String string)
	{
		return new String(Base64.getDecoder().decode(string));
	}
	
	@Override
	public String getType()
	{
		return _C__11_ENTERWORLD;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
