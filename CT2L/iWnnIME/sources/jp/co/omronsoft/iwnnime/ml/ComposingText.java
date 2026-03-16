package jp.co.omronsoft.iwnnime.ml;

import android.util.Log;
import java.util.ArrayList;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class ComposingText {
    public static final int LAYER0 = 0;
    public static final int LAYER1 = 1;
    public static final int LAYER2 = 2;
    public static final int MAX_LAYER = 3;
    protected ArrayList<StrSegment>[] mStringLayer = new ArrayList[3];
    protected int[] mCursor = new int[3];

    public ComposingText() {
        for (int i = 0; i < 3; i++) {
            this.mStringLayer[i] = new ArrayList<>();
            this.mCursor[i] = 0;
        }
    }

    public void debugout() {
        for (int i = 0; i < 3; i++) {
            Log.d("IWnnIME", "ComposingText[" + i + "]");
            Log.d("IWnnIME", "  cur = " + this.mCursor[i]);
            StringBuffer tmp = new StringBuffer();
            for (StrSegment ss : this.mStringLayer[i]) {
                tmp.append("(");
                tmp.append(ss.string);
                tmp.append(iWnnEngine.DECO_OPERATION_SEPARATOR);
                tmp.append(ss.from);
                tmp.append(iWnnEngine.DECO_OPERATION_SEPARATOR);
                tmp.append(ss.to);
                tmp.append(")");
            }
            Log.d("IWnnIME", "  str = " + tmp.toString());
        }
    }

    public StrSegment getStrSegment(int layer, int pos) {
        try {
            ArrayList<StrSegment> strLayer = this.mStringLayer[layer];
            if (pos < 0) {
                pos = strLayer.size() - 1;
            }
            if (pos >= strLayer.size() || pos < 0) {
                return null;
            }
            return strLayer.get(pos);
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(int layer, int from, int to) {
        try {
            StringBuffer buf = new StringBuffer();
            ArrayList<StrSegment> strLayer = this.mStringLayer[layer];
            for (int i = from; i <= to; i++) {
                StrSegment ss = strLayer.get(i);
                buf.append(ss.string);
            }
            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(int layer) {
        return toString(layer, 0, this.mStringLayer[layer].size() - 1);
    }

    private void modifyUpper(int layer, int mod_from, int mod_len, int org_len) {
        if (layer < 2) {
            int uplayer = layer + 1;
            ArrayList<StrSegment> strUplayer = this.mStringLayer[uplayer];
            if (strUplayer.size() <= 0) {
                strUplayer.add(new StrSegment(toString(layer), 0, this.mStringLayer[layer].size() - 1));
                modifyUpper(uplayer, 0, 1, 0);
                return;
            }
            int mod_to = mod_from + (mod_len == 0 ? 0 : mod_len - 1);
            int org_to = mod_from + (org_len == 0 ? 0 : org_len - 1);
            StrSegment last = strUplayer.get(strUplayer.size() - 1);
            if (last.to < mod_from) {
                last.to = mod_to;
                last.string = toString(layer, last.from, last.to);
                modifyUpper(uplayer, strUplayer.size() - 1, 1, 1);
                return;
            }
            int uplayer_mod_from = -1;
            int uplayer_org_to = -1;
            int i = 0;
            while (true) {
                if (i >= strUplayer.size()) {
                    break;
                }
                StrSegment ss = strUplayer.get(i);
                if (ss.from > mod_from) {
                    if (ss.to <= org_to) {
                        if (uplayer_mod_from < 0) {
                            uplayer_mod_from = i;
                        }
                        uplayer_org_to = i;
                        i++;
                    } else {
                        uplayer_org_to = i;
                        break;
                    }
                } else if (org_len == 0 && ss.from == mod_from) {
                    uplayer_mod_from = i - 1;
                    uplayer_org_to = i - 1;
                    break;
                } else {
                    uplayer_mod_from = i;
                    uplayer_org_to = i;
                    if (ss.to >= org_to) {
                        break;
                    } else {
                        i++;
                    }
                }
            }
            if (uplayer_mod_from < 0) {
                uplayer_mod_from = 0;
                uplayer_org_to = 0;
            }
            int diff = mod_len - org_len;
            StrSegment ss2 = strUplayer.get(uplayer_mod_from);
            int last_to = ss2.to;
            int next = uplayer_mod_from + 1;
            for (int i2 = next; i2 <= uplayer_org_to; i2++) {
                ss2 = strUplayer.get(next);
                if (last_to > ss2.to) {
                    last_to = ss2.to;
                }
                strUplayer.remove(next);
            }
            if (last_to >= mod_to) {
                mod_to = last_to + diff;
            }
            ss2.to = mod_to;
            ss2.string = toString(layer, ss2.from, ss2.to);
            for (int i3 = next; i3 < strUplayer.size(); i3++) {
                StrSegment ss3 = strUplayer.get(i3);
                ss3.from += diff;
                ss3.to += diff;
            }
            modifyUpper(uplayer, uplayer_mod_from, 1, (uplayer_org_to - uplayer_mod_from) + 1);
        }
    }

    public void insertStrSegment(int layer, StrSegment str) {
        int cursor = this.mCursor[layer];
        this.mStringLayer[layer].add(cursor, str);
        modifyUpper(layer, cursor, 1, 0);
        setCursor(layer, cursor + 1);
    }

    public void insertStrSegment(int layer1, int layer2, StrSegment str) {
        this.mStringLayer[layer1].add(this.mCursor[layer1], str);
        int[] iArr = this.mCursor;
        iArr[layer1] = iArr[layer1] + 1;
        for (int i = layer1 + 1; i <= layer2; i++) {
            int pos = this.mCursor[i - 1] - 1;
            StrSegment tmp = new StrSegment(str.string, pos, pos);
            ArrayList<StrSegment> strLayer = this.mStringLayer[i];
            strLayer.add(this.mCursor[i], tmp);
            int[] iArr2 = this.mCursor;
            iArr2[i] = iArr2[i] + 1;
            for (int j = this.mCursor[i]; j < strLayer.size(); j++) {
                StrSegment ss = strLayer.get(j);
                ss.from++;
                ss.to++;
            }
        }
        int cursor = this.mCursor[layer2];
        modifyUpper(layer2, cursor - 1, 1, 0);
        setCursor(layer2, cursor);
    }

    protected void replaceStrSegment0(int layer, StrSegment[] str, int from, int to) {
        ArrayList<StrSegment> strLayer = this.mStringLayer[layer];
        if (from < 0 || from > strLayer.size()) {
            from = strLayer.size();
        }
        if (to < 0 || to > strLayer.size()) {
            to = strLayer.size();
        }
        for (int i = from; i <= to; i++) {
            strLayer.remove(from);
        }
        for (int i2 = str.length - 1; i2 >= 0; i2--) {
            strLayer.add(from, str[i2]);
        }
        modifyUpper(layer, from, str.length, (to - from) + 1);
    }

    public void replaceStrSegment(int layer, StrSegment[] str, int num) {
        int cursor = this.mCursor[layer];
        replaceStrSegment0(layer, str, cursor - num, cursor - 1);
        setCursor(layer, (str.length + cursor) - num);
    }

    public void replaceStrSegment(int layer, StrSegment[] str) {
        int cursor = this.mCursor[layer];
        replaceStrSegment0(layer, str, cursor - 1, cursor - 1);
        setCursor(layer, (str.length + cursor) - 1);
    }

    public void deleteStrSegment(int layer, int from, int to) {
        int[] fromL = {-1, -1, -1};
        int[] toL = {-1, -1, -1};
        ArrayList<StrSegment> strLayer2 = this.mStringLayer[2];
        ArrayList<StrSegment> strLayer1 = this.mStringLayer[1];
        if (layer == 2) {
            fromL[2] = from;
            toL[2] = to;
            fromL[1] = strLayer2.get(from).from;
            toL[1] = strLayer2.get(to).to;
            fromL[0] = strLayer1.get(fromL[1]).from;
            toL[0] = strLayer1.get(toL[1]).to;
        } else if (layer == 1) {
            fromL[1] = from;
            toL[1] = to;
            fromL[0] = strLayer1.get(from).from;
            toL[0] = strLayer1.get(to).to;
        } else {
            fromL[0] = from;
            toL[0] = to;
        }
        int diff = (to - from) + 1;
        for (int lv = 0; lv < 3; lv++) {
            if (fromL[lv] >= 0) {
                deleteStrSegment0(lv, fromL[lv], toL[lv], diff);
            } else {
                int boundary_from = -1;
                int boundary_to = -1;
                ArrayList<StrSegment> strLayer = this.mStringLayer[lv];
                int i = 0;
                while (true) {
                    if (i >= strLayer.size()) {
                        break;
                    }
                    StrSegment ss = strLayer.get(i);
                    if ((ss.from >= fromL[lv - 1] && ss.from <= toL[lv - 1]) || (ss.to >= fromL[lv - 1] && ss.to <= toL[lv - 1])) {
                        if (fromL[lv] < 0) {
                            fromL[lv] = i;
                            boundary_from = ss.from;
                        }
                        toL[lv] = i;
                        boundary_to = ss.to;
                    } else {
                        if (ss.from <= fromL[lv - 1] && ss.to >= toL[lv - 1]) {
                            boundary_from = ss.from;
                            boundary_to = ss.to;
                            fromL[lv] = i;
                            toL[lv] = i;
                            break;
                        }
                        if (ss.from > toL[lv - 1]) {
                            break;
                        }
                    }
                    i++;
                }
                if (boundary_from != fromL[lv - 1] || boundary_to != toL[lv - 1]) {
                    deleteStrSegment0(lv, fromL[lv] + 1, toL[lv], diff);
                    StrSegment[] tmp = {new StrSegment(toString(lv - 1), boundary_from, boundary_to - diff)};
                    replaceStrSegment0(lv, tmp, fromL[lv], fromL[lv]);
                    return;
                }
                deleteStrSegment0(lv, fromL[lv], toL[lv], diff);
            }
            diff = (toL[lv] - fromL[lv]) + 1;
        }
    }

    private void deleteStrSegment0(int layer, int from, int to, int diff) {
        ArrayList<StrSegment> strLayer = this.mStringLayer[layer];
        if (diff != 0) {
            for (int i = to + 1; i < strLayer.size(); i++) {
                StrSegment ss = strLayer.get(i);
                ss.from -= diff;
                ss.to -= diff;
            }
        }
        for (int i2 = from; i2 <= to; i2++) {
            strLayer.remove(from);
        }
    }

    public int delete(int layer, boolean rightside) {
        int cursor = this.mCursor[layer];
        ArrayList<StrSegment> strLayer = this.mStringLayer[layer];
        if (!rightside && cursor > 0) {
            deleteStrSegment(layer, cursor - 1, cursor - 1);
            setCursor(layer, cursor - 1);
        } else if (rightside && cursor < strLayer.size()) {
            deleteStrSegment(layer, cursor, cursor);
            setCursor(layer, cursor);
        }
        return strLayer.size();
    }

    public int deleteForward(int layer) {
        int cursor = this.mCursor[layer];
        ArrayList<StrSegment> strLayer = this.mStringLayer[layer];
        if (strLayer.size() > cursor) {
            deleteStrSegment(layer, cursor, cursor);
            setCursor(layer, cursor);
        }
        return strLayer.size();
    }

    public ArrayList<StrSegment> getStringLayer(int layer) {
        try {
            return this.mStringLayer[layer];
        } catch (Exception e) {
            return null;
        }
    }

    private int included(int layer, int pos) {
        if (pos == 0) {
            return 0;
        }
        int uplayer = layer + 1;
        ArrayList<StrSegment> strLayer = this.mStringLayer[uplayer];
        int i = 0;
        while (i < strLayer.size()) {
            StrSegment ss = strLayer.get(i);
            if (ss.from > pos || pos > ss.to) {
                i++;
            } else {
                return i;
            }
        }
        return i;
    }

    public int setCursor(int layer, int pos) {
        if (pos > this.mStringLayer[layer].size()) {
            pos = this.mStringLayer[layer].size();
        }
        if (pos < 0) {
            pos = 0;
        }
        if (layer == 0) {
            this.mCursor[0] = pos;
            this.mCursor[1] = included(0, pos);
            this.mCursor[2] = included(1, this.mCursor[1]);
        } else if (layer == 1) {
            this.mCursor[2] = included(1, pos);
            this.mCursor[1] = pos;
            this.mCursor[0] = pos > 0 ? this.mStringLayer[1].get(pos - 1).to + 1 : 0;
        } else {
            this.mCursor[2] = pos;
            this.mCursor[1] = pos > 0 ? this.mStringLayer[2].get(pos - 1).to + 1 : 0;
            this.mCursor[0] = this.mCursor[1] > 0 ? this.mStringLayer[1].get(this.mCursor[1] - 1).to + 1 : 0;
        }
        return pos;
    }

    public int moveCursor(int layer, int diff) {
        int c = this.mCursor[layer] + diff;
        return setCursor(layer, c);
    }

    public int getCursor(int layer) {
        return this.mCursor[layer];
    }

    public int size(int layer) {
        return this.mStringLayer[layer].size();
    }

    public void clear() {
        for (int i = 0; i < 3; i++) {
            this.mStringLayer[i].clear();
            this.mCursor[i] = 0;
        }
    }
}
