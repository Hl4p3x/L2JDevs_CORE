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
package org.l2jdevs.roguelike;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.GameTimeController;
import org.l2jdevs.gameserver.ThreadPoolManager;
import org.l2jdevs.gameserver.ai.CtrlEvent;
import org.l2jdevs.gameserver.ai.CtrlIntention;
import org.l2jdevs.gameserver.ai.L2AttackableAI;
import org.l2jdevs.gameserver.ai.L2CharacterAI;
import org.l2jdevs.gameserver.ai.L2FortSiegeGuardAI;
import org.l2jdevs.gameserver.ai.L2SiegeGuardAI;
import org.l2jdevs.gameserver.datatables.EventDroplist.DateDrop;
import org.l2jdevs.gameserver.datatables.EventDroplist;
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
import org.l2jdevs.gameserver.model.StatsSet;
import org.l2jdevs.gameserver.model.actor.L2Attackable;
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
import org.l2jdevs.gameserver.model.items.L2Weapon;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.gameserver.model.quest.Quest;
import org.l2jdevs.gameserver.model.skills.Skill;
import org.l2jdevs.gameserver.model.zone.ZoneId;
import org.l2jdevs.gameserver.network.SystemMessageId;
import org.l2jdevs.gameserver.network.clientpackets.Say2;
import org.l2jdevs.gameserver.network.serverpackets.CreatureSay;
import org.l2jdevs.gameserver.network.serverpackets.SystemMessage;
import org.l2jdevs.gameserver.taskmanager.DecayTaskManager;
import org.l2jdevs.gameserver.util.Util;
import org.l2jdevs.util.Rnd;
import org.l2jdevs.gameserver.enums.Race;
import org.l2jdevs.gameserver.model.items.type.MaterialType;

import org.l2jdevs.util.MarkovNameGen;
import static org.l2jdevs.roguelike.L2RogueLikeDrops.*;

/**
 * implementation of Roguelike items drop mechanics.
 * entry point: doItemDropNethack,
 * everything other are private and static.
 * @author RKorskov
 *
 * L2RogueLike.doItemDropNethack() :
 * @param L2Attackable NPC killed by PC
 * @param L2NpcTemplate template of NPC
 * @param L2PcInstance PC killed NPC
 * @return void
 */

public final class L2RogueLike {
    private static final Logger LOG = //
        LoggerFactory.getLogger(L2RogueLike.class);

    private static GameTimeController SYSTEM_TIMER = //
        GameTimeController.getInstance();

    final static boolean __LOG_DROPS = false, // enable drop logging
        __USE_LOG10 = true, // used in item rating evaluation as price bias
        __USE_LOGE = false,
        __USE_LOG2 = false,
        FIX_ITEM_EVAL = true; // fixme! set to false to use sophisticated item and npc rating evaluation code

    private final static int ITEM_ID_ADENA = 57,
        DROP_ITEM_ID_DEFAULT = ITEM_ID_ADENA,
        FIND_DROP_ITEM_MAX_TRIES = 8, // used for finding appropriate drop item
        USE_LOWER_ITEM_RATING = 8; // if (n=rand(100)) < U.L.I.R. then lower item rating by 2*(ULIR-n)

    private L2RogueLike(){;}

    /**
     * RNG ranges for drop items
     */
    final static int RNG_FAVOR_RANGE = Config.L2JMOD_RNG_FAVOR_RANGE,
        RNG_BASE_CHANCE = Integer.max(Config.L2JMOD_RNG_BASE_CHANCE, Config.MAX_PLAYER_LEVEL),
        RNG_GIFTS_AMMO = (Config.L2JMOD_RNG_GIFTS_AMMO_MUL * Config.L2JMOD_RNG_FAVOR_RANGE) / Config.L2JMOD_RNG_GIFTS_AMMO_DIV;

    final static boolean EVAL_NG_STRATEGY_LOG10 = false; // use log10 or log in NG item rate by price evaluation
    final static int MAX_ITEM_PRICE = 237930000, // values taken from data
        MAX_NPC_LEVEL = 87;
    final static double NG_STRATEGY_LOG10_SCALE
        = MAX_NPC_LEVEL / Math.log(MAX_ITEM_PRICE);

    public static boolean isAnimal(final L2NpcTemplate npcT) {
        return npcT.getRace() == Race.ANIMAL //
            || npcT.getRace() == Race.BEAST //
            || npcT.getRace() == Race.BUG;
    }

    /**
     * all constructs, but not every construct (?)
     */
    public static boolean isGolem(final L2NpcTemplate npcT) {
        return npcT.getRace() == Race.CONSTRUCT;
    }

    public static boolean isDropExtraGems(final L2NpcTemplate npcT) {
        return npcT.getRace() == Race.BEAST //
            || npcT.getRace() == Race.CONSTRUCT;
    }

