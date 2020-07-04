-*- mode: markdown; coding: utf-8; indent-tabs-mode: nil; word-wrap: t -*-  
-*- eval: (set-language-environment Russian) -*-  
--- Time-stamp: <2019-12-23 09:07:04 roukoru>

L2JDevs RogueLike modification
==============================

# Changes vs L2J/L2JDevs #

## Core ##

Most of changes are configurable in L2JMods.properties

1. Random (relatively small) item drop ("All hail to RNG!") from NPCs
   (like a music crystal, key, potion, small accessory, scroll,
   s.shot, fish, lure, pet food, gem, rope, ...);
2. Equipment drop from NPCs;
3. Shopkeepers buy price configurable;
4. Champions have aura and random name;
5. Free teleportation max level is configurable;
6. Teleportaion price by PC level modifier is configurable with price
   scalable with PC's level;
7. Configurable minimal quest item drop chance (up to 1.0);
8. Configurable maximal NPCInteractionDistance as workaround over bugs
   in geodata;
9. Permadeath -- server-wide setting PCAllowRevive;
10. Killed PC always get death penalty and drops inventory;
11. NPC Respawn delay upped to several hours to prevent grinding (1
    hour, due limitations of respawn engine);
12. Distribution of newbie SShots from tutorial quests is configurable;
13. Fish monster despawn time is configurable;
14. Archers and xbowers often drop ammo;
15. Summoned in quest monsters have XP, drop and spoil: honey bear,
    kasha bear totem spirit, stakato marquess, ol mahum support troop,
    black legion stormtrooper, ol mahum officer tak;
16. Static quest monsters have XP, drop and spoil: plague zombie,
    varool foulclaw, kirunak, spirit of mirror, kaboo orc chief (all),
    water seer, undead priest, ol mahum sentry, warhound of a kaboo
    orc chief;
17. Items made stackable: scroll of enchant weapon or armor, s-shot packs;
18. Usage of (blessed) (spirit|soul) shots consumes MP, according to
    number and grade of s-shots activated;
19. Item craft success has base chance of 50%, modified by PC level and
    crafting skill level, recipe level (negative);
20. Raid boss curse level difference is configurable;
21. NPC's equipment drop, for_npc==t items replaced to random item of
    appropriate grade and type;
22. Champion's aura color related to its strength;
23. Males and females are bit different in stats;
24. Champion different in strength, in full range from base to maximum
    configured;
25. Failure in crop trade due insufficient amount of given crop to get
    any reward has verbose notification message;
26. All mobs have individual variability in power (max dispersion
    defined in config);
27. Alternative item craft have success rate, depending on PC's level,
    skill, recipe level, RNG; also, failure in item creation gives
    partial XP&SP reward;
28. Beasts counts as animals for drop-wise;
29. NPC chance to engage in combat depends on level difference with PC;
30. NPC chance to engage in combat increases on remained health of PC;
31. Console command `.Get_Quests_Completed` (`.gqc`) implemented;
32. NPC chance to engage in combat increases, if PC is in combat;
33. NPC chance to engage in combat depends on P/M attack difference
    rate with PC;
34. NPC aggro influenced by time of day: night is for undead, beasts
    and half-aggressive mobs;
35. Animals also may drop necklace, ring, earring pieces;
36. Enchant item have recoverable failure rate (42%);
37. '/unstuck' command not work during fight;

## Data ##

1. Fish monsters have normal drop;
2. Animal drops gradually stripped from complex items (animal body
   parts are acceptable);
3. Many graded items have their crystall value (pins, scrolls, pouches, ...);
4. Enabled manor crop and seed setup (why it was disabled, after all?);
5. Many items made stackable;
6. Some item name typos fixed (like a missing spaces and so);
7. Quest 330 'Adept of taste' have quest item grade boundary
   (for moss, honey, root);
8. NG-orcs stripped from D+ weapons; they are receive appropriate NG
   weapons (club, bone club, short sword, broadsword, orcish sword);
