/*
 * Copyright © 2020 L2RogueLike
 *
 * This file is part of L2JDevs/L2RogueLike fork.
 *
 * L2JDevs & L2RogueLike is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2JDevs & L2RogueLike is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jdevs.roguelike;

import org.l2jdevs.gameserver.model.actor.instance.L2ChestInstance;
import org.l2jdevs.gameserver.model.actor.instance.L2PcInstance;
import org.l2jdevs.gameserver.model.items.instance.L2ItemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * collection of deluxe chest key manipulation methods, used by L2ChestInstance and L2Chest
 *
 * @author RKorskov
 */
public class DeluxeKeyUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DeluxeKeyUtils.class);

    private final static int[] __DELUXE_CHEST_KEY = {6665, 6666, 6667, 6668, 6669, 6670, 6671, 6672};

    /**
     * check of if PC have any amount of any Deluxe Chest Key(s)
     * 6665 Deluxe Chest Key - Grade 1
     * 6666 Deluxe Chest Key - Grade 2
     * 6667 Deluxe Chest Key - Grade 3
     * 6668 Deluxe Chest Key - Grade 4
     * 6669 Deluxe Chest Key - Grade 5
     * 6670 Deluxe Chest Key - Grade 6
     * 6671 Deluxe Chest Key - Grade 7
     * 6672 Deluxe Chest Key - Grade 8
     *
     * @param pc
     * @return true if PC has any Deluxe Chest Key
     */
    public static boolean hasDeluxeKey(final L2PcInstance pc) {
        boolean f = false;
        for (int i : __DELUXE_CHEST_KEY) {
            L2ItemInstance[] keys = pc.getInventory().getAllItemsByItemId(i);
            for (L2ItemInstance k : keys) {
                long n = k.getCount();
                LOG.error("DeluxeKeyUtils : hasDeluxeKey : key " + i + " of " + n);
                f |= n > 0;
            }
        }
        return f;
    }

    public static int getDeluxeKeyByLevel(final L2PcInstance pc, final int chestLevel) {
        int i = (chestLevel / 10) - 1;
        if (i < 0)
            i = __DELUXE_CHEST_KEY[0];
        else {
            if (i > __DELUXE_CHEST_KEY.length)
                i = __DELUXE_CHEST_KEY[__DELUXE_CHEST_KEY.length - 1];
            else
                i = __DELUXE_CHEST_KEY[i];
        }
        L2ItemInstance[] keys = pc.getInventory().getAllItemsByItemId(i);
        for (L2ItemInstance k : keys) {
            long n = k.getCount();
            LOG.error("DeluxeKeyUtils : hasDeluxeKey : key " + i + " of " + n);
            if (n > 0)
                return long2int(n);
        }
        return 0;
    }

    public static L2ItemInstance[] getDeluxeKeys(final L2PcInstance pc) {
        List<L2ItemInstance> dkl = new ArrayList<>();
        for (int i : __DELUXE_CHEST_KEY) {
            L2ItemInstance[] keys = pc.getInventory().getAllItemsByItemId(i);
            for (L2ItemInstance k : keys) {
                long n = k.getCount();
                LOG.error("DeluxeKeyUtils : hasDeluxeKey : key " + i + " of " + n);
                if (n > 0)
                    dkl.add(k);
            }
        }
        return dkl.toArray(new L2ItemInstance[0]);
    }

    private static int long2int(final long n) {
        if (n > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        return (int) n;
    }

    /**
     * returns key grade by itemId
     * @param key
     * @return key grade [1,8]
     */
    public static int getKeyGrade(L2ItemInstance key) {
        for(int i = 0; i < __DELUXE_CHEST_KEY.length; ++i)
            if(__DELUXE_CHEST_KEY[i] == key.getId())
                return i+1;
        return 0;
    }

    /**
     * returns key level by itemId
     * @param key
     * @return key grade {19,29,...,89}
     */
    public static int getKeyLevel(L2ItemInstance key) {
        for(int i = 0; i < __DELUXE_CHEST_KEY.length; ++i)
            if(__DELUXE_CHEST_KEY[i] == key.getId())
                return i * 10 + 19;
        return 0;
    }
}