    /**
     * roguelike-style (nethack 3.6.1) extra item drop
     * nethack uses flat 30/100 chance
     * I use : chance = MAX_PLAYER_LEVEL + recom/4 + (npcLvl - pcLvl)*2
     * in : rand(1000) <= chance
     * rolled 0 means streak of luck and increased drop amounts.
     * Also, if PC got relatively lucky, mob may drop its weapon or armor.
     */
    public static void doItemDropNethack(final L2Attackable npc,
                                         final L2NpcTemplate npcT,
                                         final L2PcInstance player) {
        if (npcT == null || player == null)
            return;
        int rng = getRNG(npc);
        final boolean lucky = (rng == 0); // R.N.G. was ... generous today!
        int npclvl = npcT.getLevel();
        if(Config.L2JMOD_CHAMPION_ENABLE && npc.isChampion())
            npclvl += npc.getPowerMultiplier(20) + 4;
        final int pclvl = player.getBaseLevel(),
            recom = player.getRecomHave(),
            npcRating = evalNPCRating(npcT),
            chance = RNG_BASE_CHANCE + getDropBaseChance(recom, npclvl, pclvl);
        if(!isAnimal(npcT)) {
            if (rng < RNG_GIFTS_AMMO)
                doDropAmmo(npc, player, npcT.getRHandId(), lucky, npclvl);
            // probability of equipment drop checks already build-in
            doDropEquipment(npc, npcT, player, pclvl, recom, npclvl, lucky);
        }
        if(isDropExtraGems(npcT) // constructs, beasts may drop extra gems
           && rng < (chance * Config.L2JMOD_EXTRA_GEM_CHANCE_MULTIPLIER)) {
            int amount = getNethackDropAmount(npc, pclvl, recom, npclvl, lucky);
            doExtraGemsDrop(npc, player, amount,
                            lucky ? (npcRating + 16) : npcRating);
        }
        if(rng > chance) // no luck. sorry
            return;
        if(isAnimal(npcT)) {// animals don't have sshots and equipment
            doAnimalDropsWrapper(npc, player, npclvl, npcRating);
            return;
        }
        if(isGolem(npcT))
            doGolemScrollDrop(npc, player, npcRating, rng);
        int ssid = getSShotId(L2RogueLikeDrops.SPIRIT_SHOT_ID_LIST, npclvl);
        dropSShots(npc, player, ssid, npcT.getSpiritShot());
        ssid = getSShotId(L2RogueLikeDrops.SOUL_SHOT_ID_LIST, npclvl);
        dropSShots(npc, player, ssid, npcT.getSoulShot());
        // REPL
        for(; rng < chance; rng = getRNG(npc))
            doItemDropInLoop(npc, player, pclvl, recom,
                             npclvl, lucky, npcRating);
    }

    /**
     * modified RNG value in range [0;Config.L2JMOD_RNG_FAVOR_RANGE).
     * all hail to R.N.G.!
     * (less is better)
     */
    private static int getRNG(final L2Attackable npc) {
        return (int)(Rnd.get(Config.L2JMOD_RNG_FAVOR_RANGE) / npc.getPower());
    }

    /**
     * see getRNG()
     * (less is better)
     */
    private static int getDropEquipmentRNG(final L2Attackable npc) {
        return (int)(Rnd.get(Config.L2JMOD_RNG_EQUIP_FAVOR_RANGE) //
	    / npc.getPower());
    }

    /**
     * Evaluates usefullness and rarity rating of crystal-graded item.
     * Used to evaluate drop probability from NPCs.
     * @param item ID
     * @return rating of item with given item_id, [0,Config.MAX_PLAYER_LEVEL]
     */
    private static int evalItemRating(final int iid) {
        if (iid < 1) {
            LOG.error(String.format("invalid item id=%d", iid));
            return Integer.MAX_VALUE;
        }
        final L2Item item = ItemTable.getInstance().getTemplate(iid);
        if (item == null) {
            LOG.error(String.format("item id=%d template not found", iid));
            return Integer.MAX_VALUE;
        }
        final int itemGrade = getItemGradeInt(item),
            price = item.getReferencePrice(),
            surplus = evalPriceSurplus(price);
        return getLevelByGradeFuzzy(itemGrade, surplus); // itemLevel+bias
    }

    /**
     * surplus rating by item's price
     * @param price of item
     * @return surplus to rating
     */
    final static double log2div = Math.log(2);
    private static int evalPriceSurplus(final int price) {
        if (price < 2)
            return 0;
        int itemRating = 0;
        if (__USE_LOG10)
            if (price > 9)
                itemRating = (int)Math.log10(price);
        if (__USE_LOGE)
            if (price > 2)
                itemRating = (int)Math.log(price);
        if (__USE_LOG2)
            if (price > 1)
                itemRating = (int)(Math.log(price) / log2div);
        return itemRating;
    }

    /**
     * Evaluates usefullness and rarity rating of *Non-Grade* item.
     * Used to evaluate drop probability from NPC.
     * Uses item price [0, 237930000] as grading factor
     * (max price take from data).
     * distribution of price values nearly normal
     * @param item ID
     * @return rating of item with given item_id, [0,Config.MAX_PLAYER_LEVEL]
     *  specifically for log10 startegy:
     *    lvl = 10*log10(price)
     *    rand (max(lvl-10, 0), min(lvl+10, MAX_PLAYER_LEVEL))
     *  ... and for log startegy:
     *    kE = MAX_NPC_LEVEL / log(MAX_ITEM_PRICE),
     *    lvl = kE * log(price)
     *    rand (max(lvl-10, 0), min(lvl+10, MAX_PLAYER_LEVEL))
     */
    private static int evalItemRatingNG(final int iid) {
        if (iid < 1) {
            LOG.error(String.format("invalid item id=%d", iid));
            return Integer.MAX_VALUE;
        }
        final L2Item item = ItemTable.getInstance().getTemplate(iid);
        if (item == null) {
            LOG.error(String.format("item id=%d template not found", iid));
            return Integer.MAX_VALUE;
        }
        return evalItemRatingNG(item);
    }

