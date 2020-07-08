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
package org.l2jdevs.gameserver.model.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.lang.Math;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.ThreadPoolManager;
import org.l2jdevs.gameserver.ai.CtrlEvent;
import org.l2jdevs.gameserver.ai.CtrlIntention;
import org.l2jdevs.gameserver.ai.L2AttackableAI;
import org.l2jdevs.gameserver.ai.L2CharacterAI;
import org.l2jdevs.gameserver.ai.L2FortSiegeGuardAI;
import org.l2jdevs.gameserver.ai.L2SiegeGuardAI;
import org.l2jdevs.gameserver.datatables.EventDroplist;
import org.l2jdevs.gameserver.datatables.EventDroplist.DateDrop;
import org.l2jdevs.gameserver.datatables.ItemTable;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.instancemanager.CursedWeaponsManager;
import org.l2jdevs.gameserver.instancemanager.WalkingManager;
import org.l2jdevs.gameserver.model.AbsorberInfo;
import org.l2jdevs.gameserver.model.AggroInfo;
import org.l2jdevs.gameserver.model.DamageDoneInfo;
import org.l2jdevs.gameserver.model.L2CommandChannel;
import org.l2jdevs.gameserver.model.L2Object;
import org.l2jdevs.gameserver.model.L2Party;
import org.l2jdevs.gameserver.model.L2Seed;
import org.l2jdevs.gameserver.model.actor.instance.L2GrandBossInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2ServitorInstance;
import org.l2jdevs.gameserver.model.actor.knownlist.AttackableKnownList;
import org.l2jdevs.gameserver.model.actor.status.AttackableStatus;
import org.l2jdevs.gameserver.model.actor.tasks.attackable.CommandChannelTimer;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2jdevs.gameserver.model.drops.DropListScope;
import org.l2jdevs.gameserver.model.events.EventDispatcher;
import org.l2jdevs.gameserver.model.events.impl.character.npc.attackable.OnAttackableAggroRangeEnter;
import org.l2jdevs.gameserver.model.events.impl.character.npc.attackable.OnAttackableAttack;
import org.l2jdevs.gameserver.model.events.impl.character.npc.attackable.OnAttackableKill;
import org.l2jdevs.gameserver.model.holders.ItemHolder;
import org.l2jdevs.gameserver.model.items.L2Item;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.zone.ZoneId;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.clientpackets.Say2;
import org.l2jdevs.gameserver.network.serverpackets.CreatureSay;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;
import org.l2jdevs.gameserver.taskmanager.DecayTaskManager;
import org.l2jdevs.gameserver.util.Util;
import org.l2jdevs.util.Rnd;
import org.l2jdevs.gameserver.model.items.L2Weapon;

import org.l2jdevs.util.MarkovNameGen;
import org.l2jdevs.roguelike.L2RogueLike;

public class L2Attackable extends L2Npc
{
	private static final Logger LOG = LoggerFactory.getLogger(L2Attackable.class);
        // https://www.w3schools.com/colors/colors_names.asp
        // fixme! colors are ... different
        private static final int NAME_COLOR_RED = 0xFF0000, 
            NAME_COLOR_ORANGERED = 0xFF4500,
            NAME_COLOR_CRIMSON = 0XDC143C,
            NAME_COLOR_GOLD = 0xFFD700,
            NAME_COLOR_LEMON_YELLOW = 0x97F8FC,
            NAME_COLOR_DEFAULT = 0x0;

        // Raid
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;
	//
	//private boolean _champion = false;
	private final Map<L2Character, AggroInfo> _aggroList = new ConcurrentHashMap<>();
	private boolean _isReturningToSpawnPoint = false;
	private boolean _canReturnToSpawnPoint = true;
	private boolean _seeThroughSilentMove = false;
	// Manor
	private boolean _seeded = false;
	private L2Seed _seed = null;
	private int _seederObjId = 0;
	private final AtomicReference<ItemHolder> _harvestItem = new AtomicReference<>();
	// Spoil
	private int _spoilerObjectId;
	private final AtomicReference<Collection<ItemHolder>> _sweepItems = new AtomicReference<>();
	// Over-hit
	private boolean _overhit;
	private double _overhitDamage;
	private L2Character _overhitAttacker;
	// Command channel
	private volatile L2CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;
	// Soul crystal
	private boolean _absorbed;
	private final Map<Integer, AbsorberInfo> _absorbersList = new ConcurrentHashMap<>();
	// Misc
	private boolean _mustGiveExpSp;
	protected int _onKillDelay = 5000;

    // L2JMod Championhood and around
	protected static final int NAME_LENGTH_MIN = 4;
	protected static final float CHAMPION_POWER_MIN = 0.2F; // (1+CHAMPION_POWER_MIN), actually
	protected static final int _nameColor = NAME_COLOR_DEFAULT;
	protected boolean _champion = false;
	protected float _scaleXP = 1, // scale factor in XP/SP/... calculations [CHAMPION_POWER_MIN,1)
		_scaleHP = 1, // scale factor of HP [1,Config.L2JMOD_CHAMPION_HP]
		_powerJitter = 1; // personal mob non-HP/XP scale factor (0,maxInt]
	protected int _aggro;
	protected String _name = ""; // nameless mob
	// protected hp, attack_speed, damage, xp, sp, ...; // and so on
	
