package jp.co.omronsoft.iwnnime.ml;

import android.view.KeyEvent;
import java.util.HashMap;

public class IWnnImeEvent {
    public static final int ADD_WORD = -268435434;
    public static final int AUTO_LEARNING = -268431355;
    public static final int CALL_MUSHROOM = -268427264;
    public static final int CANCEL_WEBAPI = -268431357;
    public static final int CHANGE_FLOATING = -268435417;
    public static final int CHANGE_INPUT_CANDIDATE_VIEW = -268435427;
    public static final int CHANGE_INPUT_MODE = -268435407;
    public static final int CHANGE_INPUT_VIEW = -268435428;
    public static final int CHANGE_KEYBOARD_TYPE = -268435406;
    public static final int CHANGE_MODE = -268435441;
    public static final int CLOSE_VIEW = -268435451;
    public static final int COMMIT_COMPOSING_TEXT = -268435440;
    public static final int CONVERT = -268435454;
    public static final int DELETE_WORD = -268435433;
    public static final int EDIT_WORDS_IN_USER_DICTIONARY = -268435430;
    public static final int FIT_INPUT_TYPE = -268435404;
    public static final int FLICK_INPUT_CHAR = -268435421;
    public static final int FOCUS_CANDIDATE_END = -268419071;
    public static final int FOCUS_CANDIDATE_START = -268419072;
    public static final int FOCUS_OUT_CANDIDATE_VIEW = -268435446;
    public static final int FOCUS_TO_CANDIDATE_VIEW = -268435447;
    public static final int GET_WORD = -268435432;
    public static final int INITIALIZE_LEARNING_DICTIONARY = -268435436;
    public static final int INITIALIZE_USER_DICTIONARY = -268435437;
    public static final int INIT_CONVERTER = -268435415;
    public static final int INIT_KEYBOARD_DONE = -268435403;
    public static final int INPUT_CHAR = -268435450;
    public static final int INPUT_KEY = -268435449;
    public static final int INPUT_SOFT_KEY = -268435442;
    public static final int KEYLONGPRESS = -268435420;
    public static final int KEYUP = -268435425;
    public static final int LIST_CANDIDATES_FULL = -268435452;
    public static final int LIST_CANDIDATES_NORMAL = -268435453;
    public static final int LIST_SYMBOLS = -268435439;
    public static final int LIST_WORDS_IN_USER_DICTIONARY = -268435435;
    public static final int PREDICT = -268435448;
    public static final int PRIVATE_EVENT_OFFSET = -16777216;
    public static final int RECEIVE_DECOEMOJI = -268414976;
    public static final int REPLACE_CHAR = -268435443;
    public static final int RESULT_WEBAPI_NG = -268431358;
    public static final int RESULT_WEBAPI_OK = -268431359;
    public static final int SELECT_CANDIDATE = -268435445;
    public static final int SELECT_WEBAPI = -268431360;
    public static final int SELECT_WEBAPI_GET_AGAIN = -268431354;
    public static final int SPACE_KEY = -268435408;
    public static final int START_SYMBOL_MODE = -268435405;
    public static final int SWITCH_LANGUAGE = -268435438;
    public static final int TIMEOUT_WEBAPI = -268431356;
    public static final int TOGGLE_CHAR = -268435444;
    public static final int TOGGLE_INPUT_CANCEL = -268435418;
    public static final int TOGGLE_REVERSE_CHAR = -268435455;
    public static final int TOUCH_OTHER_KEY = -268435424;
    public static final int UNDEFINED = 0;
    public static final int UNDO = -268435429;
    public static final int UPDATE_CANDIDATE = -268435431;
    public static final int VOICE_INPUT = -268435419;
    public char[] chars;
    public int code;
    public KeyEvent keyEvent;
    public int mode;
    public HashMap<?, ?> replaceTable;
    public String string;
    public String[] toggleTable;
    public WnnWord word;

    public static final class Mode {
        public static final int DEFAULT = 0;
        public static final int DIRECT = 1;
        public static final int NO_LV1_CONV = 2;
        public static final int NO_LV2_CONV = 3;
    }

    public IWnnImeEvent(int code) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
    }

    public IWnnImeEvent(int code, int mode) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        this.mode = mode;
    }

    public IWnnImeEvent(int code, char c) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        this.chars = new char[1];
        this.chars[0] = c;
    }

    public IWnnImeEvent(int code, String[] toggleTable) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        if (toggleTable != null) {
            this.toggleTable = (String[]) toggleTable.clone();
        }
    }

    public IWnnImeEvent(int code, HashMap<?, ?> replaceTable) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        if (replaceTable != null) {
            this.replaceTable = (HashMap) replaceTable.clone();
        }
    }

    public IWnnImeEvent(KeyEvent ev) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        if (ev != null) {
            if (ev.getAction() != 1) {
                this.code = INPUT_KEY;
            } else {
                this.code = KEYUP;
            }
            this.keyEvent = ev;
        }
    }

    public IWnnImeEvent(int code, KeyEvent ev) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        this.keyEvent = ev;
    }

    public IWnnImeEvent(int code, WnnWord word) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        this.word = word;
    }

    public IWnnImeEvent(int code, String string) {
        this.code = 0;
        this.mode = 0;
        this.chars = null;
        this.keyEvent = null;
        this.toggleTable = null;
        this.replaceTable = null;
        this.word = null;
        this.string = null;
        this.code = code;
        this.string = string;
    }
}