    private static int evalItemRatingNG(final L2Item item) {
        if (item == null)
            return 0;
        final int price = item.getReferencePrice();
        final double logRate = EVAL_NG_STRATEGY_LOG10
            ? 10 * Math.log10(price)
            : NG_STRATEGY_LOG10_SCALE * Math.log(price);
        int level = price > 1 ? (int)logRate : 1;
        int n = level - 10,
            m = level + 10;
        n = Integer.max(n, 0);
        m = Integer.min(m, Config.MAX_PLAYER_LEVEL);
        return Rnd.get(n, m + 1);
    }

    /**
     * Evaluates difficulty rating of given NPC by template.
     * Used to evaluate drop probability from npc.
     * 1. Level;
     * 2. hp rating (base group : exp(2.75+-0.63) per level);
     * 3. active skills;
     * 4. passive skills;
     * 5. attack speed;
     * 6. cast speed;
     * 7. attack strength;
     * 8. cast strength;
     * 9. physical defence;
     * 10. magical defence;
     * 11. ?.
     * @return rating of NPC by its template
     * NB! until it is fixed, it returns NPC level
     */
    private static int evalNPCRating(final L2NpcTemplate npcT) {
        if (npcT == null)
            return 0;
        final int lvl = npcT.getLevel();
        if (lvl < 1)
            return 0;
	if (FIX_ITEM_EVAL)
            return lvl;
        else {
            //int hp = Math.log (npcT.getHp());
            final double rp = npcT.getRaidPoints();
            int rating = lvl * lvl;
            if (npcT.isUnique()) rating *= 2;
            if (rp > 1) rating *= rp;
            return rating;
	}
    }

    /**
     * 1. picks item from drop lists,
     * 2. picks amount of items,
     * 3. drops it
     */
    private static void doItemDropInLoop(final L2Attackable npc,
                                         final L2PcInstance player,
                                         final int pclvl, final int recom,
                                         final int npclvl, final boolean lucky,
                                         final int npcRating) {
        if (npc == null || player == null)
            return;
        final int totalItems = L2RogueLikeDrops.ROGUELIKE_DROP_LIST.length
            + L2RogueLikeDrops.ROGUELIKE_SINGLE_DROP_LIST.length
            + L2RogueLikeDrops.ROGUELIKE_SINGLE_DROP_LIST_NG.length;
        final int rng = Rnd.get(totalItems);
        if (rng < L2RogueLikeDrops.ROGUELIKE_DROP_LIST.length) {
            // stackable items came in ... piles
            int amount = getNethackDropAmount(npc, pclvl, recom, npclvl, lucky);
            doItemDropPile(npc, player, amount, npcRating);
        }
        else { // singular items came in ... dogs? (单身狗)
            if (rng < (L2RogueLikeDrops.ROGUELIKE_DROP_LIST.length
                       + L2RogueLikeDrops.ROGUELIKE_SINGLE_DROP_LIST.length))
                doItemDropDrops(npc, player, npcRating,
                                L2RogueLikeDrops.ROGUELIKE_SINGLE_DROP_LIST,
                                false);
            else
                doItemDropDrops(npc, player, npcRating,
                                L2RogueLikeDrops.ROGUELIKE_SINGLE_DROP_LIST_NG,
                                true);
        }
    }

    /**
     * evaluates rewards amount from mob
     * @return 1 if not champion else [1,Config.L2JMOD_CHAMPION_REWARDS_AMOUNT)
     */
    private static int evalNPCRewardsAmount(final L2Attackable npc) {
        int amount;
        if(npc.isChampion() && Config.L2JMOD_CHAMPION_REWARDS_AMOUNT > 1)
            amount = 1 + (int)npc.getPowerMultiplier(Config.L2JMOD_CHAMPION_REWARDS_AMOUNT - 1);
        else
            amount = 1;
        return amount;
    }

    /**
     * drop single item for player from ordinary mob.
     * drop several single items for player from champion.
     * NB! amount scaled by Config.L2JMOD_CHAMPION_REWARDS_AMOUNT
     */
    private static void doItemDropDrops(final L2Attackable npc,
                                        final L2PcInstance player,
                                        final int npcRating,
                                        final int[] itemList,
                                        final boolean evalNG) {
        if (npc == null || player == null) return;
        int amount = evalNPCRewardsAmount(npc);
        int itemId = DROP_ITEM_ID_DEFAULT;
        int itemRating = 0;
        L2RDropListIterator iter = L2RDropListIterator.L2RDropListIteratorFactory(itemList);
        // O(n^2) :(
        endl:
        for (int i = 0; i < FIND_DROP_ITEM_MAX_TRIES; ++i) {
            int id = iter.next();
            for(int j = 0; j < FIND_DROP_ITEM_MAX_TRIES; ++j) {
                itemRating = evalNG ? evalItemRatingNG(id) : evalItemRating(id);
                int litc = Rnd.get(100);
                if (litc < USE_LOWER_ITEM_RATING)
                    itemRating -= (USE_LOWER_ITEM_RATING - litc) * 2;
                if (itemRating < npcRating) {
                    itemId = id;
                    break endl;
                }
                id = iter.slide();
            }
        }
        if (itemId != ITEM_ID_ADENA) {
            ItemHolder item = new ItemHolder (itemId, 1);
            for (int j = 0; j < amount; ++j)
                dropItem(npc, player, item);
        }
        else {
            amount = getDropAmountAdena(amount, npcRating);
            // fixme - adena drops may (should?) be served by other method
            dropItem(npc, player, new ItemHolder (ITEM_ID_ADENA, amount));
        }
    }