	/**
	 * Creates an attackable NPC.
	 * @param template the attackable NPC template
	 */
	public L2Attackable(L2NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.L2Attackable);
		setIsInvul(false);
		_mustGiveExpSp = true;
                _powerJitter = getRNGPowerJitter();
                _scaleXP = _powerJitter;
                _scaleHP = _powerJitter;
                _aggro = getRandomAggroLevel(template, _powerJitter);
	}
	
	@Override
	public AttackableKnownList getKnownList()
	{
		return (AttackableKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new AttackableKnownList(this));
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	@Override
	protected L2CharacterAI initAI()
	{
		return new L2AttackableAI(this);
	}
	
	public final Map<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setisReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	public final boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}
	
	public final void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}
	
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	public void setSeeThroughSilentMove(boolean val)
	{
		_seeThroughSilentMove = val;
	}
	
	/**
	 * Use the skill if minimum checks are pass.
	 * @param skill the skill
	 */
	public void useMagic(Skill skill)
	{
		if ((skill == null) || isAlikeDead() || skill.isPassive() || isCastingNow() || isSkillDisabled(skill))
		{
			return;
		}
		
		if ((getCurrentMp() < (getStat().getMpConsume1(skill) + getStat().getMpConsume2(skill))) || (getCurrentHp() <= skill.getHpConsume()))
		{
			return;
		}
		
		if (!skill.isStatic())
		{
			if (skill.isMagic())
			{
				if (isMuted())
				{
					return;
				}
			}
			else
			{
				if (isPhysicalMuted())
				{
					return;
				}
			}
		}
		
		final L2Object target = skill.getFirstOfTargetList(this);
		if (target != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		}
	}
	
	/**
	 * Reduce the current HP of the L2Attackable.
	 * @param damage The HP decrease value
	 * @param attacker The L2Character who attacks
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}
	
	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.
	 * @param damage The HP decrease value
	 * @param attacker The L2Character who attacks
	 * @param awake The awake state (If True : stop sleeping)
	 * @param isDOT
	 * @param skill
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (isRaid() && !isMinion() && (attacker != null) && (attacker.getParty() != null) && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
		{
			if (_firstCommandChannelAttacked == null) // looting right isn't set
			{
				synchronized (this)
				{
					if (_firstCommandChannelAttacked == null)
					{
						_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
						if (_firstCommandChannelAttacked != null)
						{
							_commandChannelTimer = new CommandChannelTimer(this);
							_commandChannelLastAttack = System.currentTimeMillis();
							ThreadPoolManager.getInstance().scheduleGeneral(_commandChannelTimer, 10000); // check for last attack
							_firstCommandChannelAttacked.broadcastPacket(new CreatureSay(0, Say2.PARTYROOM_ALL, "", "You have looting rights!")); // TODO: retail msg
						}
					}
				}
			}
			else if (attacker.getParty().getCommandChannel().equals(_firstCommandChannelAttacked)) // is in same channel
			{
				_commandChannelLastAttack = System.currentTimeMillis(); // update last attack time
			}
		}
		
		if (isEventMob())
		{
			return;
		}
		
		// Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList
		if (attacker != null)
		{
			addDamage(attacker, (int) damage, skill);
		}
		
		// If this L2Attackable is a L2MonsterInstance and it has spawned minions, call its minions to battle
		if (this instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) this;
			
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
		}
		// Reduce the current HP of the L2Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}
	
	/**
	 * Kill the L2Attackable (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<br>
	 * Actions:<br>
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members<br>
	 * Notify the Quest Engine of the L2Attackable death if necessary.<br>
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds)<br>
	 * Caution: This method DOESN'T GIVE rewards to L2PetInstance.
	 * @param killer The L2Character that has killed the L2Attackable
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2NpcInstance (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if ((killer != null) && killer.isPlayable())
		{
			// Delayed notification
			EventDispatcher.getInstance().notifyEventAsyncDelayed(new OnAttackableKill(killer.getActingPlayer(), this, killer.isSummon()), this, _onKillDelay);
		}
		
		// Notify to minions if there are.
		if (isMonster())
		{
			final L2MonsterInstance mob = (L2MonsterInstance) this;
			if ((mob.getLeader() != null) && mob.getLeader().hasMinions())
			{
				final int respawnTime = Config.MINIONS_RESPAWN_TIME.containsKey(getId()) ? Config.MINIONS_RESPAWN_TIME.get(getId()) * 1000 : -1;
				mob.getLeader().getMinionList().onMinionDie(mob, respawnTime);
			}
			
			if (mob.hasMinions())
			{
				mob.getMinionList().onMasterDie(false);
			}
		}
		return true;
	}
	
	/**
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members.<br>
	 * Actions:<br>
	 * Get the L2PcInstance owner of the L2ServitorInstance (if necessary) and L2Party in progress.<br>
	 * Calculate the Experience and SP rewards in function of the level difference.<br>
	 * Add Exp and SP rewards to L2PcInstance (including Summon penalty) and to Party members in the known area of the last attacker.<br>
	 * Caution : This method DOESN'T GIVE rewards to L2PetInstance.
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	@Override
	protected void calculateRewards(L2Character lastAttacker)
	{
		try
		{
			if (_aggroList.isEmpty())
			{
				return;
			}
			
			// NOTE: Concurrent-safe map is used because while iterating to verify all conditions sometimes an entry must be removed.
			final Map<L2PcInstance, DamageDoneInfo> rewards = new ConcurrentHashMap<>();
			
			L2PcInstance maxDealer = null;
			int maxDamage = 0;
			long totalDamage = 0;
			// While Iterating over This Map Removing Object is Not Allowed
			// Go through the _aggroList of the L2Attackable
			for (AggroInfo info : _aggroList.values())
			{
				if (info == null)
				{
					continue;
				}
				
				// Get the L2Character corresponding to this attacker
				final L2PcInstance attacker = info.getAttacker().getActingPlayer();
				if (attacker != null)
				{
					// Get damages done by this attacker
					final int damage = info.getDamage();
					
					// Prevent unwanted behavior
					if (damage > 1)
					{
						// Check if damage dealer isn't too far from this (killed monster)
						if (!Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, attacker, true))
						{
							continue;
						}
						
						totalDamage += damage;
						
						// Calculate real damages (Summoners should get own damage plus summon's damage)
						final DamageDoneInfo reward = rewards.computeIfAbsent(attacker, DamageDoneInfo::new);
						reward.addDamage(damage);
						
						if (reward.getDamage() > maxDamage)
						{
							maxDealer = attacker;
							maxDamage = reward.getDamage();
						}
					}
				}
			}
			
			// Manage Base, Quests and Sweep drops of the L2Attackable
			doItemDrop((maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker);
			
			// Manage drop of Special Events created by GM for a defined period
			doEventDrop(lastAttacker);
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				for (DamageDoneInfo reward : rewards.values())
				{
					if (reward == null)
					{
						continue;
					}
					
					// Attacker to be rewarded
					final L2PcInstance attacker = reward.getAttacker();
					
					// Total amount of damage done
					final int damage = reward.getDamage();
					
					// Get party
					final L2Party attackerParty = attacker.getParty();
					
					// Penalty applied to the attacker's XP
					// If this attacker have servitor, get Exp Penalty applied for the servitor.
					final float penalty = attacker.hasServitor() ? ((L2ServitorInstance) attacker.getSummon()).getExpMultiplier() : 1;
					
					// If there's NO party in progress
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards
						if (attacker.getKnownList().knowsObject(this))
						{
							// Calculate the difference of level between this attacker (player or servitor owner) and the L2Attackable
							// mob = 24, atk = 10, diff = -14 (full xp)
							// mob = 24, atk = 28, diff = 4 (some xp)
							// mob = 24, atk = 50, diff = 26 (no xp)
							final int levelDiff = attacker.getLevel() - getLevel();
							
							final int[] expSp = calculateExpAndSp(levelDiff, damage, totalDamage);
							long exp = expSp[0];
							int sp = expSp[1];
							
							if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
							{
                                                            exp *= getPowerMultiplier(Config.L2JMOD_CHAMPION_REWARDS_EXP_SP);
                                                            sp *= getPowerMultiplier(Config.L2JMOD_CHAMPION_REWARDS_EXP_SP);
							}
                                                        else if(Config.L2JMOD_NPC_POWER_JITTER_ENABLE) {
                                                            exp *= _powerJitter;
                                                            sp *= _powerJitter;
                                                        }

							exp *= penalty;
							
							// Check for an over-hit enabled strike
							L2Character overhitAttacker = getOverhitAttacker();
							if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
							{
								attacker.sendPacket(SystemMessageId.OVER_HIT);
								exp += calculateOverhitExp(exp);
							}
							
							// Distribute the Exp and SP between the L2PcInstance and its L2Summon
							if (!attacker.isDead())
							{
								attacker.addExpAndSp(exp, sp, useVitalityRate());
								if (exp > 0)
								{
									if (!attacker.isInsideZone(ZoneId.PEACE) && ((attacker.getLevel() - getLevel()) <= 9))
									{
										attacker.getNevitSystem().startAdventTask();
										attacker.getNevitSystem().checkIfMustGivePoints(exp, this);
										
										attacker.updateVitalityPoints(getVitalityPoints(damage), true, false);
									}
								}
							}
						}
					}
					else
					{
						// share with party members
						int partyDmg = 0;
						float partyMul = 1;
						int partyLvl = 0;
						
						// Get all L2Character that can be rewarded in the party
						final List<L2PcInstance> rewardedMembers = new ArrayList<>();
						// Go through all L2PcInstance in the party
						final List<L2PcInstance> groupMembers = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers();
						for (L2PcInstance partyPlayer : groupMembers)
						{
							if ((partyPlayer == null) || partyPlayer.isDead())
							{
								continue;
							}
							
							// Get the RewardInfo of this L2PcInstance from L2Attackable rewards
							final DamageDoneInfo reward2 = rewards.get(partyPlayer);
							
							// If the L2PcInstance is in the L2Attackable rewards add its damages to party damages
							if (reward2 != null)
							{
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, partyPlayer, true))
								{
									partyDmg += reward2.getDamage(); // Add L2PcInstance damages to party damages
									rewardedMembers.add(partyPlayer);
									
									if (partyPlayer.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = partyPlayer.getLevel();
										}
									}
								}
								rewards.remove(partyPlayer); // Remove the L2PcInstance from the L2Attackable rewards
							}
							else
							{
								// Add L2PcInstance of the party (that have attacked or not) to members that can be rewarded
								// and in range of the monster.
								if (Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, partyPlayer, true))
								{
									rewardedMembers.add(partyPlayer);
									if (partyPlayer.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = partyPlayer.getLevel();
										}
									}
								}
							}
						}
						
						// If the party didn't killed this L2Attackable alone
						if (partyDmg < totalDamage)
						{
							partyMul = ((float) partyDmg / totalDamage);
						}
						
						// Calculate the level difference between Party and L2Attackable
						final int levelDiff = partyLvl - getLevel();
						
						// Calculate Exp and SP rewards
						final int[] expSp = calculateExpAndSp(levelDiff, partyDmg, totalDamage);
						long exp = expSp[0];
						int sp = expSp[1];

						if (Config.L2JMOD_CHAMPION_ENABLE && isChampion())
						{
                                                    exp *= getPowerMultiplier(Config.L2JMOD_CHAMPION_REWARDS_EXP_SP);
                                                    sp *= getPowerMultiplier(Config.L2JMOD_CHAMPION_REWARDS_EXP_SP);
						}
                                                else if(Config.L2JMOD_NPC_POWER_JITTER_ENABLE) {
                                                    exp *= _powerJitter;
                                                    sp *= _powerJitter;
                                                }

						exp *= partyMul;
						sp *= partyMul;
						
						// Check for an over-hit enabled strike
						// (When in party, the over-hit exp bonus is given to the whole party and splitted proportionally through the party members)
						L2Character overhitAttacker = getOverhitAttacker();
						if (isOverhit() && (overhitAttacker != null) && (overhitAttacker.getActingPlayer() != null) && (attacker == overhitAttacker.getActingPlayer()))
						{
							attacker.sendPacket(SystemMessageId.OVER_HIT);
							exp += calculateOverhitExp(exp);
						}
						
						// Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last attacker
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, partyDmg, this);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("{}", e);
		}
	}
	
	@Override
	public void addAttackerToAttackByList(L2Character player)
	{
		if ((player == null) || (player == this) || getAttackByList().contains(player))
		{
			return;
		}
		getAttackByList().add(player);
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 * @param skill
	 */
	public void addDamage(L2Character attacker, int damage, Skill skill)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Notify the L2Attackable AI with EVT_ATTACKED
		if (!isDead())
		{
			try
			{
				// If monster is on walk - stop it
				if (isWalker() && !isCoreAIDisabled() && WalkingManager.getInstance().isOnWalk(this))
				{
					WalkingManager.getInstance().stopMoving(this, false, true);
				}
				
				getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
				addDamageHate(attacker, damage, (damage * 100) / (getLevel() + 7));
				
				final L2PcInstance player = attacker.getActingPlayer();
				if (player != null)
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(player, this, damage, skill, attacker.isSummon()), this);
				}
			}
			catch (Exception e)
			{
				LOG.error("{}", e);
			}
		}
	}
	
	/**
	 * Adds damage and hate to the attacker aggression list for this character.
	 * @param attacker The L2Character that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker L2Character
	 * @param aggro The hate (=damage) given by the attacker L2Character
	 */
	public void addDamageHate(L2Character attacker, int damage, long aggro)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Get the AggroInfo of the attacker L2Character from the _aggroList of the L2Attackable
		final AggroInfo ai = _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		ai.addDamage(damage);
		
		// Traps does not cause aggro
		// making this hack because not possible to determine if damage made by trap
		// so just check for triggered trap here
		final L2PcInstance targetPlayer = attacker.getActingPlayer();
		if ((targetPlayer == null) || (targetPlayer.getTrap() == null) || !targetPlayer.getTrap().isTriggered())
		{
			ai.addHate(aggro);
		}
		
		if ((targetPlayer != null) && (aggro == 0))
		{
			addDamageHate(attacker, 0, 1);
			
			// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
			if (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}
			
			// Notify to scripts
			EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAggroRangeEnter(this, targetPlayer, attacker.isSummon()), this);
		}
		else if ((targetPlayer == null) && (aggro == 0))
		{
			aggro = 1;
			ai.addHate(1);
		}
		
		// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
		if ((aggro != 0) && (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
	
	public void reduceHate(L2Character target, long amount)
	{
		if ((getAI() instanceof L2SiegeGuardAI) || (getAI() instanceof L2FortSiegeGuardAI))
		{
			// TODO: this just prevents error until siege guards are handled properly
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return;
		}
		
		if (target == null) // whole aggrolist
		{
			L2Character mostHated = getMostHated();
			if (mostHated == null) // makes target passive for a moment more
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (AggroInfo ai : _aggroList.values())
			{
				ai.addHate(amount);
			}
			
			amount = getHating(mostHated);
			if (amount >= 0)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
			return;
		}
		
		AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			if (Config.DEBUG)
			{
				LOG.info("Target {} not present in aggro list of {}.", target, this);
			}
			return;
		}
		
		ai.addHate(amount);
		if ((ai.getHate() >= 0) && (getMostHated() == null))
		{
			((L2AttackableAI) getAI()).setGlobalAggro(-25);
			clearAggroList();
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			setWalking();
		}
	}
	
	/**
	 * Clears _aggroList hate of the L2Character without removing from the list.
	 * @param target
	 */
	public void stopHating(L2Character target)
	{
		if (target == null)
		{
			return;
		}
		AggroInfo ai = _aggroList.get(target);
		if (ai != null)
		{
			ai.stopHate();
		}
	}
	
	/**
	 * @return the most hated L2Character of the L2Attackable _aggroList.
	 */
	public L2Character getMostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		L2Character mostHated = null;
		long maxHate = 0;
		
		// While Interacting over This Map Removing Object is Not Allowed
		// Go through the aggroList of the L2Attackable
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		return mostHated;
	}
	
	/**
	 * @return the 2 most hated L2Character of the L2Attackable _aggroList.
	 */
	public List<L2Character> get2MostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		L2Character mostHated = null;
		L2Character secondMostHated = null;
		long maxHate = 0;
		List<L2Character> result = new ArrayList<>();
		
		// While iterating over this map removing objects is not allowed
		// Go through the aggroList of the L2Attackable
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				secondMostHated = mostHated;
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		result.add(mostHated);
		
		if (getAttackByList().contains(secondMostHated))
		{
			result.add(secondMostHated);
		}
		else
		{
			result.add(null);
		}
		return result;
	}
	
	public List<L2Character> getHateList()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		List<L2Character> result = new ArrayList<>();
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			ai.checkHate(this);
			
			result.add(ai.getAttacker());
		}
		return result;
	}
	
	/**
	 * @param target The L2Character whose hate level must be returned
	 * @return the hate level of the L2Attackable against this L2Character contained in _aggroList.
	 */
	public long getHating(final L2Character target)
	{
		if (_aggroList.isEmpty() || (target == null))
		{
			return 0;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return 0;
		}
		
		if (ai.getAttacker() instanceof L2PcInstance)
		{
			L2PcInstance act = (L2PcInstance) ai.getAttacker();
			if (act.isInvisible() || ai.getAttacker().isInvul() || act.isSpawnProtected())
			{
				// Remove Object Should Use This Method and Can be Blocked While Interacting
				_aggroList.remove(target);
				return 0;
			}
		}
		
		if (!ai.getAttacker().isVisible() || ai.getAttacker().isInvisible())
		{
			_aggroList.remove(target);
			return 0;
		}
		
		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		return ai.getHate();
	}
	
	public void doItemDrop(L2Character mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}
	
	/**
	 * Manage Base, Quests and Special Events drops of L2Attackable (called by calculateRewards).<br>
	 * Concept:<br>
	 * During a Special Event all L2Attackable can drop extra Items.<br>
	 * Those extra Items are defined in the table allNpcDateDrops of the EventDroplist.<br>
	 * Each Special Event has a start and end date to stop to drop extra Items automatically.<br>
	 * Actions:<br>
	 * Manage drop of Special Events created by GM for a defined period.<br>
	 * Get all possible drops of this L2Attackable from L2NpcTemplate and add it Quest drops.<br>
	 * For each possible drops (base + quests), calculate which one must be dropped (random).<br>
	 * Get each Item quantity dropped (random).<br>
	 * Create this or these L2ItemInstance corresponding to each Item Identifier dropped.<br>
	 * If the autoLoot mode is actif and if the L2Character that has killed the L2Attackable is a L2PcInstance, Give the item(s) to the L2PcInstance that has killed the L2Attackable.<br>
	 * If the autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these item(s) in the world as a visible object at the position where mob was last.
	 * @param npcTemplate
	 * @param mainDamageDealer
	 */
	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character mainDamageDealer)
	{
		if (mainDamageDealer == null)
		{
			return;
		}
		
		L2PcInstance player = mainDamageDealer.getActingPlayer();
		
		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		if (player == null)
		{
			return;
		}
		
		CursedWeaponsManager.getInstance().checkDrop(this, player);
		
		if (isSpoiled())
		{
			_sweepItems.set(npcTemplate.calculateDrops(DropListScope.CORPSE, this, player));
		}
		
		Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropListScope.DEATH, this, player);
		if (deathItems != null)
		{
			for (ItemHolder drop : deathItems)
			{
				L2Item item = ItemTable.getInstance().getTemplate(drop.getId());
				// Check if the autoLoot mode is active
				if (isFlying() || (!item.hasExImmediateEffect() && ((!isRaid() && Config.AUTO_LOOT) || (isRaid() && Config.AUTO_LOOT_RAIDS))) || (item.hasExImmediateEffect() && Config.AUTO_LOOT_HERBS))
				{
					player.doAutoLoot(this, drop); // Give the item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, drop); // drop the item on the ground
				}
				
				// Broadcast message if RaidBoss was defeated
				if (isRaid() && !isRaidMinion() && (drop.getCount() > 0))
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DIED_DROPPED_S3_S2);
					sm.addCharName(this);
					sm.addItemName(item);
					sm.addLong(drop.getCount());
					broadcastPacket(sm);
				}
			}
		}
                if (Config.L2JMOD_CHAMPION_ENABLE)
                    doItemDropChampion(player);
                if (Config.L2JMOD_ROGUELIKE_DROP)
                    L2RogueLike.doItemDropNethack(this, npcTemplate, player);
	}

    /**
     * Apply Special Item drop with random(rnd) quantity(qty) for champions.
     */
    private void doItemDropChampion(final L2PcInstance player) {
        if (isChampion()
            && (Config.L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE > 0
                || Config.L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE > 0)) {
            int champqty = Rnd.get(Config.L2JMOD_CHAMPION_REWARD_QTY);
            ItemHolder item = new ItemHolder
                (Config.L2JMOD_CHAMPION_REWARD_ID, ++champqty);
            int rng = Rnd.get(100);
            if (player.getLevel() <= getLevel()
                && (rng < Config.L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE))
                giveChampionLoot(player, item, champqty);
            else
            if (player.getLevel() > getLevel()
                && (rng < Config.L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE))
                giveChampionLoot(player, item, champqty);
        }
    }

    /**
     * Give the item(s) to the L2PcInstance that has killed the L2Attackable
     */
    private void giveChampionLoot (final L2PcInstance player,
                                   final ItemHolder item, final int champqty) {
        if (Config.AUTO_LOOT || isFlying())
            player.addItem("ChampionLoot", item.getId(),
                           item.getCount(), this, true);
        else
            dropItem(player, item);
    }

	/**
	 * Manage Special Events drops created by GM for a defined period.<br>
	 * Concept:<br>
	 * During a Special Event all L2Attackable can drop extra Items.<br>
	 * Those extra Items are defined in the table allNpcDateDrops of the EventDroplist.<br>
	 * Each Special Event has a start and end date to stop to drop extra Items automatically.<br>
	 * Actions: <I>If an extra drop must be generated</I><br>
	 * Get an Item Identifier (random) from the DateDrop Item table of this Event.<br>
	 * Get the Item quantity dropped (random).<br>
	 * Create this or these L2ItemInstance corresponding to this Item Identifier.<br>
	 * If the autoLoot mode is actif and if the L2Character that has killed the L2Attackable is a L2PcInstance, Give the item(s) to the L2PcInstance that has killed the L2Attackable<br>
	 * If the autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these item(s) in the world as a visible object at the position where mob was last
	 * @param lastAttacker The L2Character that has killed the L2Attackable
	 */
	public void doEventDrop(L2Character lastAttacker)
	{
		if (lastAttacker == null)
		{
			return;
		}
		
		L2PcInstance player = lastAttacker.getActingPlayer();
		
		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		if (player == null)
		{
			return;
		}
		
		if ((player.getLevel() - getLevel()) > 9)
		{
			return;
		}
		
		// Go through DateDrop of EventDroplist allNpcDateDrops within the date range
		for (DateDrop drop : EventDroplist.getInstance().getAllDrops())
		{
			if (Rnd.get(1000000) < drop.getEventDrop().getDropChance())
			{
				final int itemId = drop.getEventDrop().getItemIdList()[Rnd.get(drop.getEventDrop().getItemIdList().length)];
				final long itemCount = Rnd.get(drop.getEventDrop().getMinCount(), drop.getEventDrop().getMaxCount());
				if (Config.AUTO_LOOT || isFlying())
				{
					player.doAutoLoot(this, itemId, itemCount); // Give the item(s) to the L2PcInstance that has killed the L2Attackable
				}
				else
				{
					dropItem(player, itemId, itemCount); // drop the item on the ground
				}
			}
		}
	}
	
	/**
	 * @return the active weapon of this L2Attackable (= null).
	 */
	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Verifies if the creature is in the aggro list.
	 * @param creature the creature
	 * @return {@code true} if the creature is in the aggro list, {@code false} otherwise
	 */
	public boolean isInAggroList(L2Character creature)
	{
		return _aggroList.containsKey(creature);
	}
	
	/**
	 * Clear the _aggroList of the L2Attackable.
	 */
	public void clearAggroList()
	{
		_aggroList.clear();
		
		// clear overhit values
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	@Override
	public boolean isSweepActive()
	{
		return _sweepItems.get() != null;
	}
	
	/**
	 * @return a copy of dummy items for the spoil loot.
	 */
	public List<L2Item> getSpoilLootItems()
	{
		final Collection<ItemHolder> sweepItems = _sweepItems.get();
		final List<L2Item> lootItems = new LinkedList<>();
		if (sweepItems != null)
		{
			for (ItemHolder item : sweepItems)
			{
				lootItems.add(ItemTable.getInstance().getTemplate(item.getId()));
			}
		}
		return lootItems;
	}
	
	/**
	 * @return table containing all L2ItemInstance that can be spoiled.
	 */
	public Collection<ItemHolder> takeSweep()
	{
		return _sweepItems.getAndSet(null);
	}
	
	/**
	 * @return table containing all L2ItemInstance that can be harvested.
	 */
	public ItemHolder takeHarvest()
	{
		return _harvestItem.getAndSet(null);
	}
	
	/**
	 * Checks if the corpse is too old.
	 * @param attacker the player to validate
	 * @param remainingTime the time to check
	 * @param sendMessage if {@code true} will send a message of corpse too old
	 * @return {@code true} if the corpse is too old
	 */
	public boolean isOldCorpse(L2PcInstance attacker, int remainingTime, boolean sendMessage)
	{
		if (isDead() && (DecayTaskManager.getInstance().getRemainingTime(this) < remainingTime))
		{
			if (sendMessage && (attacker != null))
			{
				attacker.sendPacket(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @param sweeper the player to validate.
	 * @param sendMessage sendMessage if {@code true} will send a message of sweep not allowed.
	 * @return {@code true} if is the spoiler or is in the spoiler party.
	 */
	public boolean checkSpoilOwner(L2PcInstance sweeper, boolean sendMessage)
	{
		if ((sweeper.getObjectId() != getSpoilerObjectId()) && !sweeper.isInLooterParty(getSpoilerObjectId()))
		{
			if (sendMessage)
			{
				sweeper.sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Set the over-hit flag on the L2Attackable.
	 * @param status The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Set the over-hit values like the attacker who did the strike and the amount of damage done by the skill.
	 * @param attacker The L2Character who hit on the L2Attackable using the over-hit enabled skill
	 * @param damage The amount of damage done by the over-hit enabled skill on the L2Attackable
	 */
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		double overhitDmg = -(getCurrentHp() - damage);
		if (overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike)
			// let's just clear all the over-hit related values
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Return the L2Character who hit on the L2Attackable using an over-hit enabled skill.
	 * @return L2Character attacker
	 */
	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Return the amount of damage done on the L2Attackable using an over-hit enabled skill.
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * @return True if the L2Attackable was hit by an over-hit enabled skill.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Activate the absorbed soul condition on the L2Attackable.
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}
	
	/**
	 * @return True if the L2Attackable had his soul absorbed.
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}
	
	/**
	 * Adds an attacker that successfully absorbed the soul of this L2Attackable into the _absorbersList.
	 * @param attacker
	 */
	public void addAbsorber(L2PcInstance attacker)
	{
		// If we have no _absorbersList initiated, do it
		final AbsorberInfo ai = _absorbersList.get(attacker.getObjectId());
		
		// If the L2Character attacker isn't already in the _absorbersList of this L2Attackable, add it
		if (ai == null)
		{
			_absorbersList.put(attacker.getObjectId(), new AbsorberInfo(attacker.getObjectId(), getCurrentHp()));
		}
		else
		{
			ai.setAbsorbedHp(getCurrentHp());
		}
		
		// Set this L2Attackable as absorbed
		absorbSoul();
	}
	
	public void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}
	
	public Map<Integer, AbsorberInfo> getAbsorbersList()
	{
		return _absorbersList;
	}
	
	/**
	 * Calculate the Experience and SP to distribute to attacker (L2PcInstance, L2ServitorInstance or L2Party) of the L2Attackable.
	 * @param diff The difference of level between attacker (L2PcInstance, L2ServitorInstance or L2Party) and the L2Attackable
	 * @param damage The damages given by the attacker (L2PcInstance, L2ServitorInstance or L2Party)
	 * @param totalDamage The total damage done
	 * @return
	 */
	private int[] calculateExpAndSp(int diff, int damage, long totalDamage)
	{
		double xp;
		double sp;
		
		if (diff < -5)
		{
			diff = -5; // makes possible to use ALT_GAME_EXPONENT configuration
		}
		
		xp = ((double) getExpReward() * damage) / totalDamage;
		if (Config.ALT_GAME_EXPONENT_XP != 0)
		{
			xp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_XP);
		}
		
		sp = ((double) getSpReward() * damage) / totalDamage;
		if (Config.ALT_GAME_EXPONENT_SP != 0)
		{
			sp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_SP);
		}
		
		if ((Config.ALT_GAME_EXPONENT_XP == 0) && (Config.ALT_GAME_EXPONENT_SP == 0))
		{
			if (diff > 5) // formula revised May 07
			{
				double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = xp * pow;
				sp = sp * pow;
			}
			
			if (xp <= 0)
			{
				xp = 0;
				sp = 0;
			}
			else if (sp <= 0)
			{
				sp = 0;
			}
		}
		int[] tmp =
		{
			(int) xp,
			(int) sp
		};
		return tmp;
	}
	
	public long calculateOverhitExp(long normalExp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) ammount of HP on the L2Attackable
		double overhitPercentage = ((getOverhitDamage() * 100) / getMaxHp());
		
		// Over-hit damage percentages are limited to 25% max
		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}
		
		// Get the overhit exp bonus according to the above over-hit damage percentage
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and so on...)
		double overhitExp = ((overhitPercentage / 100) * normalExp);
		
		// Return the rounded ammount of exp points to be added to the player's normal exp reward
		long bonusOverhit = Math.round(overhitExp);
		return bonusOverhit;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		// Clear mob spoil, seed
		setSpoilerObjectId(0);
		// Clear all aggro char from list
		clearAggroList();
		// Clear Harvester reward
		_harvestItem.set(null);
		// Clear mod Seeded stat
		_seeded = false;
		_seed = null;
		_seederObjId = 0;
		// Clear overhit value
		overhitEnabled(false);
		
		_sweepItems.set(null);
		resetAbsorbList();
		
		setWalking();
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		if (!isInActiveRegion())
		{
			if (hasAI())
			{
				getAI().stopAITask();
			}
		}
	}
	
	/**
	 * Checks if its spoiled.
	 * @return {@code true} if its spoiled, {@code false} otherwise
	 */
	public final boolean isSpoiled()
	{
		return _spoilerObjectId != 0;
	}
	
	/**
	 * Gets the spoiler object id.
	 * @return the spoiler object id if its spoiled, 0 otherwise
	 */
	public final int getSpoilerObjectId()
	{
		return _spoilerObjectId;
	}
	
	/**
	 * Sets the spoiler object id.
	 * @param spoilerObjectId the spoiler object id
	 */
	public final void setSpoilerObjectId(int spoilerObjectId)
	{
		_spoilerObjectId = spoilerObjectId;
	}
	
	/**
	 * Sets state of the mob to seeded. Parameters needed to be set before.
	 * @param seeder
	 */
	public final void setSeeded(L2PcInstance seeder)
	{
		if ((_seed != null) && (_seederObjId == seeder.getObjectId()))
		{
			_seeded = true;
			
			int count = 1;
			for (int skillId : getTemplate().getSkills().keySet())
			{
				switch (skillId)
				{
					case 4303: // Strong type x2
						count *= 2;
						break;
					case 4304: // Strong type x3
						count *= 3;
						break;
					case 4305: // Strong type x4
						count *= 4;
						break;
					case 4306: // Strong type x5
						count *= 5;
						break;
					case 4307: // Strong type x6
						count *= 6;
						break;
					case 4308: // Strong type x7
						count *= 7;
						break;
					case 4309: // Strong type x8
						count *= 8;
						break;
					case 4310: // Strong type x9
						count *= 9;
						break;
				}
			}
			
			// hi-lvl mobs bonus
			final int diff = getLevel() - _seed.getLevel() - 5;
			if (diff > 0)
			{
				count += diff;
			}
			_harvestItem.set(new ItemHolder(_seed.getCropId(), count * Config.RATE_DROP_MANOR));
		}
	}
	
	/**
	 * Sets the seed parameters, but not the seed state
	 * @param seed - instance {@link L2Seed} of used seed
	 * @param seeder - player who sows the seed
	 */
	public final void setSeeded(L2Seed seed, L2PcInstance seeder)
	{
		if (!_seeded)
		{
			_seed = seed;
			_seederObjId = seeder.getObjectId();
		}
	}
	
	public final int getSeederId()
	{
		return _seederObjId;
	}
	
	public final L2Seed getSeed()
	{
		return _seed;
	}
	
	public final boolean isSeeded()
	{
		return _seeded;
	}
	
	/**
	 * Set delay for onKill() call, in ms Default: 5000 ms
	 * @param delay
	 */
	public final void setOnKillDelay(int delay)
	{
		_onKillDelay = delay;
	}
	
	public final int getOnKillDelay()
	{
		return _onKillDelay;
	}
	
	/**
	 * Check if the server allows Random Animation.
	 */
	// This is located here because L2Monster and L2FriendlyMob both extend this class. The other non-pc instances extend either L2NpcInstance or L2MonsterInstance.
	@Override
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_MONSTER_ANIMATION > 0) && isRandomAnimationEnabled() && !(this instanceof L2GrandBossInstance));
	}
	
	@Override
	public boolean isMob()
	{
		return true; // This means we use MAX_MONSTER_ANIMATION instead of MAX_NPC_ANIMATION
	}
	
	public void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	public L2CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	public void setFirstCommandChannelAttacked(L2CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	/**
	 * @return the _commandChannelLastAttack
	 */
	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}
	
	/**
	 * @param channelLastAttack the _commandChannelLastAttack to set
	 */
	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}
	
	public void returnHome()
	{
		clearAggroList();
		
		if (hasAI() && (getSpawn() != null))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, getSpawn().getLocation(this));
		}
	}
	
	/*
	 * Return vitality points decrease (if positive) or increase (if negative) based on damage. Maximum for damage = maxHp.
	 */
	public float getVitalityPoints(int damage)
	{
		// sanity check
		if (damage <= 0)
		{
			return 0;
		}
		
		final float divider = (getLevel() > 0) && (getExpReward() > 0) ? (getTemplate().getBaseHpMax() * 9 * getLevel() * getLevel()) / (100 * getExpReward()) : 0;
		if (divider == 0)
		{
			return 0;
		}
		
		// negative value - vitality will be consumed
		return -Math.min(damage, getMaxHp()) / divider;
	}
	
	/*
	 * True if vitality rate for exp and sp should be applied
	 */
	public boolean useVitalityRate()
	{
		return isChampion() ? Config.L2JMOD_CHAMPION_ENABLE_VITALITY : true;
	}
	
	/** Return True if the L2Character is RaidBoss or his minion. */
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Set this Npc as a Raid instance.
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/**
	 * Set this Npc as a Minion instance.
	 * @param val
	 */
	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isRaidMinion = val;
	}
	
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}
	
	/**
	 * @return leader of this minion or null.
	 */
	public L2Attackable getLeader()
	{
		return null;
	}

    /**
     * champions are unique mobs.
     * They have personal inventory, stats, skills and so
     * nethack : src/makemon.c :
     * if ((int) mtmp->m_lev > rn2(50))
     *   (void) mongets(mtmp, rnd_defensive_item(mtmp));
     * if ((int) mtmp->m_lev > rn2(100))
     *   (void) mongets(mtmp, rnd_misc_item(mtmp));
     *
     * Control variables:
     * _powerJitter
     * _scaleXP
     * _scaleHP -- hp
     * _aggro
     */
    public void setChampion(boolean champ) {
        if (champ == _champion)
	    return;
        if (champ)
            doChampionSet();
        else
            doChampionUnset();
    }

    private void doChampionSet() {
        // LOG.info(String.format("L2J Champion: crowned. ObjectId %d", getId()));
        _champion = true;
        // also, look at cha.getTemplate().isUsingServerSideTitle() and isUsingServerSideNames
        // fixme! title regenerated always if not present in parent class? Look at cha.getTemplate().isUsingServerSideTitle()
        _name = Config.L2JMOD_CHAMPION_RANDNAMES
            ? (new MarkovNameGen(NAME_LENGTH_MIN)).getName() // it is singleton, so overhead is light (todo : be static)
            : Config.L2JMOD_CHAMP_TITLE; // On every subclass
        /*
          _nameColor = Config.L2JMOD_CHAMPION_COLOR_NAMES
          ? NAME_COLOR_GOLD
          : NAME_COLOR_DEFAULT; // clent default (green)
        */
        // _nameColor = NAME_COLOR_DEFAULT; // fixme! coloring?
        // computing champion power level between [1.25, L2JMOD_CHAMPION_HP]
        if (Config.L2JMOD_CHAMPION_HP > 1) {
            _scaleXP = evalChampionRateXP();
            _scaleHP = evalChampionRateHP(_scaleXP); // champion HP rating, in [1.25, L2JMOD_CHAMPION_HP]
            _powerJitter = evalChampionPowerJitter(_scaleXP);
        }
        /*
        // "weak" champions don't need any tweaks
        else {
            _powerJitter = getRNGPowerJitter();
            _scaleHP = _powerJitter;
            _scaleXP = _powerJitter;
        }
        */
        if(Config.L2JMOD_CHAMPION_PASSIVE)
            _aggro = Rnd.get(__AGGRO_PASSIVE);
        _aggro += (int)(_powerJitter * __AGGRO_BONUS_OF_CHAMPION);
    }

    private void doChampionUnset() {
        // LOG.info(String.format("L2J Champion: de-crowned. ObjectId %d", getId()));
        _champion = false;
        _name = "";
        _powerJitter = getRNGPowerJitter();
        _scaleXP = _powerJitter;
        _scaleHP = _powerJitter;
        _aggro = getRandomAggroLevel(getTemplate(), _powerJitter);
    }

    @Override
    /**
     * this is NPC's title, actually. At least, serverpacket wise
     */
    public String getChampionName() {
        return _name;
    }

    public int getChampionNameColor() {
        return _nameColor;
    }

    /**
     * actually, incoming damage is _divided_ by this value.
     * also used to scale drop chance and amount
     * also, see L2Character::reduceCurrentHp :
     *    if(getPower() > 1) dmg /= getPower();
     * also, L2RogueLike::doDropAmmo (and so on) :
     *    int n = (int)((4 + Rnd.get(npclvl)) * npc.getPower());
     */
    @Override
    public double getPower() {
        if(_champion)
            return _powerJitter;
        else
            return _powerJitter;
    }

    /**
     * actually, incoming damage is _divided_ by this value
     * also, see L2Character::reduceCurrentHp :
     *     dmg /= getMultiplierHP();
     * ... and Formulas::calcHpRegen :
     *     hpRegenMultiplier *= cha.getMultiplierHP();
     * @return _powerJitter if not champion,
     *     HP multiplier if champion
     */
    @Override
    public double getMultiplierHP() {
        if(_champion)
            return _scaleHP;
        else
            return _powerJitter;
    }

    /**
     * used for a common npc P/M attack speed / power related calculations.
     * ex: bonusAtk = _activeChar.getBonusAttackMultiplier();
     * @return [1,2) if isChampion
     *         powerJitter if not isChampion
     */
    @Override
    public double getAtkBonusMultiplier() {
        return getBonusMultiplier(Config.L2JMOD_CHAMPION_ATK);
    }

    /**
     * used for a champion's P/M attack speed / power related calculations.
     * ex: bonusAtk = _activeChar.getBonusAttackMultiplier(CHAMPION_ATK);
     * @return [0,n) if isChampion
     *         n * powerJitter if not isChampion
     */
    @Override
    public double getAtkBonusMultiplier(final double n) {
        return getPowerMultiplier(n);
    }

    /**
     * used for P/M attack speed related calculations
     * ex: bonusAtk = _activeChar.getSpdBonusMultiplier();
     */
    @Override
    public double getSpdBonusMultiplier() {
        return getBonusMultiplier(Config.L2JMOD_CHAMPION_SPD_ATK);
    }

    @Override
    public double getSpdBonusMultiplier(double n) {
        return getPowerMultiplier(n);
    }

    /**
     * underlying function for getXXXBonusMultiplier family
     */
    protected double getBonusMultiplier(final double n) {
        if(_champion) {
            if(n <= 1)
                return 1;
            return 1 + _scaleXP * (n - 1);
        }
        else
            return _powerJitter;
    }

    /**
     * Scales n according to mob's power rating (_champion ? _scaleXP*(n-1)+1 : _powerJitter)
     * Used to calculate attack bouses and so on,
     * something like this:
     * ex1 : exp *= getPowerMultiplier(Config.L2JMOD_CHAMPION_REWARDS_EXP_SP);
     * ex2 : hpRegenMultiplier *= cha.getPowerMultiplier(Config.L2JMOD_CHAMPION_HP_REGEN);
     * ex3 : bonusAtk = _activeChar.getPowerMultiplier(Config.L2JMOD_CHAMPION_ATK);
     * @return [1, Inf)
     */
    @Override
    public double getPowerMultiplier(final double n) {
        if(_champion)
            return n * _scaleXP;
        else
            return n * _powerJitter;
    }

    public float getPowerXP() {
        return _scaleXP;
    }

    public float getPowerJitter() {
        return _powerJitter;
    }

    public int getAggroLevel() {
        return _aggro;
    }
	
	@Override
	public boolean isChampion()
	{
		return _champion;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}

    /*
     * @param range of area around 1
     * @returns number from range (1-off, 1+off)
     */
    /*
    protected static float getRNGRange1(final float off) {
        return 1 - off + off*2*Rnd.get();
    }
    */

    protected static float getRNGPowerJitter() {
        if(Config.L2JMOD_NPC_POWER_JITTER_ENABLE)
            return Config.L2JMOD_NPC_POWER_JITTER_1M //
                + Config.L2JMOD_NPC_POWER_JITTER_X2 //
                * (float)(Rnd.get());
        return 1;
    }

    private final static int __AGGRO_RNG_MAX = L2RogueLike.AGGRO_RNG_MAX,
        __AGGRO_PASSIVE = L2RogueLike.AGGRO_PASSIVE,
        __AGGRO_AGGRESSIVE = L2RogueLike.AGGRO_AGGRESSIVE,
        __AGGRO_BONUS_OF_CHAMPION = L2RogueLike.AGGRO_BONUS_OF_CHAMPION;

    /**
     * evaluates aggro level for basic mobs
     */
    protected static int getRandomAggroLevel(final L2NpcTemplate template,
                                             final float power) {
        int agro;
        if(template.isAggressive())
            agro = __AGGRO_AGGRESSIVE //
                + Rnd.get(__AGGRO_RNG_MAX - __AGGRO_AGGRESSIVE);
        else
            agro = Rnd.get(__AGGRO_PASSIVE);
        return (int)(agro * power);
    }

    /**
     * gets random champion XP/SP partial multiplier.
     * @return [CHAMPION_POWER_MIN, 1.0)
     */
    protected static float evalChampionRateXP() {
        return Float.max((float)Rnd.get(), CHAMPION_POWER_MIN);
    }

    /**
     * evaluates champion HP multiplier, based on given scale.
     * HP, regeneration, drop.
     * For *attack & *speed -- see evalChampionPowerJitter()
     * @param scale [0,1)
     * @return (combat) stats scale factor, in [1, CHAMPION_HP)
     */
    protected static float evalChampionRateHP(final float scale) {
        if(scale <= 0)
            return 1;
        return scale * (Config.L2JMOD_CHAMPION_HP1M) + 1;
    }

    /**
     * evaluates champion combat related multiplier, based on given scale.
     * @param scale, in range of (0,1), also see evalChampionRateXP
     * @return (combat) stats scale factor
     */
    protected static float evalChampionPowerJitter(final float scale) {
        if(scale < 0 || scale > 1)
            return 1; // wat? should not be...
        return (float)(scale * Config.L2JMOD_CHAMPION_ATK) + 1;
    }
}
