package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import com.android.common.speech.LoggingEvents;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import org.xmlpull.v1.XmlPullParserException;

public class Keyboard {
    public static final int EDGE_BOTTOM = 8;
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    public static final int EDGE_TOP = 4;
    public static final int GRID_HEIGHT = 5;
    private static final int GRID_SIZE = 50;
    private static final int GRID_WIDTH = 10;
    public static final int KEY_LINE_DEFAULT_KEYBOARD = 4;
    public static final int ROW_GRAVITY_CENTER = 0;
    public static final int ROW_GRAVITY_LEFT = 2;
    public static final int ROW_GRAVITY_RIGHT = 1;
    private static final int SETTING_ITEM_KEY_PREVIEW = 1;
    private static final int SETTING_ITEM_KEY_TOP = 0;
    static final String TAG = "Keyboard";
    private static final String TAG_CASE = "case";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_KEY = "Key";
    private static final String TAG_KEYBOARD = "Keyboard";
    private static final String TAG_ROW = "Row";
    private static final String TAG_ROW_KEY = "Row-Key";
    private static final String TAG_SWITCH = "switch";
    private static Context mContext;
    private int mCellHeight;
    private int mCellWidth;
    private int mDefaultHorizontalGap;
    private float mDefaultHorizontalGapRate;
    private int mDefaultKeyWidth;
    private int mDefaultVerticalGap;
    private int mDisplayWidth;
    private int[][] mGridNeighbors;
    private boolean mIsLangPack;
    private int mKeyHeight;
    private ArrayList<Integer> mKeyHeightCorrectionList;
    private int mKeyX;
    private int mKeyY;
    protected int mKeyboardCondition;
    private int mKeyboardMode;
    private int mKeyboardType;
    private List<Key> mKeys;
    private int mMiniKeyboardWidth;
    private int mPaddingLeft;
    private int mProximityThreshold;
    private float mRate;
    private int mRowCnt;
    private List<Row> mRows;
    private Key mShiftKey;
    private boolean mShifted;
    private int mTotalHeight;
    private int mTotalWidth;
    private int mXmlDefaultVerticalGap;
    private int mXmlLayoutResId;
    public static int CONDITION_MODE_HIRAGANA = 1;
    public static int CONDITION_MODE_FULL_ALPHA = 2;
    public static int CONDITION_MODE_FULL_NUM = 3;
    public static int CONDITION_MODE_FULL_KATAKANA = 4;
    public static int CONDITION_MODE_HALF_ALPHA = 5;
    public static int CONDITION_MODE_HALF_NUM = 6;
    public static int CONDITION_MODE_HALF_KATAKANA = 7;
    public static int CONDITION_MODE_HANGUL = 8;
    public static int CONDITION_MODE_PINYIN = 9;
    public static int CONDITION_MODE_BOPOMOFO = 10;
    public static int CONDITION_MODE_CYRILLIC = 11;
    public static int CONDITION_MODE = 15;
    public static int CONDITION_VOICE_OFF = 16;
    public static int CONDITION_VOICE_ON = 32;
    public static int CONDITION_SWITCH_IME_OFF = 64;
    public static int CONDITION_SWITCH_IME_ON = 128;
    public static int CONDITION_CURSOR_OFF = 256;
    public static int CONDITION_CURSOR_ON = 512;
    public static int CONDITION_NOINPUT = 1024;
    public static int CONDITION_INPUT = 2048;
    public static int CONDITION_SHIFT_OFF = 4096;
    public static int CONDITION_SHIFT_ON = 8192;
    public static int CONDITION_50KEY_VERTICAL_RIGHT = 16384;
    public static int CONDITION_50KEY_VERTICAL_LEFT = 32768;
    public static int CONDITION_50KEY_HORIZONTAL = 49152;
    public static int CONDITION_50KEY = 49152;
    public static int CONDITION_NUMBER_OFF = 65536;
    public static int CONDITION_NUMBER_ON = 131072;
    private static float SEARCH_DISTANCE = 1.8f;
    private static int VALUE_UNSPECIFIED = -1;

    public static class Row {
        public int defaultHeight;
        public int defaultHorizontalGap;
        public float defaultHorizontalGapRate;
        public int defaultWidth;
        public float defaultWidthRate;
        public int gravity;
        public ArrayList<Key> keysInRow;
        public int mode;
        private Keyboard parent;
        public int rowEdgeFlags;
        public int totalRowWidth;
        public int verticalGap;

        public Row(Keyboard parent) {
            this.parent = parent;
        }