    /**
     * Evaluates amounts of items to drop.
     * Takes in account stroke of luck and champion's multiplier
     * NB! amount scaled by Config.L2JMOD_CHAMPION_REWARDS_AMOUNT
     */
    private static int getNethackDropAmount(final L2Attackable npc,
                                            final int pclvl, final int recom,
                                            final int npclvl,
                                            final boolean lucky) {
        int amount = Integer.max(1, Rnd.get (npclvl));
        if (lucky) { // streak of Luck, 1/1000
            int maxpclvl = Integer.max(100, Config.MAX_PLAYER_LEVEL);
            amount += Rnd.get(recom + npclvl + maxpclvl - pclvl);
        }
        return amount * evalNPCRewardsAmount(npc);
    }

    /**
     * Animal item drop wrapper,
     * evaluates amount and drops drops
     */
    private static void doAnimalDropsWrapper(final L2Attackable npc,
                                             final L2PcInstance player,
                                             final int npclvl,
                                             final int npcRating) {
        // int amount = getNethackDropAmount(npc, pclvl, recom, npclvl, lucky);
        int amount = Integer.max(1, Rnd.get(npclvl) / 16);
        doItemDropNethackNPC(npc, player, amount, npcRating,
                             L2RogueLikeDrops.ANIMAL_DROP_LIST);
    }

    /**
     * Animal item drop. Plain and simple.
     */
    private static void doAnimalDrops(final L2Attackable npc,
                                      final L2PcInstance player,
                                      final int amount,
                                      final int npcRating) {
        doItemDropNethackNPC(npc, player, amount, npcRating,
                             L2RogueLikeDrops.ANIMAL_DROP_LIST);
    }

    /**
     * Beast, constructs gem drop
     */
    private static void doExtraGemsDrop(final L2Attackable npc,
                                        final L2PcInstance player,
                                        final int amount,
                                        final int npcRating) {
        doItemDropNethackNPC(npc, player, amount, npcRating,
                             L2RogueLikeDrops.ROGUELIKE_DROP_STONES);
    }

    /**
     * TODO: random scroll from golem
     */
    private static void doGolemScrollDrop(final L2Attackable npc,
                                          final L2PcInstance player,
                                          final int npcRating,
                                          final int rng) {
        if(rng < npcRating) // fixme!
            doItemDropNethackNPC(npc, player, 1, npcRating,
                                 L2RogueLikeDrops.ROGUELIKE_DROP_SCROLLS);
    }

    /**
     * drop pile of given items for player
     */
    private static void doItemDropPile(final L2Attackable npc,
                                       final L2PcInstance player,
                                       final int amount,
                                       final int npcRating) {
        doItemDropNethackNPC(npc, player, amount, npcRating,
                             L2RogueLikeDrops.ROGUELIKE_DROP_LIST);
    }

    /**
     * picks a random element from the given array
     * NB. it is right thing to get null-pointer exception here
     * @return a random element from the list
     */
    private static int getItemFromList(final int[] list) {
        //if (list == null || list.length < 1) return 0;
        return list[Rnd.get(list.length)];
    }

    /**
     * picks a random element from the given ragged matrix (array of arrays)
     * NB. it is right thing to get null-pointer exception here
     * @return a random element from the grid
     */
    private static int getItemFromList(final int[][] grid) {
        //if (grid == null || grid.length < 1) return 0;
        //int[] size = new int[grid.length];
        int gridItems = 0;
        for (int i = 0; i < grid.length; ++i) {
            //size[i] = grid[i].length;
            gridItems += grid[i].length;
        }
        int item = Rnd.get(gridItems);
        for (int i = 0;
             i < grid.length && item >= 0;
             item -= grid[i++].length) {
            if (item < grid[i].length)
                return grid[i][item];
        }
        return DROP_ITEM_ID_DEFAULT;
    }

    /**
     * picks a random element from the given array
     * NB. it is right thing to get null-pointer exception here
     * @return an iterator for random element(s) from the list
     */
    private static L2RDropListIterator getItemRIteratorFromList(final int[] list) {
        //if (list == null || list.length < 1) return 0;
        return new L2RDropListIterator (list);
    }

    /**
     * generic item drop function
     * Directly drops given amount w/o any modifications.
     * Iterates FIND_DROP_ITEM_MAX_TRIES times over
     * possible items until item to drop is found,
     * if not, drops DROP_ITEM_ID_DEFAULT (a.k.a. adena).
     * @param amount of items to drop, has special meaning for adena
     */
    private static void doItemDropNethackNPC(final L2Attackable npc,
                                             final L2PcInstance player,
                                             final int amount,
                                             final int npcRating,
                                             final int[] itemList) {
        if (npc == null || amount <= 0 || player == null
            || itemList == null || itemList.length < 1)
            return;
        int itemId = DROP_ITEM_ID_DEFAULT;
        int itemRating = 0;
        for (int i = 0; i < FIND_DROP_ITEM_MAX_TRIES; ++i) {
            int id = getItemFromList(itemList);
            itemRating = evalItemRating(id);
            if (itemRating <= npcRating) {
                itemId = id;
                break; // return
            }
	}
        int n = itemId != ITEM_ID_ADENA
            ? amount
            : getDropAmountAdena(amount, npcRating);
        if (__LOG_DROPS)
            LOG.info("-=> drop piles: to " + player.getName()
                     + " drop " + n + " of " + itemId
                     + " with ratings of item " + itemRating
                     + " vs npc " + npcRating);
        dropItem(npc, player, new ItemHolder (itemId, n));
    }

