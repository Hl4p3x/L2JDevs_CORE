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

import org.l2jdevs.gameserver.handler.ChatHandler;
import org.l2jdevs.gameserver.handler.IChatHandler;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.quest.Quest;
import org.l2jdevs.gameserver.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User chat 'dot' commands handlers, such as `.gcq` and so.
 *
 * todo: refactor it to chain of responsibility
 *
 * @author RKorskov
 */

public class UserDotCommands {
    private static final Logger LOG = //
        LoggerFactory.getLogger(UserDotCommands.class);

    /**
     * chat line processor.
     * commands case-insensetive (unless stated otherwise).
     * @return true, if at least one command evaluated
     */
    public static boolean processChatLine(final L2PcInstance pc,
                                          final String text) {
        switch(text.toUpperCase()) {
        case ".GQC":
        case ".GETQUESTSCOMPLETED":
        case ".GET_QUESTS_COMPLETED": {
            evalListQuestsCompleted(pc, text);
            return true;
        }
        case ".UPTIME":
        case ".UPT": {
            evalPCUptime(pc);
            return true;
        }
        case ".I":
        case ".INV": {
            evalListInventory(pc);
            return true;
        }
        case ".HELP":
        case ".LSC":
        case ".LISTOFCOMMANDS":
        case ".LIST_OF_COMMANDS": {
            evalListOfCommands(pc);
            break;
        }
        }
        return false;
    }

    /**
     * show list of quests with 'completed' flag
     * fixme! fetch list of quests from DB
     */
    private static void evalListQuestsCompleted(final L2PcInstance pc,
                                                final String text) {
        StringBuilder msg = new StringBuilder("<html><title>Completed Quests</title><body><table border=\"0\" width=\"100%\">");
        msg.append("<tr><td>FIX</td><td>ME!</td></tr>");
        msg.append("<tr><td>get list of quests of PC from DB</td></tr>");
        for(Quest q : pc.getAllCompletedQuests()) { // fixme!
            msg.append("<tr><td>");
            msg.append(q.getId());
            msg.append("</td><td>");
            msg.append(q.getName());
            msg.append("</td></tr>");
        }
        msg.append("</table></body></html>");
        Util.sendHtml(pc, msg.toString());
    }

    final static int CHAT_TYPE_TELL = 2;

    /**
     * tell PC's uptime to himself
     */
    private static void evalPCUptime(final L2PcInstance pc) {
        IChatHandler handler = ChatHandler.getInstance() //
            .getHandler(CHAT_TYPE_TELL);
        if(handler == null) return;
        int um = (int)(pc.getUptime() / 60000),
            uh = um / 60;
        if(um > 59) um %= 60;
        String msg = String.format("uptime %d:%02d", uh, um);
        handler.handleChat(CHAT_TYPE_TELL, pc, pc.getName(), msg);
    }

    private static void evalListInventory(final L2PcInstance pc) {
        StringBuilder msg = new StringBuilder("<html><title>Inventory</title><body><table border=\"0\" width=\"100%\">");
        msg.append("</table></body></html>");
        Util.sendHtml(pc, msg.toString());
    }

    private static void evalListOfCommands(final L2PcInstance pc) {
        StringBuilder msg = new StringBuilder("<html><title>List of user's commands</title><body><table border=\"0\" width=\"100%\">");
        msg.append("<tr><td>.gqc</td><td>list of completed quests</td></tr>");
        msg.append("<tr><td>.lsc</td><td>list of commands</td></tr>");
        msg.append("<tr><td>.help</td><td>this help</td></tr>");
        msg.append("<tr><td>.upt</td><td>PC's uptime</td></tr>");
        msg.append("</table></body></html>");
        Util.sendHtml(pc, msg.toString());
    }
}
