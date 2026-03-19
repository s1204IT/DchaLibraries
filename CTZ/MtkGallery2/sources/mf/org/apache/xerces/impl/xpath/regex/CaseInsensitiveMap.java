package mf.org.apache.xerces.impl.xpath.regex;

import java.lang.reflect.Array;

final class CaseInsensitiveMap {
    private static int[][][] caseInsensitiveMap;
    private static int CHUNK_SHIFT = 10;
    private static int CHUNK_SIZE = 1 << CHUNK_SHIFT;
    private static int CHUNK_MASK = CHUNK_SIZE - 1;
    private static int INITIAL_CHUNK_COUNT = 64;
    private static int LOWER_CASE_MATCH = 1;
    private static int UPPER_CASE_MATCH = 2;

    CaseInsensitiveMap() {
    }

    static {
        buildCaseInsensitiveMap();
    }

    public static int[] get(int codePoint) {
        if (codePoint < 65536) {
            return getMapping(codePoint);
        }
        return null;
    }

    private static int[] getMapping(int codePoint) {
        int chunk = codePoint >>> CHUNK_SHIFT;
        int offset = CHUNK_MASK & codePoint;
        return caseInsensitiveMap[chunk][offset];
    }

    private static void buildCaseInsensitiveMap() {
        caseInsensitiveMap = (int[][][]) Array.newInstance((Class<?>) int[].class, INITIAL_CHUNK_COUNT, CHUNK_SIZE);
        for (int i = 0; i < 65536; i++) {
            int lc = Character.toLowerCase((char) i);
            int uc = Character.toUpperCase((char) i);
            if (lc != uc || lc != i) {
                int[] map = new int[2];
                int index = 0;
                if (lc != i) {
                    int index2 = 0 + 1;
                    map[0] = lc;
                    index = index2 + 1;
                    map[index2] = LOWER_CASE_MATCH;
                    int[] lcMap = getMapping(lc);
                    if (lcMap != null) {
                        map = updateMap(i, map, lc, lcMap, LOWER_CASE_MATCH);
                    }
                }
                if (uc != i) {
                    if (index == map.length) {
                        map = expandMap(map, 2);
                    }
                    int index3 = index + 1;
                    map[index] = uc;
                    int index4 = index3 + 1;
                    map[index3] = UPPER_CASE_MATCH;
                    int[] ucMap = getMapping(uc);
                    if (ucMap != null) {
                        map = updateMap(i, map, uc, ucMap, UPPER_CASE_MATCH);
                    }
                }
                set(i, map);
            }
        }
    }

    private static int[] expandMap(int[] srcMap, int expandBy) {
        int oldLen = srcMap.length;
        int[] newMap = new int[oldLen + expandBy];
        System.arraycopy(srcMap, 0, newMap, 0, oldLen);
        return newMap;
    }

    private static void set(int codePoint, int[] map) {
        int chunk = codePoint >>> CHUNK_SHIFT;
        int offset = CHUNK_MASK & codePoint;
        caseInsensitiveMap[chunk][offset] = map;
    }

    private static int[] updateMap(int codePoint, int[] codePointMap, int ciCodePoint, int[] ciCodePointMap, int matchType) {
        for (int i = 0; i < ciCodePointMap.length; i += 2) {
            int c = ciCodePointMap[i];
            int[] cMap = getMapping(c);
            if (cMap != null && contains(cMap, ciCodePoint, matchType)) {
                if (!contains(cMap, codePoint)) {
                    set(c, expandAndAdd(cMap, codePoint, matchType));
                }
                if (!contains(codePointMap, c)) {
                    codePointMap = expandAndAdd(codePointMap, c, matchType);
                }
            }
        }
        if (!contains(ciCodePointMap, codePoint)) {
            set(ciCodePoint, expandAndAdd(ciCodePointMap, codePoint, matchType));
        }
        return codePointMap;
    }

    private static boolean contains(int[] map, int codePoint) {
        for (int i = 0; i < map.length; i += 2) {
            if (map[i] == codePoint) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(int[] map, int codePoint, int matchType) {
        for (int i = 0; i < map.length; i += 2) {
            if (map[i] == codePoint && map[i + 1] == matchType) {
                return true;
            }
        }
        return false;
    }

    private static int[] expandAndAdd(int[] srcMap, int codePoint, int matchType) {
        int oldLen = srcMap.length;
        int[] newMap = new int[oldLen + 2];
        System.arraycopy(srcMap, 0, newMap, 0, oldLen);
        newMap[oldLen] = codePoint;
        newMap[oldLen + 1] = matchType;
        return newMap;
    }
}
