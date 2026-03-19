package android.media;

import android.graphics.Rect;
import android.os.Parcel;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public final class TimedText {
    private static final int FIRST_PRIVATE_KEY = 101;
    private static final int FIRST_PUBLIC_KEY = 1;
    private static final int KEY_BACKGROUND_COLOR_RGBA = 3;
    private static final int KEY_DISPLAY_FLAGS = 1;
    private static final int KEY_END_CHAR = 104;
    private static final int KEY_FONT_ID = 105;
    private static final int KEY_FONT_SIZE = 106;
    private static final int KEY_GLOBAL_SETTING = 101;
    private static final int KEY_HIGHLIGHT_COLOR_RGBA = 4;
    private static final int KEY_LOCAL_SETTING = 102;
    private static final int KEY_SCROLL_DELAY = 5;
    private static final int KEY_START_CHAR = 103;
    private static final int KEY_START_TIME = 7;
    private static final int KEY_STRUCT_BLINKING_TEXT_LIST = 8;
    private static final int KEY_STRUCT_FONT_LIST = 9;
    private static final int KEY_STRUCT_HIGHLIGHT_LIST = 10;
    private static final int KEY_STRUCT_HYPER_TEXT_LIST = 11;
    private static final int KEY_STRUCT_JUSTIFICATION = 15;
    private static final int KEY_STRUCT_KARAOKE_LIST = 12;
    private static final int KEY_STRUCT_STYLE_LIST = 13;
    private static final int KEY_STRUCT_TEXT = 16;
    private static final int KEY_STRUCT_TEXT_POS = 14;
    private static final int KEY_STYLE_FLAGS = 2;
    private static final int KEY_TEXT_COLOR_RGBA = 107;
    private static final int KEY_WRAP_TEXT = 6;
    private static final int LAST_PRIVATE_KEY = 107;
    private static final int LAST_PUBLIC_KEY = 16;
    private static final String TAG = "TimedText";
    private Justification mJustification;
    private final HashMap<Integer, Object> mKeyObjectMap = new HashMap<>();
    private int mDisplayFlags = -1;
    private int mBackgroundColorRGBA = -1;
    private int mHighlightColorRGBA = -1;
    private int mScrollDelay = -1;
    private int mWrapText = -1;
    private List<CharPos> mBlinkingPosList = null;
    private List<CharPos> mHighlightPosList = null;
    private List<Karaoke> mKaraokeList = null;
    private List<Font> mFontList = null;
    private List<Style> mStyleList = null;
    private List<HyperText> mHyperTextList = null;
    private Rect mTextBounds = null;
    private String mTextChars = null;

    public static final class CharPos {
        public final int endChar;
        public final int startChar;

        public CharPos(int i, int i2) {
            this.startChar = i;
            this.endChar = i2;
        }
    }

    public static final class Justification {
        public final int horizontalJustification;
        public final int verticalJustification;

        public Justification(int i, int i2) {
            this.horizontalJustification = i;
            this.verticalJustification = i2;
        }
    }

    public static final class Style {
        public final int colorRGBA;
        public final int endChar;
        public final int fontID;
        public final int fontSize;
        public final boolean isBold;
        public final boolean isItalic;
        public final boolean isUnderlined;
        public final int startChar;

        public Style(int i, int i2, int i3, boolean z, boolean z2, boolean z3, int i4, int i5) {
            this.startChar = i;
            this.endChar = i2;
            this.fontID = i3;
            this.isBold = z;
            this.isItalic = z2;
            this.isUnderlined = z3;
            this.fontSize = i4;
            this.colorRGBA = i5;
        }
    }

    public static final class Font {
        public final int ID;
        public final String name;

        public Font(int i, String str) {
            this.ID = i;
            this.name = str;
        }
    }

    public static final class Karaoke {
        public final int endChar;
        public final int endTimeMs;
        public final int startChar;
        public final int startTimeMs;

        public Karaoke(int i, int i2, int i3, int i4) {
            this.startTimeMs = i;
            this.endTimeMs = i2;
            this.startChar = i3;
            this.endChar = i4;
        }
    }

    public static final class HyperText {
        public final String URL;
        public final String altString;
        public final int endChar;
        public final int startChar;

        public HyperText(int i, int i2, String str, String str2) {
            this.startChar = i;
            this.endChar = i2;
            this.URL = str;
            this.altString = str2;
        }
    }

    public TimedText(Parcel parcel) {
        if (!parseParcel(parcel)) {
            this.mKeyObjectMap.clear();
            throw new IllegalArgumentException("parseParcel() fails");
        }
    }

    public String getText() {
        return this.mTextChars;
    }

    public Rect getBounds() {
        return this.mTextBounds;
    }

    private boolean parseParcel(Parcel parcel) {
        Object objValueOf;
        parcel.setDataPosition(0);
        if (parcel.dataAvail() == 0) {
            return false;
        }
        int i = parcel.readInt();
        if (i == 102) {
            int i2 = parcel.readInt();
            if (i2 != 7) {
                return false;
            }
            this.mKeyObjectMap.put(Integer.valueOf(i2), Integer.valueOf(parcel.readInt()));
            if (parcel.readInt() != 16) {
                return false;
            }
            parcel.readInt();
            byte[] bArrCreateByteArray = parcel.createByteArray();
            if (bArrCreateByteArray == null || bArrCreateByteArray.length == 0) {
                this.mTextChars = null;
            } else {
                this.mTextChars = new String(bArrCreateByteArray);
            }
        } else if (i != 101) {
            Log.w(TAG, "Invalid timed text key found: " + i);
            return false;
        }
        while (parcel.dataAvail() > 0) {
            int i3 = parcel.readInt();
            if (!isValidKey(i3)) {
                Log.w(TAG, "Invalid timed text key found: " + i3);
                return false;
            }
            switch (i3) {
                case 1:
                    this.mDisplayFlags = parcel.readInt();
                    objValueOf = Integer.valueOf(this.mDisplayFlags);
                    break;
                case 2:
                case 7:
                default:
                    objValueOf = null;
                    break;
                case 3:
                    this.mBackgroundColorRGBA = parcel.readInt();
                    objValueOf = Integer.valueOf(this.mBackgroundColorRGBA);
                    break;
                case 4:
                    this.mHighlightColorRGBA = parcel.readInt();
                    objValueOf = Integer.valueOf(this.mHighlightColorRGBA);
                    break;
                case 5:
                    this.mScrollDelay = parcel.readInt();
                    objValueOf = Integer.valueOf(this.mScrollDelay);
                    break;
                case 6:
                    this.mWrapText = parcel.readInt();
                    objValueOf = Integer.valueOf(this.mWrapText);
                    break;
                case 8:
                    readBlinkingText(parcel);
                    objValueOf = this.mBlinkingPosList;
                    break;
                case 9:
                    readFont(parcel);
                    objValueOf = this.mFontList;
                    break;
                case 10:
                    readHighlight(parcel);
                    objValueOf = this.mHighlightPosList;
                    break;
                case 11:
                    readHyperText(parcel);
                    objValueOf = this.mHyperTextList;
                    break;
                case 12:
                    readKaraoke(parcel);
                    objValueOf = this.mKaraokeList;
                    break;
                case 13:
                    readStyle(parcel);
                    objValueOf = this.mStyleList;
                    break;
                case 14:
                    this.mTextBounds = new Rect(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    objValueOf = null;
                    break;
                case 15:
                    this.mJustification = new Justification(parcel.readInt(), parcel.readInt());
                    objValueOf = this.mJustification;
                    break;
            }
            if (objValueOf != null) {
                if (this.mKeyObjectMap.containsKey(Integer.valueOf(i3))) {
                    this.mKeyObjectMap.remove(Integer.valueOf(i3));
                }
                this.mKeyObjectMap.put(Integer.valueOf(i3), objValueOf);
            }
        }
        return true;
    }

    private void readStyle(Parcel parcel) {
        int i = -1;
        int i2 = -1;
        int i3 = -1;
        int i4 = -1;
        int i5 = -1;
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        while (!z && parcel.dataAvail() > 0) {
            int i6 = parcel.readInt();
            if (i6 != 2) {
                switch (i6) {
                    case 103:
                        i = parcel.readInt();
                        break;
                    case 104:
                        i2 = parcel.readInt();
                        break;
                    case 105:
                        i3 = parcel.readInt();
                        break;
                    case 106:
                        i4 = parcel.readInt();
                        break;
                    case 107:
                        i5 = parcel.readInt();
                        break;
                    default:
                        parcel.setDataPosition(parcel.dataPosition() - 4);
                        z = true;
                        break;
                }
            } else {
                int i7 = parcel.readInt();
                z2 = i7 % 2 == 1;
                z3 = i7 % 4 >= 2;
                z4 = i7 / 4 == 1;
            }
        }
        Style style = new Style(i, i2, i3, z2, z3, z4, i4, i5);
        if (this.mStyleList == null) {
            this.mStyleList = new ArrayList();
        }
        this.mStyleList.add(style);
    }

    private void readFont(Parcel parcel) {
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            Font font = new Font(parcel.readInt(), new String(parcel.createByteArray(), 0, parcel.readInt()));
            if (this.mFontList == null) {
                this.mFontList = new ArrayList();
            }
            this.mFontList.add(font);
        }
    }

    private void readHighlight(Parcel parcel) {
        CharPos charPos = new CharPos(parcel.readInt(), parcel.readInt());
        if (this.mHighlightPosList == null) {
            this.mHighlightPosList = new ArrayList();
        }
        this.mHighlightPosList.add(charPos);
    }

    private void readKaraoke(Parcel parcel) {
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            Karaoke karaoke = new Karaoke(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
            if (this.mKaraokeList == null) {
                this.mKaraokeList = new ArrayList();
            }
            this.mKaraokeList.add(karaoke);
        }
    }

    private void readHyperText(Parcel parcel) {
        HyperText hyperText = new HyperText(parcel.readInt(), parcel.readInt(), new String(parcel.createByteArray(), 0, parcel.readInt()), new String(parcel.createByteArray(), 0, parcel.readInt()));
        if (this.mHyperTextList == null) {
            this.mHyperTextList = new ArrayList();
        }
        this.mHyperTextList.add(hyperText);
    }

    private void readBlinkingText(Parcel parcel) {
        CharPos charPos = new CharPos(parcel.readInt(), parcel.readInt());
        if (this.mBlinkingPosList == null) {
            this.mBlinkingPosList = new ArrayList();
        }
        this.mBlinkingPosList.add(charPos);
    }

    private boolean isValidKey(int i) {
        if ((i >= 1 && i <= 16) || (i >= 101 && i <= 107)) {
            return true;
        }
        return false;
    }

    private boolean containsKey(int i) {
        if (isValidKey(i) && this.mKeyObjectMap.containsKey(Integer.valueOf(i))) {
            return true;
        }
        return false;
    }

    private Set keySet() {
        return this.mKeyObjectMap.keySet();
    }

    private Object getObject(int i) {
        if (containsKey(i)) {
            return this.mKeyObjectMap.get(Integer.valueOf(i));
        }
        throw new IllegalArgumentException("Invalid key: " + i);
    }
}
