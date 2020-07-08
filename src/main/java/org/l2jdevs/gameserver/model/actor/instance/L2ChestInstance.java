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
package org.l2jdevs.gameserver.model.actor.instance;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.data.xml.impl.NpcData;
import org.l2jdevs.gameserver.enums.InstanceType;
import org.l2jdevs.gameserver.handler.BypassHandler;
import org.l2jdevs.gameserver.handler.IBypassHandler;
import org.l2jdevs.gameserver.model.actor.L2Attackable;
import org.l2jdevs.gameserver.model.actor.L2Character;
import org.l2jdevs.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2jdevs.gameserver.network.serverpackets.ActionFailed;
import org.l2jdevs.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jdevs.gameserver.util.Util;
import org.l2jdevs.util.Rnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages all chest.
 *
 * TODO:
 * 1. drop loot on open (drop list disabled somehow for L2Attackable?);
 * 2. verify trap checking logic;
 * 3. remove fire ring;
 * 4. implement lockpick puzzles;
 * 5. implement use of skill Unlock;
 * 6. implement use of Deluxe Key;
 *
 * @author Julian
 * @author RKorskov
 */
public final class L2ChestInstance extends L2Attackable { // L2MonsterInstance
    private static final Logger LOG = LoggerFactory.getLogger(L2ChestInstance.class);
    private volatile boolean _specialDrop;
    private boolean trapped, trapKnown;
    private boolean locked, lockJam; // true if lock jammed -- chest may be opened only by forcing lock or attacking
    private final int level;

    /**
     * Creates a chest.
     *
     * @param template the chest NPC template
     */
    public L2ChestInstance(L2NpcTemplate template) {
        super(template);
        setInstanceType(InstanceType.L2ChestInstance);
        setIsNoRndWalk(true);
        setBusyMessage("Object occupied");
        setAutoAttackable(false);
        _specialDrop = false;
        trapped = Rnd.nextBoolean();
        trapKnown = false;
        lockJam = false;
        locked = true;
        level = getTemplate().getLevel(); // + 5 - Rnd.get(15);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        _specialDrop = false;
        setMustRewardExpSp(true);
    }

    public synchronized void setSpecialDrop() {
        _specialDrop = true;
    }

    @Override
    public void doItemDrop(final L2NpcTemplate npcTemplate, final L2Character lastAttacker) {
        int id = getTemplate().getId();
        LOG.error("chest doItemDrop id = " + id);
        if (!_specialDrop) {
            if ((id >= 18265) && (id <= 18286)) {
                id += 3536;
            } else if ((id == 18287) || (id == 18288)) {
                id = 21671;
            } else if ((id == 18289) || (id == 18290)) {
                id = 21694;
            } else if ((id == 18291) || (id == 18292)) {
                id = 21717;
            } else if ((id == 18293) || (id == 18294)) {
                id = 21740;
            } else if ((id == 18295) || (id == 18296)) {
                id = 21763;
            } else if ((id == 18297) || (id == 18298)) {
                id = 21786;
            }
        }
        LOG.error("chest _specialDrop id = " + id);
        L2NpcTemplate tid = NpcData.getInstance().getTemplate(id);
        LOG.error("chest drop template id = " + (tid == null ? "NULL" : tid.getId()));
        super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
    }

    @Override
    public boolean isMovementDisabled() {
        return true;
    }

    @Override
    public boolean hasRandomAnimation() {
        return false;
    }

    @Override
    public boolean isAggressive() {
        return false;
    }

    /**
     * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<br>
     * <B><U> Example of use </U> :</B>
     * <ul>
     * <li>Client packet : RequestBypassToServer</li>
     * </ul>
     *
     * @param pc
     * @param cmd The command string received from client
     */
    @Override
    public void onBypassFeedback(final L2PcInstance pc, final String cmd) {
        // if (canInteract(player))
        {
            LOG.error("chest onBypassFeedback cmd = " + cmd);
            if (isBusy() && (getBusyMessage().length() > 0)) {
                chestIsBusy(this, pc);
                return;
            }
            IBypassHandler handler = BypassHandler.getInstance().getHandler(cmd);
            if (handler != null) {
                handler.useBypass(cmd, pc, this);
                return;
            } else {
                LOG.error("Unknown NPC bypass: \"{}\" NpcId: {}", cmd, getId()); // info
                return;
            }
        }
    }

    @Override
    public void showChatWindow(final L2PcInstance pc) {
        showChatWindow(pc,-1);
    }