    /**
     * Evaluates amount of adena to drop,
     * according to npcRating
     * and initial amounts of items to drop.
     * NB! ignores Config.L2JMOD_CHAMPION_REWARDS_AMOUNT,
     * because it have to be already in amount
     * @return amount [1,+inf) of adena to drop
     */
    private static int getDropAmountAdena(final int amount,
                                          final int npcRating) {
        int m = amount * npcRating;
        m = m > 0 ? m : 1;
        int n = Rnd.get(m);
        return n == 0 ? m * 2 : n;
    }

    /**
     * drop several s.+shots,
     * but no more, than amount given
     */
    private static void dropSShots(final L2Attackable npc,
                                   final L2PcInstance player,
                                   final int ssid, final int amount) {
        if (npc == null || amount <= 0 || player == null || ssid <= 0)
            return;
        int n = Rnd.get(amount) + 1;
        dropItem(npc, player, new ItemHolder (ssid, n * evalNPCRewardsAmount(npc)));
    }

    private static int getSShotId(final int[] ssIdList, final int lvl) {
        int grade = getGradeByLevel(lvl);
        if (grade >= 0 && grade < ssIdList.length)
            return ssIdList[grade];
        return DROP_ITEM_ID_DEFAULT;
    }

    /**
     * mapping function Level -> Grade
     * https://lineage2.fandom.com/el/wiki/Equipment
     * NG 0..19
     * D  20..39
     * C  40..51
     * B  52..60
     * A  61..75
     * S  76..MAXINT
     * S80 80..MAXINT
     * S84 84..MAXINT
     * R   85..95
     * R95 95..98
     * R99 99..MAXINT
     */
    private static int getGradeByLevel(final int lvl) {
        if (lvl < 20) return 0; // NG
        if (lvl < 40) return 1; // D
        if (lvl < 52) return 2; // C
        if (lvl < 61) return 3; // B
        if (lvl < 76) return 4; // A
        if (lvl >= 84) return 7; // S84
        if (lvl >= 80) return 6; // S80
        return 5; // S
    }

    /**
     * mapping function Grade -> Level
     * @param grade [0,7]
     * @return level {0, 20, 40, 52, 61, 76, 80, 84}
     */
    private static int getLevelByGrade(final int grade) {
        if (grade < 0)
            return 0;
        if (grade >= L2RogueLikeDrops.LEVEL_BY_GRADE.length)
            return L2RogueLikeDrops.LEVEL_BY_GRADE [L2RogueLikeDrops.LEVEL_BY_GRADE.length-1];
        return L2RogueLikeDrops.LEVEL_BY_GRADE[grade];
    }

    /**
     * mapping function Grade to Level with bias
     * @param grade [0,7]
     * @return level (0,Config.MAX_PLAYER_LEVEL] as rand(base_level,next_level)
     */
    private static int getLevelByGradeFuzzy(final int grade) {
        if (grade < 0)
            return 0;
        if (grade >= (L2RogueLikeDrops.LEVEL_BY_GRADE.length-1))
            return L2RogueLikeDrops.LEVEL_BY_GRADE [L2RogueLikeDrops.LEVEL_BY_GRADE.length-1];
        int n = L2RogueLikeDrops.LEVEL_BY_GRADE[grade],
            m = L2RogueLikeDrops.LEVEL_BY_GRADE[grade + 1];
        return Rnd.get(n, m);
    }

    /**
     * mapping function Grade to Level with extended bias
     * @param grade [0,7]
     * @return level (0,Config.MAX_PLAYER_LEVEL] as
     *    rand (base_level, next_level + bias)
     */
    private static int getLevelByGradeFuzzy(final int grade, final int bias) {
        if (grade < 0)
            return 0;
        if (grade >= (L2RogueLikeDrops.LEVEL_BY_GRADE.length-1))
            return L2RogueLikeDrops.LEVEL_BY_GRADE [L2RogueLikeDrops.LEVEL_BY_GRADE.length-1];
        int n = L2RogueLikeDrops.LEVEL_BY_GRADE[grade],
            m = L2RogueLikeDrops.LEVEL_BY_GRADE[grade + 1];
        int lvl = Rnd.get(n, m + bias);
        if (lvl > Config.MAX_PLAYER_LEVEL)
            return Config.MAX_PLAYER_LEVEL;
        return lvl;
    }

    /**
     * @return item (weapon) grade [0,5]
     * also, fixme -- see Crystal*
     */
    private static int getWeaponGrade(final int id) {
        //L2Item item = ItemTable.getInstance().getTemplate(id);
        L2Weapon item = (L2Weapon) ItemTable.getInstance().getTemplate(id);
        if (item == null)
            return 0;
        switch (item.getItemGrade()) {
        case D:
            return 1;
        case C:
            return 2;
        case B:
            return 3;
        case A:
            return 4;
        case S:
        case S80: // 6
        case S84: // 7
            return 5;
        default:
            return 0;
        }
    }

    /**
     * @return item (weapon) grade as int 0..5 (7)
     * also, fixme -- see Crystal*
     */
    private static int getWeaponGrade84(final int id) {
        //L2Item item = ItemTable.getInstance().getTemplate(id);
        L2Weapon item = (L2Weapon) ItemTable.getInstance().getTemplate(id);
        if (item == null)
            return 0;
        switch (item.getItemGrade()) {
        case D:
            return 1;
        case C:
            return 2;
        case B:
            return 3;
        case A:
            return 4;
        case S:
            return 5;
        case S80:
            return 6;
        case S84:
            return 7;
        default:
            return 0;
        }
    }

    /**
     * @return item (weapon) grade as int [0,5]
     */
    private static int getItemGradeInt(final int id) {
        if (id < 1)
            return 0;
        return getItemGradeInt(ItemTable.getInstance().getTemplate(id));
    }

