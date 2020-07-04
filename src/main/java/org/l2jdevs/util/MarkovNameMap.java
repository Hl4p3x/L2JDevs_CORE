// -*- coding: utf-8; indent-tabs-mode: nil; word-wrap: t; mode: java; -*-
// -*- eval: (set-language-environment Russian) -*-
// Time-stamp: <2019-09-05 20:58:34 roukoru>

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

package org.l2jdevs.util;

import java.util.HashMap;

/**
 * https://github.com/Tw1ddle/MarkovNameGenerator
 * http://www.roguebasin.com/index.php?title=Markov_chains_name_generator_in_Python
 * Markov Name model
 * A random name generator, by Peter Corbett
 * http://www.pick.ucam.org/~ptc24/mchain.html
 * This script is hereby entered into the public domain
 * from http://www.geocities.com/anvrill/names/cc_goth.html
 *
 * @author RKorskov
 */
public class MarkovNameMap {

    protected HashMap <String, String> nameDict = null;
    public final static char END_OF_NAME = 0x0; // name generation terminator
    private Rnd RNG = null;

    private static class SingletonHolder {
        public static final MarkovNameMap instance = new MarkovNameMap();
    }

    private MarkovNameMap() {
        if(nameDict == null)
            nameDict = new HashMap <String, String> ();
        if(RNG == null)
            RNG = new Rnd();
    }

    public static MarkovNameMap getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * if K exists in the map,
     * than append V to existing value in the map,
     * else put new K:V pair into map
     */
    public void put(final String K, final char V) {
        if(nameDict.containsKey(K)) {
            String v = nameDict.get(K);
            if(v.indexOf(V) < 0)
                nameDict.put(K, v + String.valueOf(V));
        }
        else
            nameDict.put(K, String.valueOf(V));
    }

    public char get(final String K) {
        char rc = END_OF_NAME;
        if(nameDict.containsKey(K)) {
            String rs = nameDict.get(K);
            int cp = rs.length();
            if(cp > 0) {
                cp = cp > 1 ? RNG.nextInt(cp) : 0;
                rc = rs.charAt(cp);
            }
        }
        return rc;
    }

    public void print() {
        for(String k : nameDict.keySet())
            System.out.printf("%s : %s\n", k, nameDict.get(k));
    }
}