    @Override
    /**
     * Open a chat window on client with the text of the L2NpcInstance.<br>
     * <B><U>Actions</U>:</B>
     * <ul>
     * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
     * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
     * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li>
     * </ul>
     * @param player The L2PcInstance that talk with the L2NpcInstance
     * @param val The number of the page of the L2NpcInstance to display
     */
    public void showChatWindow(final L2PcInstance pc, final int val) {
        LOG.error("L2ChestInstance : showChatWindow(L2PcInstance, int)");
        if (!isTalking()) {
            pc.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        /*
        // Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
        final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(player.getHtmlPrefix(), filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStr());
        player.sendPacket(html);
        */
        openChestDialog(pc, this, String.valueOf(val));

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        pc.sendPacket(ActionFailed.STATIC_PACKET);
    }

    @Override
    public void showChatWindow(final L2PcInstance pc, final String str) {
        LOG.error("L2ChestInstance : showChatWindow(L2PcInstance, String)");
        openChestDialog(pc, this, str);
        pc.sendPacket(ActionFailed.STATIC_PACKET);
    }

    public boolean isTrapped() { return trapped; }

    public void setTrapped(final boolean trapped) { this.trapped = trapped; }

    public boolean isTrapKnown() { return trapKnown; }

    public void setTrapKnown(boolean trapKnown) { this.trapKnown = trapKnown; }

    public String getLockState() {
        if(locked) {
            if(lockJam)
                return "jammed";
            return "locked";
        }
        return "unlocked";
    }

    public String getTrapState() {
        if(trapKnown) {
            if (trapped)
                return "trapped";
            return "untrapped";
        }
        return "unknown";
    }

    public boolean isLockJammed() { return lockJam; }

    public boolean lockOpen(final L2PcInstance pc) {
        // fixme : stub
        LOG.error("L2ChestInstance : lock open");
        if(lockJam)
            return false;
        if(locked) {
            int n = pc.getLevel() - level;
            if(n > 0) {
                locked = false;
                doChestOpen(pc);
                return true;
            }
            else if(n < -17)
                lockJam = true;
        }
        return false;
    }

    public boolean lockForce(final L2PcInstance pc) {
        // fixme : stub
        LOG.error("L2ChestInstance : lock force");
        if(trapped) {
            evalTrap(pc);
            return false;
        }
        if(!locked) {
            doChestOpen(pc);
            return true;
        }
        int n = pc.getLevel() - level;
        if(n > 7) {
            locked = false;
            doChestOpen(pc);
            return true;
        }
        else {
            if(n < -7)
                lockJam = true;
        }
        return false;
    }

    private void doChestOpen(L2PcInstance pc) {
        // fixme : replace with corpse
        LOG.error("L2ChestInstance : add adena xp sp, kill NPC");
        pc.addAdena("Loot",level + Rnd.get(level * 8), this, true);
        pc.addExpAndSp(level + Rnd.get(level * 8), level + Rnd.get(level * 8));
        reduceCurrentHp(Integer.MAX_VALUE, pc, null);
        //doDie(pc); // ... and drop contents (if any)
    }

    public void evalTrap(final L2PcInstance pc) {
        // fixme : stub
        LOG.error("L2ChestInstance : trap spring");
        pc.say("Oops...");
        int n = level * 16 + Rnd.get(level * 48);
        pc.reduceCurrentHp(n, this, null);
        pc.addExpAndSp(n, n/2);
        // pc.say("chest contents was destroyed by explosion");
        trapped = false;
        trapKnown = true;
        return;
    }

    private static void chestIsBusy(final L2ChestInstance obj, final L2PcInstance pc) {
        pc.sendPacket(ActionFailed.STATIC_PACKET);
        final NpcHtmlMessage html = new NpcHtmlMessage(obj.getObjectId());
        html.setFile(pc.getHtmlPrefix(), "data/html/npcbusy.htm");
        html.replace("%busymessage%", obj.getBusyMessage());
        html.replace("%npcname%", obj.getName());
        html.replace("%playername%", pc.getName());
        pc.sendPacket(html);
    }

    public void leave() {
        setBusy(false);
        trapKnown = false; // per PC?
    }

    public static void openChestDialog(final L2PcInstance pc, final L2ChestInstance chest, final String str) {
        openChestDialog(pc, chest);
    }

    /**
     * Open a L2Chest window on client with the text of the L2NpcInstance.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li>
     * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li>
     * <li>Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet</li><BR>
     *
     * @param pc    The L2PcInstance that talk with the L2NpcInstance
     * @param chest affected L2World object
     */
    public static void openChestDialog(final L2PcInstance pc, final L2ChestInstance chest) {
        // fixme: test stub
        StringBuilder msg = new StringBuilder("<html><title>L2Chest</title><body>");
        int oid;
        if (chest != null) {
            oid = chest.getObjectId();
            msg.append("Object ID : " + oid + "<br1/><table>");
            msg.append("<tr><td width=64>Lock</td><td>" + chest.getLockState() + "</td></tr>");
            msg.append("<tr><td width=64>Trap</td><td>" + chest.getTrapState() + "</td></tr>");
            msg.append("</table><br1/>");
        } else {
            msg.append("ObjectID : NULL (object not found)<br1/>");
            oid = -1;
        }
        msg.append("Actions:<br1/><table>");
        msg.append("<tr><td align=center><button value=\"Open lock\" width=120 height=25 action=\"bypass L2Chest " + oid + " open\"/></td></tr>");
        msg.append("<tr><td align=center><button value=\"Pick lock\" width=120 height=25 action=\"bypass L2Chest " + oid + " pick\"/></td></tr>");
        msg.append("<tr><td align=center><button value=\"Force lock\" width=120 height=25 action=\"bypass L2Chest " + oid + " force\"/></td></tr>");
        if (chest.isTrapKnown()) {
            if (chest.isTrapped())
                msg.append("<tr><td align=\"center\"><button value=\"Untrap\" width=120 height=25 action=\"bypass L2Chest " + oid + " untrap\"/></td></tr>");
        }
        else
            msg.append("<tr><td align=center><button value=\"Check for traps\" width=120 height=25 action=\"bypass L2Chest " + oid + " check\"/></td></tr>");
        msg.append("<tr><td align=center><button value=\"Leave it be\" width=120 height=25 action=\"bypass L2Chest " + oid + " leave\"/></td></tr>");
        msg.append("</table>");
        msg.append("</body></html>");
        Util.sendHtml(pc, msg.toString());
    }
}