        public Row(Resources res, Keyboard parent, XmlResourceParser parser) {
            this.parent = parent;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.WnnKeyboardView);
            this.defaultWidth = Keyboard.getDimensionOrFraction(a, 26, parent.mDisplayWidth, parent.mDefaultKeyWidth);
            this.defaultWidthRate = Keyboard.getFractionRate(a, 26, parent.mRate);
            this.defaultHeight = parent.mKeyHeight;
            int index = parent.mRowCnt - 1;
            if (index >= 0 && index < parent.mKeyHeightCorrectionList.size()) {
                this.defaultHeight = ((Integer) parent.mKeyHeightCorrectionList.get(index)).intValue() + this.defaultHeight;
            }
            this.defaultHorizontalGap = Keyboard.getDimensionOrFraction(a, 24, parent.mDisplayWidth, parent.mDefaultHorizontalGap);
            this.defaultHorizontalGapRate = Keyboard.getFractionRate(a, 24, parent.mDefaultHorizontalGapRate);
            this.verticalGap = parent.mDefaultVerticalGap;
            this.rowEdgeFlags = a.getInt(23, 0);
            this.mode = a.getResourceId(22, 0);
            this.gravity = a.getInt(45, 0);
            a.recycle();
            this.totalRowWidth = 0;
            this.keysInRow = new ArrayList<>();
        }
    }

    public static class Key {
        private static final int DEFAULT_HEIGHT_ROW = 1;
        private static final String NUMBER_CHARACTER = "[0-9０-９]";
        public int[] codes;
        public int contentHeight;
        public int contentWidth;
        public CharSequence description;
        public int edgeFlags;
        public int gap;
        public float gapRate;
        public int height;
        public int heightRow;
        public CharSequence hintLabel;
        public CharSequence hintLabelLeft;
        public CharSequence hintLabelRight;
        public Drawable icon;
        public Drawable iconPreview;
        public boolean isEmptyKey;
        public boolean isSecondKey;
        public CharSequence keyPreviewLabel;
        private Keyboard keyboard;
        public CharSequence label;
        public boolean mIsIconSkin;
        public boolean mIsPreviewSkin;
        private boolean mIsSkin;
        public boolean on;
        public CharSequence popupCharacters;
        public int popupResId;
        public boolean pressed;
        public int previewPosX;
        public boolean repeatable;
        public boolean sticky;
        public CharSequence text;
        public int vgap;
        public int width;
        public float widthRate;
        public int x;
        public int y;
        private static final int[] KEY_STATE_NORMAL_ON = {android.R.attr.state_checkable, android.R.attr.state_checked};
        private static final int[] KEY_STATE_PRESSED_ON = {android.R.attr.state_pressed, android.R.attr.state_checkable, android.R.attr.state_checked};
        private static final int[] KEY_STATE_NORMAL_OFF = {android.R.attr.state_checkable};
        private static final int[] KEY_STATE_PRESSED_OFF = {android.R.attr.state_pressed, android.R.attr.state_checkable};
        private static final int[] KEY_STATE_NORMAL = new int[0];
        private static final int[] KEY_STATE_PRESSED = {android.R.attr.state_pressed};

        public Key(Row parent) {
            this.mIsIconSkin = false;
            this.mIsPreviewSkin = false;
            this.previewPosX = -1;
            this.isEmptyKey = false;
            this.mIsSkin = false;
            this.keyboard = parent.parent;
        }

        public Key(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
            this(parent);
            this.x = x;
            this.y = y;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.WnnKeyboardView);
            this.width = Keyboard.getDimensionOrFraction(a, 26, this.keyboard.mDisplayWidth, parent.defaultWidth);
            this.widthRate = Keyboard.getFractionRate(a, 26, parent.defaultWidthRate);
            this.gap = Keyboard.getDimensionOrFraction(a, 24, this.keyboard.mDisplayWidth, parent.defaultHorizontalGap);
            this.gapRate = Keyboard.getFractionRate(a, 24, parent.defaultHorizontalGapRate);
            this.vgap = parent.verticalGap;
            this.x += this.gap;
            TypedValue codesValue = new TypedValue();
            a.getValue(11, codesValue);
            if (codesValue.type == 16 || codesValue.type == 17) {
                this.codes = new int[]{codesValue.data};
            } else if (codesValue.type == 3) {
                this.codes = parseCSV(codesValue.string.toString());
            }
            this.heightRow = a.getInt(43, 1);
            this.height = this.keyboard.calculateKeyHeight(this.keyboard.mRowCnt, this.heightRow);
            if (this.codes != null) {
                this.iconPreview = getDrawable(a, 12, this.codes[0], 1);
            }
            this.mIsPreviewSkin = this.mIsSkin;
            if (this.iconPreview != null) {
                this.iconPreview.setBounds(0, 0, this.iconPreview.getIntrinsicWidth(), this.iconPreview.getIntrinsicHeight());
            }
            this.keyPreviewLabel = getText(a, 34);
            this.hintLabel = a.getText(40);
            this.popupCharacters = getText(a, 20);
            this.popupResId = a.getResourceId(21, 0);
            if (this.keyboard.isEnableQwertyNumberKeyboard()) {
                if (this.hintLabel != null) {
                    this.hintLabel = this.hintLabel.toString().replaceAll(NUMBER_CHARACTER, LoggingEvents.EXTRA_CALLING_APP_NAME);
                    if (this.hintLabel.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                        this.hintLabel = null;
                    }
                }
                if (this.popupCharacters != null) {
                    this.popupCharacters = this.popupCharacters.toString().replaceAll(NUMBER_CHARACTER, LoggingEvents.EXTRA_CALLING_APP_NAME);
                    if (this.popupCharacters.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                        this.popupCharacters = null;
                        this.popupResId = 0;
                    }
                }
            }
            if (this.popupResId != 0 && this.keyboard.mIsLangPack) {
                this.popupResId = 13;
            }
            this.repeatable = a.getBoolean(14, false);
            this.sticky = a.getBoolean(15, false);
            this.edgeFlags = a.getInt(16, 0);
            this.edgeFlags |= parent.rowEdgeFlags;
            if (this.codes != null) {
                this.icon = getDrawable(a, 17, this.codes[0], 0);
            }
            this.mIsIconSkin = this.mIsSkin;
            if (this.icon != null) {
                this.icon.setBounds(0, 0, this.icon.getIntrinsicWidth(), this.icon.getIntrinsicHeight());
            }
            this.label = getText(a, 18);
            this.text = getText(a, 19);
            if (this.codes == null && !TextUtils.isEmpty(this.label)) {
                this.codes = new int[]{this.label.charAt(0)};
            }
            this.isSecondKey = a.getBoolean(33, false);
            this.hintLabelLeft = a.getText(41);
            this.hintLabelRight = a.getText(42);
            this.description = a.getText(46);
            this.isEmptyKey = a.getBoolean(44, false);
            a.recycle();
        }

        private Drawable getDrawable(TypedArray array, int id, int keycode, int item) {
            Drawable result;
            KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
            KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
            this.mIsSkin = false;
            if (this.keyboard.mIsLangPack) {
                result = langPack.getDrawableDirect(array.getResourceId(id, 0));
            } else {
                result = array.getDrawable(id);
            }
            if (keyskin.isValid()) {
                Drawable tmpicon = null;
                boolean shifted = this.keyboard.isShifted();
                this.keyboard.setShifted(false);
                if (this.keyboard.mIsLangPack) {
                    String reskey = langPack.conversionSkinKey(Keyboard.mContext, langPack.getEnableKeyboardLanguagePackDataName(), array.getResourceId(17, 0));
                    if (reskey != null && reskey.equals("key_qwerty_shift_locked")) {
                        this.keyboard.setShifted(true);
                    }
                } else {
                    int keytopId = array.getResourceId(17, 0);
                    if (keytopId == R.drawable.key_qwerty_shift_locked) {
                        this.keyboard.setShifted(true);
                    }
                }
                switch (item) {
                    case 0:
                        tmpicon = keyskin.getDrawable(keycode, this.keyboard, 1073741824);
                        break;
                    case 1:
                        tmpicon = keyskin.getDrawablePreview(keycode, this.keyboard, 1073741824);
                        break;
                }
                if (tmpicon != null) {
                    result = tmpicon;
                    this.mIsSkin = true;
                }
                this.keyboard.setShifted(shifted);
            }
            return result;
        }

        private CharSequence getText(TypedArray array, int id) {
            int resId = array.getResourceId(id, 0);
            if (this.keyboard.mIsLangPack && resId != 0) {
                KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
                CharSequence result = langPack.getTextDirect(resId);
                return result;
            }
            CharSequence result2 = array.getText(id);
            return result2;
        }

        public void onPressed() {
            this.pressed = true;
        }

        public void onReleased(boolean inside) {
            this.pressed = false;
        }

        int[] parseCSV(String value) {
            int count = 0;
            int lastIndex = 0;
            if (value.length() > 0) {
                do {
                    count++;
                    lastIndex = value.indexOf(iWnnEngine.DECO_OPERATION_SEPARATOR, lastIndex + 1);
                } while (lastIndex > 0);
            }
            int[] values = new int[count];
            int count2 = 0;
            StringTokenizer st = new StringTokenizer(value, iWnnEngine.DECO_OPERATION_SEPARATOR);
            while (st.hasMoreTokens()) {
                int count3 = count2 + 1;
                try {
                    values[count2] = Integer.parseInt(st.nextToken());
                    count2 = count3;
                } catch (NumberFormatException e) {
                    Log.e("Keyboard", "Error parsing keycodes " + value);
                    count2 = count3;
                }
            }
            return values;
        }

        public boolean isInside(int x, int y) {
            boolean leftEdge = (this.edgeFlags & 1) > 0;
            boolean rightEdge = (this.edgeFlags & 2) > 0;
            boolean topEdge = (this.edgeFlags & 4) > 0;
            boolean bottomEdge = (this.edgeFlags & 8) > 0;
            if ((x >= this.x - this.gap || (leftEdge && x <= this.x + this.width)) && ((x < this.x + this.width || (rightEdge && x >= this.x)) && (y >= this.y || (topEdge && y <= this.y + this.height)))) {
                if (y < this.y + this.height + this.vgap) {
                    return true;
                }
                if (bottomEdge && y >= this.y) {
                    return true;
                }
            }
            return false;
        }

        public boolean isInside(int x, int y, int w, int h) {
            return this.x <= x + w && x <= this.x + this.width && this.y <= y + h && y <= this.y + this.height;
        }

        public int squaredDistanceFrom(int x, int y) {
            int xDist = (this.x + (this.width / 2)) - x;
            int yDist = (this.y + (this.height / 2)) - y;
            return (xDist * xDist) + (yDist * yDist);
        }

        public int[] getCurrentDrawableState() {
            int[] states = KEY_STATE_NORMAL;
            if (this.on) {
                if (this.pressed) {
                    int[] states2 = KEY_STATE_PRESSED_ON;
                    return states2;
                }
                int[] states3 = KEY_STATE_NORMAL_ON;
                return states3;
            }
            if (this.sticky) {
                if (this.pressed) {
                    int[] states4 = KEY_STATE_PRESSED_OFF;
                    return states4;
                }
                int[] states5 = KEY_STATE_NORMAL_OFF;
                return states5;
            }
            if (this.pressed) {
                int[] states6 = KEY_STATE_PRESSED;
                return states6;
            }
            return states;
        }
    }

    public Keyboard(Context context, int xmlLayoutResId, int condition) {
        this(context, xmlLayoutResId, 0, 0, condition);
    }

    public Keyboard(Context context, int xmlLayoutResId, int modeId, int keyboardType, int condition) {
        boolean z = false;
        this.mPaddingLeft = 0;
        this.mIsLangPack = false;
        this.mMiniKeyboardWidth = 0;
        this.mKeyX = 0;
        this.mKeyY = 0;
        this.mKeyHeightCorrectionList = new ArrayList<>();
        this.mRowCnt = 0;
        mContext = context;
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        this.mDisplayWidth = dm.widthPixels;
        this.mDefaultHorizontalGap = 0;
        this.mDefaultKeyWidth = this.mDisplayWidth / 10;
        this.mDefaultVerticalGap = 0;
        this.mKeyHeight = 0;
        this.mRows = new ArrayList();
        this.mKeys = new ArrayList();
        this.mKeyboardMode = modeId;
        this.mKeyboardType = keyboardType;
        this.mKeyboardCondition = condition;
        this.mXmlLayoutResId = xmlLayoutResId;
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        XmlResourceParser langPackParser = langPack.getXmlParser(xmlLayoutResId);
        if (langPack.isValid() && langPackParser != null) {
            z = true;
        }
        this.mIsLangPack = z;
        if (this.mIsLangPack) {
            loadKeyboard(context, langPack.getResources(), langPackParser);
        } else {
            loadKeyboard(context, res, res.getXml(xmlLayoutResId));
        }
        correctWidthOfSpaceKey();
        correctKeyPositionX();
        this.mPaddingLeft = (this.mDisplayWidth - this.mTotalWidth) / 2;
    }

    public Keyboard(Context context, int layoutTemplateResId, CharSequence characters, int condition) {
        this(context, layoutTemplateResId, condition);
        int x = 0;
        int y = 0;
        int column = 0;
        this.mTotalWidth = 0;
        Row row = new Row(this);
        row.defaultHeight = this.mKeyHeight;
        row.defaultWidth = this.mDefaultKeyWidth;
        row.defaultHorizontalGap = this.mDefaultHorizontalGap;
        row.verticalGap = this.mDefaultVerticalGap;
        row.rowEdgeFlags = 12;
        int maxColumns = context.getResources().getInteger(R.integer.mini_keyboard_max_div);
        for (int i = 0; i < characters.length(); i++) {
            char c = characters.charAt(i);
            if (column >= maxColumns) {
                x = 0;
                y += this.mDefaultVerticalGap + this.mKeyHeight;
                column = 0;
            }
            Key key = new Key(row);
            key.x = x;
            key.y = y;
            key.width = this.mDefaultKeyWidth;
            key.height = this.mKeyHeight;
            key.gap = this.mDefaultHorizontalGap;
            key.label = String.valueOf(c);
            key.codes = new int[]{c};
            column++;
            x += key.width + key.gap;
            this.mKeys.add(key);
            if (x > this.mTotalWidth) {
                this.mTotalWidth = x;
            }
        }
        this.mTotalHeight = this.mKeyHeight + y;
        this.mMiniKeyboardWidth = this.mTotalWidth;
        this.mPaddingLeft = 0;
    }

    public List<Key> getKeys() {
        return this.mKeys;
    }

    public int getKeyWidth() {
        return this.mDefaultKeyWidth;
    }

    public int getHeight() {
        return this.mTotalHeight;
    }

    public int getDisplayWidth() {
        return this.mDisplayWidth;
    }

    public int getMiniKeyboardWidth() {
        return this.mMiniKeyboardWidth;
    }

    public int getKeyboardCondition() {
        return this.mKeyboardCondition;
    }

    public boolean setShifted(boolean shiftState) {
        if (this.mShiftKey != null) {
            this.mShiftKey.on = shiftState;
        }
        if (this.mShifted == shiftState) {
            return false;
        }
        this.mShifted = shiftState;
        return true;
    }

    public boolean isShifted() {
        return this.mShifted;
    }

    public Key getShiftKey() {
        return this.mShiftKey;
    }

    public int getXmlLayoutResId() {
        return this.mXmlLayoutResId;
    }

    public int getPaddingLeft() {
        return this.mPaddingLeft;
    }

    private void computeNearestNeighbors() {
        this.mCellWidth = ((this.mDisplayWidth + 10) - 1) / 10;
        this.mCellHeight = ((getHeight() + 5) - 1) / 5;
        this.mGridNeighbors = new int[50][];
        int[] indices = new int[this.mKeys.size()];
        int gridWidth = this.mCellWidth * 10;
        int gridHeight = this.mCellHeight * 5;
        int x = 0;
        while (x < gridWidth) {
            int y = 0;
            while (y < gridHeight) {
                int count = 0;
                for (int i = 0; i < this.mKeys.size(); i++) {
                    Key key = this.mKeys.get(i);
                    if (key.squaredDistanceFrom(x, y) < this.mProximityThreshold || key.squaredDistanceFrom((this.mCellWidth + x) - 1, y) < this.mProximityThreshold || key.squaredDistanceFrom((this.mCellWidth + x) - 1, (this.mCellHeight + y) - 1) < this.mProximityThreshold || key.squaredDistanceFrom(x, (this.mCellHeight + y) - 1) < this.mProximityThreshold || ((key.codes[0] == 12288 || key.codes[0] == 32) && key.isInside(x, y, this.mCellWidth, this.mCellHeight))) {
                        indices[count] = i;
                        count++;
                    }
                }
                int[] cell = new int[count];
                System.arraycopy(indices, 0, cell, 0, count);
                this.mGridNeighbors[((y / this.mCellHeight) * 10) + (x / this.mCellWidth)] = cell;
                y += this.mCellHeight;
            }
            x += this.mCellWidth;
        }
    }

    public int[] getNearestKeys(int x, int y) {
        int index;
        if (this.mGridNeighbors == null) {
            computeNearestNeighbors();
        }
        return (x < 0 || x >= this.mDisplayWidth || y < 0 || y >= getHeight() || (index = ((y / this.mCellHeight) * 10) + (x / this.mCellWidth)) >= 50) ? new int[0] : this.mGridNeighbors[index];
    }

    private Row createRowFromXml(Resources res, XmlResourceParser parser) {
        this.mRowCnt++;
        Row ret = new Row(res, this, parser);
        this.mRows.add(ret);
        return ret;
    }

    private Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
        Key key = new Key(res, parent, x, y, parser);
        parent.keysInRow.add(key);
        if (!key.isEmptyKey) {
            this.mKeys.add(key);
            if (key.codes[0] == -1) {
                this.mShiftKey = key;
            }
        }
        return key;
    }

    private void loadKeyboard(Context context, Resources res, XmlResourceParser parser) {
        boolean inKey = false;
        boolean inRow = false;
        boolean inInclude = false;
        boolean selected = false;
        Key key = null;
        Row currentRow = null;
        this.mKeyX = 0;
        this.mKeyY = 0;
        this.mRowCnt = 0;
        if (parser == null || res == null) {
            return;
        }
        while (true) {
            try {
                int event = parser.next();
                if (event != 1) {
                    String tag = parser.getName();
                    if (event == 2) {
                        if (!selected) {
                            if (TAG_ROW.equals(tag)) {
                                inRow = true;
                                this.mKeyX = 0;
                                currentRow = createRowFromXml(res, parser);
                                boolean skipRow = (currentRow.mode == 0 || currentRow.mode == this.mKeyboardMode) ? false : true;
                                if (skipRow) {
                                    skipToEndOfRow(parser);
                                    inRow = false;
                                }
                            } else if (TAG_KEY.equals(tag)) {
                                if (currentRow != null) {
                                    inKey = true;
                                    key = createKeyFromXml(res, currentRow, this.mKeyX, this.mKeyY, parser);
                                }
                            } else if (TAG_INCLUDE.equals(tag)) {
                                loadTagInclude(parser, res, currentRow);
                                inInclude = true;
                            } else if (TAG_SWITCH.equals(tag)) {
                                selected = loadTagSwitchFromXml(parser, res, currentRow);
                            } else if ("Keyboard".equals(tag)) {
                                parseKeyboardAttributes(res, parser);
                            }
                        }
                    } else if (event == 3) {
                        if (inKey) {
                            inKey = false;
                            this.mKeyX += key.gap + key.width;
                        } else if (inInclude) {
                            inInclude = false;
                        } else if (inRow) {
                            inRow = false;
                            processEndRow(currentRow, currentRow.defaultHeight, currentRow.verticalGap);
                        } else if (TAG_SWITCH.equals(tag)) {
                            selected = false;
                        }
                    }
                } else {
                    return;
                }
            } catch (Exception e) {
                Log.e("Keyboard", "Parse error:" + e);
                e.printStackTrace();
                return;
            }
        }
    }

    private XmlResourceParser getParserOfLayout(Resources res, XmlResourceParser parser) {
        AttributeSet attr = Xml.asAttributeSet(parser);
        TypedArray keyboardAttr = res.obtainAttributes(attr, R.styleable.WnnKeyboardView);
        int resId = keyboardAttr.getResourceId(38, 0);
        keyboardAttr.recycle();
        if (this.mIsLangPack) {
            KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
            XmlResourceParser parserForInclude = langPack.getSimpleXmlParser(Integer.valueOf(resId));
            return parserForInclude;
        }
        XmlResourceParser parserForInclude2 = res.getXml(resId);
        return parserForInclude2;
    }

    private void loadTagInclude(XmlResourceParser parser, Resources res, Row currentRow) {
        boolean selected = false;
        XmlResourceParser parserForInclude = getParserOfLayout(res, parser);
        if (parserForInclude == null) {
            return;
        }
        while (true) {
            try {
                int event = parserForInclude.next();
                if (event != 1) {
                    if (event == 2) {
                        String tag = parserForInclude.getName();
                        if (!selected) {
                            if (TAG_ROW_KEY.equals(tag)) {
                                selected = loadTagKeyFromXml(parserForInclude, res, currentRow);
                            } else if (TAG_SWITCH.equals(tag)) {
                                selected = loadTagSwitchFromXml(parserForInclude, res, currentRow);
                            }
                        }
                    }
                } else {
                    return;
                }
            } catch (Exception e) {
                Log.e("Keyboard", "Parse error:" + e);
                e.printStackTrace();
                return;
            }
        }
    }

    private boolean loadTagSwitchFromXml(XmlResourceParser parser, Resources res, Row currentRow) {
        boolean inCase = false;
        boolean inRow = false;
        boolean inKey = false;
        boolean inInclude = false;
        Key key = null;
        boolean isMatch = false;
        while (true) {
            try {
                int event = parser.next();
                if (event == 1) {
                    return false;
                }
                String tag = parser.getName();
                if (event == 2) {
                    if (!isMatch) {
                        if (TAG_CASE.equals(tag)) {
                            inCase = true;
                            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.WnnKeyboardView);
                            int rowCondition = a.getInt(39, 0);
                            isMatch = isMatchConditon(rowCondition, this.mKeyboardCondition);
                            a.recycle();
                        }
                    } else if (TAG_ROW.equals(tag)) {
                        inRow = true;
                        this.mKeyX = 0;
                        currentRow = createRowFromXml(res, parser);
                        boolean skipRow = (currentRow.mode == 0 || currentRow.mode == this.mKeyboardMode) ? false : true;
                        if (skipRow) {
                            skipToEndOfRow(parser);
                            inRow = false;
                        }
                    } else if (TAG_KEY.equals(tag)) {
                        if (currentRow != null) {
                            inKey = true;
                            key = createKeyFromXml(res, currentRow, this.mKeyX, this.mKeyY, parser);
                        }
                    } else if (TAG_INCLUDE.equals(tag)) {
                        loadTagInclude(parser, res, currentRow);
                        inInclude = true;
                    }
                } else if (event != 3) {
                    continue;
                } else if (inKey) {
                    inKey = false;
                    this.mKeyX += key.gap + key.width;
                } else if (inInclude) {
                    inInclude = false;
                } else if (inRow) {
                    inRow = false;
                    processEndRow(currentRow, currentRow.defaultHeight, currentRow.verticalGap);
                } else if (inCase) {
                    inCase = false;
                    if (isMatch) {
                        return true;
                    }
                } else if (TAG_SWITCH.equals(tag)) {
                    return false;
                }
            } catch (IOException e) {
                Log.e("Keyboard", "IOException error:" + e);
                e.printStackTrace();
                return false;
            } catch (XmlPullParserException e2) {
                Log.e("Keyboard", "XmlPullParserException error:" + e2);
                e2.printStackTrace();
                return false;
            }
        }
    }

    private boolean loadTagKeyFromXml(XmlResourceParser parser, Resources res, Row currentRow) {
        boolean inRow = false;
        boolean inKey = false;
        Key key = null;
        while (true) {
            try {
                int event = parser.next();
                if (event != 1) {
                    if (event == 2) {
                        String tag = parser.getName();
                        if (TAG_ROW.equals(tag)) {
                            inRow = true;
                            this.mKeyX = 0;
                            currentRow = createRowFromXml(res, parser);
                            boolean skipRow = (currentRow.mode == 0 || currentRow.mode == this.mKeyboardMode) ? false : true;
                            if (skipRow) {
                                skipToEndOfRow(parser);
                                inRow = false;
                            }
                        } else if (TAG_KEY.equals(tag) && currentRow != null) {
                            inKey = true;
                            key = createKeyFromXml(res, currentRow, this.mKeyX, this.mKeyY, parser);
                        }
                    } else if (event == 3) {
                        if (inKey) {
                            inKey = false;
                            this.mKeyX += key.gap + key.width;
                        } else if (inRow) {
                            inRow = false;
                            processEndRow(currentRow, currentRow.defaultHeight, currentRow.verticalGap);
                        }
                    }
                } else {
                    return true;
                }
            } catch (IOException e) {
                Log.e("Keyboard", "IOException error:" + e);
                e.printStackTrace();
                return true;
            } catch (XmlPullParserException e2) {
                Log.e("Keyboard", "XmlPullParserException error:" + e2);
                e2.printStackTrace();
                return true;
            }
        }
    }

    private void skipToEndOfRow(XmlResourceParser parser) throws XmlPullParserException, IOException {
        while (true) {
            int event = parser.next();
            if (event != 1) {
                if (event == 3 && parser.getName().equals(TAG_ROW)) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
        TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.WnnKeyboardView);
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            KeyboardManager km = wnn.getCurrentKeyboardManager();
            Point size = km.getKeyboardSize(true);
            this.mDisplayWidth = size.x;
            int totalHeight = size.y;
            this.mDefaultKeyWidth = getDimensionOrFraction(a, 26, this.mDisplayWidth, this.mDisplayWidth / 10);
            this.mRate = getFractionRate(a, 26, 0.1f);
            this.mDefaultHorizontalGap = getDimensionOrFraction(a, 24, this.mDisplayWidth, 0);
            this.mDefaultHorizontalGapRate = getFractionRate(a, 24, 0.0f);
            calculateKeyboardHeight(a, totalHeight);
            a.recycle();
        }
    }

    private void calculateKeyboardHeight(TypedArray typeArray, int totalHeight) {
        calculateKeyboardHeight(typeArray, VALUE_UNSPECIFIED, VALUE_UNSPECIFIED, totalHeight);
    }

    private void calculateKeyboardHeight(int line, int verticalGap, int totalHeight) {
        calculateKeyboardHeight(null, line, verticalGap, totalHeight);
    }

    private void calculateKeyboardHeight(TypedArray typeArray, int line, int verticalGap, int totalHeight) {
        Resources res = mContext.getResources();
        this.mTotalHeight = totalHeight;
        if (typeArray != null) {
            line = typeArray.getInt(36, 4);
            if (isEnableQwertyNumberKeyboard()) {
                line++;
            }
        }
        int tempDefaultVerticalGap = (res.getDimensionPixelSize(R.dimen.keyboard_qwerty_common_vertical_gap) * 4) / line;
        if (typeArray != null) {
            this.mXmlDefaultVerticalGap = getDimensionOrFraction(typeArray, 27, this.mTotalHeight, tempDefaultVerticalGap);
            tempDefaultVerticalGap = this.mXmlDefaultVerticalGap;
        } else if (verticalGap > VALUE_UNSPECIFIED) {
            tempDefaultVerticalGap = verticalGap;
        }
        this.mDefaultVerticalGap = ((line - 1) * tempDefaultVerticalGap) / line;
        int keyboardTopPadding = res.getDimensionPixelSize(R.dimen.keyboard_qwerty_common_top_padding);
        int lastRowVerticalGap = res.getDimensionPixelSize(R.dimen.keyboard_qwerty_common_row4_vertical_gap);
        int baseHeight = ((this.mTotalHeight - keyboardTopPadding) - lastRowVerticalGap) + this.mDefaultVerticalGap;
        int baseKeyHeight = baseHeight / line;
        int heightRest = baseHeight % line;
        int heightCoreZeroCnt = line - heightRest;
        this.mKeyHeightCorrectionList.clear();
        for (int i = 0; i < line; i++) {
            if (heightCoreZeroCnt > 0) {
                this.mKeyHeightCorrectionList.add(0);
            } else {
                this.mKeyHeightCorrectionList.add(1);
            }
            heightCoreZeroCnt--;
        }
        this.mKeyHeight = baseKeyHeight - this.mDefaultVerticalGap;
        this.mKeyY = keyboardTopPadding - 5;
        this.mProximityThreshold = (int) (this.mDefaultKeyWidth * SEARCH_DISTANCE);
        this.mProximityThreshold *= this.mProximityThreshold;
    }

    static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
        TypedValue value = a.peekValue(index);
        if (value != null) {
            if (value.type == 5) {
                return a.getDimensionPixelSize(index, defValue);
            }
            if (value.type == 6) {
                return Math.round(a.getFraction(index, base, base, defValue));
            }
            return defValue;
        }
        return defValue;
    }

    public int getKeyboardMode() {
        return this.mKeyboardMode;
    }

    public int getKeyboardType() {
        return this.mKeyboardType;
    }

    public int getKeyIndex(int keycode) {
        int size = this.mKeys.size();
        for (int i = 0; i < size; i++) {
            if (this.mKeys.get(i).codes[0] == keycode) {
                int retindex = i;
                return retindex;
            }
        }
        return -1;
    }

    public Key getKey(int keyindex) {
        Key retKey = this.mKeys.get(keyindex);
        return retKey;
    }

    public static int getOneRowKeyboardHeight() {
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn == null) {
            return 0;
        }
        KeyboardManager km = wnn.getCurrentKeyboardManager();
        int height = km.getKeyboardSize(false).y;
        return height / 4;
    }

    private int checkImeSettingsCondition(int xmlCondition, int keyboardCondition, int on, int off) {
        int check = 0 | (on & xmlCondition);
        if ((check | (off & xmlCondition)) != 0) {
            return keyboardCondition;
        }
        int ret = keyboardCondition & ((on | off) ^ (-1));
        return ret;
    }

    private int checkModeCondition(int xmlCondition, int keyboardCondition) {
        int check = 0 | (CONDITION_MODE & xmlCondition);
        if (check != 0) {
            return keyboardCondition;
        }
        int ret = keyboardCondition & (CONDITION_MODE ^ (-1));
        return ret;
    }

    private int check50keyTypeCondition(int xmlCondition, int keyboardCondition) {
        int check = 0 | (CONDITION_50KEY & xmlCondition);
        if (check != 0) {
            return keyboardCondition;
        }
        int ret = keyboardCondition & (CONDITION_50KEY ^ (-1));
        return ret;
    }

    private boolean isMatchConditon(int xmlCondition, int keyboardCondition) {
        if (xmlCondition != check50keyTypeCondition(xmlCondition, checkModeCondition(xmlCondition, checkImeSettingsCondition(xmlCondition, checkImeSettingsCondition(xmlCondition, checkImeSettingsCondition(xmlCondition, checkImeSettingsCondition(xmlCondition, checkImeSettingsCondition(xmlCondition, checkImeSettingsCondition(xmlCondition, keyboardCondition, CONDITION_VOICE_ON, CONDITION_VOICE_OFF), CONDITION_SWITCH_IME_ON, CONDITION_SWITCH_IME_OFF), CONDITION_CURSOR_ON, CONDITION_CURSOR_OFF), CONDITION_INPUT, CONDITION_NOINPUT), CONDITION_SHIFT_ON, CONDITION_SHIFT_OFF), CONDITION_NUMBER_ON, CONDITION_NUMBER_OFF)))) {
            return false;
        }
        return true;
    }

    public void changeKeyboardSize(int width, int height) {
        this.mDisplayWidth = width;
        this.mDefaultKeyWidth = Math.round(width * this.mRate);
        this.mDefaultHorizontalGap = Math.round(width * this.mDefaultHorizontalGapRate);
        this.mTotalWidth = 0;
        calculateKeyboardHeight(this.mRowCnt, this.mXmlDefaultVerticalGap, height);
        int rowSize = this.mRows.size();
        int preOneRowHeight = 0;
        for (int line = 0; line < rowSize; line++) {
            Row row = this.mRows.get(line);
            int keySize = row.keysInRow.size();
            this.mKeyX = 0;
            for (int index = 0; index < keySize; index++) {
                Key key = row.keysInRow.get(index);
                key.width = Math.round(width * key.widthRate);
                key.gap = Math.round(width * key.gapRate);
                this.mKeyX += key.gap;
                key.x = this.mKeyX;
                this.mKeyX += key.width;
                key.height = calculateKeyHeight(line + 1, key.heightRow);
                key.vgap = this.mDefaultVerticalGap;
                key.y = this.mKeyY;
                if (key.heightRow == 1) {
                    preOneRowHeight = key.height;
                }
                if (index + 1 == keySize) {
                    processEndRow(row, preOneRowHeight, this.mDefaultVerticalGap);
                }
            }
        }
        correctWidthOfSpaceKey();
        correctKeyPositionX();
        computeNearestNeighbors();
        this.mPaddingLeft = (this.mDisplayWidth - this.mTotalWidth) / 2;
    }

    private void correctWidthOfSpaceKey() {
        DefaultSoftKeyboard softKeyboard;
        boolean isPhoneMode = false;
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null && (softKeyboard = wnn.getCurrentDefaultSoftKeyboard()) != null) {
            isPhoneMode = softKeyboard.isPhoneMode().booleanValue();
        }
        if (this.mKeyboardType == 0 && !isPhoneMode && !this.mRows.isEmpty()) {
            Row row = this.mRows.get(this.mRows.size() - 1);
            boolean isCorrect = false;
            int correctWidth = this.mTotalWidth - this.mKeyX;
            for (Key key : row.keysInRow) {
                if (isCorrect) {
                    key.x += correctWidth;
                } else if (!key.isEmptyKey && (key.codes[0] == -121 || key.codes[0] == 32)) {
                    key.width += correctWidth;
                    isCorrect = true;
                }
            }
            if (isCorrect) {
                row.totalRowWidth = this.mTotalWidth;
            }
        }
    }

    private static float getFractionRate(TypedArray attribute, int index, float defValue) {
        TypedValue value = attribute.peekValue(index);
        if (value == null || value.type != 6) {
            return defValue;
        }
        float rate = attribute.getFraction(index, 1, 1, defValue);
        return rate;
    }

    protected boolean isEnableQwertyNumberKeyboard() {
        return (this.mKeyboardType != 0 || (CONDITION_NUMBER_ON & this.mKeyboardCondition) == 0 || this.mKeyboardMode == 1 || this.mKeyboardMode == 7 || this.mKeyboardMode == 2) ? false : true;
    }

    protected int calculateKeyHeight(int line, int heightRow) {
        int height = this.mKeyHeight;
        int index = line - 1;
        if (index < 0 || index >= this.mKeyHeightCorrectionList.size()) {
            return height;
        }
        int height2 = height + this.mKeyHeightCorrectionList.get(index).intValue();
        for (int i = 1; i < heightRow; i++) {
            height2 = height2 + this.mDefaultVerticalGap + this.mKeyHeight;
            if (index + i < this.mKeyHeightCorrectionList.size()) {
                height2 += this.mKeyHeightCorrectionList.get(index + i).intValue();
            }
        }
        return height2;
    }

    private void correctKeyPositionX() {
        for (Row row : this.mRows) {
            if (row.gravity != 2) {
                int diff = this.mTotalWidth - row.totalRowWidth;
                if (row.gravity == 0) {
                    diff /= 2;
                }
                if (diff != 0) {
                    int size = row.keysInRow.size();
                    for (int index = 0; index < size; index++) {
                        Key key = row.keysInRow.get(index);
                        if (index == 0) {
                            key.gap += diff;
                        }
                        key.x += diff;
                    }
                }
            }
        }
    }

    private void processEndRow(Row row, int height, int vgap) {
        if (row != null) {
            this.mKeyY += height;
            this.mKeyY += vgap;
            if (!row.keysInRow.isEmpty()) {
                this.mKeyX = row.keysInRow.get(0).gap + this.mKeyX;
            }
            row.totalRowWidth = this.mKeyX;
            if (this.mKeyX > this.mTotalWidth) {
                this.mTotalWidth = this.mKeyX;
            }
        }
    }
}
