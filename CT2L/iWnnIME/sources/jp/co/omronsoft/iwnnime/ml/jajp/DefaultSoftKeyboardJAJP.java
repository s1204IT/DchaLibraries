package jp.co.omronsoft.iwnnime.ml.jajp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import com.android.common.speech.LoggingEvents;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import jp.co.omronsoft.iwnnime.ml.BaseInputView;
import jp.co.omronsoft.iwnnime.ml.DefaultSoftKeyboard;
import jp.co.omronsoft.iwnnime.ml.FlickKeyboardView;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.IWnnImeBase;
import jp.co.omronsoft.iwnnime.ml.IWnnImeEvent;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.KeyboardSkinData;
import jp.co.omronsoft.iwnnime.ml.KeyboardView;
import jp.co.omronsoft.iwnnime.ml.MultiTouchKeyboardView;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.StrSegment;
import jp.co.omronsoft.iwnnime.ml.WnnAccessibility;
import jp.co.omronsoft.iwnnime.ml.WnnArrayAdapter;
import jp.co.omronsoft.iwnnime.ml.WnnKeyboardFactory;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.controlpanel.Keyboard50KeyTypeListPreference;
import jp.co.omronsoft.iwnnime.ml.controlpanel.KeyboardTypeListPreference;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class DefaultSoftKeyboardJAJP extends DefaultSoftKeyboard {
    private static final int ALPHA_NUM_MODE = 1;
    private static final int INPUT_TYPE_INSTANT = 2;
    private static final int INPUT_TYPE_TOGGLE = 1;
    private static final int JP_KEYMODE_BIT_UNLIMITED;
    private static final int KANA_MODE = 0;
    private static final int KEYBOARD_FACTORY_SIZE_JA_INPUT_COLUMN = 2;
    private static final int KEYBOARD_FACTORY_SIZE_JA_MODE_COLUMN = 8;
    private static final int KEYBOARD_FACTORY_SIZE_JA_TYPE_COLUMN = 6;
    private static final String TAG = "DefaultSoftKeyboardJAJP";
    private static final boolean USE_ENGLISH_PREDICT = true;
    private boolean mCanUndo;
    private char[] mCurrentInstantTable;
    private boolean mEnableFlickToggle;
    private boolean mEnableUndoKey;
    private Drawable mInputIconPreviewUndo;
    private Drawable mInputIconUndo;
    private Keyboard mInputKeyBoard;
    private int mInputKeyCode;
    private int mInputType;
    private Drawable mNoInputIconPreviewUndo;
    private Drawable mNoInputIconUndo;
    private Keyboard mNoInputKeyBoard;
    private int mNoInputKeyCode;
    private Drawable mShiftIconUndo;
    private Keyboard mShiftKeyBoard;
    private int mShiftKeyCode;
    private int[] mSlideCycleTable;
    private String mUndoKey;
    private int mUndoKeyMode;
    private static final int[] JP_MODE_ALL_TABLE = {3, 4, 5, 6, 0, 7, 1, 2, 8};
    private static final int[] JP_MODE_CYCLE_TABLE = {3, 0, 1};
    private static final int[] JP_MODE_PASSWORD_TABLE = {0, 1};
    private static final int[] JP_MODE_DEFAULT_TABLE = {0};
    private static final String[][] JP_FULL_HIRAGANA_CYCLE_TABLE = {new String[]{"あ", "い", "う", "え", "お", "ぁ", "ぃ", "ぅ", "ぇ", "ぉ"}, new String[]{"か", "き", "く", "け", "こ"}, new String[]{"さ", "し", "す", "せ", "そ"}, new String[]{"た", "ち", "つ", "て", "と", "っ"}, new String[]{"な", "に", "ぬ", "ね", "の"}, new String[]{"は", "ひ", "ふ", "へ", "ほ"}, new String[]{"ま", "み", "む", "め", "も"}, new String[]{"や", "ゆ", "よ", "ゃ", "ゅ", "ょ"}, new String[]{"ら", "り", "る", "れ", "ろ"}, new String[]{"わ", "を", "ん", "ゎ", "ー"}, new String[]{"、", "。", "？", "！", "・", "\u3000"}};
    private static final String[][] JP_FULL_HIRAGANA_CYCLE_TABLE_FLICK = {new String[]{"あ", "い", "う", "え", "お"}, new String[]{"か", "き", "く", "け", "こ"}, new String[]{"さ", "し", "す", "せ", "そ"}, new String[]{"た", "ち", "つ", "て", "と"}, new String[]{"な", "に", "ぬ", "ね", "の"}, new String[]{"は", "ひ", "ふ", "へ", "ほ"}, new String[]{"ま", "み", "む", "め", "も"}, new String[]{"や", "（", "ゆ", "）", "よ"}, new String[]{"ら", "り", "る", "れ", "ろ"}, new String[]{"わ", "を", "ん", "ー", null}, new String[]{"、", "。", "？", "！", null}};
    private static final HashMap<String, String> JP_FULL_HIRAGANA_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("あ", "ぁ");
            put("い", "ぃ");
            put("う", "ぅ");
            put("え", "ぇ");
            put("お", "ぉ");
            put("ぁ", "あ");
            put("ぃ", "い");
            put("ぅ", "ヴ");
            put("ぇ", "え");
            put("ぉ", "お");
            put("か", "が");
            put("き", "ぎ");
            put("く", "ぐ");
            put("け", "げ");
            put("こ", "ご");
            put("が", "か");
            put("ぎ", "き");
            put("ぐ", "く");
            put("げ", "け");
            put("ご", "こ");
            put("さ", "ざ");
            put("し", "じ");
            put("す", "ず");
            put("せ", "ぜ");
            put("そ", "ぞ");
            put("ざ", "さ");
            put("じ", "し");
            put("ず", "す");
            put("ぜ", "せ");
            put("ぞ", "そ");
            put("た", "だ");
            put("ち", "ぢ");
            put("つ", "っ");
            put("て", "で");
            put("と", "ど");
            put("だ", "た");
            put("ぢ", "ち");
            put("っ", "づ");
            put("で", "て");
            put("ど", "と");
            put("づ", "つ");
            put("ヴ", "う");
            put("は", "ば");
            put("ひ", "び");
            put("ふ", "ぶ");
            put("へ", "べ");
            put("ほ", "ぼ");
            put("ば", "ぱ");
            put("び", "ぴ");
            put("ぶ", "ぷ");
            put("べ", "ぺ");
            put("ぼ", "ぽ");
            put("ぱ", "は");
            put("ぴ", "ひ");
            put("ぷ", "ふ");
            put("ぺ", "へ");
            put("ぽ", "ほ");
            put("や", "ゃ");
            put("ゆ", "ゅ");
            put("よ", "ょ");
            put("ゃ", "や");
            put("ゅ", "ゆ");
            put("ょ", "よ");
            put("わ", "ゎ");
            put("ゎ", "わ");
            put("゛", "゜");
            put("゜", "゛");
        }
    };
    private static final HashMap<String, String> JP_FULL_HIRAGANA_DAKUTEN_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ぅ", "ヴ");
            put("う", "ヴ");
            put("か", "が");
            put("き", "ぎ");
            put("く", "ぐ");
            put("け", "げ");
            put("こ", "ご");
            put("さ", "ざ");
            put("し", "じ");
            put("す", "ず");
            put("せ", "ぜ");
            put("そ", "ぞ");
            put("た", "だ");
            put("ち", "ぢ");
            put("つ", "づ");
            put("て", "で");
            put("と", "ど");
            put("っ", "づ");
            put("は", "ば");
            put("ひ", "び");
            put("ふ", "ぶ");
            put("へ", "べ");
            put("ほ", "ぼ");
            put("ぱ", "ば");
            put("ぴ", "び");
            put("ぷ", "ぶ");
            put("ぺ", "べ");
            put("ぽ", "ぼ");
            put("゜", "゛");
        }
    };
    private static final HashMap<String, String> JP_FULL_HIRAGANA_HANDAKUTEN_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("は", "ぱ");
            put("ひ", "ぴ");
            put("ふ", "ぷ");
            put("へ", "ぺ");
            put("ほ", "ぽ");
            put("ば", "ぱ");
            put("び", "ぴ");
            put("ぶ", "ぷ");
            put("べ", "ぺ");
            put("ぼ", "ぽ");
            put("゛", "゜");
        }
    };
    private static final HashMap<String, String> JP_FULL_HIRAGANA_CAPITAL_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("あ", "ぁ");
            put("い", "ぃ");
            put("う", "ぅ");
            put("え", "ぇ");
            put("お", "ぉ");
            put("ぁ", "あ");
            put("ぃ", "い");
            put("ぅ", "う");
            put("ぇ", "え");
            put("ぉ", "お");
            put("ヴ", "ぅ");
            put("つ", "っ");
            put("っ", "つ");
            put("づ", "っ");
            put("や", "ゃ");
            put("ゆ", "ゅ");
            put("よ", "ょ");
            put("ゃ", "や");
            put("ゅ", "ゆ");
            put("ょ", "よ");
            put("わ", "ゎ");
            put("ゎ", "わ");
        }
    };
    private static final String[][] JP_FULL_KATAKANA_CYCLE_TABLE = {new String[]{"ア", "イ", "ウ", "エ", "オ", "ァ", "ィ", "ゥ", "ェ", "ォ"}, new String[]{"カ", "キ", "ク", "ケ", "コ"}, new String[]{"サ", "シ", "ス", "セ", "ソ"}, new String[]{"タ", "チ", "ツ", "テ", "ト", "ッ"}, new String[]{"ナ", "ニ", "ヌ", "ネ", "ノ"}, new String[]{"ハ", "ヒ", "フ", "ヘ", "ホ"}, new String[]{"マ", "ミ", "ム", "メ", "モ"}, new String[]{"ヤ", "ユ", "ヨ", "ャ", "ュ", "ョ"}, new String[]{"ラ", "リ", "ル", "レ", "ロ"}, new String[]{"ワ", "ヲ", "ン", "ヮ", "ー"}, new String[]{"、", "。", "？", "！", "・", "\u3000"}};
    private static final String[][] JP_FULL_KATAKANA_CYCLE_TABLE_FLICK = {new String[]{"ア", "イ", "ウ", "エ", "オ"}, new String[]{"カ", "キ", "ク", "ケ", "コ"}, new String[]{"サ", "シ", "ス", "セ", "ソ"}, new String[]{"タ", "チ", "ツ", "テ", "ト"}, new String[]{"ナ", "ニ", "ヌ", "ネ", "ノ"}, new String[]{"ハ", "ヒ", "フ", "ヘ", "ホ"}, new String[]{"マ", "ミ", "ム", "メ", "モ"}, new String[]{"ヤ", "（", "ユ", "）", "ヨ"}, new String[]{"ラ", "リ", "ル", "レ", "ロ"}, new String[]{"ワ", "ヲ", "ン", "ー", null}, new String[]{"、", "。", "？", "！", null}};
    private static final HashMap<String, String> JP_FULL_KATAKANA_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ア", "ァ");
            put("イ", "ィ");
            put("ウ", "ゥ");
            put("エ", "ェ");
            put("オ", "ォ");
            put("ァ", "ア");
            put("ィ", "イ");
            put("ゥ", "ヴ");
            put("ェ", "エ");
            put("ォ", "オ");
            put("カ", "ガ");
            put("キ", "ギ");
            put("ク", "グ");
            put("ケ", "ゲ");
            put("コ", "ゴ");
            put("ガ", "カ");
            put("ギ", "キ");
            put("グ", "ク");
            put("ゲ", "ケ");
            put("ゴ", "コ");
            put("サ", "ザ");
            put("シ", "ジ");
            put("ス", "ズ");
            put("セ", "ゼ");
            put("ソ", "ゾ");
            put("ザ", "サ");
            put("ジ", "シ");
            put("ズ", "ス");
            put("ゼ", "セ");
            put("ゾ", "ソ");
            put("タ", "ダ");
            put("チ", "ヂ");
            put("ツ", "ッ");
            put("テ", "デ");
            put("ト", "ド");
            put("ダ", "タ");
            put("ヂ", "チ");
            put("ッ", "ヅ");
            put("デ", "テ");
            put("ド", "ト");
            put("ヅ", "ツ");
            put("ヴ", "ウ");
            put("ハ", "バ");
            put("ヒ", "ビ");
            put("フ", "ブ");
            put("ヘ", "ベ");
            put("ホ", "ボ");
            put("バ", "パ");
            put("ビ", "ピ");
            put("ブ", "プ");
            put("ベ", "ペ");
            put("ボ", "ポ");
            put("パ", "ハ");
            put("ピ", "ヒ");
            put("プ", "フ");
            put("ペ", "ヘ");
            put("ポ", "ホ");
            put("ヤ", "ャ");
            put("ユ", "ュ");
            put("ヨ", "ョ");
            put("ャ", "ヤ");
            put("ュ", "ユ");
            put("ョ", "ヨ");
            put("ワ", "ヮ");
            put("ヮ", "ワ");
        }
    };
    private static final HashMap<String, String> JP_FULL_KATAKANA_DAKUTEN_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ウ", "ヴ");
            put("ゥ", "ヴ");
            put("カ", "ガ");
            put("キ", "ギ");
            put("ク", "グ");
            put("ケ", "ゲ");
            put("コ", "ゴ");
            put("サ", "ザ");
            put("シ", "ジ");
            put("ス", "ズ");
            put("セ", "ゼ");
            put("ソ", "ゾ");
            put("タ", "ダ");
            put("チ", "ヂ");
            put("ツ", "ヅ");
            put("テ", "デ");
            put("ト", "ド");
            put("ッ", "ヅ");
            put("ハ", "バ");
            put("ヒ", "ビ");
            put("フ", "ブ");
            put("ヘ", "ベ");
            put("ホ", "ボ");
            put("パ", "バ");
            put("ピ", "ビ");
            put("プ", "ブ");
            put("ペ", "ベ");
            put("ポ", "ボ");
        }
    };
    private static final HashMap<String, String> JP_FULL_KATAKANA_HANDAKUTEN_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ハ", "パ");
            put("ヒ", "ピ");
            put("フ", "プ");
            put("ヘ", "ペ");
            put("ホ", "ポ");
            put("バ", "パ");
            put("ビ", "ピ");
            put("ブ", "プ");
            put("ベ", "ペ");
            put("ボ", "ポ");
        }
    };
    private static final HashMap<String, String> JP_FULL_KATAKANA_CAPITAL_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ア", "ァ");
            put("イ", "ィ");
            put("ウ", "ゥ");
            put("エ", "ェ");
            put("オ", "ォ");
            put("ァ", "ア");
            put("ィ", "イ");
            put("ゥ", "ウ");
            put("ェ", "エ");
            put("ォ", "オ");
            put("ヴ", "ゥ");
            put("ツ", "ッ");
            put("ッ", "ツ");
            put("ヅ", "ッ");
            put("ヤ", "ャ");
            put("ユ", "ュ");
            put("ヨ", "ョ");
            put("ャ", "ヤ");
            put("ュ", "ユ");
            put("ョ", "ヨ");
            put("ワ", "ヮ");
            put("ヮ", "ワ");
        }
    };
    private static final String[][] JP_HALF_KATAKANA_CYCLE_TABLE = {new String[]{"ｱ", "ｲ", "ｳ", "ｴ", "ｵ", "ｧ", "ｨ", "ｩ", "ｪ", "ｫ"}, new String[]{"ｶ", "ｷ", "ｸ", "ｹ", "ｺ"}, new String[]{"ｻ", "ｼ", "ｽ", "ｾ", "ｿ"}, new String[]{"ﾀ", "ﾁ", "ﾂ", "ﾃ", "ﾄ", "ｯ"}, new String[]{"ﾅ", "ﾆ", "ﾇ", "ﾈ", "ﾉ"}, new String[]{"ﾊ", "ﾋ", "ﾌ", "ﾍ", "ﾎ"}, new String[]{"ﾏ", "ﾐ", "ﾑ", "ﾒ", "ﾓ"}, new String[]{"ﾔ", "ﾕ", "ﾖ", "ｬ", "ｭ", "ｮ"}, new String[]{"ﾗ", "ﾘ", "ﾙ", "ﾚ", "ﾛ"}, new String[]{"ﾜ", "ｦ", "ﾝ", "ｰ"}, new String[]{"､", "｡", "?", "!", "･", " "}};
    private static final String[][] JP_HALF_KATAKANA_CYCLE_TABLE_FLICK = {new String[]{"ｱ", "ｲ", "ｳ", "ｴ", "ｵ"}, new String[]{"ｶ", "ｷ", "ｸ", "ｹ", "ｺ"}, new String[]{"ｻ", "ｼ", "ｽ", "ｾ", "ｿ"}, new String[]{"ﾀ", "ﾁ", "ﾂ", "ﾃ", "ﾄ"}, new String[]{"ﾅ", "ﾆ", "ﾇ", "ﾈ", "ﾉ"}, new String[]{"ﾊ", "ﾋ", "ﾌ", "ﾍ", "ﾎ"}, new String[]{"ﾏ", "ﾐ", "ﾑ", "ﾒ", "ﾓ"}, new String[]{"ﾔ", "(", "ﾕ", ")", "ﾖ"}, new String[]{"ﾗ", "ﾘ", "ﾙ", "ﾚ", "ﾛ"}, new String[]{"ﾜ", "ｦ", "ﾝ", "ｰ", null}, new String[]{"､", "｡", "?", "!", null}};
    private static final HashMap<String, String> JP_HALF_KATAKANA_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ｱ", "ｧ");
            put("ｲ", "ｨ");
            put("ｳ", "ｩ");
            put("ｴ", "ｪ");
            put("ｵ", "ｫ");
            put("ｧ", "ｱ");
            put("ｨ", "ｲ");
            put("ｩ", "ｳﾞ");
            put("ｪ", "ｴ");
            put("ｫ", "ｵ");
            put("ｶ", "ｶﾞ");
            put("ｷ", "ｷﾞ");
            put("ｸ", "ｸﾞ");
            put("ｹ", "ｹﾞ");
            put("ｺ", "ｺﾞ");
            put("ｶﾞ", "ｶ");
            put("ｷﾞ", "ｷ");
            put("ｸﾞ", "ｸ");
            put("ｹﾞ", "ｹ");
            put("ｺﾞ", "ｺ");
            put("ｻ", "ｻﾞ");
            put("ｼ", "ｼﾞ");
            put("ｽ", "ｽﾞ");
            put("ｾ", "ｾﾞ");
            put("ｿ", "ｿﾞ");
            put("ｻﾞ", "ｻ");
            put("ｼﾞ", "ｼ");
            put("ｽﾞ", "ｽ");
            put("ｾﾞ", "ｾ");
            put("ｿﾞ", "ｿ");
            put("ﾀ", "ﾀﾞ");
            put("ﾁ", "ﾁﾞ");
            put("ﾂ", "ｯ");
            put("ﾃ", "ﾃﾞ");
            put("ﾄ", "ﾄﾞ");
            put("ﾀﾞ", "ﾀ");
            put("ﾁﾞ", "ﾁ");
            put("ｯ", "ﾂﾞ");
            put("ﾃﾞ", "ﾃ");
            put("ﾄﾞ", "ﾄ");
            put("ﾂﾞ", "ﾂ");
            put("ﾊ", "ﾊﾞ");
            put("ﾋ", "ﾋﾞ");
            put("ﾌ", "ﾌﾞ");
            put("ﾍ", "ﾍﾞ");
            put("ﾎ", "ﾎﾞ");
            put("ﾊﾞ", "ﾊﾟ");
            put("ﾋﾞ", "ﾋﾟ");
            put("ﾌﾞ", "ﾌﾟ");
            put("ﾍﾞ", "ﾍﾟ");
            put("ﾎﾞ", "ﾎﾟ");
            put("ﾊﾟ", "ﾊ");
            put("ﾋﾟ", "ﾋ");
            put("ﾌﾟ", "ﾌ");
            put("ﾍﾟ", "ﾍ");
            put("ﾎﾟ", "ﾎ");
            put("ﾔ", "ｬ");
            put("ﾕ", "ｭ");
            put("ﾖ", "ｮ");
            put("ｬ", "ﾔ");
            put("ｭ", "ﾕ");
            put("ｮ", "ﾖ");
            put("ｳﾞ", "ｳ");
        }
    };
    private static final HashMap<String, String> JP_HALF_KATAKANA_DAKUTEN_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ｩ", "ｳﾞ");
            put("ｳ", "ｳﾞ");
            put("ｶ", "ｶﾞ");
            put("ｷ", "ｷﾞ");
            put("ｸ", "ｸﾞ");
            put("ｹ", "ｹﾞ");
            put("ｺ", "ｺﾞ");
            put("ｻ", "ｻﾞ");
            put("ｼ", "ｼﾞ");
            put("ｽ", "ｽﾞ");
            put("ｾ", "ｾﾞ");
            put("ｿ", "ｿﾞ");
            put("ﾀ", "ﾀﾞ");
            put("ﾁ", "ﾁﾞ");
            put("ﾂ", "ﾂﾞ");
            put("ﾃ", "ﾃﾞ");
            put("ﾄ", "ﾄﾞ");
            put("ｯ", "ﾂﾞ");
            put("ﾊ", "ﾊﾞ");
            put("ﾋ", "ﾋﾞ");
            put("ﾌ", "ﾌﾞ");
            put("ﾍ", "ﾍﾞ");
            put("ﾎ", "ﾎﾞ");
            put("ﾊﾟ", "ﾊﾞ");
            put("ﾋﾟ", "ﾋﾞ");
            put("ﾌﾟ", "ﾌﾞ");
            put("ﾍﾟ", "ﾍﾞ");
            put("ﾎﾟ", "ﾎﾞ");
        }
    };
    private static final HashMap<String, String> JP_HALF_KATAKANA_HANDAKUTEN_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ﾊ", "ﾊﾟ");
            put("ﾋ", "ﾋﾟ");
            put("ﾌ", "ﾌﾟ");
            put("ﾍ", "ﾍﾟ");
            put("ﾎ", "ﾎﾟ");
            put("ﾊﾞ", "ﾊﾟ");
            put("ﾋﾞ", "ﾋﾟ");
            put("ﾌﾞ", "ﾌﾟ");
            put("ﾍﾞ", "ﾍﾟ");
            put("ﾎﾞ", "ﾎﾟ");
        }
    };
    private static final HashMap<String, String> JP_HALF_KATAKANA_CAPITAL_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("ｱ", "ｧ");
            put("ｲ", "ｨ");
            put("ｳ", "ｩ");
            put("ｴ", "ｪ");
            put("ｵ", "ｫ");
            put("ｧ", "ｱ");
            put("ｨ", "ｲ");
            put("ｩ", "ｳ");
            put("ｪ", "ｴ");
            put("ｫ", "ｵ");
            put("ｳﾞ", "ｩ");
            put("ﾂ", "ｯ");
            put("ｯ", "ﾂ");
            put("ﾂﾞ", "ｯ");
            put("ﾔ", "ｬ");
            put("ﾕ", "ｭ");
            put("ﾖ", "ｮ");
            put("ｬ", "ﾔ");
            put("ｭ", "ﾕ");
            put("ｮ", "ﾖ");
        }
    };
    private static final String[][] JP_FULL_ALPHABET_CYCLE_TABLE = {new String[]{"．", "＠", "－", "＿", "／", "：", "～", "１"}, new String[]{"ａ", "ｂ", "ｃ", "Ａ", "Ｂ", "Ｃ", "２"}, new String[]{"ｄ", "ｅ", "ｆ", "Ｄ", "Ｅ", "Ｆ", "３"}, new String[]{"ｇ", "ｈ", "ｉ", "Ｇ", "Ｈ", "Ｉ", "４"}, new String[]{"ｊ", "ｋ", "ｌ", "Ｊ", "Ｋ", "Ｌ", "５"}, new String[]{"ｍ", "ｎ", "ｏ", "Ｍ", "Ｎ", "Ｏ", "６"}, new String[]{"ｐ", "ｑ", "ｒ", "ｓ", "Ｐ", "Ｑ", "Ｒ", "Ｓ", "７"}, new String[]{"ｔ", "ｕ", "ｖ", "Ｔ", "Ｕ", "Ｖ", "８"}, new String[]{"ｗ", "ｘ", "ｙ", "ｚ", "Ｗ", "Ｘ", "Ｙ", "Ｚ", "９"}, new String[]{"－", "０"}, new String[]{"，", "．", "？", "！", "・", "\u3000"}};
    private static final String[][] JP_FULL_ALPHABET_CYCLE_TABLE_FLICK = {new String[]{"．", "＠", "－", "＿", "１"}, new String[]{"ａ", "ｂ", "ｃ", null, "２"}, new String[]{"ｄ", "ｅ", "ｆ", null, "３"}, new String[]{"ｇ", "ｈ", "ｉ", null, "４"}, new String[]{"ｊ", "ｋ", "ｌ", null, "５"}, new String[]{"ｍ", "ｎ", "ｏ", null, "６"}, new String[]{"ｐ", "ｑ", "ｒ", "ｓ", "７"}, new String[]{"ｔ", "ｕ", "ｖ", null, "８"}, new String[]{"ｗ", "ｘ", "ｙ", "ｚ", "９"}, new String[]{"－", null, "０", null, null}, new String[]{"，", "．", "？", "！", null}};
    private static final HashMap<String, String> JP_FULL_ALPHABET_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("Ａ", "ａ");
            put("Ｂ", "ｂ");
            put("Ｃ", "ｃ");
            put("Ｄ", "ｄ");
            put("Ｅ", "ｅ");
            put("ａ", "Ａ");
            put("ｂ", "Ｂ");
            put("ｃ", "Ｃ");
            put("ｄ", "Ｄ");
            put("ｅ", "Ｅ");
            put("Ｆ", "ｆ");
            put("Ｇ", "ｇ");
            put("Ｈ", "ｈ");
            put("Ｉ", "ｉ");
            put("Ｊ", "ｊ");
            put("ｆ", "Ｆ");
            put("ｇ", "Ｇ");
            put("ｈ", "Ｈ");
            put("ｉ", "Ｉ");
            put("ｊ", "Ｊ");
            put("Ｋ", "ｋ");
            put("Ｌ", "ｌ");
            put("Ｍ", "ｍ");
            put("Ｎ", "ｎ");
            put("Ｏ", "ｏ");
            put("ｋ", "Ｋ");
            put("ｌ", "Ｌ");
            put("ｍ", "Ｍ");
            put("ｎ", "Ｎ");
            put("ｏ", "Ｏ");
            put("Ｐ", "ｐ");
            put("Ｑ", "ｑ");
            put("Ｒ", "ｒ");
            put("Ｓ", "ｓ");
            put("Ｔ", "ｔ");
            put("ｐ", "Ｐ");
            put("ｑ", "Ｑ");
            put("ｒ", "Ｒ");
            put("ｓ", "Ｓ");
            put("ｔ", "Ｔ");
            put("Ｕ", "ｕ");
            put("Ｖ", "ｖ");
            put("Ｗ", "ｗ");
            put("Ｘ", "ｘ");
            put("Ｙ", "ｙ");
            put("ｕ", "Ｕ");
            put("ｖ", "Ｖ");
            put("ｗ", "Ｗ");
            put("ｘ", "Ｘ");
            put("ｙ", "Ｙ");
            put("Ｚ", "ｚ");
            put("ｚ", "Ｚ");
        }
    };
    private static final String[][] JP_HALF_ALPHABET_CYCLE_TABLE = {new String[]{".", "@", "-", "_", "/", ":", "~", "1"}, new String[]{"a", "b", "c", "A", "B", "C", "2"}, new String[]{"d", "e", "f", "D", "E", "F", "3"}, new String[]{"g", "h", "i", "G", "H", "I", "4"}, new String[]{"j", "k", "l", "J", "K", "L", "5"}, new String[]{"m", "n", "o", "M", "N", "O", "6"}, new String[]{"p", "q", "r", "s", "P", "Q", "R", "S", "7"}, new String[]{"t", "u", "v", "T", "U", "V", "8"}, new String[]{"w", "x", "y", "z", "W", "X", "Y", "Z", "9"}, new String[]{"-", "0"}, new String[]{iWnnEngine.DECO_OPERATION_SEPARATOR, ".", "?", "!", ";", " "}};
    private static final String[][] JP_HALF_ALPHABET_CYCLE_TABLE_FLICK = {new String[]{".", "@", "-", "_", "1"}, new String[]{"a", "b", "c", null, "2"}, new String[]{"d", "e", "f", null, "3"}, new String[]{"g", "h", "i", null, "4"}, new String[]{"j", "k", "l", null, "5"}, new String[]{"m", "n", "o", null, "6"}, new String[]{"p", "q", "r", "s", "7"}, new String[]{"t", "u", "v", null, "8"}, new String[]{"w", "x", "y", "z", "9"}, new String[]{"-", null, "0", null, null}, new String[]{iWnnEngine.DECO_OPERATION_SEPARATOR, ".", "?", "!", null}};
    private static final HashMap<String, String> JP_HALF_ALPHABET_REPLACE_TABLE = new HashMap<String, String>() {
        {
            put("A", "a");
            put("B", "b");
            put("C", "c");
            put("D", "d");
            put("E", "e");
            put("a", "A");
            put("b", "B");
            put("c", "C");
            put("d", "D");
            put("e", "E");
            put("F", "f");
            put("G", "g");
            put("H", "h");
            put("I", "i");
            put("J", "j");
            put("f", "F");
            put("g", "G");
            put("h", "H");
            put("i", "I");
            put("j", "J");
            put("K", "k");
            put("L", "l");
            put("M", "m");
            put("N", "n");
            put("O", "o");
            put("k", "K");
            put("l", "L");
            put("m", "M");
            put("n", "N");
            put("o", "O");
            put("P", "p");
            put("Q", "q");
            put("R", "r");
            put("S", "s");
            put("T", "t");
            put("p", "P");
            put("q", "Q");
            put("r", "R");
            put("s", "S");
            put("t", "T");
            put("U", "u");
            put("V", "v");
            put("W", "w");
            put("X", "x");
            put("Y", "y");
            put("u", "U");
            put("v", "V");
            put("w", "W");
            put("x", "X");
            put("y", "Y");
            put("Z", "z");
            put("z", "Z");
        }
    };
    private static final HashMap<String, String> JP_EMPTY_REPLACE_TABLE = new HashMap<>();
    private static final char[] INSTANT_CHAR_CODE_FULL_NUMBER = "１２３４５６７８９０＃＊".toCharArray();
    private static final String[][] JP_FULL_NUMBER_CYCLE_TABLE_FLICK = {new String[]{"１", "．", "＠", "－", null}, new String[]{"２", "／", "：", "＿", null}, new String[]{"３", "～", "％", "＾", null}, new String[]{"４", "［", "‘", "］", "’"}, new String[]{"５", "＜", "＄", "＞", "￥"}, new String[]{"６", "｛", "＆", "｝", "”"}, new String[]{"７", "＼", null, "｜", null}, new String[]{"８", "（", null, "）", null}, new String[]{"９", "＝", null, "；", null}, new String[]{"０", "！", "＋", "？", null}, new String[]{"＃", "、", "＊", "。", null}, new String[]{"＊", "！", null, "？", null}};
    private static final char[] INSTANT_CHAR_CODE_HALF_NUMBER = "1234567890#*".toCharArray();
    private static final String[][] JP_HALF_NUMBER_CYCLE_TABLE_FLICK = {new String[]{"1", ".", "@", "-", null}, new String[]{"2", "/", ":", "_", null}, new String[]{"3", "~", "%", "^", null}, new String[]{"4", "[", "`", "]", "'"}, new String[]{"5", "<", "$", ">", "¥"}, new String[]{"6", "{", "&", "}", "\""}, new String[]{"7", "\\", null, "|", null}, new String[]{"8", "(", null, ")", null}, new String[]{"9", "=", null, ";", null}, new String[]{"0", "!", "+", "?", null}, new String[]{"#", iWnnEngine.DECO_OPERATION_SEPARATOR, "*", ".", null}, new String[]{"*", "!", null, "?", null}};
    private static final int[] FLICK_GUIDE_CAPS_TEXT_ID_TABLE = {R.string.ti_key_12key_caps_kana_flick_guide_txt, R.string.ti_key_12key_caps_alpha_flick_guide_txt};

    static {
        int unlimitedBit = 0;
        int[] arr$ = JP_MODE_ALL_TABLE;
        for (int mode : arr$) {
            if (mode != 2) {
                unlimitedBit |= 1 << mode;
            }
        }
        JP_KEYMODE_BIT_UNLIMITED = unlimitedBit;
    }

    public DefaultSoftKeyboardJAJP(IWnnIME wnn) {
        super(wnn);
        this.mInputType = 1;
        this.mCurrentInstantTable = null;
        this.mUndoKey = null;
        this.mInputKeyBoard = null;
        this.mNoInputKeyBoard = null;
        this.mShiftKeyBoard = null;
        this.mUndoKeyMode = -1;
        this.mInputIconUndo = null;
        this.mInputIconPreviewUndo = null;
        this.mInputKeyCode = 0;
        this.mNoInputIconUndo = null;
        this.mNoInputIconPreviewUndo = null;
        this.mShiftIconUndo = null;
        this.mNoInputKeyCode = 0;
        this.mShiftKeyCode = 0;
        this.mCanUndo = false;
        this.mEnableUndoKey = USE_ENGLISH_PREDICT;
        this.mEnableFlickToggle = USE_ENGLISH_PREDICT;
        this.mSlideCycleTable = null;
    }

    @Override
    protected void createKeyboards() {
        if (this.mWnn != null) {
            this.mUndoKey = this.mWnn.getResources().getString(R.string.ti_key_12key_undo_txt);
            this.mKeyboard = (WnnKeyboardFactory[][][][][]) Array.newInstance((Class<?>) WnnKeyboardFactory.class, 2, 6, 2, 8, 2);
            if (this.mHardKeyboardHidden) {
                if (this.mDisplayMode == 0) {
                    createKeyboardsPortrait();
                } else {
                    createKeyboardsLandscape();
                }
                if (this.mCurrentKeyboardType == 1) {
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_12KEY));
                    return;
                } else if (this.mCurrentKeyboardType == 3) {
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_50KEY));
                    return;
                } else {
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
                    return;
                }
            }
            if (this.mEnableHardware12Keyboard) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_12KEY));
            } else {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
            }
        }
    }

    @Override
    public void changeKeyMode(int keyMode) {
        int targetMode;
        if (this.mWnn != null && (targetMode = filterKeyMode(keyMode)) != -1) {
            commitText();
            int keyboardTypeBack = this.mCurrentKeyboardType;
            if (targetMode != 8) {
                this.mCurrentKeyboardType = getKeyboardTypePref(targetMode);
                mCurrentKeyMode = targetMode;
            }
            this.mPrevInputKeyCode = 0;
            if (this.mCurrentKeyboardType != keyboardTypeBack) {
                changeKeyboardType(this.mCurrentKeyboardType);
            }
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                this.mKeyboardView.setShifted(false);
                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(false);
                this.mCapsLock = false;
                if (targetMode != 8) {
                    mCurrentKeyMode = targetMode;
                }
                this.mPrevInputKeyCode = 0;
            } else {
                if (this.mCapsLock) {
                    this.mCapsLock = false;
                }
                this.mShiftOn = 0;
            }
            Keyboard kbd = getModeChangeKeyboard(targetMode);
            int mode = 1;
            switch (targetMode) {
                case 0:
                    this.mInputType = 1;
                    mode = 2;
                    break;
                case 1:
                    this.mInputType = 2;
                    mode = 1;
                    this.mCurrentInstantTable = INSTANT_CHAR_CODE_HALF_NUMBER;
                    break;
                case 3:
                    this.mInputType = 1;
                    mode = 0;
                    break;
                case 4:
                    this.mInputType = 1;
                    mode = IWnnImeBase.ENGINE_MODE_FULL_KATAKANA;
                    break;
                case 5:
                    this.mInputType = 1;
                    mode = IWnnImeBase.ENGINE_MODE_HALF_KATAKANA;
                    break;
                case 6:
                    this.mInputType = 1;
                    mode = 1;
                    break;
                case 7:
                    this.mInputType = 2;
                    mode = 1;
                    this.mCurrentInstantTable = INSTANT_CHAR_CODE_FULL_NUMBER;
                    break;
                case 8:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.VOICE_INPUT));
                    return;
            }
            changeKeyboard(kbd);
            setUndoKey(this.mCanUndo);
            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, mode));
            super.changeKeyMode(keyMode);
        }
    }

    @Override
    public View initView(int width) {
        if (IWnnIME.isDebugging()) {
            Log.d(TAG, "initView()" + (this.mKeyboardView != null));
        }
        View view = super.initView(width);
        this.mMultiTouchKeyboardView.setOnHoverListener(this);
        this.mFlickKeyboardView.setOnHoverListener(this);
        boolean isKeep = false;
        if (this.mWnn != null) {
            isKeep = this.mWnn.isKeepInput();
        }
        WnnKeyboardFactory keyboard = this.mKeyboard[this.mDisplayMode][this.mCurrentKeyboardType][this.mShiftOn][mCurrentKeyMode][0];
        if (keyboard != null && this.mKeyboardView != null) {
            if (isKeep) {
                this.mCurrentKeyboard = keyboard.getKeyboard(mCurrentKeyMode, this.mCurrentKeyboardType, getKeyboardCondition(false, false));
                if (this.mIsSymbolKeyboard) {
                    setSymbolKeyboard();
                } else {
                    setNormalKeyboard();
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        if (this.mCapsLock) {
                            ((MultiTouchKeyboardView) this.mKeyboardView).setShifted(USE_ENGLISH_PREDICT);
                            if (isSoftLockEnabled()) {
                                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(USE_ENGLISH_PREDICT);
                            } else {
                                ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLockMode(USE_ENGLISH_PREDICT);
                            }
                        }
                    } else {
                        setShifted(this.mShiftOn);
                    }
                }
            } else {
                changeKeyMode(mCurrentKeyMode);
            }
        }
        if (IWnnIME.isDebugging()) {
            Log.d(TAG, "initView(): width=" + width + ", kbdView=" + this.mKeyboardView);
        }
        return view;
    }

    public void change50KeyType(int type) {
        if (this.mWnn != null) {
            commitText();
            this.mCurrent50KeyType = type;
            Keyboard kbd = getTypeChangeKeyboard(this.mCurrentKeyboardType);
            if (kbd != null) {
                changeKeyboard(kbd);
                setShiftByEditorInfo(USE_ENGLISH_PREDICT);
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_50KEY));
            }
        }
    }

    public void changeKeyboardType(int type) {
        if (this.mWnn != null) {
            commitText();
            Keyboard kbd = getTypeChangeKeyboard(type);
            if (kbd != null) {
                this.mCurrentKeyboardType = type;
                changeKeyboard(kbd);
                setShiftByEditorInfo(USE_ENGLISH_PREDICT);
            }
            if (type == 1) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_12KEY));
            } else if (type == 3) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_50KEY));
            } else {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
            }
        }
    }

    @Override
    public boolean onKey(int primaryCode, int[] keyCodes) {
        boolean ret = super.onKey(primaryCode, keyCodes);
        if (!ret && this.mWnn != null) {
            switch (primaryCode) {
                case DefaultSoftKeyboard.KEYCODE_CLOSE_WINDOWS:
                    if (this.mWnn.isInputViewShown()) {
                        commitText();
                        closing();
                        this.mWnn.requestHideSelf(0);
                    }
                    break;
                case DefaultSoftKeyboard.KEYCODE_SWITCH_VOICE:
                    changeKeyMode(8);
                    break;
                case DefaultSoftKeyboard.KEYCODE_EISU_KANA:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_EISU_KANA));
                    break;
                case DefaultSoftKeyboard.KEYCODE_SETTING_MENU:
                    startControlPanelStandard();
                    break;
                case DefaultSoftKeyboard.KEYCODE_UNDO:
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.UNDO));
                    break;
                case DefaultSoftKeyboard.KEYCODE_JP12_REVERSE:
                    if (!this.mNoInput) {
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_REVERSE_CHAR, this.mCurrentCycleTable));
                    }
                    break;
                case DefaultSoftKeyboard.KEYCODE_JP12_ASTER:
                    if (this.mInputType == 2) {
                        commitText();
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_CHAR, this.mCurrentInstantTable[getTableIndex(primaryCode)]));
                    } else if (!this.mNoInput) {
                        HashMap<String, String> replaceTable = getReplaceTable();
                        if (replaceTable == null) {
                            Log.e(TAG, "not founds replace table");
                        } else {
                            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.REPLACE_CHAR, replaceTable));
                            this.mPrevInputKeyCode = primaryCode;
                        }
                    }
                    break;
                case DefaultSoftKeyboard.KEYCODE_JP12_SHARP:
                case DefaultSoftKeyboard.KEYCODE_JP12_0:
                case DefaultSoftKeyboard.KEYCODE_JP12_9:
                case DefaultSoftKeyboard.KEYCODE_JP12_8:
                case DefaultSoftKeyboard.KEYCODE_JP12_7:
                case DefaultSoftKeyboard.KEYCODE_JP12_6:
                case DefaultSoftKeyboard.KEYCODE_JP12_5:
                case DefaultSoftKeyboard.KEYCODE_JP12_4:
                case DefaultSoftKeyboard.KEYCODE_JP12_3:
                case DefaultSoftKeyboard.KEYCODE_JP12_2:
                case DefaultSoftKeyboard.KEYCODE_JP12_1:
                    if (this.mInputType == 2) {
                        commitText();
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_CHAR, this.mCurrentInstantTable[getTableIndex(primaryCode)]));
                    } else {
                        if (this.mEnableFlick && !this.mEnableFlickToggle && isEnableFlickMode(primaryCode)) {
                            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOUCH_OTHER_KEY));
                        }
                        if (this.mPrevInputKeyCode != primaryCode) {
                            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOUCH_OTHER_KEY));
                            if (mCurrentKeyMode == 0 && primaryCode == -211) {
                                commitText();
                            }
                        }
                        String[][] cycleTable = getCycleTable();
                        if (cycleTable == null) {
                            Log.e(TAG, "not founds cycle table");
                        } else {
                            int index = getTableIndex(primaryCode);
                            this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TOGGLE_CHAR, cycleTable[index]));
                            this.mCurrentCycleTable = cycleTable[index];
                        }
                        this.mPrevInputKeyCode = primaryCode;
                    }
                    break;
                default:
                    this.mPrevInputKeyCode = primaryCode;
                    break;
            }
            if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                this.mCapsLock = ((MultiTouchKeyboardView) this.mKeyboardView).isCapsLock();
            }
            if (!this.mCapsLock && primaryCode != -1) {
                setShiftByEditorInfo(false);
            }
        }
        return USE_ENGLISH_PREDICT;
    }

    @Override
    public boolean isEnableReplace(String input) {
        if (input == null) {
            return false;
        }
        HashMap<String, String> replaceTable = getReplaceTable();
        if (replaceTable == null) {
            Log.e(TAG, "not founds replace table");
            return false;
        }
        return replaceTable.containsKey(input);
    }

    @Override
    protected void setPreferencesCharacteristic(SharedPreferences pref, EditorInfo editor) {
        if (editor != null && this.mWnn != null) {
            boolean preEnableFlick = this.mEnableFlick;
            Resources res = this.mWnn.getResources();
            if (WnnAccessibility.isAccessibility(this.mWnn)) {
                this.mEnableFlick = false;
            } else {
                this.mEnableFlick = pref.getBoolean(ControlPanelPrefFragment.FLICK_INPUT_KEY, res.getBoolean(R.bool.flick_input_default_value));
            }
            this.mEnableFlickToggle = pref.getBoolean(ControlPanelPrefFragment.FLICK_TOGGLE_INPUT_KEY, res.getBoolean(R.bool.flick_toggle_input_default_value));
            this.mEnableUndoKey = pref.getBoolean(ControlPanelPrefFragment.DISPLAY_UNDO_KEY, res.getBoolean(R.bool.opt_display_undo_key_default_value));
            this.mCurrent50KeyType = Integer.parseInt(pref.getString(Keyboard50KeyTypeListPreference.PREF_50KEY_TYPE, res.getString(R.string.keyboard_50key_type_list_item_vertical_right)));
            if (preEnableFlick != this.mEnableFlick) {
                createKeyboards();
                this.mLastInputType = -1;
            }
            if (!this.mWnn.isKeepInput()) {
                int inputType = editor.inputType;
                if (inputType == 0 && editor.packageName.equals("jp.co.omronsoft.iwnnime.ml")) {
                    inputType = 1;
                }
                if (this.mHardKeyboardHidden) {
                    if (this.mIsInputTypeNull) {
                        this.mIsInputTypeNull = false;
                    }
                } else if (this.mEnableHardware12Keyboard && this.mIsInputTypeNull) {
                    this.mIsInputTypeNull = false;
                    changeKeyboardType(1);
                }
                this.mPreferenceKeyMode = -1;
                boolean lastNoInput = this.mNoInput;
                this.mNoInput = USE_ENGLISH_PREDICT;
                this.mDisableKeyInput = false;
                if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                    ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(false);
                } else {
                    this.mCapsLock = false;
                }
                this.mAutoCaps = false;
                this.mForceShift = USE_ENGLISH_PREDICT;
                boolean setDefault = false;
                int changeKeyMode = -1;
                int imeOptions = editor.imeOptions;
                this.mAllowedKeyMode = JP_KEYMODE_BIT_UNLIMITED;
                boolean enableHalfAlphabet = USE_ENGLISH_PREDICT;
                if (this.mHardKeyboardHidden) {
                    enableHalfAlphabet = pref.getBoolean(ControlPanelPrefFragment.ENABLE_HALF_ALPHABET_MODE_KEY, res.getBoolean(R.bool.half_alphabet_mode_default_value));
                }
                if (!enableHalfAlphabet) {
                    disableKeyModeFlag(0);
                }
                int defaultKeyMode = IWnnImeJaJp.getDefaultKeyMode(editor);
                if (defaultKeyMode == -1) {
                    switch (inputType & 15) {
                        case 1:
                            switch (inputType & 4080) {
                                case 16:
                                case 32:
                                case 208:
                                    this.mPreferenceKeyMode = 0;
                                    break;
                                case 128:
                                case 144:
                                case 224:
                                    setAllowedKeyMode(JP_MODE_PASSWORD_TABLE);
                                    disableVoiceInput();
                                    break;
                                case 192:
                                    disableVoiceInput();
                                    break;
                            }
                            if ((inputType & 28672) != 0) {
                                this.mAutoCaps = USE_ENGLISH_PREDICT;
                            }
                            break;
                        case 2:
                            this.mPreferenceKeyMode = 1;
                            setAllowedKeyMode(JP_MODE_PASSWORD_TABLE);
                            disableVoiceInput();
                            break;
                        case 3:
                            this.mForceShift = false;
                            if (this.mHardKeyboardHidden) {
                                setAllowedKeyMode(MODE_PHONE_TABLE);
                            } else if (this.mEnableHardware12Keyboard) {
                                setAllowedKeyMode(MODE_NUMBER_TABLE);
                            } else {
                                setAllowedKeyMode(JP_MODE_DEFAULT_TABLE);
                            }
                            break;
                        case 4:
                            this.mPreferenceKeyMode = 1;
                            setAllowedKeyMode(MODE_DATETIME_TABLE);
                            if (!enableHalfAlphabet || 16 == (inputType & 4080)) {
                                disableKeyModeFlag(0);
                            }
                            disableVoiceInput();
                            break;
                        default:
                            if (inputType == 0) {
                                this.mIsInputTypeNull = USE_ENGLISH_PREDICT;
                                setAllowedKeyMode(MODE_NULL_TABLE);
                                disableVoiceInput();
                                this.mAutoCaps = false;
                                changeKeyboardType(0);
                            }
                            break;
                    }
                    if ((Integer.MIN_VALUE & imeOptions) != 0 && this.mPreferenceKeyMode == -1) {
                        this.mPreferenceKeyMode = 0;
                    }
                } else {
                    switch (defaultKeyMode) {
                        case 0:
                        case 1:
                        case 3:
                            this.mPreferenceKeyMode = defaultKeyMode;
                            break;
                        case 2:
                            this.mPreferenceKeyMode = defaultKeyMode;
                            if (this.mHardKeyboardHidden) {
                                this.mForceShift = false;
                                setAllowedKeyMode(MODE_PHONE_TABLE);
                            }
                            break;
                        default:
                            changeKeyMode = defaultKeyMode;
                            break;
                    }
                    this.mLastInputType = -1;
                }
                if (this.mPreferenceKeyMode == 0) {
                    enableKeyModeFlag(0);
                }
                if (!mEnableVoiceInput) {
                    disableKeyModeFlag(8);
                }
                setHardwareKeyModeFilter();
                updateKeyboards();
                forceCloseVoiceInputKeyboard();
                createSlideCycleTable();
                if (this.mKeyboardView != null) {
                    this.mKeyboardView.setIsInputTypeNull(this.mIsInputTypeNull);
                }
                this.mEnableAutoCaps = (this.mAutoCaps && pref.getBoolean(ControlPanelPrefFragment.AUTO_CAPS_KEY, res.getBoolean(R.bool.auto_caps_default_value))) ? USE_ENGLISH_PREDICT : false;
                boolean hasInputTypeChanged = inputType != this.mLastInputType ? USE_ENGLISH_PREDICT : false;
                boolean hasImeOptionsChanged = imeOptions != this.mLastImeOptions ? USE_ENGLISH_PREDICT : false;
                boolean conflictedSettings = (mCurrentKeyMode != 0 || isEnableKeyMode(0)) ? false : USE_ENGLISH_PREDICT;
                if (hasInputTypeChanged || hasImeOptionsChanged || conflictedSettings) {
                    if (changeKeyMode != -1) {
                        changeKeyMode(changeKeyMode);
                    } else {
                        setDefaultKeyboard();
                    }
                    setDefault = USE_ENGLISH_PREDICT;
                    if (defaultKeyMode == -1) {
                        this.mLastInputType = inputType;
                        this.mLastImeOptions = imeOptions;
                    }
                }
                setShiftByEditorInfo(this.mForceShift);
                int type = getKeyboardTypePref(mCurrentKeyMode);
                if (this.mCurrentKeyboardType != type) {
                    changeKeyboardType(type);
                } else if (!setDefault && !lastNoInput) {
                    Keyboard newKeyboard = getKeyboardInputted(false);
                    changeKeyboard(newKeyboard);
                }
                setStatusIcon();
            }
        }
    }

    private void createKeyboardsPortrait() {
        createKeyboardsPortraitQwerty();
        createKeyboardsPortrait12Key();
        createKeyboardsSelect50keyType(0, 0);
        createKeyboardsSelect50keyType(0, 1);
        createKeyboardsSelect50keyType(0, 2);
    }

    private void createKeyboardsPortraitQwerty() {
        if (this.mWnn != null) {
            WnnKeyboardFactory[][] keyList = this.mKeyboard[0][0][0];
            keyList[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_full_symbols);
            keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_half_symbols);
            keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[2][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_phone);
            WnnKeyboardFactory[][] keyList2 = this.mKeyboard[0][0][1];
            keyList2[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_full_symbols_shift);
            keyList2[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_half_symbols_shift);
            keyList2[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[2][0] = this.mKeyboard[0][0][0][2][0];
        }
    }

    private void createKeyboardsPortrait12Key() {
        if (this.mWnn != null) {
            WnnKeyboardFactory[][] keyList = this.mKeyboard[0][1][0];
            keyList[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
            keyList[3][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            keyList[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
            keyList[6][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            if (this.mEnableFlick) {
                keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[4][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
                keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[5][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            } else {
                keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[4][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input_flick_off);
                keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[5][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input_flick_off);
            }
            keyList[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
            keyList[0][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            keyList[2][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_phone);
            this.mKeyboard[0][1][1] = this.mKeyboard[0][1][0];
        }
    }

    private void createKeyboardsSelect50keyType(int rotation, int type) {
        int layoutType = type + 3;
        WnnKeyboardFactory[][] keyList = this.mKeyboard[rotation][layoutType][0];
        keyList[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[3][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[4][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[5][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyList[2][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_phone);
        WnnKeyboardFactory[][] keyListShift = this.mKeyboard[rotation][layoutType][1];
        keyListShift[3][0] = keyList[3][0];
        keyListShift[3][1] = keyList[3][1];
        keyListShift[4][0] = keyList[4][0];
        keyListShift[4][1] = keyList[4][1];
        keyListShift[5][0] = keyList[5][0];
        keyListShift[5][1] = keyList[5][1];
        keyListShift[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyListShift[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_50key_jp_layout);
        keyListShift[7][0] = keyList[7][0];
        keyListShift[1][0] = keyList[1][0];
        keyListShift[2][0] = keyList[2][0];
    }

    private void createKeyboardsLandscape() {
        createKeyboardsLandscapeQwerty();
        createKeyboardsLandscape12Key();
        createKeyboardsSelect50keyType(1, 0);
        createKeyboardsSelect50keyType(1, 1);
        createKeyboardsSelect50keyType(1, 2);
    }

    private void createKeyboardsLandscapeQwerty() {
        if (this.mWnn != null) {
            WnnKeyboardFactory[][] keyList = this.mKeyboard[1][0][0];
            keyList[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_full_symbols);
            keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_half_symbols);
            keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout);
            keyList[2][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_phone);
            WnnKeyboardFactory[][] keyList2 = this.mKeyboard[1][0][1];
            keyList2[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_full_symbols_shift);
            keyList2[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_half_symbols_shift);
            keyList2[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_qwerty_jp_layout_shift);
            keyList2[2][0] = this.mKeyboard[1][0][0][2][0];
        }
    }

    private void createKeyboardsLandscape12Key() {
        if (this.mWnn != null) {
            WnnKeyboardFactory[][] keyList = this.mKeyboard[1][1][0];
            keyList[3][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
            keyList[3][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            keyList[6][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
            keyList[6][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            if (this.mEnableFlick) {
                keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[4][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
                keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
                keyList[5][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            } else {
                keyList[7][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[1][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[4][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[4][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input_flick_off);
                keyList[5][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_flick_off);
                keyList[5][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input_flick_off);
            }
            keyList[0][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout);
            keyList[0][1] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_12key_jp_layout_input);
            keyList[2][0] = new WnnKeyboardFactory(this.mWnn, R.xml.keyboard_phone);
            this.mKeyboard[1][1][1] = this.mKeyboard[1][1][0];
        }
    }

    private int getTableIndex(int keyCode) {
        if (keyCode == -201) {
            return 0;
        }
        if (keyCode == -202) {
            return 1;
        }
        if (keyCode == -203) {
            return 2;
        }
        if (keyCode == -204) {
            return 3;
        }
        if (keyCode == -205) {
            return 4;
        }
        if (keyCode == -206) {
            return 5;
        }
        if (keyCode == -207) {
            return 6;
        }
        if (keyCode == -208) {
            return 7;
        }
        if (keyCode == -209) {
            return 8;
        }
        if (keyCode == -210) {
            return 9;
        }
        if (keyCode == -211) {
            return 10;
        }
        return keyCode == -213 ? 11 : 0;
    }

    private String[][] getCycleTable() {
        String[][] cycleTable = (String[][]) null;
        switch (mCurrentKeyMode) {
            case 0:
                String[][] cycleTable2 = JP_HALF_ALPHABET_CYCLE_TABLE;
                return cycleTable2;
            case 1:
            case 2:
            case 7:
            default:
                return cycleTable;
            case 3:
                String[][] cycleTable3 = JP_FULL_HIRAGANA_CYCLE_TABLE;
                return cycleTable3;
            case 4:
                String[][] cycleTable4 = JP_FULL_KATAKANA_CYCLE_TABLE;
                return cycleTable4;
            case 5:
                String[][] cycleTable5 = JP_HALF_KATAKANA_CYCLE_TABLE;
                return cycleTable5;
            case 6:
                String[][] cycleTable6 = JP_FULL_ALPHABET_CYCLE_TABLE;
                return cycleTable6;
        }
    }

    private String[][] getFlickCycleTable() {
        String[][] cycleTable = (String[][]) null;
        switch (mCurrentKeyMode) {
            case 0:
                String[][] cycleTable2 = JP_HALF_ALPHABET_CYCLE_TABLE_FLICK;
                return cycleTable2;
            case 1:
                String[][] cycleTable3 = JP_HALF_NUMBER_CYCLE_TABLE_FLICK;
                return cycleTable3;
            case 2:
            default:
                return cycleTable;
            case 3:
                String[][] cycleTable4 = JP_FULL_HIRAGANA_CYCLE_TABLE_FLICK;
                return cycleTable4;
            case 4:
                String[][] cycleTable5 = JP_FULL_KATAKANA_CYCLE_TABLE_FLICK;
                return cycleTable5;
            case 5:
                String[][] cycleTable6 = JP_HALF_KATAKANA_CYCLE_TABLE_FLICK;
                return cycleTable6;
            case 6:
                String[][] cycleTable7 = JP_FULL_ALPHABET_CYCLE_TABLE_FLICK;
                return cycleTable7;
            case 7:
                String[][] cycleTable8 = JP_FULL_NUMBER_CYCLE_TABLE_FLICK;
                return cycleTable8;
        }
    }

    private HashMap<String, String> getReplaceTable() {
        switch (mCurrentKeyMode) {
            case 0:
                HashMap<String, String> hashTable = JP_HALF_ALPHABET_REPLACE_TABLE;
                return hashTable;
            case 1:
            case 2:
            case 7:
            default:
                return null;
            case 3:
                HashMap<String, String> hashTable2 = JP_FULL_HIRAGANA_REPLACE_TABLE;
                return hashTable2;
            case 4:
                HashMap<String, String> hashTable3 = JP_FULL_KATAKANA_REPLACE_TABLE;
                return hashTable3;
            case 5:
                HashMap<String, String> hashTable4 = JP_HALF_KATAKANA_REPLACE_TABLE;
                return hashTable4;
            case 6:
                HashMap<String, String> hashTable5 = JP_FULL_ALPHABET_REPLACE_TABLE;
                return hashTable5;
        }
    }

    private HashMap<String, String> getReplaceTable(int direction) {
        switch (mCurrentKeyMode) {
            case 0:
                switch (direction) {
                    case 0:
                    case 1:
                    case 3:
                    case 4:
                        HashMap<String, String> hashTable = JP_EMPTY_REPLACE_TABLE;
                        return hashTable;
                    case 2:
                        HashMap<String, String> hashTable2 = JP_HALF_ALPHABET_REPLACE_TABLE;
                        return hashTable2;
                    default:
                        return null;
                }
            case 1:
            case 7:
                return null;
            case 2:
            default:
                return null;
            case 3:
                switch (direction) {
                    case 0:
                    case 4:
                        HashMap<String, String> hashTable3 = JP_EMPTY_REPLACE_TABLE;
                        return hashTable3;
                    case 1:
                        HashMap<String, String> hashTable4 = JP_FULL_HIRAGANA_DAKUTEN_REPLACE_TABLE;
                        return hashTable4;
                    case 2:
                        HashMap<String, String> hashTable5 = JP_FULL_HIRAGANA_CAPITAL_REPLACE_TABLE;
                        return hashTable5;
                    case 3:
                        HashMap<String, String> hashTable6 = JP_FULL_HIRAGANA_HANDAKUTEN_REPLACE_TABLE;
                        return hashTable6;
                    default:
                        return null;
                }
            case 4:
                switch (direction) {
                    case 0:
                    case 4:
                        HashMap<String, String> hashTable7 = JP_EMPTY_REPLACE_TABLE;
                        return hashTable7;
                    case 1:
                        HashMap<String, String> hashTable8 = JP_FULL_KATAKANA_DAKUTEN_REPLACE_TABLE;
                        return hashTable8;
                    case 2:
                        HashMap<String, String> hashTable9 = JP_FULL_KATAKANA_CAPITAL_REPLACE_TABLE;
                        return hashTable9;
                    case 3:
                        HashMap<String, String> hashTable10 = JP_FULL_KATAKANA_HANDAKUTEN_REPLACE_TABLE;
                        return hashTable10;
                    default:
                        return null;
                }
            case 5:
                switch (direction) {
                    case 0:
                    case 4:
                        HashMap<String, String> hashTable11 = JP_EMPTY_REPLACE_TABLE;
                        return hashTable11;
                    case 1:
                        HashMap<String, String> hashTable12 = JP_HALF_KATAKANA_DAKUTEN_REPLACE_TABLE;
                        return hashTable12;
                    case 2:
                        HashMap<String, String> hashTable13 = JP_HALF_KATAKANA_CAPITAL_REPLACE_TABLE;
                        return hashTable13;
                    case 3:
                        HashMap<String, String> hashTable14 = JP_HALF_KATAKANA_HANDAKUTEN_REPLACE_TABLE;
                        return hashTable14;
                    default:
                        return null;
                }
            case 6:
                switch (direction) {
                    case 0:
                    case 1:
                    case 3:
                    case 4:
                        HashMap<String, String> hashTable15 = JP_EMPTY_REPLACE_TABLE;
                        return hashTable15;
                    case 2:
                        HashMap<String, String> hashTable16 = JP_FULL_ALPHABET_REPLACE_TABLE;
                        return hashTable16;
                    default:
                        return null;
                }
        }
    }

    @Override
    protected void setStatusIcon() {
        if (this.mWnn != null && this.mTextView != null) {
            if (this.mWnn.isSubtypeEmojiInput()) {
                this.mWnn.hideStatusIcon();
                return;
            }
            int icon = 0;
            switch (mCurrentKeyMode) {
                case 0:
                    icon = R.drawable.immodeic_half_alphabet;
                    this.mTextView.setText(R.string.ti_key_switch_half_alphabet_txt);
                    break;
                case 1:
                case 2:
                    icon = R.drawable.immodeic_half_number;
                    this.mTextView.setText(R.string.ti_key_switch_half_number_txt);
                    break;
                case 3:
                    icon = R.drawable.immodeic_hiragana;
                    this.mTextView.setText(R.string.ti_key_switch_full_hiragana_txt);
                    break;
                case 4:
                    icon = R.drawable.immodeic_full_kana;
                    this.mTextView.setText(R.string.ti_key_switch_full_katakana_txt);
                    break;
                case 5:
                    icon = R.drawable.immodeic_half_kana;
                    this.mTextView.setText(R.string.ti_key_switch_half_katakana_txt);
                    break;
                case 6:
                    icon = R.drawable.immodeic_full_alphabet;
                    this.mTextView.setText(R.string.ti_key_switch_full_alphabet_txt);
                    break;
                case 7:
                    icon = R.drawable.immodeic_full_number;
                    this.mTextView.setText(R.string.ti_key_switch_full_number_txt);
                    break;
            }
            if (this.mWnn.isInputViewShown()) {
                this.mWnn.showStatusIcon(icon);
            }
        }
    }

    @Override
    public void setHardKeyboardHidden(boolean hidden) {
        if (this.mWnn != null) {
            if (!hidden) {
                if (this.mEnableHardware12Keyboard) {
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_12KEY));
                } else {
                    this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
                }
            }
            if (this.mHardKeyboardHidden != hidden) {
                if (this.mAllowedKeyMode != JP_KEYMODE_BIT_UNLIMITED || (!this.mEnableHardware12Keyboard && mCurrentKeyMode != 3 && mCurrentKeyMode != 0)) {
                    this.mLastInputType = 0;
                    this.mLastImeOptions = -1;
                    if (this.mWnn.isInputViewShown()) {
                        setDefaultKeyboard();
                    }
                }
                if (hidden) {
                    this.mLastInputType = -1;
                    this.mLastImeOptions = -1;
                }
            }
        }
        this.mHardKeyboardHidden = hidden;
    }

    @Override
    public void setHardware12Keyboard(boolean type12Key) {
        if (this.mWnn != null && this.mEnableHardware12Keyboard != type12Key) {
            if (type12Key) {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_12KEY));
            } else {
                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.CHANGE_MODE, IWnnImeBase.ENGINE_MODE_OPT_TYPE_QWERTY));
            }
        }
        super.setHardware12Keyboard(type12Key);
    }

    @Override
    public void setUndoKey(boolean undo) {
        boolean hasChanged = false;
        this.mCanUndo = undo;
        if (!this.mIsSymbolKeyboard && this.mEnableUndoKey) {
            if (this.mInputKeyBoard != null) {
                if (this.mInputKeyBoard.getKeys().get(0).label != null) {
                    hasChanged = USE_ENGLISH_PREDICT;
                }
                this.mInputKeyBoard.getKeys().get(0).label = null;
                this.mInputKeyBoard.getKeys().get(0).icon = this.mInputIconUndo;
                this.mInputKeyBoard.getKeys().get(0).codes[0] = this.mInputKeyCode;
                this.mNoInputKeyBoard.getKeys().get(0).label = null;
                this.mNoInputKeyBoard.getKeys().get(0).icon = this.mNoInputIconUndo;
                this.mNoInputKeyBoard.getKeys().get(0).codes[0] = this.mNoInputKeyCode;
                this.mInputKeyBoard.getKeys().get(0).iconPreview = this.mInputIconPreviewUndo;
                if (this.mInputIconPreviewUndo != null) {
                    this.mInputKeyBoard.getKeys().get(0).iconPreview.setBounds(0, 0, this.mInputKeyBoard.getKeys().get(0).iconPreview.getIntrinsicWidth(), this.mInputKeyBoard.getKeys().get(0).iconPreview.getIntrinsicHeight());
                }
                this.mNoInputKeyBoard.getKeys().get(0).iconPreview = this.mNoInputIconPreviewUndo;
                if (this.mNoInputIconPreviewUndo != null) {
                    this.mNoInputKeyBoard.getKeys().get(0).iconPreview.setBounds(0, 0, this.mNoInputKeyBoard.getKeys().get(0).iconPreview.getIntrinsicWidth(), this.mNoInputKeyBoard.getKeys().get(0).iconPreview.getIntrinsicHeight());
                }
            }
            if (this.mShiftKeyBoard != null && this.mShiftKeyBoard != this.mNoInputKeyBoard) {
                this.mShiftKeyBoard.getKeys().get(0).label = null;
                this.mShiftKeyBoard.getKeys().get(0).icon = this.mShiftIconUndo;
                this.mShiftKeyBoard.getKeys().get(0).codes[0] = this.mShiftKeyCode;
            }
            if (this.mCurrentKeyboardType == 1 || this.mCurrentKeyboardType == 3) {
                if (undo) {
                    hasChanged = USE_ENGLISH_PREDICT;
                    this.mInputKeyBoard = getKeyboardInputted(USE_ENGLISH_PREDICT);
                    this.mNoInputKeyBoard = getKeyboardInputted(false);
                    this.mShiftKeyBoard = getShiftChangeKeyboard(1);
                    if (this.mInputKeyBoard != null) {
                        Keyboard.Key k = this.mInputKeyBoard.getKeys().get(0);
                        k.label = this.mUndoKey;
                        this.mInputIconUndo = k.icon;
                        k.icon = null;
                        this.mInputIconPreviewUndo = k.iconPreview;
                        k.iconPreview = null;
                        this.mInputKeyCode = k.codes[0];
                        k.codes[0] = -237;
                        if (this.mInputKeyBoard == this.mNoInputKeyBoard) {
                            this.mNoInputIconUndo = this.mInputIconUndo;
                            this.mNoInputIconPreviewUndo = this.mInputIconPreviewUndo;
                            this.mNoInputKeyCode = this.mInputKeyCode;
                        } else {
                            Keyboard.Key k2 = this.mNoInputKeyBoard.getKeys().get(0);
                            k2.label = this.mUndoKey;
                            this.mNoInputIconUndo = k2.icon;
                            k2.icon = null;
                            this.mNoInputIconPreviewUndo = k2.iconPreview;
                            k2.iconPreview = null;
                            this.mNoInputKeyCode = k2.codes[0];
                            k2.codes[0] = -237;
                        }
                        if (this.mInputKeyBoard == this.mShiftKeyBoard) {
                            this.mShiftIconUndo = this.mInputIconUndo;
                            this.mShiftKeyCode = this.mInputKeyCode;
                        } else {
                            Keyboard.Key k3 = this.mShiftKeyBoard.getKeys().get(0);
                            k3.label = this.mUndoKey;
                            this.mShiftIconUndo = k3.icon;
                            k3.icon = null;
                            this.mShiftKeyCode = k3.codes[0];
                            k3.codes[0] = -237;
                        }
                        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
                        if (keyskin.isValid()) {
                            Keyboard.Key keyboardInput = this.mInputKeyBoard.getKeys().get(0);
                            Keyboard.Key keyboardNoInput = this.mNoInputKeyBoard.getKeys().get(0);
                            Drawable tmpIcon = keyskin.getDrawable(DefaultSoftKeyboard.KEYCODE_UNDO, this.mInputKeyBoard, 1073741824);
                            if (tmpIcon != null) {
                                keyboardInput.icon = tmpIcon;
                                keyboardNoInput.icon = tmpIcon;
                            }
                            Drawable tmpPreview = keyskin.getDrawablePreview(DefaultSoftKeyboard.KEYCODE_UNDO, this.mInputKeyBoard, 1073741824);
                            if (tmpPreview != null) {
                                keyboardInput.iconPreview = tmpPreview;
                                keyboardInput.iconPreview.setBounds(0, 0, keyboardInput.iconPreview.getIntrinsicWidth(), keyboardInput.iconPreview.getIntrinsicHeight());
                                keyboardNoInput.iconPreview = tmpPreview;
                                keyboardNoInput.iconPreview.setBounds(0, 0, keyboardNoInput.iconPreview.getIntrinsicWidth(), keyboardNoInput.iconPreview.getIntrinsicHeight());
                            }
                            keyskin.setEnableUndoKeyBackground(USE_ENGLISH_PREDICT);
                        }
                    }
                }
                if (this.mKeyboardView != null && this.mCurrentKeyboard != null && hasChanged) {
                    boolean isShifted = false;
                    boolean isCapsLock = false;
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        isShifted = ((MultiTouchKeyboardView) this.mKeyboardView).isShifted();
                        isCapsLock = ((MultiTouchKeyboardView) this.mKeyboardView).isCapsLock();
                    }
                    this.mKeyboardView.setKeyboard(this.mCurrentKeyboard);
                    if (isShifted) {
                        ((MultiTouchKeyboardView) this.mKeyboardView).setShifted(USE_ENGLISH_PREDICT);
                    }
                    if (isCapsLock) {
                        ((MultiTouchKeyboardView) this.mKeyboardView).setCapsLock(USE_ENGLISH_PREDICT);
                    }
                }
            }
            if (!undo) {
                this.mInputKeyBoard = null;
                this.mNoInputKeyBoard = null;
                this.mShiftKeyBoard = null;
                this.mUndoKeyMode = mCurrentKeyMode;
                KeyboardSkinData keyskin2 = KeyboardSkinData.getInstance();
                if (keyskin2.isValid()) {
                    keyskin2.setEnableUndoKeyBackground(false);
                }
            }
        }
    }

    @Override
    public void undoKeyMode() {
        if (this.mUndoKeyMode != mCurrentKeyMode) {
            changeKeyMode(this.mUndoKeyMode);
        }
    }

    @Override
    protected void inputByFlickDirection(int direction, boolean isCommit) {
        if (isEnableFlickMode(this.mFlickPressKey)) {
            switch (direction) {
                case -2:
                    inputByFlick(4, isCommit);
                    break;
                case -1:
                    inputByFlick(1, isCommit);
                    break;
                case 0:
                    inputByFlick(0, isCommit);
                    break;
                case 1:
                    inputByFlick(3, isCommit);
                    break;
                case 2:
                    inputByFlick(2, isCommit);
                    break;
                case 5:
                    inputByFlick(0, isCommit);
                    break;
                case 6:
                    inputByFlick(1, isCommit);
                    break;
                case 7:
                    inputByFlick(2, isCommit);
                    break;
                case 8:
                    inputByFlick(3, isCommit);
                    break;
                case 9:
                    inputByFlick(4, isCommit);
                    break;
                case 10:
                    inputByFlick(5, isCommit);
                    break;
            }
        }
    }

    @Override
    protected void inputByFlick(int directionIndex, boolean isCommit) {
        if (this.mWnn != null && (this.mKeyboardView instanceof FlickKeyboardView)) {
            FlickKeyboardView keyboardView = (FlickKeyboardView) this.mKeyboardView;
            if (this.mFlickPressKey == -213 && hasDakutenCapitalConversion(mCurrentKeyMode)) {
                if (!this.mNoInput) {
                    HashMap<String, String> replaceTable = getReplaceTable(directionIndex);
                    if (replaceTable == null) {
                        Log.i(TAG, "not founds replace table");
                        return;
                    }
                    if (isCommit) {
                        if (directionIndex == 0) {
                            this.mIsKeyProcessFinish = false;
                            return;
                        } else {
                            if (!replaceTable.isEmpty()) {
                                this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.REPLACE_CHAR, replaceTable));
                                return;
                            }
                            return;
                        }
                    }
                    if (this.mEnablePopup) {
                        if (directionIndex == 0) {
                            keyboardView.setFlickedKeyGuide(USE_ENGLISH_PREDICT);
                            return;
                        }
                        int cursor = this.mWnn.getComposingText().getCursor(1);
                        StrSegment prevChar = this.mWnn.getComposingText().getStrSegment(1, cursor - 1);
                        if (prevChar != null) {
                            String search = prevChar.string;
                            String replaceChar = replaceTable.get(search);
                            if (replaceChar == null) {
                                keyboardView.clearFlickedKeyTop();
                                return;
                            } else {
                                keyboardView.setFlickedKeyTop(replaceChar, USE_ENGLISH_PREDICT);
                                return;
                            }
                        }
                        return;
                    }
                    return;
                }
                if (this.mEnablePopup) {
                    if (directionIndex == 0) {
                        keyboardView.setFlickedKeyGuide(USE_ENGLISH_PREDICT);
                        return;
                    } else {
                        keyboardView.clearFlickedKeyTop();
                        return;
                    }
                }
                return;
            }
            if (this.mFlickPressKey == -114) {
                if (directionIndex == 4) {
                    if (!isCommit) {
                        keyboardView.setFlickedKeyTop(null, USE_ENGLISH_PREDICT);
                        return;
                    } else {
                        nextKeyMode();
                        return;
                    }
                }
                if (isCommit) {
                    if (directionIndex != 5) {
                        int[] table = getSlideCycleTable();
                        int mode = table[directionIndex];
                        boolean enableModeKey = isEnableKeyMode(mode);
                        if (enableModeKey) {
                            changeKeyMode(mode);
                            return;
                        }
                        return;
                    }
                    return;
                }
                keyboardView.setSlidePopup(getAllowedKeyMode());
                return;
            }
            int index = getTableIndex(this.mFlickPressKey);
            String[][] cycleTable = getFlickCycleTable();
            if (cycleTable != null) {
                this.mCurrentCycleTable = cycleTable[index];
                if (isCommit && cycleTable[index][directionIndex] != null) {
                    String inputString = cycleTable[index][directionIndex];
                    if (mCurrentKeyMode == 0 && this.mFlickPressKey == -211) {
                        commitText();
                    }
                    if (hasDakutenCapitalConversion(mCurrentKeyMode)) {
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.FLICK_INPUT_CHAR, inputString.charAt(0)));
                        return;
                    } else {
                        this.mWnn.onEvent(new IWnnImeEvent(IWnnImeEvent.INPUT_CHAR, inputString.charAt(0)));
                        return;
                    }
                }
                if (this.mEnablePopup) {
                    if (directionIndex == 0) {
                        keyboardView.setFlickedKeyGuide(USE_ENGLISH_PREDICT);
                        return;
                    }
                    String str = cycleTable[index][directionIndex];
                    if (str == null) {
                        keyboardView.clearFlickedKeyTop();
                    } else {
                        keyboardView.setFlickedKeyTop(str, USE_ENGLISH_PREDICT);
                    }
                }
            }
        }
    }

    @Override
    protected boolean isEnableFlickMode(int key) {
        if (key == -114) {
            return USE_ENGLISH_PREDICT;
        }
        switch (mCurrentKeyMode) {
            case 0:
            case 1:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                if (this.mEnableFlick) {
                }
                break;
        }
        return false;
    }

    private int getKeyboardTypePref(int mode) {
        String type;
        if (this.mWnn == null || this.mIsInputTypeNull || (type = KeyboardTypeListPreference.getKeyboardTypePref(this.mWnn, mode, this.mDisplayMode)) == null) {
            return 0;
        }
        return Integer.parseInt(type);
    }

    @Override
    protected void setKeyboardTypePref() {
        KeyboardTypeListPreference.setKeyboardTypePref(this.mWnn, this.mCurrentKeyboardType, mCurrentKeyMode, this.mDisplayMode);
    }

    @Override
    protected void set50KeyTypePref() {
        if (this.mWnn != null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mWnn);
            SharedPreferences.Editor prefEditor = pref.edit();
            prefEditor.putString(Keyboard50KeyTypeListPreference.PREF_50KEY_TYPE, String.valueOf(this.mCurrent50KeyType));
            prefEditor.commit();
        }
    }

    private boolean hasDakutenCapitalConversion(int keyMode) {
        if (keyMode == 3 || keyMode == 5 || keyMode == 4 || keyMode == 0 || keyMode == 6) {
            return USE_ENGLISH_PREDICT;
        }
        return false;
    }

    @Override
    protected void showKeyboardTypeSwitchDialog() {
        Context context;
        final BaseInputView baseInputView = (BaseInputView) getCurrentView();
        if (baseInputView != null && (context = baseInputView.getContext()) != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(USE_ENGLISH_PREDICT);
            builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
            Resources r = baseInputView.getResources();
            CharSequence[] itemTitles = {r.getString(R.string.ti_input_mode_type_entry_54_txt), r.getString(R.string.ti_input_mode_type_entry_qwerty_txt), r.getString(R.string.ti_input_mode_type_entry_50key_txt)};
            final int[] itemValues = {1, 0, 3};
            int findIndex = 0;
            int i = 0;
            while (true) {
                if (i >= itemValues.length) {
                    break;
                }
                if (this.mCurrentKeyboardType != itemValues[i]) {
                    i++;
                } else {
                    findIndex = i;
                    break;
                }
            }
            WnnArrayAdapter<CharSequence> adapter = WnnUtility.makeTitleListWithIcon(context, itemTitles, itemValues, KeyboardTypeListPreference.ICON_TABLE_KEYBOARD_TYPE, findIndex);
            AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < itemValues.length) {
                        DefaultSoftKeyboardJAJP.this.changeKeyboardType(itemValues[position]);
                        DefaultSoftKeyboardJAJP.this.setKeyboardTypePref();
                    }
                    baseInputView.closeDialog();
                }
            };
            ListView listView = WnnUtility.makeSingleChoiceListView(context, adapter, findIndex, listener);
            builder.setView(listView);
            builder.setTitle(r.getString(R.string.ti_long_press_dialog_keyboard_type_txt));
            baseInputView.showDialog(builder);
        }
    }

    @Override
    protected void show50KeyTypeSwitchDialog() {
        Context context;
        final BaseInputView baseInputView = (BaseInputView) getCurrentView();
        if (baseInputView != null && (context = baseInputView.getContext()) != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(USE_ENGLISH_PREDICT);
            builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
            Resources r = baseInputView.getResources();
            CharSequence[] itemTitles = {r.getString(R.string.ti_50key_type_entry_vertical_right_txt), r.getString(R.string.ti_50key_type_entry_vertical_left_txt), r.getString(R.string.ti_50key_type_entry_horizontal_txt)};
            final int[] itemValues = {0, 1, 2};
            WnnArrayAdapter<CharSequence> adapter = WnnUtility.makeTitleListWithIcon(context, itemTitles, itemValues, Keyboard50KeyTypeListPreference.ICON_TABLE_50KEY_TYPE, this.mCurrent50KeyType);
            AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < itemValues.length) {
                        DefaultSoftKeyboardJAJP.this.change50KeyType(itemValues[position]);
                        DefaultSoftKeyboardJAJP.this.set50KeyTypePref();
                    }
                    baseInputView.closeDialog();
                }
            };
            ListView listView = WnnUtility.makeSingleChoiceListView(context, adapter, this.mCurrent50KeyType, listener);
            builder.setView(listView);
            builder.setTitle(r.getString(R.string.ti_long_press_dialog_50key_type_txt));
            baseInputView.showDialog(builder);
        }
    }

    @Override
    public boolean showInputModeSwitchDialog() {
        BaseInputView baseInputView = (BaseInputView) getCurrentView();
        AlertDialog.Builder builder = new AlertDialog.Builder(baseInputView.getContext());
        builder.setCancelable(USE_ENGLISH_PREDICT);
        builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
        final int[] itemValues = getAllowedKeyMode();
        Resources r = baseInputView.getResources();
        SparseArray<CharSequence> itemTitleList = new SparseArray<>();
        itemTitleList.put(3, r.getString(R.string.ti_input_mode_full_hirakana_title_txt));
        itemTitleList.put(4, r.getString(R.string.ti_input_mode_full_katakana_title_txt));
        itemTitleList.put(5, r.getString(R.string.ti_input_mode_half_katakana_title_txt));
        itemTitleList.put(6, r.getString(R.string.ti_input_mode_full_alphabet_title_txt));
        itemTitleList.put(0, r.getString(R.string.ti_input_mode_half_alphabet_title_txt));
        itemTitleList.put(7, r.getString(R.string.ti_input_mode_full_number_title_txt));
        itemTitleList.put(1, r.getString(R.string.ti_input_mode_half_number_title_txt));
        itemTitleList.put(8, r.getString(R.string.ti_input_mode_voice_input_title_txt));
        CharSequence[] itemTitles = new CharSequence[itemValues.length];
        for (int i = 0; i < itemValues.length; i++) {
            itemTitles[i] = itemTitleList.get(itemValues[i]);
        }
        builder.setSingleChoiceItems(itemTitles, findIndexOfValue(itemValues, mCurrentKeyMode), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface inputModeSwitchDialog, int position) {
                if (position >= 0 && position < itemValues.length) {
                    DefaultSoftKeyboardJAJP.this.changeKeyMode(itemValues[position]);
                }
                inputModeSwitchDialog.dismiss();
            }
        });
        builder.setTitle(r.getString(R.string.ti_long_press_dialog_input_mode_txt));
        baseInputView.showDialog(builder);
        return USE_ENGLISH_PREDICT;
    }

    private int findIndexOfValue(int[] value, int mode) {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == mode) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected boolean isSoftLockEnabled() {
        if (mCurrentKeyMode == 3 || mCurrentKeyMode == 6 || mCurrentKeyMode == 4 || mCurrentKeyMode == 0 || mCurrentKeyMode == 5) {
            return USE_ENGLISH_PREDICT;
        }
        return false;
    }

    @Override
    protected boolean getLongpressMushroomKey(Keyboard.Key key) {
        if (key != null) {
            switch (key.codes[0]) {
                case DefaultSoftKeyboard.KEYCODE_EISU_KANA:
                case DefaultSoftKeyboard.KEYCODE_JP12_SYM:
                case DefaultSoftKeyboard.KEYCODE_QWERTY_SYM:
                    break;
            }
            return false;
        }
        return false;
    }

    private void createSlideCycleTable() {
        ArrayList<Integer> list = new ArrayList<>();
        int[][] table = KeyboardView.SLIDE_POPUP_TABLE;
        for (int i = 1; i < table.length; i++) {
            if (isEnableKeyMode(table[i][1])) {
                list.add(Integer.valueOf(table[i][1]));
            }
        }
        if (mEnableVoiceInput) {
            list.add(8);
        }
        int size = list.size();
        this.mSlideCycleTable = new int[size];
        for (int i2 = 0; i2 < size; i2++) {
            this.mSlideCycleTable[i2] = list.get(i2).intValue();
        }
    }

    private int[] getSlideCycleTable() {
        if (this.mSlideCycleTable == null) {
            createSlideCycleTable();
        }
        return this.mSlideCycleTable;
    }

    @Override
    protected int getSlideCycleCount() {
        return getSlideCycleTable().length;
    }

    @Override
    protected boolean isFlickKey(int key) {
        switch (key) {
            case DefaultSoftKeyboard.KEYCODE_JP12_ASTER:
            case DefaultSoftKeyboard.KEYCODE_JP12_SHARP:
            case DefaultSoftKeyboard.KEYCODE_JP12_0:
            case DefaultSoftKeyboard.KEYCODE_JP12_9:
            case DefaultSoftKeyboard.KEYCODE_JP12_8:
            case DefaultSoftKeyboard.KEYCODE_JP12_7:
            case DefaultSoftKeyboard.KEYCODE_JP12_6:
            case DefaultSoftKeyboard.KEYCODE_JP12_5:
            case DefaultSoftKeyboard.KEYCODE_JP12_4:
            case DefaultSoftKeyboard.KEYCODE_JP12_3:
            case DefaultSoftKeyboard.KEYCODE_JP12_2:
            case DefaultSoftKeyboard.KEYCODE_JP12_1:
                return USE_ENGLISH_PREDICT;
            case DefaultSoftKeyboard.KEYCODE_TOGGLE_MODE:
                boolean ret = isEnableSwitchInputMode();
                return ret;
            default:
                return false;
        }
    }

    @Override
    protected boolean isEnableSwitchInputMode() {
        boolean z = USE_ENGLISH_PREDICT;
        int[] keyModes = getAllowedKeyMode();
        if (keyModes == null) {
            return false;
        }
        if (keyModes.length <= 1) {
            z = false;
        }
        return z;
    }

    private int[] getAllowedKeyMode() {
        int[] tmpArray = new int[JP_MODE_ALL_TABLE.length];
        int modeNum = 0;
        for (int i = 0; i < JP_MODE_ALL_TABLE.length; i++) {
            if (isEnableKeyMode(JP_MODE_ALL_TABLE[i])) {
                tmpArray[modeNum] = JP_MODE_ALL_TABLE[i];
                modeNum++;
            }
        }
        int[] allowedMode = new int[modeNum];
        System.arraycopy(tmpArray, 0, allowedMode, 0, modeNum);
        return allowedMode;
    }

    @Override
    public void setNormalKeyboard() {
        int type;
        if (this.mCurrentKeyboard != null && this.mIsSymbolKeyboard && this.mCurrentKeyboardType != (type = getKeyboardTypePref(mCurrentKeyMode))) {
            changeKeyboardType(type);
        }
        super.setNormalKeyboard();
    }

    public static String getDefaultKeyboardType(Context context, int mode, int displayMode) {
        if (context == null) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        StringBuffer sb = new StringBuffer();
        if (displayMode == 0) {
            sb.append("portrait_");
        } else {
            sb.append("land_");
        }
        switch (mode) {
            case 0:
                sb.append("input_mode_type_half_alphabet_default_value");
                break;
            case 1:
                sb.append("input_mode_type_half_numeric_default_value");
                break;
            case 2:
                sb.append("input_mode_type_phone_default_value");
                break;
            case 3:
                sb.append("input_mode_type_hiragana_kanji_default_value");
                break;
            case 4:
                sb.append("input_mode_type_full_katakana_default_value");
                break;
            case 5:
                sb.append("input_mode_type_half_katakana_default_value");
                break;
            case 6:
                sb.append("input_mode_type_full_alphabet_default_value");
                break;
            case 7:
                sb.append("input_mode_type_full_numeric_default_value");
                break;
            default:
                sb = new StringBuffer("input_mode_type_default_value");
                break;
        }
        Resources res = context.getResources();
        int strId = res.getIdentifier(sb.toString(), "string", context.getPackageName());
        return res.getString(strId);
    }

    @Override
    protected ArrayList<Integer> getLongPressMenuItemsUserDic() {
        return new ArrayList<Integer>() {
            {
                add(6);
                add(5);
            }
        };
    }

    @Override
    protected int getModeCondition() {
        int result = super.getModeCondition();
        if (result != 0) {
            return result;
        }
        switch (mCurrentKeyMode) {
            case 3:
                result = Keyboard.CONDITION_MODE_HIRAGANA;
                break;
            case 4:
                result = Keyboard.CONDITION_MODE_FULL_KATAKANA;
                break;
            case 5:
                result = Keyboard.CONDITION_MODE_HALF_KATAKANA;
                break;
            case 6:
                result = Keyboard.CONDITION_MODE_FULL_ALPHA;
                break;
            case 7:
                result = Keyboard.CONDITION_MODE_FULL_NUM;
                break;
        }
        return result;
    }

    @Override
    protected int getUnlimitedKeyMode() {
        return JP_KEYMODE_BIT_UNLIMITED;
    }

    @Override
    protected int[] getModeCycleTable() {
        return JP_MODE_CYCLE_TABLE;
    }

    @Override
    protected int[] getAllModeTable() {
        return JP_MODE_ALL_TABLE;
    }

    @Override
    protected void setSpaceKey(Keyboard newKeyboard, boolean inputted) {
        int index;
        Keyboard.Key newSpaceKey;
        IWnnImeBase imeBase;
        if (newKeyboard != null && this.mKeyboardView != null && mCurrentKeyMode == 3 && (index = newKeyboard.getKeyIndex(DefaultSoftKeyboard.KEYCODE_SPACE_JP)) >= 0 && (newSpaceKey = newKeyboard.getKey(index)) != null && this.mWnn != null && (imeBase = this.mWnn.getCurrentIWnnIme()) != null) {
            boolean convert = (inputted && imeBase.isEnableL2Converter()) ? USE_ENGLISH_PREDICT : false;
            newSpaceKey.description = WnnAccessibility.getDescriptionSpaceKey(this.mWnn, convert);
            if (!IWnnIME.isTabletMode() || this.mCurrentKeyboardType != 0) {
                newSpaceKey.label = null;
                newSpaceKey.keyPreviewLabel = null;
                newSpaceKey.icon = null;
                newSpaceKey.iconPreview = null;
                Resources res = this.mWnn.getResources();
                if (res != null) {
                    if (convert) {
                        String label = res.getString(R.string.ti_key_conversion_txt);
                        newSpaceKey.label = label;
                        newSpaceKey.keyPreviewLabel = label;
                    } else if (this.mCurrentKeyboardType == 0 || this.mCurrentKeyboardType == 3) {
                        newSpaceKey.icon = res.getDrawable(R.drawable.key_qwerty_space);
                        newSpaceKey.iconPreview = res.getDrawable(R.drawable.key_qwerty_space);
                    } else {
                        newSpaceKey.icon = res.getDrawable(R.drawable.key_12key_space);
                        newSpaceKey.iconPreview = res.getDrawable(R.drawable.key_12key_space);
                    }
                    KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
                    if (keyskin.isValid()) {
                        int keycode = newSpaceKey.codes[0];
                        if (convert) {
                            keycode = DefaultSoftKeyboard.KEYCODE_SPACE_CONV;
                        }
                        Drawable icon = keyskin.getDrawable(keycode, newKeyboard);
                        if (icon != null) {
                            newSpaceKey.icon = icon;
                        }
                        Drawable iconPreview = keyskin.getDrawablePreview(keycode, newKeyboard);
                        if (iconPreview != null) {
                            newSpaceKey.iconPreview = iconPreview;
                        }
                    }
                    if (newSpaceKey.iconPreview != null) {
                        newSpaceKey.iconPreview.setBounds(0, 0, newSpaceKey.iconPreview.getIntrinsicWidth(), newSpaceKey.iconPreview.getIntrinsicHeight());
                    }
                    Keyboard oldKeyboard = this.mCurrentKeyboard;
                    if (this.mKeyboardView instanceof MultiTouchKeyboardView) {
                        oldKeyboard = this.mKeyboardView.getKeyboard();
                    }
                    if (oldKeyboard == newKeyboard && this.mKeyboardView.isShown()) {
                        this.mKeyboardView.invalidateKey(index);
                    }
                }
            }
        }
    }

    @Override
    public String getInputModeKeyMainLabel() {
        if (this.mWnn == null) {
            return null;
        }
        Resources res = this.mWnn.getResources();
        return res.getString(R.string.ti_switch_input_mode_key_main_jp_txt);
    }

    @Override
    public String[] getTableForFlickGuide() {
        Resources res;
        if (this.mWnn == null || (res = this.mWnn.getResources()) == null) {
            return null;
        }
        int index = getTableIndex(this.mFlickPressKey);
        String[][] cycleTable = getFlickCycleTable();
        if (cycleTable == JP_HALF_KATAKANA_CYCLE_TABLE_FLICK) {
            cycleTable = JP_FULL_KATAKANA_CYCLE_TABLE_FLICK;
        } else if (cycleTable == JP_FULL_ALPHABET_CYCLE_TABLE_FLICK) {
            cycleTable = JP_HALF_ALPHABET_CYCLE_TABLE_FLICK;
        }
        if (cycleTable != null && index >= 0 && index < cycleTable.length) {
            return cycleTable[index];
        }
        int index2 = 0;
        switch (mCurrentKeyMode) {
            case 0:
            case 6:
                index2 = 1;
                break;
        }
        String str = res.getString(FLICK_GUIDE_CAPS_TEXT_ID_TABLE[index2]);
        return new String[]{str};
    }
}
