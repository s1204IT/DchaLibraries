package jp.co.omronsoft.iwnnime.ml;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import jp.co.omronsoft.iwnnime.ml.IEngineService;

public class IWnnServiceConnector {
    public static final int BREAK_SEQUENCE = 2;
    private static final boolean DEBUG = false;
    public static final int DICTIONARY_TYPE_LEARNING = 1;
    public static final int DICTIONARY_TYPE_USER = 0;
    public static final int EISUKANA = 1;
    public static final int ERR_NO_DICTIONARY = -126;
    public static final int INIT = 1;
    private static final String IWNNENGINESERVICE_CLASSNAME = "jp.co.omronsoft.iwnnime.ml.IWnnEngineService";
    private static final String IWNNENGINESERVICE_PACKAGENAME = "jp.co.omronsoft.iwnnime.ml";
    public static final int KAOMOJI = 2;
    public static final int LEARNDIC = 11;
    public static final int LEXICAL_NOUN = 0;
    public static final int LEXICAL_NOUN_NO_CONJ = 2;
    public static final int LEXICAL_PERSONS_NAME = 1;
    public static final int LEXICAL_SYMBOL = 3;
    public static final int NORMAL = 0;
    public static final int ORDER_FREQUENCY = 0;
    public static final int ORDER_READING = 1;
    public static final int ORDER_REGISTRATION = 2;
    public static final int PSEUDO_TYPE_HANKATA = 4;
    public static final int PSEUDO_TYPE_HAN_DATE_DD = 24;
    public static final int PSEUDO_TYPE_HAN_DATE_MD = 25;
    public static final int PSEUDO_TYPE_HAN_DATE_MDD = 26;
    public static final int PSEUDO_TYPE_HAN_DATE_MDD_SYM = 30;
    public static final int PSEUDO_TYPE_HAN_DATE_MD_SYM = 29;
    public static final int PSEUDO_TYPE_HAN_DATE_MM = 23;
    public static final int PSEUDO_TYPE_HAN_DATE_MMD = 27;
    public static final int PSEUDO_TYPE_HAN_DATE_MMDD = 28;
    public static final int PSEUDO_TYPE_HAN_DATE_MMDD_SYM = 32;
    public static final int PSEUDO_TYPE_HAN_DATE_MMD_SYM = 31;
    public static final int PSEUDO_TYPE_HAN_DATE_YYYY = 22;
    public static final int PSEUDO_TYPE_HAN_EIJI_CAP = 5;
    public static final int PSEUDO_TYPE_HAN_EIJI_LOWER = 9;
    public static final int PSEUDO_TYPE_HAN_EIJI_UPPER = 7;
    public static final int PSEUDO_TYPE_HAN_SUUJI = 11;
    public static final int PSEUDO_TYPE_HAN_SUUJI_COMMA = 13;
    public static final int PSEUDO_TYPE_HAN_TIME_HH = 14;
    public static final int PSEUDO_TYPE_HAN_TIME_HHM = 18;
    public static final int PSEUDO_TYPE_HAN_TIME_HHMM = 19;
    public static final int PSEUDO_TYPE_HAN_TIME_HHMM_SYM = 21;
    public static final int PSEUDO_TYPE_HAN_TIME_HM = 16;
    public static final int PSEUDO_TYPE_HAN_TIME_HMM = 17;
    public static final int PSEUDO_TYPE_HAN_TIME_HMM_SYM = 20;
    public static final int PSEUDO_TYPE_HAN_TIME_MM = 15;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_INIT = 242;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_MM = 245;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_MMDD = 247;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_MMDD_SYM = 246;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_MMDD_SYM_WEEK = 248;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_MMDD_WEEK = 249;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_NENGO = 244;
    public static final int PSEUDO_TYPE_HAN_YOMI2DATE_YYYY = 243;
    public static final int PSEUDO_TYPE_HAN_YOMI2TIME_HHMM = 252;
    public static final int PSEUDO_TYPE_HAN_YOMI2TIME_HHMM_12H = 253;
    public static final int PSEUDO_TYPE_HAN_YOMI2TIME_HHMM_SYM = 250;
    public static final int PSEUDO_TYPE_HAN_YOMI2TIME_HHMM_SYM_AMPM = 251;
    public static final int PSEUDO_TYPE_HIRAGANA = 2;
    public static final int PSEUDO_TYPE_KATAKANA = 3;
    public static final int PSEUDO_TYPE_NONE = 0;
    public static final int PSEUDO_TYPE_ZEN_EIJI_CAP = 6;
    public static final int PSEUDO_TYPE_ZEN_EIJI_LOWER = 10;
    public static final int PSEUDO_TYPE_ZEN_EIJI_UPPER = 8;
    public static final int PSEUDO_TYPE_ZEN_SUUJI = 12;
    public static final int RELATIONAL_LEARNING_OFF = 0;
    public static final int RELATIONAL_LEARNING_ON = 1;
    public static final int RETCODE_ANOTHER_APPLICATION_CONNECTED = -2;
    public static final int RETCODE_INVALID_STATE = -127;
    public static final int RETCODE_NG = -1;
    public static final int RETCODE_OK = 0;
    public static final int SEARCH_CONNECTION = 2;
    public static final int SEARCH_ORIGINAL_PULL_FRONT = 1;
    public static final int SEARCH_ORIGINAL_PULL_PERFECTION = 0;
    public static final int SEARCH_REVERSE_PULL_FRONT = 4;
    public static final int SEARCH_REVERSE_PULL_PERFECTION = 3;
    public static final int SERVICE_MAX_CONNECTED_COUNT = 3;
    private static final String SERVICE_PASSWORD = "";
    private static final String TAG = "iWnn";
    public static final int USERDIC = 10;
    private OnConnectListener mConnectListener;
    private boolean mIsBind;
    private IEngineService mServiceIf = null;
    private Context mContext = null;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IWnnServiceConnector.this.mServiceIf = IEngineService.Stub.asInterface(service);
            IWnnServiceConnector.this.mIsBind = true;
            IWnnServiceConnector.this.init(1);
            if (IWnnServiceConnector.this.mConnectListener != null) {
                IWnnServiceConnector.this.mConnectListener.onConnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            IWnnServiceConnector.this.mServiceIf = null;
            IWnnServiceConnector.this.mIsBind = IWnnServiceConnector.DEBUG;
            if (IWnnServiceConnector.this.mConnectListener != null) {
                IWnnServiceConnector.this.mConnectListener.onDisconnect();
            }
        }
    };

    public interface OnConnectListener {
        void onConnect();

        void onDisconnect();
    }

    public int connect(Context context, OnConnectListener listener) {
        if (context == null || listener == null) {
            return -1;
        }
        if (!this.mIsBind) {
            ActivityManager manager = (ActivityManager) context.getSystemService("activity");
            if (manager == null) {
                return -1;
            }
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (service != null && IWNNENGINESERVICE_CLASSNAME.equals(service.service.getClassName()) && service.clientCount >= 3) {
                    return -2;
                }
            }
            Intent intent = new Intent();
            intent.setClassName(IWNNENGINESERVICE_PACKAGENAME, IWNNENGINESERVICE_CLASSNAME);
            boolean success = context.bindService(intent, this.mServiceConn, 1);
            if (success) {
                this.mConnectListener = listener;
                this.mContext = context;
            }
            return success ? 0 : -1;
        }
        return RETCODE_INVALID_STATE;
    }

    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }

    public int init(int initLevel) {
        try {
            if (this.mServiceIf != null) {
                this.mServiceIf.init(this.mContext.getPackageName(), "", initLevel);
                return 0;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "init", e);
        }
        return -1;
    }

    public int predict(String stroke, int minLen, int maxLen) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.predict(this.mContext.getPackageName(), stroke, minLen, maxLen);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "predict", e);
        }
        return -1;
    }

    public int setEnableConsecutivePhraseLevelConversion(boolean enable) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.setEnableConsecutivePhraseLevelConversion(this.mContext.getPackageName(), enable);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setEnableConsecutivePhraseLevelConversion", e);
        }
        return -1;
    }

    public Bundle getNextCandidate(int numberOfCandidates) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.getNextCandidate(this.mContext.getPackageName(), numberOfCandidates);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "getNextCandidate", e);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("result", -1);
        return bundle;
    }

    public Bundle getNextCandidateWithAnnotation(int numberOfCandidates) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.getNextCandidateWithAnnotation(this.mContext.getPackageName(), numberOfCandidates);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "getNextCandidateWithAnnotation", e);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("result", -1);
        return bundle;
    }

    public Bundle getNextCandidateWithAnnotation2(int numberOfCandidates, int emojitype) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.getNextCandidateWithAnnotation2(this.mContext.getPackageName(), numberOfCandidates, emojitype);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "getNextCandidateWithAnnotationEmoji", e);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("result", -1);
        return bundle;
    }

    public int learnCandidate(int index) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.learnCandidate(this.mContext.getPackageName(), index) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "learnCandidate", e);
        }
        return -1;
    }

    public Bundle convert(String stroke, int divide) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.convert(this.mContext.getPackageName(), stroke, divide);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "convert", e);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("result", -1);
        return bundle;
    }

    public Bundle convertWithAnnotation(String stroke, int divide) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.convertWithAnnotation(this.mContext.getPackageName(), stroke, divide);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "convertWithAnnotation", e);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("result", -1);
        return bundle;
    }

    public Bundle convertWithAnnotation2(String stroke, int divide, int emojitype) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.convertWithAnnotation2(this.mContext.getPackageName(), stroke, divide, emojitype);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "convertWithAnnotation2", e);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("result", -1);
        return bundle;
    }

    public int addWord(String candidate, String stroke) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.addWord(this.mContext.getPackageName(), candidate, stroke);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "addWord", e);
        }
        return -1;
    }

    public int addWordDetail(String candidate, String stroke, int hinsi, int type, int relation) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.addWordDetail(this.mContext.getPackageName(), candidate, stroke, hinsi, type, relation);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "addWordDetail", e);
        }
        return -1;
    }

    public int searchWords(String stroke) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.searchWords(this.mContext.getPackageName(), stroke);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "searchWords", e);
        }
        return -1;
    }

    public int searchWordsDetail(String stroke, int method, int order) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.searchWordsDetail(this.mContext.getPackageName(), stroke, method, order);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "searchWordsDetail", e);
        }
        return -1;
    }

    public int deleteWord(String candidate, String stroke) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.deleteWord(this.mContext.getPackageName(), candidate, stroke) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "deleteWord", e);
        }
        return -1;
    }

    public int writeoutDictionary() {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.writeoutDictionary(this.mContext.getPackageName()) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "writeoutDictionary", e);
        }
        return -1;
    }

    public int initializeDictionary() {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.initializeDictionary(this.mContext.getPackageName()) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "initializeDictionary", e);
        }
        return -1;
    }

    public int setUserDictionary() {
        try {
            if (this.mServiceIf != null) {
                this.mServiceIf.setUserDictionary(this.mContext.getPackageName());
                return 0;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setUserDictionary", e);
        }
        return -1;
    }

    public int setLearnDictionary() {
        try {
            if (this.mServiceIf != null) {
                this.mServiceIf.setLearnDictionary(this.mContext.getPackageName());
                return 0;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setLearnDictionary", e);
        }
        return -1;
    }

    public int setNormalDictionary() {
        try {
            if (this.mServiceIf != null) {
                this.mServiceIf.setNormalDictionary(this.mContext.getPackageName());
                return 0;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setNormalDictionary", e);
        }
        return -1;
    }

    public int learnCandidateNoStore(int index) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.learnCandidateNoStore(this.mContext.getPackageName(), index) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "learnCandidateNoStore", e);
        }
        return -1;
    }

    public int learnCandidateNoConnect(int index) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.learnCandidateNoConnect(this.mContext.getPackageName(), index) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "learnCandidateNoConnect", e);
        }
        return -1;
    }

    public int learnWord(String candidate, String stroke) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.learnWord(this.mContext.getPackageName(), candidate, stroke) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "learnWord", e);
        }
        return -1;
    }

    public int learnWordNoStore(String candidate, String stroke) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.learnWordNoStore(this.mContext.getPackageName(), candidate, stroke) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "learnWordNoStore", e);
        }
        return -1;
    }

    public int learnWordNoConnect(String candidate, String stroke) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.learnWordNoConnect(this.mContext.getPackageName(), candidate, stroke) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "learnWordNoConnect", e);
        }
        return -1;
    }

    public int setDictionary(String configurationFile, int language, int dictionary, boolean flexibleSearch, boolean tenKeyType, boolean emojiFilter, boolean emailFilter, boolean convertCandidates, boolean learnNumber) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.setDictionary(this.mContext.getPackageName(), configurationFile, language, dictionary, flexibleSearch, tenKeyType, emojiFilter, emailFilter, convertCandidates, learnNumber) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setDictionary", e);
        }
        return -1;
    }

    public int setDictionaryDecoratedPict(String configurationFile, int language, int dictionary, boolean flexibleSearch, boolean tenKeyType, boolean emojiFilter, boolean decoemojiFilter, boolean emailFilter, boolean convertCandidates, boolean learnNumber) {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.setDictionaryDecoratedPict(this.mContext.getPackageName(), configurationFile, language, dictionary, flexibleSearch, tenKeyType, emojiFilter, decoemojiFilter, emailFilter, convertCandidates, learnNumber) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setDictionaryDecoratedPict", e);
        }
        return -1;
    }

    public boolean isGijiDic(int index) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.isGijiDic(this.mContext.getPackageName(), index);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "isGijiDic", e);
        }
        return DEBUG;
    }

    public boolean setGijiFilter(int[] type) {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.setGijiFilter(this.mContext.getPackageName(), type);
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "setGijiFilter", e);
        }
        return DEBUG;
    }

    public int undo() {
        try {
            if (this.mServiceIf != null) {
                int ret = this.mServiceIf.undo(this.mContext.getPackageName()) ? 0 : -1;
                if (ret < 0) {
                    return this.mServiceIf.getErrorCode(this.mContext.getPackageName());
                }
                return ret;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "undo", e);
        }
        return -1;
    }

    public int startInput() {
        try {
            if (this.mServiceIf != null) {
                this.mServiceIf.startInput(this.mContext.getPackageName());
                return 0;
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "startInput", e);
        }
        return -1;
    }

    public int getStatus() {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.getStatus(this.mContext.getPackageName());
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "getStatus", e);
        }
        return 0;
    }

    public int getDictionaryType() {
        try {
            if (this.mServiceIf != null) {
                return this.mServiceIf.getDictionaryType(this.mContext.getPackageName());
            }
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "getDictionaryType", e);
        }
        return 0;
    }

    public int disconnect() {
        if (this.mIsBind && this.mContext != null && this.mServiceConn != null) {
            try {
                if (this.mServiceIf != null) {
                    this.mServiceIf.disconnect(this.mContext.getPackageName());
                }
            } catch (Exception e) {
                Log.e("IWnnServiceConnector", "disconnect", e);
            }
            this.mContext.unbindService(this.mServiceConn);
            this.mIsBind = DEBUG;
            this.mServiceIf = null;
            return 0;
        }
        return -1;
    }

    public boolean isAlive() {
        if (this.mServiceIf == null || !this.mServiceIf.asBinder().isBinderAlive() || this.mContext == null) {
            return DEBUG;
        }
        try {
            return this.mServiceIf.isAlive(this.mContext.getPackageName());
        } catch (Exception e) {
            Log.e("IWnnServiceConnector", "isAlive", e);
            return DEBUG;
        }
    }
}