    /**
     * @return item (weapon) grade as int NG..S -> [0,5]
     * also, fixme -- see Crystal*
     */
    private static int getItemGradeInt(final L2Item item) {
        if (item == null)
            return 0;
        switch (item.getItemGrade()) {
        case D:
            return 1;
        case C:
            return 2;
        case B:
            return 3;
        case A:
            return 4;
        case S:
        case S80:
        case S84:
            return 5;
        default:
            return 0;
        }
    }

    /**
     * @return item (weapon) grade as int NG..S -> [0,5]
     * also, fixme -- see Crystal*
     */
    private static int getItemGradeInt84(final L2Item item) {
        if (item == null)
            return 0;
        return item.getItemGrade().getId();
        /*
        switch (item.getItemGrade()) {
        case D:
            return 1;
        case C:
            return 2;
        case B:
            return 3;
        case A:
            return 4;
        case S:
            return 5;
        case S80:
            return 6;
        case S84:
            return 7;
        default:
            return 0;
        }
        */
    }

    /**
     * drop of mob's equipment : weapon and armor
     * also, drops arrows if weapon is bow or xbow
     * fixme! take in account item vs npc ratings
     */
    private static void doDropEquipment(final L2Attackable npc,
                                        final L2NpcTemplate npcT,
                                        final L2PcInstance player,
                                        final int pclvl, final int recom,
                                        final int npclvl,
                                        final boolean lucky) {
        if (npc == null || npcT == null || player == null)
            return;
        final int RNG_EQUIP_BASE_CHANCE = Integer.max //
            (Config.L2JMOD_RNG_GIFTS_EQUIPMENT, //
             npclvl + Config.MAX_PLAYER_LEVEL);
        final int armorId = npcT.getChestId(), // ?
            weaponId = npcT.getRHandId(),
            shieldId = npcT.getLHandId(),
            // enchLvl = npcT.getWeaponEnchant(),
            rnpl = getDropBaseChance(recom, npclvl, pclvl);
        int probability = RNG_EQUIP_BASE_CHANCE + rnpl;
        if(lucky) probability *= 2;
        // also see L2ItemInstance(int, int)
        if(armorId > 0)
            doDropEquipmentItem(npc, player, armorId, probability);
        if(shieldId > 0)
            doDropEquipmentItem(npc, player, shieldId, probability);
        if(weaponId > 0) {
            doDropEquipmentItem(npc, player, weaponId, probability);
            int ammo_prob = RNG_GIFTS_AMMO + rnpl;
            if(lucky) ammo_prob *= 2;
            if(getDropEquipmentRNG(npc) < ammo_prob)
                doDropAmmo(npc, player, weaponId, lucky, npclvl);
        }
    }

    private static int getDropBaseChance(final int recom, final int npclvl, final int pclvl) {
        int n = ((recom / 4) > 1 ? Rnd.get(recom / 4) : 0) //
            + (npclvl - pclvl) * 4;
        return n > 4 ? n : 4;
    }


    /**
     * creates and drops for PC non-npc-only tradeable items by id,
     * also, if RNG is moody, should drop shard instead of whole item
     */
    private static void doDropEquipmentItem(final L2Attackable npc,
                                            final L2PcInstance player,
                                            final int itemId,
                                            final int probability) {
        final L2Item item = ItemTable.getInstance().getTemplate(itemId);
        if(item == null)
            return;
        final int rng = getDropEquipmentRNG(npc);
        final boolean notForNpc = !item.isForNpc() && item.isTradeable();
        if(rng < probability && notForNpc) {
            if(rng > (probability * Config.L2JMOD_RNG_EQUIP_SHARD_RATE) / 100)
                dropItem(npc, player, new ItemHolder(itemId, 1));
            else {
                int shardId = getEquipmentShard(itemId);
                int n = itemId == shardId ? 1 : Rnd.get(4) + 1;
                ItemHolder shItem = new ItemHolder(shardId, n);
                dropItem(npc, player, shItem);
            }
            return;
        }
        if(rng < (probability * 2) && notForNpc) {
            doDropEquipmentItemShard(npc, player, itemId);
            return;
        }
        if(rng < (probability * 4)) {
            doDropEquipmentItemCrystal(npc, player, itemId);
            return;
        }
        if(rng < (probability * 8))
            doDropEquipmentItemOre(npc, player, itemId);
    }

    private static void doDropEquipmentItemShard(final L2Attackable npc,
                                                 final L2PcInstance player,
                                                 final int itemId) {
        dropItem(npc, player, new ItemHolder(getEquipmentShard(itemId), 1));
    }

    /**
     * drops crystal amount for items,
     * or else drop some ore if no luck
     */
    private static void doDropEquipmentItemCrystal(final L2Attackable npc,
                                                   final L2PcInstance player,
                                                   final int itemId) {
        final L2Item item = ItemTable.getInstance().getTemplate(itemId);
        if(item == null)
            return;
        int crysId = item.getCrystalItemId();
        if(crysId > 0) {
            int crysNum = item.getCrystalCount() / 2;
            if (crysNum > 1) {
                dropItem(npc, player,
                         new ItemHolder(crysId, crysNum + Rnd.get(crysNum)));
                return;
            }
        }
        doDropEquipmentItemOre(npc, player, itemId);
    }

