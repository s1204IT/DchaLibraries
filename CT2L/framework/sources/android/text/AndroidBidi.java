package android.text;

import android.text.Layout;

class AndroidBidi {
    private static native int runBidi(int i, char[] cArr, byte[] bArr, int i2, boolean z);

    AndroidBidi() {
    }

    public static int bidi(int dir, char[] chs, byte[] chInfo, int n, boolean haveInfo) {
        int dir2;
        if (chs == null || chInfo == null) {
            throw new NullPointerException();
        }
        if (n < 0 || chs.length < n || chInfo.length < n) {
            throw new IndexOutOfBoundsException();
        }
        switch (dir) {
            case -2:
                dir2 = -1;
                break;
            case -1:
                dir2 = 1;
                break;
            case 0:
            default:
                dir2 = 0;
                break;
            case 1:
                dir2 = 0;
                break;
            case 2:
                dir2 = -2;
                break;
        }
        int result = runBidi(dir2, chs, chInfo, n, haveInfo);
        return (result & 1) == 0 ? 1 : -1;
    }

    public static Layout.Directions directions(int dir, byte[] levels, int lstart, char[] chars, int cstart, int len) {
        boolean swap;
        if (len == 0) {
            return Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
        int baseLevel = dir == 1 ? 0 : 1;
        int curLevel = levels[lstart];
        int minLevel = curLevel;
        int runCount = 1;
        int e = lstart + len;
        for (int i = lstart + 1; i < e; i++) {
            int level = levels[i];
            if (level != curLevel) {
                curLevel = level;
                runCount++;
            }
        }
        int visLen = len;
        if ((curLevel & 1) != (baseLevel & 1)) {
            while (true) {
                visLen--;
                if (visLen < 0) {
                    break;
                }
                char ch = chars[cstart + visLen];
                if (ch == '\n') {
                    visLen--;
                    break;
                }
                if (ch != ' ' && ch != '\t') {
                    break;
                }
            }
            visLen++;
            if (visLen != len) {
                runCount++;
            }
        }
        if (runCount == 1 && minLevel == baseLevel) {
            if ((minLevel & 1) != 0) {
                return Layout.DIRS_ALL_RIGHT_TO_LEFT;
            }
            return Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
        int[] ld = new int[runCount * 2];
        int maxLevel = minLevel;
        int levelBits = minLevel << 26;
        int prev = lstart;
        int curLevel2 = minLevel;
        int i2 = lstart;
        int e2 = lstart + visLen;
        int n = 1;
        while (i2 < e2) {
            int level2 = levels[i2];
            if (level2 != curLevel2) {
                curLevel2 = level2;
                if (level2 > maxLevel) {
                    maxLevel = level2;
                } else if (level2 < minLevel) {
                    minLevel = level2;
                }
                int n2 = n + 1;
                ld[n] = (i2 - prev) | levelBits;
                n = n2 + 1;
                ld[n2] = i2 - lstart;
                levelBits = curLevel2 << 26;
                prev = i2;
            }
            i2++;
            n = n;
        }
        ld[n] = ((lstart + visLen) - prev) | levelBits;
        if (visLen < len) {
            int n3 = n + 1;
            ld[n3] = visLen;
            ld[n3 + 1] = (len - visLen) | (baseLevel << 26);
        }
        if ((minLevel & 1) == baseLevel) {
            minLevel++;
            swap = maxLevel > minLevel;
        } else {
            swap = runCount > 1;
        }
        if (swap) {
            for (int level3 = maxLevel - 1; level3 >= minLevel; level3--) {
                int i3 = 0;
                while (i3 < ld.length) {
                    if (levels[ld[i3]] >= level3) {
                        int e3 = i3 + 2;
                        while (e3 < ld.length && levels[ld[e3]] >= level3) {
                            e3 += 2;
                        }
                        int low = i3;
                        for (int hi = e3 - 2; low < hi; hi -= 2) {
                            int x = ld[low];
                            ld[low] = ld[hi];
                            ld[hi] = x;
                            int x2 = ld[low + 1];
                            ld[low + 1] = ld[hi + 1];
                            ld[hi + 1] = x2;
                            low += 2;
                        }
                        i3 = e3 + 2;
                    }
                    i3 += 2;
                }
            }
        }
        return new Layout.Directions(ld);
    }
}
