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
package org.l2jdevs.gameserver.handler;

import java.util.logging.Logger;

import org.l2jdevs.Config;
import org.l2jdevs.gameserver.model.actor.L2Playable;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.l2jdevs.util.Rnd;

/**
 * Mother class of all Item Handlers.
 */
public interface IItemHandler
{
	public static final Logger _log = Logger.getLogger(IItemHandler.class.getName());
	
	/**
	 * Launch task associated to the item.
	 * @param playable the non-NPC character using the item
	 * @param item L2ItemInstance designating the item to use
	 * @param forceUse ctrl hold on item use
	 * @return {@code true} if the item all conditions are met and the item is used, {@code false} otherwise.
	 */
	public boolean useItem(L2Playable playable, L2ItemInstance item, boolean forceUse);

    default int evalMPCostSS(final int pc_level,
                             final int weapon_grade,
                             final int sshots_count) {
        if (Config.L2JMOD_SSHOT_USE_MP <= 0)
            return 0;
        // see L2Character : use bow
        final int smp = (int)((sshots_count * (weapon_grade + 1) * Config.L2JMOD_SSHOT_USE_MP) / 2);
        if(smp < 2) return 1;
        return smp + Rnd.get(smp);
    }

    default int evalMPCostBSS(final int pc_level,
                              final int weapon_grade,
                              final int sshots_count) {
        if (Config.L2JMOD_BSSHOT_USE_MP <= 0)
            return 0;
        // see L2Character : use bow
        final int smp = (int)((sshots_count * (weapon_grade + 1) * Config.L2JMOD_BSSHOT_USE_MP) / 2);
        if(smp < 2) return 1;
        return smp + Rnd.get(smp);
    }
}