    /**
     * TODO: oreId to correct "main" material of item
     * drops iron ore / steel / ... for items
     */
    private static void doDropEquipmentItemOre(final L2Attackable npc,
                                               final L2PcInstance player,
                                               final int itemId) {
        L2Item item = ItemTable.getInstance().getTemplate(itemId);
        int oreId; // = L2RogueLikeDrops.ITEM_ID_IRON_ORE;
        switch(item.getMaterialType()) {
        case STEEL: oreId = L2RogueLikeDrops.ITEM_ID_IRON_ORE; break;
        case FINE_STEEL: oreId = L2RogueLikeDrops.ITEM_ID_STEEL; break;
        case COTTON: oreId = L2RogueLikeDrops.ITEM_ID_THREAD; break;
        case BLOOD_STEEL: oreId = L2RogueLikeDrops.ITEM_ID_MITHRIL_ORE; break;
        case BRONZE: oreId = L2RogueLikeDrops.ITEM_ID_IRON_ORE; break;
        case SILVER: oreId = L2RogueLikeDrops.ITEM_ID_SILVER; break;
        case MITHRIL: oreId = L2RogueLikeDrops.ITEM_ID_MITHRIL_ORE; break;
        case ORIHARUKON: oreId = L2RogueLikeDrops.ITEM_ID_ORIHARUKON_ORE; break;
        case PAPER: oreId = L2RogueLikeDrops.ITEM_ID_THREAD; break;
        case WOOD: oreId = L2RogueLikeDrops.ITEM_ID_STEM; break;
        case CLOTH: oreId = L2RogueLikeDrops.ITEM_ID_THREAD; break;
        case LEATHER: oreId = L2RogueLikeDrops.ITEM_ID_LEATHER; break;
        case BONE: oreId = L2RogueLikeDrops.ITEM_ID_BONE; break;
        case HORN: oreId = L2RogueLikeDrops.ITEM_ID_BONE; break;
        case DAMASCUS: oreId = L2RogueLikeDrops.ITEM_ID_MITHRIL_ALLOY; break;
        case ADAMANTAITE: oreId = L2RogueLikeDrops.ITEM_ID_ADAMANTITE; break;
        case CHRYSOLITE: oreId = L2RogueLikeDrops.ITEM_ID_MITHRIL_ORE; break;
        case CRYSTAL:
        case LIQUID:
        case SCALE_OF_DRAGON:
        default:
            oreId = L2RogueLikeDrops.ITEM_ID_IRON_ORE;
        }
        int orePrice = ItemTable.getInstance().getTemplate(oreId) //
            .getReferencePrice();
        int oreNum = (int) Math.sqrt(item.getReferencePrice() / orePrice) / 2;
        if(oreNum < 1) {
            // if(item instanceof L2Weapon) cnum = item.getPatk() + item.getMatk(); // fixme!
            oreNum = item.getWeight() / 64;
        }
        int amount = oreNum < 4 //
            ? oreNum + 1 //
            : oreNum + Rnd.get(oreNum);
        dropItem(npc, player,
                 new ItemHolder(oreId, amount));
    }

    /**
     * select from (quasi-)map of tuples : [(itemId, shardId)],
     * wrapping logic
     * @param  whole item Id
     * @return shard item Id
     */
    private static int getEquipmentShard(final int itemId) {
        // equipment_shards_shield
        // equipment_shards_weapon
        if(itemId < 1) return itemId;
        int sid = searchEquipmentShard(equipment_shards_shield, itemId);
        if(sid > 0) return sid;
        sid = searchEquipmentShard(equipment_shards_weapon, itemId);
        if(sid > 0) return sid;
        return itemId;
    }

    /**
     * search in quasi-map array of tuples : [(itemId, shardId)],
     * core implementation
     * @return shardItemId if @itemId in @list else @itemId
     */
    private static int searchEquipmentShard(final int[] list,
                                            final int itemId) {
        if(list.length < 2) return itemId;
        if(list.length == 2) {
            if(list[0] == itemId) return list[1];
            return itemId;
        }
        if(list[list.length - 2] < itemId) return itemId;
        for(int cpos = list.length / 2, hpos = 0, tpos = list.length - 2; //
            tpos > hpos; cpos = hpos + (tpos - hpos) / 2) {
            if(list[cpos] == itemId)
                return list[cpos+1];
            if(list[cpos] < itemId)
                hpos = cpos;
            else // list[cpos] < itemId
                tpos = cpos;
            if((tpos - hpos) < 3)
                switch(tpos - hpos) {
                case 2:
                    if(list[hpos+2] == itemId)
                        return list[hpos+3];
                case 1:
                    if(list[tpos] == itemId)
                        return list[tpos+1];
                case 0:
                    if(list[hpos] == itemId)
                        return list[hpos+1];
                default:
                        return itemId;
                }
        }
        return itemId;
    }

    /**
     * @return ammo ID if weapon (by id) is launcher else 0
     */
    private static int getLauncherAmmoId(final int id) {
        L2Weapon item = (L2Weapon) ItemTable.getInstance().getTemplate(id);
        if (item == null)
            return 0;
        int[] ammo;
        if (item.isBow())
            ammo = L2RogueLikeDrops.ROGUELIKE_DROP_AMMO_LIST[0];
        else if (item.isCrossBow())
            ammo = L2RogueLikeDrops.ROGUELIKE_DROP_AMMO_LIST[1];
        else
            return 0;
        return ammo[getItemGradeInt(id)];
    }

    /**
     * @return ammo ID if weapon (by id) is launcher else 0
     */
    private static void doDropAmmo(final L2Attackable npc,
                                   final L2PcInstance player,
                                   final int weapon,
                                   final boolean lucky,
                                   final int npclvl) {
        int ammoId = getLauncherAmmoId(weapon);
        if (ammoId < 1) return;
        int n = (int)((4 + Rnd.get(npclvl)) * npc.getPower());
        if (lucky)
            n *= 2;
        dropItem(npc, player, new ItemHolder (ammoId, n));
    }

