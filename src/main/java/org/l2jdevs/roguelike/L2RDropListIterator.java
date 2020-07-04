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

import org.l2jdevs.util.Rnd;

public class L2RDropListIterator {
    private final int[] dropList;
    private int pos;

    protected L2RDropListIterator(int[] itemList) {
        dropList = itemList;
        pos = 0;
    }

    public static L2RDropListIterator L2RDropListIteratorFactory
        (final int[] itemList)
    {
        return new L2RDropListIterator(itemList);
    }

    public int next() {
        pos = Rnd.get(dropList.length);
        return dropList[pos];
    }

    public int slide() {
        if(pos < 1)
            pos = dropList.length;
        return dropList[--pos];
    }
}
