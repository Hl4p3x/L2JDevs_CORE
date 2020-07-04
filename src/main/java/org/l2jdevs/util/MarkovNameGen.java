// -*- coding: utf-8; indent-tabs-mode: nil; word-wrap: t; mode: java; -*-
// -*- eval: (set-language-environment Russian) -*-
// Time-stamp: <2019-11-15 11:44:28 roukoru>

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

import java.lang.Character;

/**
 * https://github.com/Tw1ddle/MarkovNameGenerator
 * http://www.roguebasin.com/index.php?title=Markov_chains_name_generator_in_Python
 * Markov Name model
 * A random name generator, by Peter Corbett
 * http://www.pick.ucam.org/~ptc24/mchain.html
 * This script is hereby entered into the public domain
 * from http://www.geocities.com/anvrill/names/cc_goth.html
 *
 * Usage (something like this) :
 * MarkovNameGen mng = new MarkovNameGen();
 * ...
 * System.out.println (mng.getName());
 *
 * @author RKorskov
 */

public class MarkovNameGen {

    final static String[] PLACES = {
        "adara", "adena", "adrianne", "alarice", "alvita", "amara", "ambika",
        "antonia", "araceli", "balandria", "basha", "beryl", "bryn", "callia",
        "caryssa", "cassandra", "casondrah", "chatha", "ciara", "cynara",
        "cytheria", "dabria", "darcei", "deandra", "deirdre", "delores",
        "desdomna", "devi", "dominique", "drucilla", "duvessa", "ebony",
        "fantine", "fuscienne", "gabi", "gallia", "hanna", "hedda", "jerica",
        "jetta", "joby", "kacila", "kagami", "kala", "kallie", "keelia",
        "kerry", "kerry-ann", "kimberly", "killian", "kory", "lilith",
        "lucretia", "lysha", "mercedes", "mia", "maura", "perdita", "quella",
        "riona", "safiya", "salina", "severin", "sidonia", "sirena", "solita",
        "tempest", "thea", "treva", "trista", "vala", "winta"
    };

    private final static int MAX_NAME_LENGTH = 9,
        MIN_CHAIN_LENGTH = 2,
        MAX_CHAIN_LENGTH = 10;
    private final static byte PAD_BYTE = ' '; // space
    // protected int chainLength;
    protected String namePad;
    protected MarkovNameMap nameMap = null;

    public MarkovNameGen() {
        this(2);
    }

    public MarkovNameGen(final int markovChainLength) {
        initNameMap(markovChainLength);
    }

    private void initNameMap(final int markovChainLength) {
        int i;
        int chainLength =
            markovChainLength < MIN_CHAIN_LENGTH
            ? MIN_CHAIN_LENGTH
            : (markovChainLength > MAX_CHAIN_LENGTH
               ? MAX_CHAIN_LENGTH
               : markovChainLength);
        byte[] pfx = new byte [chainLength];
        for (i = 0; i < pfx.length; pfx[i++] = PAD_BYTE);
        namePad = new String (pfx); // ?
        nameMap = MarkovNameMap.getInstance();
        for (String p : PLACES) {
            String s = namePad + p;
            for (i = 0; i < p.length(); ++i)
                nameMap.put(s.substring(i, i + chainLength),
                            s.charAt(i + chainLength));
            nameMap.put(s.substring(p.length(), p.length() + chainLength),
                        MarkovNameMap.END_OF_NAME);
        }
    }

    /**
     * (main) name generator function.
     * name will always start with letter;
     * name always have length no less than 1 char (? is it always true by implementation of Markov chain ?);
     * name may contain a non-letter chars;
     * name always start from capital letter;
     * char from non-letter char always capitalised;
     * name will not have dangling non-letter chars;
     * name will not have any pairs of non-letter chars
     */
    public String getName() {
        // if (nameMap == null) {initNameMap();}
        StringBuilder name = new StringBuilder();
        char suffix;
        String prefix = namePad;
        boolean N1CF, // Not a 1st Char Flag
            CF, // CharFlag : isLetter by current prefix
            SF; // SingleFlag : name length is exactly 1 (one) char long
        for(N1CF = false;;) {
            suffix = nameMap.get(prefix.toLowerCase());
            // if (suffix == MarkovNameMap.END_OF_NAME) System.out.println ("--> End_of_Chain"); else System.out.println ("--> " + suffix);
            CF = Character.isLetter(suffix);
            if(!(CF || N1CF)) {
                /** name should start from non-alphabet chars*/
                continue;
            }
            if(suffix == MarkovNameMap.END_OF_NAME
               || name.length() > MAX_NAME_LENGTH)
                break;
            if(N1CF) {
                /** dirty capitalization hack.
                 * always toupper 1st letter,
                 * if current char is not alphabetic, next letter have
                 * to be upped in prevention of "Anna-kaisa",
                 * "Maya-liisa" and so.
                 */
                N1CF = CF; // Character.isLetter(suffix); // not in [a-z] 
            }
            else {
                suffix = Character.toUpperCase(suffix);
                N1CF = true;
            }
            name.append(suffix);
            prefix = prefix.substring(1) + String.valueOf(suffix);
        }
        // strip dangling non-alphabet characters, if any
        return name.toString();
    }

    public String getConstructName() {
        return getName();
    }

    public void print() { nameMap.print(); }
}