    private static void dropItem(final L2Attackable npc,
                                 final L2PcInstance player,
                                 final ItemHolder item) {
        if (Config.AUTO_LOOT || npc.isFlying())
            player.addItem("NPCLoot", item.getId(),
                           item.getCount(), npc, true);
        else
            npc.dropItem(player, item);
    }

    public final static int AGGRO_RNG_MAX = 1024,
        AGGRO_PASSIVE = 250,
        AGGRO_AGGRESSIVE = 750,
        AGGRO_BONUS_OF_CHAMPION = 128,
        AGGRO_BONUS_BY_INCOMBAT = 64,
        AGGRO_BONUS_BY_LEVEL = 80,
        AGGRO_BONUS_BY_HP100 = 300, // aggro change per 100% HP
        AGGRO_BONUS_BY_XATK = 64, // P/M attack difference
        AGGRO_BONUS_BY_INPARTY = 64,
        AGGRO_FLEE_BY_HP_LEFT = AGGRO_BONUS_BY_HP100/10; // stop attack (and flee)

    /**
     * is this L2Monster wants to attack the L2PC?
     * called each ~1s for applicable targets.
     * (do NOT change existing attack state)
     */
    public static boolean doIWantToAttackPC(final L2Attackable npc,
                                            final L2PcInstance pc) {
        if(pc == null || npc == null)
            return false;
        final int npcHPrate = evalHPAggroRate((int)pc.getCurrentHp(),
                                              pc.getMaxHp());
        if(npcHPrate < AGGRO_FLEE_BY_HP_LEFT)
            return false;
        final int pcHPrate = evalHPAggroRate((int)pc.getCurrentHp(),
                                             pc.getMaxHp());
        int agro = npc.getAggroLevel() //
            + evalLevelDiffAggroBonus(npc, pc) //
            - npcHPrate + (AGGRO_BONUS_BY_HP100 - pcHPrate) //
            + evalPMAtkRateDiffBonus(npc, pc) //
            // + evalTimeOfDayAggroBonus(npc) // todo
            ;
        if(pc.isInCombat())
            agro += AGGRO_BONUS_BY_INCOMBAT;
        if(pc.isInParty())
            agro -= evalPartyArgoBonus(pc);
        if(agro < 0)
            return false;
        return agro > (AGGRO_RNG_MAX / 2);
    }

    private static int evalHPAggroRate(final int hp, final int mhp) {
        return (AGGRO_BONUS_BY_HP100 * hp) / mhp;
    }

    /**
     * Aggro bonus depending of attack and defence differences
     * 4 PCs, equipment counts :)
     */
    private static int evalPMAtkRateDiffBonus(final L2Attackable npc,
                                              final L2PcInstance pc) {
        final int npcatk = Integer.max((int)npc.getPAtk(null),
                                       (int)npc.getMAtk(null, null)),
            pcatk = Integer.max((int)pc.getPAtk(null), //
                                (int)pc.getMAtk(null, null));
        int adif = AGGRO_BONUS_BY_XATK; // P/M attack difference (ratio) bonus
        if(npcatk > pcatk)
            adif = adif * npcatk / pcatk;
        else
            adif = -(adif * pcatk / npcatk);
        return adif;
    }

    private static int evalPartyArgoBonus(final L2PcInstance pc) {
        if(pc == null || pc.isInParty() == false)
            return 0;
        L2Party ppc = pc.getParty();
        if(ppc == null)
            return 0;
        return AGGRO_BONUS_BY_INPARTY;
    }

    final static int NPC_HALF_AGGRESSIVE_VALUE = 2;
    /**
     * Aggro bonus depending of day or night time
     * Morning: 0
     * Day:     beast--, undead--
     * Evening: 0
     * Night:   beast++, undead++
     *
     * also, npc.parameters.HalfAggressive : bat=2
     */
    private static int evalTimeOfDayAggroBonus(final L2Attackable npc) {
        int agro = 0;
        final L2NpcTemplate npcT = npc.getTemplate();
        int itisBat;
        try {itisBat = npcT.getParameters().getInt("HalfAggressive");}
        catch (Exception ex) {itisBat = 0;}
        if(SYSTEM_TIMER.isNight()) {
            if(npcT.isType("UNDEAD") || npcT.isType("BEAST"))
                agro += Config.L2JMOD_DAY_NIGHT_BONUS;
            else
                agro -= Config.L2JMOD_DAY_NIGHT_BONUS;
            if(itisBat == NPC_HALF_AGGRESSIVE_VALUE)
                agro += (AGGRO_AGGRESSIVE - AGGRO_PASSIVE) / 2;
        }
        else { // day time
            if(npcT.isType("UNDEAD") || npcT.isType("BEAST"))
                agro -= Config.L2JMOD_DAY_NIGHT_BONUS;
            else
                agro += Config.L2JMOD_DAY_NIGHT_BONUS;
            if(itisBat == NPC_HALF_AGGRESSIVE_VALUE)
                agro -= (AGGRO_AGGRESSIVE - AGGRO_PASSIVE) / 2;
        }
        return agro;
    }

    /**
     * Aggro bonus per levels difference
     */
    private static int evalLevelDiffAggroBonus(final L2Attackable npc,
                                               final L2PcInstance pc) {
        return AGGRO_BONUS_BY_LEVEL * (npc.getLevel() - pc.getLevel());
    }

}