9. Quest 335 'The Song Of The Hunter' quest monsters have normal xp,
   drop and spoil;
10. Quest 258 'Bring Wolf Pelt' now have wider range of rewards;
11. Quest 159 'Protect The Water Source' plague zombies affected by
    quest item drop config setting;
12. Race of fisherman Ogord (in orc village) set to orc;
13. Quest giver pays extra reward for each tenth quest item (affected:
    257 258 259 260 269 273 283 292 293 306 316 317)  
    (i.e.: paid = items * reward + (items / 10) * bonus);
14. Crops have reference price;
15. Quest mob list expanded for quests: 292 294 296 297;
16. Quest 156: Lilith rewards mages with spiritshots;
17. Quest mobs list expanded: 292 294 296 297 406;
18. Quest 418: quest items always drops;
19. Mob 'Puma' 20510 renamed to 'Pumette' (collides with Puma-15);
20. Quest 297: gatekeeper rewards PC with `ceil(starstones/10)`
    tokens, starstones gathering unlimited;
21. NPC archers (level 1..40) now have weapon, corresponding to their level;
22. Conditionally give 1st class tranfer quest reward (configurable):
    {62 63 401 402 403 404 405 406 407 408 409 414 415 416 417 418};
23. In (most) quests, replaced `giveAdena` on `giveAdenaFuzzy`, which
    gives player adena in range [n/2, n);
24. Quests randomly distribute quest item on party members:
    {102 151 152 159 162 163 258 261 262 264 295 266 268 277 291 295 297 313 319 330};
25. Beast s.* shot packs priced and may be sold;
26. Cruma Marsh NPCs inventory reshuffled and re-equipped;
27. Ruins of Agony and Despair NPCs re-equipped;
28. Quest's Dimensional Diamond reward configurable;
29. NPC's droplist expanded;

# TODO #

1. Deluxe Chest Key to Thief Key convertor skill;
2. Deluxe Chest Key normal work as chest open item;
3. Champion aura change from TVT-aura to random visual effect (halo,
   glow, sparks, ...);
4. Respawn time upper limit fix (currently ~1h in most cases);
5. NPC's equipment drop with enchantment level 0..max as in
   npcTemplate (and in some cases -- 1m);
6. (magical) Skills requires books to learn;
7. Magic learn books in loot;
8. Offline money withdrawal, somewhat like a lodging tax (see vitality);
9. Champion's extra combat skills;
10. Console command to show completed quests (fixme! `.gqc`);
11. Conditionally give 1st class tranfer quest reward (configurable):
    ```
    Q00410_PathOfThePalusKnight
    Q00411_PathOfTheAssassin
    Q00412_PathOfTheDarkWizard
    Q00413_PathOfTheShillienOracle
    ```

## video recording ##

Для win7-0, живущей в VBox'е, размеры брать по: `xwininfo -name
win7-0` и -geometry `512x288+1081+298`.

Писать как (например):  
```
ffmpeg -f x11grab -s 512,288 -framerate 25 -i :0.0+1081,298 -async 1 -vsync 1 -y -vcodec libx264 /tmp/win7-0.`date +%F_%H%M%S`.mkv
```  
NB! пишется область экрана X11, а не содержимое окна VBox'а.

Перекодировать в avi с масштабированием до 512:  
```
mencoder 162207.mkv -vf scale -zoom -xy 512 -oac copy -ovc lavc -lavcopts vcodec=mjpeg:mbd=1:vbitrate=1800 -o 162207.avi
```

## mob on PCs attack notes ##

src/main/java/org/l2jdevs/gameserver/ai/L2AttackableAI.java :: autoAttackCondition

```java
// L2Attackable me = getActiveChar();
boolean wanna2Attack(pc) {
  agro = me.isAggressive() ? 80 : 20;
  pwr = me.getPower();
  if(me.isChampion())
    agro += pwr * 2;
  else
    agro *= pwr;
  ldif = me.level - pc.level; // ~80 vs 95, so: [-94;94]
  agro += ldif * 2;
  return agro < Rnd.get(100);
}
```
