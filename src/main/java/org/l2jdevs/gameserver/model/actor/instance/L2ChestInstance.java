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
import org.l2jdevs.gameserver.SevenSignsFestival;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages all chest.
 *
 * @author Julian
 */
public final class L2ChestInstance extends L2Attackable { // L2MonsterInstance
    private static final Logger LOG = LoggerFactory.getLogger(L2ChestInstance.class);
    private volatile boolean _specialDrop;

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
        LOG.error("chest doItemDrop id=" + id);
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
        LOG.error("chest _specialDrop id=" + id);
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
     * @param command The command string received from client
     */
    @Override
    public void onBypassFeedback(final L2PcInstance pc, final String cmd) {
        // if (canInteract(player))
        {
            LOG.error("chest onBypassFeedback cmd=" + cmd);
            if (isBusy() && (getBusyMessage().length() > 0)) {
                chestIsBusy(this, pc);
                return;
            }
            IBypassHandler handler = BypassHandler.getInstance().getHandler(cmd);
            if (handler != null) {
                handler.useBypass(cmd, pc, this);
            } else {
                LOG.info("Unknown NPC bypass: \"{}\" NpcId: {}", cmd, getId());
                return;
            }
            // if (cmd.startsWith("Chat"))
            {
                // fixme: stub code
                /*
                try {
                    StringTokenizer st = new StringTokenizer(command.trim());
                    // ...
                } catch (Exception e3) {
                    LOG.warn("Failed to retrieve data from bypass command. NpcId: {},  Command: {}", getId(), cmd);
                }
                */
                if (openChest(this, pc, cmd))
                    doDie(pc); // die to loot
            }
        }
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
    public void showChatWindow(L2PcInstance pc, int val) {
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
        openChest(this, pc, String.valueOf(val));

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        pc.sendPacket(ActionFailed.STATIC_PACKET);
    }

    private static boolean openChest(final L2ChestInstance obj, final L2PcInstance pc, final String cmd) {
        // fixme: stub
        StringBuilder msg = new StringBuilder("<html><title>Chest</title><body>");
        msg.append("Actions:<br1/><table>");
        msg.append("<tr><td><button value=\"Open\" width=40 height=25 action=\"bypass -h npc_");
        msg.append(obj.getId());
        msg.append("_chest open\"/></td></tr>");
        msg.append("<tr><td><button value=\"Leave\" width=40 height=25 action=\"bypass -h npc_");
        msg.append(obj.getId());
        msg.append("_chest leave\"/></td></tr>");
        msg.append("</table><br1/>Server command was: <br>");
        msg.append(cmd);
        msg.append("</body></html>");
        Util.sendHtml(pc, msg.toString());
        /*
        String msg = "Open chest";
        // ... some code
        obj.showChatWindow(pc, msg);
        */
        return false;
    }
}
