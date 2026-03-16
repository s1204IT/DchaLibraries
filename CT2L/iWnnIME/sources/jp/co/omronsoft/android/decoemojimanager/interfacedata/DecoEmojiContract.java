package jp.co.omronsoft.android.decoemojimanager.interfacedata;

import android.net.Uri;
import com.android.common.speech.LoggingEvents;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class DecoEmojiContract {
    public static final String DATA_TYPE_CATEGORY_LIST = "categorylist";
    public static final int KIND_NOTSQ = 3;
    public static final int KIND_PICTURE = 4;
    public static final int KIND_SQ = 2;
    public static final int KIND_SQ20 = 1;
    public static final int LAYER_CATEGORY = 1;
    public static final int LAYER_PREFIX = 0;
    public static final String QUERY_PARAM_DISTINCT = "distinct";
    public static final String DATA_TYPE_DECOINFO_LIST = "decoinfolist";
    public static final Uri CONTENT_DECOINFOLIST_URI = Uri.parse("content://" + AUTHORITY() + "/" + DATA_TYPE_DECOINFO_LIST);
    public static final String DATA_TYPE_DECOINFO_COUNT = "decoinfocount";
    public static final Uri CONTENT_DECOINFOLIST_COUNT_URI = Uri.parse("content://" + AUTHORITY() + "/" + DATA_TYPE_DECOINFO_COUNT);
    public static final String DATA_TYPE_DECODIC_LIST = "decodiclist";
    public static final Uri CONTENT_DECODICLIST_URI = Uri.parse("content://" + AUTHORITY() + "/" + DATA_TYPE_DECODIC_LIST);
    public static final String DATA_TYPE_DECODIC_COUNT = "decodiccount";
    public static final Uri CONTENT_DECODICLIST_COUNT_URI = Uri.parse("content://" + AUTHORITY() + "/" + DATA_TYPE_DECODIC_COUNT);

    public interface DecoEmojiDicColumns {
        public static final String DECOEMOJI_DIC_COUNT = "decoemoji_dic_count";
        public static final String DECOEMOJI_ID = "decoemoji_id";
        public static final String DECOEMOJI_NAME = "decoemoji_name";
        public static final String DECOEMOJI_NOTE = "decoemoji_note";
        public static final String DECOEMOJI_PART = "decoemoji_part";
        public static final String FILE_LASTMODIFIED = "file_lastmodified";
        public static final String TIMESTAMP = "timestamp";
        public static final String URI = "uri";
    }

    public interface DecoEmojiInfoColumns {
        public static final String CATEGORY_ID = "category_id";
        public static final String CATEGORY_NAME_ENG = "category_name_eng";
        public static final String CATEGORY_NAME_JPN = "category_name_jpn";
        public static final String CATEGORY_PRESET_ID = "category_preset_id";
        public static final String DECOEMOJI_ID = "decoemoji_id";
        public static final String DECOEMOJI_INFO_COUNT = "decoemoji_info_count";
        public static final String DECOME_POP_FLAG = "decome_pop_flag";
        public static final String DIRECTORY_ID = "directory_id";
        public static final String DIRECTORY_NAME = "directory_name";
        public static final String FILE_LASTMODIFIED = "file_lastmodified";
        public static final String FILE_SIZE = "file_size";
        public static final String HEIGHT = "height";
        public static final String HISTORY_CNT = "history_cnt";
        public static final String KIND = "kind";
        public static final String LAST_USE_CNT = "last_use_cnt";
        public static final String TAGS = "tags";
        public static final String TIMESTAMP = "timestamp";
        public static final String URI = "uri";
        public static final String WIDTH = "width";
    }

    public static String AUTHORITY() {
        return DecoEmojiContract.class.getPackage().getName().replace("interfacedata", "provider");
    }

    public static String makeStringEmojiKind(int emojiType) {
        String rtnStr = new String(LoggingEvents.EXTRA_CALLING_APP_NAME);
        if ((((byte) emojiType) & 1) == 1) {
            rtnStr = String.valueOf(rtnStr) + iWnnEngine.DECO_OPERATION_SEPARATOR + String.valueOf(1);
        }
        if ((((byte) emojiType) & 2) == 2) {
            rtnStr = String.valueOf(rtnStr) + iWnnEngine.DECO_OPERATION_SEPARATOR + String.valueOf(2);
        }
        if ((((byte) emojiType) & 4) == 4) {
            rtnStr = String.valueOf(rtnStr) + iWnnEngine.DECO_OPERATION_SEPARATOR + String.valueOf(3);
        }
        if ((((byte) emojiType) & 8) == 8) {
            rtnStr = String.valueOf(rtnStr) + iWnnEngine.DECO_OPERATION_SEPARATOR + String.valueOf(4);
        }
        if (rtnStr.length() == 0) {
            return String.valueOf(1);
        }
        if (rtnStr.substring(0, 1).equalsIgnoreCase(iWnnEngine.DECO_OPERATION_SEPARATOR)) {
            return rtnStr.substring(1);
        }
        return rtnStr;
    }

    public static int convertEmojiType(int emojiKind) {
        if (emojiKind == 1) {
            return 1;
        }
        if (emojiKind == 2) {
            return 2;
        }
        if (emojiKind == 3) {
            return 4;
        }
        if (emojiKind != 4) {
            return 0;
        }
        return 8;
    }
}
