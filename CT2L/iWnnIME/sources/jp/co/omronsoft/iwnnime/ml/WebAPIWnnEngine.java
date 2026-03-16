package jp.co.omronsoft.iwnnime.ml;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;

public class WebAPIWnnEngine {
    private static final int DELAY_TIMEOUT = 5000;
    private static final int MSG_TIMEOUT = 0;
    private static final int RESULT_ERROR = 1;
    private static final int RESULT_NO_RESPONSE = 2;
    private static final int RESULT_OK = 0;
    public static final String WEBAPI_ACTION_CODE = "jp.co.omronsoft.iwnnime.ml.GET_CANDIDATES_FROM_PLURALITY";
    public static final int WEBAPI_CANDIDATE_MAX = 500;
    private static final String WEBAPI_YOMI_KEYCODE = "yomi_key";
    private ArrayList<WnnWord> mCandidateList;
    private int mCurrentIndex = 0;
    private String mSendingYomi = null;
    private Set<String> mWebApiClassName = null;
    private HashMap<String, Integer> mResultHash = new HashMap<>();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    WebAPIWnnEngine.this.onDoneGettingCandidates();
                    IWnnIME wnn = IWnnIME.getCurrentIme();
                    if (wnn != null) {
                        wnn.onEvent(new IWnnImeEvent(IWnnImeEvent.TIMEOUT_WEBAPI));
                    }
                    break;
            }
        }
    };

    public WebAPIWnnEngine() {
        init();
    }

    private void sendGetCandidateIntent(String input) {
        clearCandidates();
        this.mSendingYomi = input;
        if (this.mWebApiClassName != null && !this.mWebApiClassName.isEmpty()) {
            this.mResultHash.clear();
            IWnnIME wnn = IWnnIME.getCurrentIme();
            if (wnn != null) {
                Intent intent = new Intent(WEBAPI_ACTION_CODE);
                intent.putExtra(WEBAPI_YOMI_KEYCODE, input);
                for (String className : this.mWebApiClassName) {
                    intent.setComponent(new ComponentName(className.substring(0, className.lastIndexOf(46)), className));
                    wnn.sendBroadcast(intent);
                    this.mResultHash.put(className, 2);
                }
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 5000L);
            }
        }
    }

    private WnnWord getCandidate(int index) {
        if (index >= this.mCandidateList.size()) {
            return null;
        }
        WnnWord ret = this.mCandidateList.get(index);
        return ret;
    }

    public void setCandidates(String yomi, String[] candidates, short[] hinshi) {
        if (candidates != null) {
            int size = (hinshi == null || candidates.length <= hinshi.length) ? candidates.length : hinshi.length;
            for (int i = 0; i < size; i++) {
                if (hinshi != null) {
                    this.mCandidateList.add(new WnnWord(0, candidates[i], yomi, 1024, hinshi[i]));
                } else {
                    this.mCandidateList.add(new WnnWord(0, candidates[i], yomi, 1024, 0));
                }
            }
        }
    }

    public String getSendingYomi() {
        return this.mSendingYomi;
    }

    public void onDoneGettingCandidates() {
        this.mSendingYomi = null;
        this.mHandler.removeMessages(0);
    }

    public void clearCandidates() {
        this.mCurrentIndex = 0;
        this.mCandidateList.clear();
        onDoneGettingCandidates();
    }

    public void init() {
        this.mCandidateList = new ArrayList<>();
        clearCandidates();
    }

    public void start(ComposingText text) {
        String input = getSearchString(text);
        if (input != null) {
            sendGetCandidateIntent(input);
        }
    }

    public WnnWord getNextCandidate() {
        WnnWord word = getCandidate(this.mCurrentIndex);
        if (word != null) {
            this.mCurrentIndex++;
        }
        return word;
    }

    public void setPreferences(SharedPreferences pref) {
        this.mWebApiClassName = pref.getStringSet(ControlPanelPrefFragment.WEBAPI_KEY, null);
    }

    public void getAgain(ComposingText text) {
        String input = getSearchString(text);
        if (input != null) {
            sendGetAgainCandidateIntent(input);
        }
    }

    private void sendGetAgainCandidateIntent(String input) {
        IWnnIME wnn;
        this.mCurrentIndex = 0;
        onDoneGettingCandidates();
        this.mSendingYomi = input;
        if (this.mWebApiClassName != null && !this.mWebApiClassName.isEmpty() && (wnn = IWnnIME.getCurrentIme()) != null) {
            Intent intent = new Intent(WEBAPI_ACTION_CODE);
            intent.putExtra(WEBAPI_YOMI_KEYCODE, input);
            for (String className : this.mWebApiClassName) {
                Integer result = this.mResultHash.get(className);
                if (result != null && result.intValue() != 0) {
                    intent.setComponent(new ComponentName(className.substring(0, className.lastIndexOf(46)), className));
                    wnn.sendBroadcast(intent);
                    this.mResultHash.put(className, 2);
                }
            }
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 5000L);
        }
    }

    public String getSearchString(ComposingText text) {
        String input = null;
        if (text != null && (input = text.toString(1)) != null) {
            IWnnIME wnn = IWnnIME.getCurrentIme();
            if (wnn == null) {
                return null;
            }
            boolean isConverting = wnn.getCurrentEngine().isConverting();
            if (isConverting) {
                StrSegment strseg = text.getStrSegment(2, 0);
                if (strseg != null) {
                    input = input.substring(strseg.from, strseg.to + 1);
                }
            } else {
                input = input.substring(0, text.mCursor[1]);
            }
        }
        return input;
    }

    public void setWebApiResult(String packageName, boolean success) {
        if (success) {
            this.mResultHash.put(packageName, 0);
        } else {
            this.mResultHash.put(packageName, 1);
        }
    }

    public boolean isWebApiAllReceived() {
        return !this.mResultHash.containsValue(2);
    }

    public boolean isWebApiSuccessReceived() {
        return this.mResultHash.containsValue(0);
    }

    public boolean isWebApiAllSuccessReceived() {
        return (this.mResultHash.containsValue(2) || this.mResultHash.containsValue(1)) ? false : true;
    }
}
