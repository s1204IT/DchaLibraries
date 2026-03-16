package jp.co.omronsoft.iwnnime.ml;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.HashSet;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.IEngineService;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiOperationQueue;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiUtil;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnCore;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class IWnnEngineService extends Service {
    private static final int DELAY_MS_DELETE_DECOEMOJI_LEARNING_DICTIONARY = 0;
    private static final int DELAY_MS_UPDATE_DECOEMOJI_DICTIONARY = 0;
    private static final int ENGINE_INITIALIZE = 2;
    private static final int ENGINE_INITIALIZE_REGISTRATION_DICTIONARY = 6;
    private static final int ENGINE_PREFIXATION = 4;
    private static final int ENGINE_PREFIXATION_AND_SEARCH = 5;
    private static final int ENGINE_SEARCH = 3;
    private static final int ENGINE_SEARCH_REGISTRATION_DICTIONARY = 7;
    private static final int ENGINE_UNCONNECTION = 0;
    private static final int ENGINE_UNSETTING = 1;
    private static final int ERR_COMMON = -1;
    private static final int ERR_INVALID_STATE = -127;
    private static final int ERR_NO_DICTIONARY = -126;
    private static final int FUNCTION_ADDWORD = 13;
    private static final int FUNCTION_CONNECT = 0;
    private static final int FUNCTION_CONVERT = 9;
    private static final int FUNCTION_DELETEWORD = 15;
    private static final int FUNCTION_DISCONNECT = 1;
    private static final int FUNCTION_GETNEXTCANDIDATE = 10;
    private static final int FUNCTION_INITIALIZEDICTIONARY = 17;
    private static final int FUNCTION_INIT_BREAK = 3;
    private static final int FUNCTION_INIT_INIT = 2;
    private static final int FUNCTION_IS_GIJI_DIC = 20;
    private static final int FUNCTION_LEARNCANDIDATE = 11;
    private static final int FUNCTION_LEARNWORD = 12;
    private static final int FUNCTION_PREDICT = 8;
    private static final int FUNCTION_SEARCHWORDS = 14;
    private static final int FUNCTION_SETDICTIONARY = 4;
    private static final int FUNCTION_SETLEARNDICTIONARY = 7;
    private static final int FUNCTION_SETNORMALDICTIONARY = 5;
    private static final int FUNCTION_SETUSERDICTIONARY = 6;
    private static final int FUNCTION_STARTINPUT = 19;
    private static final int FUNCTION_UNDO = 18;
    private static final int FUNCTION_WRITEOUTDICTIONARY = 16;
    private static final int MSG_DELETE_DECOEMOJI_LEARNING_DICTIONARY = 2;
    private static final int MSG_UPDATE_DECOEMOJI_DICTIONARY = 1;
    private static final int PASSWORD_MAX_LENGTH = 16;
    private static final int RESULTLIST_ARRAY = 2;
    private static final String TAG = "iWnn";
    private boolean mIsBind;
    private static final boolean DEBUG = false;
    private static final boolean[][] ENGINE_STATUS_CHECK_TABLE = {new boolean[]{true, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG}, new boolean[]{DEBUG, true, true, true, true, true, true, true}, new boolean[]{DEBUG, true, true, true, true, true, true, true}, new boolean[]{DEBUG, DEBUG, true, true, true, true, DEBUG, DEBUG}, new boolean[]{DEBUG, true, true, true, true, true, true, true}, new boolean[]{DEBUG, DEBUG, true, true, true, true, true, true}, new boolean[]{DEBUG, DEBUG, true, true, true, true, true, true}, new boolean[]{DEBUG, DEBUG, true, true, true, true, true, true}, new boolean[]{DEBUG, DEBUG, true, true, true, true, DEBUG, DEBUG}, new boolean[]{DEBUG, DEBUG, true, true, true, true, DEBUG, DEBUG}, new boolean[]{DEBUG, DEBUG, DEBUG, true, DEBUG, true, DEBUG, true}, new boolean[]{DEBUG, DEBUG, DEBUG, true, DEBUG, true, DEBUG, DEBUG}, new boolean[]{DEBUG, DEBUG, true, true, true, true, DEBUG, DEBUG}, new boolean[]{DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, true, true}, new boolean[]{DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, true, true}, new boolean[]{DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, true, true}, new boolean[]{DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, true, true}, new boolean[]{DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, DEBUG, true, true}, new boolean[]{DEBUG, DEBUG, DEBUG, true, true, true, DEBUG, DEBUG}, new boolean[]{DEBUG, DEBUG, true, true, true, true, true, true}, new boolean[]{DEBUG, DEBUG, DEBUG, true, DEBUG, true, DEBUG, DEBUG}};
    private static ArrayList<EngineService> mEngineServiceArray = new ArrayList<>();
    private static IWnnEngineService mCurrentService = null;
    public Context mContext = null;
    private String mFilesDirPath = null;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) throws Throwable {
            String packageName = msg.getData().getString("packageName");
            boolean result = IWnnEngineService.DEBUG;
            int delay = -1;
            switch (msg.what) {
                case 1:
                    result = DecoEmojiOperationQueue.getInstance().executeOperation(packageName, IWnnEngineService.this.mContext);
                    delay = 0;
                    break;
                case 2:
                    iWnnEngine engine = iWnnEngine.getEngineForService(packageName);
                    if (engine != null) {
                        result = engine.executeOperation(IWnnEngineService.mCurrentService);
                        delay = 0;
                    }
                    break;
            }
            if (result) {
                Message message = IWnnEngineService.this.mHandler.obtainMessage(msg.what);
                Bundle bundle = new Bundle();
                bundle.putString("packageName", packageName);
                message.setData(bundle);
                IWnnEngineService.this.mHandler.sendMessageDelayed(message, delay);
            }
        }
    };
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private IEngineService.Stub mEngineService = new IEngineService.Stub() {
        @Override
        public void init(String packageName, String password, int initLevel) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    if (initLevel == 1) {
                        long oldtime = 0;
                        String removecomoponentName = LoggingEvents.EXTRA_CALLING_APP_NAME;
                        for (int i = 0; i < IWnnEngineService.mEngineServiceArray.size(); i++) {
                            if (((EngineService) IWnnEngineService.mEngineServiceArray.get(i)).mUsedTime < oldtime || i == 0) {
                                oldtime = ((EngineService) IWnnEngineService.mEngineServiceArray.get(i)).mUsedTime;
                                removecomoponentName = ((EngineService) IWnnEngineService.mEngineServiceArray.get(i)).mConnectedPackage;
                            }
                        }
                        IWnnEngineService.this.clearServiceInfo(removecomoponentName);
                        engineService = IWnnEngineService.this.getEngineService(packageName);
                        if (engineService == null) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                switch (initLevel) {
                    case 1:
                        if (password.length() > 16) {
                            engineService.mPassword = password.substring(0, 16);
                        } else {
                            engineService.mPassword = password;
                        }
                        if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[2][engineService.mEngineStatus]) {
                            IWnnEngineService.this.stopDecoEmojiUpdating();
                            engineService.mEngine.close();
                            engineService.mEngine.init(IWnnEngineService.this.mFilesDirPath);
                            engineService.mIsFlexibleCharsetInit = true;
                            engineService.mIsConverting = IWnnEngineService.DEBUG;
                            engineService.mEngineStatus = 1;
                            engineService.mCachedDicSet.clear();
                        }
                        break;
                    case 2:
                        if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[3][engineService.mEngineStatus]) {
                            engineService.mEngine.breakSequence();
                            engineService.mEngine.init(IWnnEngineService.this.mFilesDirPath);
                            engineService.mIsConverting = IWnnEngineService.DEBUG;
                            engineService.mEngineStatus = 2;
                        }
                        break;
                }
            }
        }

        @Override
        public int predict(String packageName, String stroke, int minLen, int maxLen) {
            int result;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[8][engineService.mEngineStatus]) {
                        ComposingText composingText = new ComposingText();
                        composingText.insertStrSegment(0, 1, new StrSegment(stroke));
                        engineService.mWnnWordArray.clear();
                        engineService.mIsConverting = IWnnEngineService.DEBUG;
                        result = engineService.mEngine.predict(composingText, minLen, maxLen);
                        if (engineService.mEngineStatus == 4) {
                            engineService.mEngineStatus = 5;
                        } else {
                            engineService.mEngineStatus = 3;
                        }
                    } else {
                        result = engineService.mEngineStatus == 1 ? -126 : -127;
                    }
                } else {
                    result = -127;
                }
            }
            return result;
        }

        @Override
        public int setEnableConsecutivePhraseLevelConversion(String packageName, boolean enable) {
            int result;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    result = -127;
                } else {
                    result = 0;
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[8][engineService.mEngineStatus]) {
                        engineService.mEngine.setEnableHeadConversion(enable);
                    } else if (engineService.mEngineStatus == 1) {
                        result = -126;
                    } else {
                        result = -127;
                    }
                }
            }
            return result;
        }

        @Override
        public Bundle getNextCandidate(String packageName, int numberOfCandidates) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[10][engineService.mEngineStatus]) {
                        if (numberOfCandidates <= 0) {
                            Bundle ret = new Bundle();
                            ret.putInt("result", -1);
                            return ret;
                        }
                        ArrayList<String>[] resultLists = new ArrayList[2];
                        int[] attributes = new int[numberOfCandidates];
                        for (int i = 0; i < 2; i++) {
                            resultLists[i] = new ArrayList<>();
                        }
                        for (int i2 = 0; i2 < numberOfCandidates; i2++) {
                            WnnWord word = engineService.mEngine.getNextCandidate();
                            if (word == null) {
                                break;
                            }
                            engineService.mWnnWordArray.add(word);
                            resultLists[0].add(word.candidate);
                            resultLists[1].add(word.stroke);
                            attributes[i2] = word.attribute;
                        }
                        Bundle candidateList = new Bundle();
                        int size = resultLists[0].size();
                        candidateList.putStringArray("candidate", (String[]) resultLists[0].toArray(new String[size]));
                        candidateList.putStringArray("stroke", (String[]) resultLists[1].toArray(new String[size]));
                        candidateList.putIntArray("attributes", attributes);
                        return candidateList;
                    }
                    Bundle ret2 = new Bundle();
                    if (engineService.mEngineStatus == 1) {
                        ret2.putInt("result", -126);
                    } else {
                        ret2.putInt("result", -127);
                    }
                    return ret2;
                }
                Bundle ret3 = new Bundle();
                ret3.putInt("result", -1);
                return ret3;
            }
        }

        @Override
        public Bundle getNextCandidateWithAnnotation(String packageName, int numberOfCandidates) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    Bundle ret = new Bundle();
                    ret.putInt("result", -1);
                    return ret;
                }
                Bundle ret2 = getNextCandidate(packageName, numberOfCandidates);
                String[] candidateList = ret2.getStringArray("candidate");
                if (candidateList != null && candidateList.length > 0) {
                    CharSequence[] decoText = new CharSequence[candidateList.length];
                    int[] decoTextStatus = new int[candidateList.length];
                    for (int i = 0; i < candidateList.length; i++) {
                        int[] status = new int[1];
                        decoText[i] = DecoEmojiUtil.getSpannedText(candidateList[i], status);
                        decoTextStatus[i] = status[0];
                    }
                    ret2.putCharSequenceArray("annotation_candidate", decoText);
                    ret2.putIntArray("annotation_result", decoTextStatus);
                }
                return ret2;
            }
        }

        @Override
        public Bundle getNextCandidateWithAnnotation2(String packageName, int numberOfCandidates, int emojitype) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    Bundle ret = new Bundle();
                    ret.putInt("result", -1);
                    return ret;
                }
                IWnnIME.setEmojiType(emojitype);
                return getNextCandidateWithAnnotation(packageName, numberOfCandidates);
            }
        }

        @Override
        public boolean learnCandidate(String packageName, int index) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[11][engineService.mEngineStatus]) {
                    if (engineService.mWnnWordArray.size() <= index || index < 0) {
                        return IWnnEngineService.DEBUG;
                    }
                    boolean result = learn(packageName, (WnnWord) engineService.mWnnWordArray.get(index), true);
                    engineService.mErrorCode = result ? 0 : -1;
                    return result;
                }
                if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return IWnnEngineService.DEBUG;
            }
        }

        @Override
        public Bundle convert(String packageName, String stroke, int divide) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[9][engineService.mEngineStatus]) {
                        if (stroke == null) {
                            Bundle ret = new Bundle();
                            ret.putInt("result", -1);
                            return ret;
                        }
                        ComposingText composingText = new ComposingText();
                        String[] strSpilit = stroke.split(LoggingEvents.EXTRA_CALLING_APP_NAME);
                        int length = stroke.split(LoggingEvents.EXTRA_CALLING_APP_NAME).length;
                        int length2 = length - 1;
                        for (int i = 0; i < length2; i++) {
                            composingText.insertStrSegment(0, 1, new StrSegment(strSpilit[i + 1]));
                        }
                        composingText.setCursor(1, divide);
                        int result = engineService.mEngine.convert(composingText);
                        Bundle bundle = new Bundle();
                        bundle.putInt("result", result);
                        if (result > 0) {
                            ArrayList<String> candidates = new ArrayList<>();
                            ArrayList<String> strokes = new ArrayList<>();
                            for (int i2 = 0; i2 < result; i2++) {
                                StrSegment candidate = composingText.getStrSegment(2, i2);
                                if (candidate != null) {
                                    candidates.add(candidate.string);
                                    strokes.add(composingText.toString(1, candidate.from, candidate.to));
                                }
                            }
                            bundle.putStringArray("candidate", (String[]) candidates.toArray(new String[result]));
                            bundle.putStringArray("stroke", (String[]) strokes.toArray(new String[result]));
                            engineService.mIsConverting = true;
                            engineService.mWnnWordArray.clear();
                            engineService.mSegment = 0;
                        }
                        if (engineService.mEngineStatus == 4) {
                            engineService.mEngineStatus = 5;
                        } else {
                            engineService.mEngineStatus = 3;
                        }
                        return bundle;
                    }
                    Bundle ret2 = new Bundle();
                    if (engineService.mEngineStatus == 1) {
                        ret2.putInt("result", -126);
                    } else {
                        ret2.putInt("result", -127);
                    }
                    return ret2;
                }
                Bundle ret3 = new Bundle();
                ret3.putInt("result", -1);
                return ret3;
            }
        }

        @Override
        public Bundle convertWithAnnotation(String packageName, String stroke, int divide) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    Bundle ret = new Bundle();
                    ret.putInt("result", -1);
                    return ret;
                }
                Bundle ret2 = convert(packageName, stroke, divide);
                String[] candidateList = ret2.getStringArray("candidate");
                if (candidateList != null && candidateList.length > 0) {
                    CharSequence[] decoText = new CharSequence[candidateList.length];
                    int[] decoTextStatus = new int[candidateList.length];
                    for (int i = 0; i < candidateList.length; i++) {
                        int[] status = new int[1];
                        decoText[i] = DecoEmojiUtil.getSpannedText(candidateList[i], status);
                        decoTextStatus[i] = status[0];
                    }
                    ret2.putCharSequenceArray("annotation_candidate", decoText);
                    ret2.putIntArray("annotation_result", decoTextStatus);
                }
                return ret2;
            }
        }

        @Override
        public Bundle convertWithAnnotation2(String packageName, String stroke, int divide, int emojitype) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    Bundle ret = new Bundle();
                    ret.putInt("result", -1);
                    return ret;
                }
                IWnnIME.setEmojiType(emojitype);
                return convertWithAnnotation(packageName, stroke, divide);
            }
        }

        @Override
        public int addWordDetail(String packageName, String candidate, String stroke, int hinsi, int type, int relation) {
            int result;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    result = -127;
                } else {
                    result = -1;
                    if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[13][engineService.mEngineStatus]) {
                        if (engineService.mEngineStatus == 1) {
                            result = -126;
                        } else {
                            result = -127;
                        }
                    } else {
                        WnnWord word = new WnnWord();
                        word.candidate = candidate;
                        word.stroke = stroke;
                        word.attribute |= 4096;
                        int dictionary = engineService.mEngine.getDictionary();
                        if (dictionary == 10) {
                            result = engineService.mEngine.addWord(word, hinsi, type, relation);
                        } else if (dictionary == 11) {
                            if (learn(packageName, word, true)) {
                                result = 0;
                            } else {
                                result = -1;
                            }
                        }
                        engineService.mEngineStatus = 6;
                    }
                }
            }
            return result;
        }

        @Override
        public int addWord(String packageName, String candidate, String stroke) {
            int result;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    result = -127;
                } else {
                    result = -1;
                    if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[13][engineService.mEngineStatus]) {
                        if (engineService.mEngineStatus == 1) {
                            result = -126;
                        } else {
                            result = -127;
                        }
                    } else {
                        WnnWord word = new WnnWord();
                        word.candidate = candidate;
                        word.stroke = stroke;
                        word.attribute |= 4096;
                        int dictionary = engineService.mEngine.getDictionary();
                        if (dictionary == 10) {
                            result = engineService.mEngine.addWord(word);
                        } else if (dictionary == 11) {
                            if (learn(packageName, word, true)) {
                                result = 0;
                            } else {
                                result = -1;
                            }
                        }
                        engineService.mEngineStatus = 6;
                    }
                }
            }
            return result;
        }

        @Override
        public int searchWordsDetail(String packageName, String stroke, int method, int order) {
            int iSearchWords = -127;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[14][engineService.mEngineStatus]) {
                        engineService.mWnnWordArray.clear();
                        engineService.mSegment = 0;
                        engineService.mEngineStatus = 7;
                        iSearchWords = engineService.mEngine.searchWords(stroke, method, order);
                    } else if (engineService.mEngineStatus == 1) {
                        iSearchWords = -126;
                    }
                }
            }
            return iSearchWords;
        }

        @Override
        public int searchWords(String packageName, String stroke) {
            int iSearchWords = -127;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[14][engineService.mEngineStatus]) {
                        engineService.mWnnWordArray.clear();
                        engineService.mSegment = 0;
                        engineService.mEngineStatus = 7;
                        iSearchWords = engineService.mEngine.searchWords(stroke);
                    } else if (engineService.mEngineStatus == 1) {
                        iSearchWords = -126;
                    }
                }
            }
            return iSearchWords;
        }

        @Override
        public boolean deleteWord(String packageName, String candidate, String stroke) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                boolean result = IWnnEngineService.DEBUG;
                if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[15][engineService.mEngineStatus]) {
                    if (engineService.mEngineStatus == 1) {
                        engineService.mErrorCode = -126;
                    } else {
                        engineService.mErrorCode = -127;
                    }
                    return IWnnEngineService.DEBUG;
                }
                WnnWord getWord = null;
                int i = 0;
                while (true) {
                    if (i < engineService.mWnnWordArray.size()) {
                        if (engineService.mWnnWordArray.get(i) != null && ((WnnWord) engineService.mWnnWordArray.get(i)).candidate.equals(candidate) && ((WnnWord) engineService.mWnnWordArray.get(i)).stroke.equals(stroke)) {
                            getWord = (WnnWord) engineService.mWnnWordArray.get(i);
                            break;
                        }
                        i++;
                    } else {
                        break;
                    }
                }
                if (getWord != null) {
                    result = engineService.mEngine.deleteWord(getWord);
                }
                engineService.mEngineStatus = 6;
                engineService.mErrorCode = result ? 0 : -1;
                return result;
            }
        }

        @Override
        public boolean writeoutDictionary(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[16][engineService.mEngineStatus]) {
                    boolean result = engineService.mEngine.writeoutDictionary(engineService.mEngine.getLanguage(), engineService.mEngine.getDictionary());
                    engineService.mErrorCode = result ? 0 : -1;
                    return result;
                }
                if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return IWnnEngineService.DEBUG;
            }
        }

        @Override
        public boolean initializeDictionary(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                boolean result = IWnnEngineService.DEBUG;
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[17][engineService.mEngineStatus]) {
                    int dictionary = engineService.mEngine.getDictionary();
                    if (dictionary == 10) {
                        result = engineService.mEngine.initializeUserDictionary(engineService.mEngine.getLanguage(), dictionary);
                    } else if (dictionary == 11) {
                        result = engineService.mEngine.initializeLearnDictionary(engineService.mEngine.getLanguage());
                    }
                    engineService.mEngineStatus = 6;
                    engineService.mErrorCode = result ? 0 : -1;
                } else if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return result;
            }
        }

        @Override
        public void setUserDictionary(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[6][engineService.mEngineStatus]) {
                        engineService.mEngine.setDictionary(engineService.mEngine.getLanguage(), 10, hashCode(), engineService.mConfigurationFile, engineService.mConnectedPackage, engineService.mPassword);
                        engineService.mEngineStatus = 6;
                    }
                }
            }
        }

        @Override
        public void setLearnDictionary(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[7][engineService.mEngineStatus]) {
                        engineService.mEngine.setDictionary(engineService.mEngine.getLanguage(), 11, hashCode(), engineService.mConfigurationFile, engineService.mConnectedPackage, engineService.mPassword);
                        engineService.mEngineStatus = 6;
                    }
                }
            }
        }

        @Override
        public void setNormalDictionary(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[5][engineService.mEngineStatus]) {
                        engineService.mEngine.setDictionary(engineService.mEngine.getLanguage(), 0, hashCode(), engineService.mConfigurationFile, engineService.mConnectedPackage, engineService.mPassword);
                        engineService.mEngineStatus = 2;
                    }
                }
            }
        }

        private boolean learn(String packageName, WnnWord word, boolean connected) {
            EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
            if (engineService != null) {
                boolean zLearn = engineService.mEngine.learn(word, connected);
                if (!engineService.mIsConverting) {
                    engineService.mEngineStatus = 4;
                    return zLearn;
                }
                boolean result = makeNextCandidateList(packageName);
                engineService.mEngineStatus = 5;
                return result;
            }
            return IWnnEngineService.DEBUG;
        }

        private boolean makeNextCandidateList(String packageName) {
            EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
            if (engineService == null) {
                return IWnnEngineService.DEBUG;
            }
            engineService.mWnnWordArray.clear();
            EngineService.access$1708(engineService);
            return engineService.mEngine.makeCandidateListOf(engineService.mSegment) == 1;
        }

        @Override
        public boolean learnCandidateNoStore(String packageName, int index) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[11][engineService.mEngineStatus]) {
                    if (engineService.mWnnWordArray.size() <= index || index < 0) {
                        return IWnnEngineService.DEBUG;
                    }
                    WnnWord word = (WnnWord) engineService.mWnnWordArray.get(index);
                    word.attribute |= 64;
                    boolean result = learn(packageName, word, true);
                    engineService.mErrorCode = result ? 0 : -1;
                    return result;
                }
                if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return IWnnEngineService.DEBUG;
            }
        }

        @Override
        public boolean learnCandidateNoConnect(String packageName, int index) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[11][engineService.mEngineStatus]) {
                    if (engineService.mWnnWordArray.size() <= index || index < 0) {
                        return IWnnEngineService.DEBUG;
                    }
                    WnnWord word = (WnnWord) engineService.mWnnWordArray.get(index);
                    boolean result = learn(packageName, word, IWnnEngineService.DEBUG);
                    engineService.mErrorCode = result ? 0 : -1;
                    return result;
                }
                if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return IWnnEngineService.DEBUG;
            }
        }

        @Override
        public boolean learnWord(String packageName, String candidate, String stroke) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[12][engineService.mEngineStatus]) {
                    if (engineService.mEngineStatus == 1) {
                        engineService.mErrorCode = -126;
                    } else {
                        engineService.mErrorCode = -127;
                    }
                    return IWnnEngineService.DEBUG;
                }
                WnnWord word = new WnnWord(0, candidate, stroke, 4096);
                boolean result = learn(packageName, word, true);
                engineService.mErrorCode = result ? 0 : -1;
                return result;
            }
        }

        @Override
        public boolean learnWordNoStore(String packageName, String candidate, String stroke) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[12][engineService.mEngineStatus]) {
                    if (engineService.mEngineStatus == 1) {
                        engineService.mErrorCode = -126;
                    } else {
                        engineService.mErrorCode = -127;
                    }
                    return IWnnEngineService.DEBUG;
                }
                WnnWord word = new WnnWord(0, candidate, candidate, 68);
                boolean result = learn(packageName, word, true);
                engineService.mErrorCode = result ? 0 : -1;
                return result;
            }
        }

        @Override
        public boolean learnWordNoConnect(String packageName, String candidate, String stroke) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[12][engineService.mEngineStatus]) {
                    if (engineService.mEngineStatus == 1) {
                        engineService.mErrorCode = -126;
                    } else {
                        engineService.mErrorCode = -127;
                    }
                    return IWnnEngineService.DEBUG;
                }
                WnnWord word = new WnnWord(0, candidate, stroke, 4096);
                boolean result = learn(packageName, word, IWnnEngineService.DEBUG);
                engineService.mErrorCode = result ? 0 : -1;
                return result;
            }
        }

        @Override
        public boolean setDictionary(String packageName, String configurationFile, int language, int dictionary, boolean flexibleSearch, boolean tenKeyType, boolean emojiFilter, boolean emailFilter, boolean convertCandidates, boolean learnNumber) {
            boolean dictionaryDecoratedPict;
            synchronized (this) {
                dictionaryDecoratedPict = setDictionaryDecoratedPict(packageName, configurationFile, language, dictionary, flexibleSearch, tenKeyType, emojiFilter, true, emailFilter, convertCandidates, learnNumber);
            }
            return dictionaryDecoratedPict;
        }

        @Override
        public boolean setDictionaryDecoratedPict(String packageName, String configurationFile, int language, int dictionary, boolean flexibleSearch, boolean tenKeyType, boolean emojiFilter, boolean decoEmojiFilter, boolean emailFilter, boolean convertCandidates, boolean learnNumber) {
            boolean z;
            int flexible;
            int keyType;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    iWnnEngine engine = engineService.mEngine;
                    if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[4][engineService.mEngineStatus]) {
                        if (engineService.mEngineStatus == 1) {
                            engineService.mErrorCode = -126;
                        } else {
                            engineService.mErrorCode = -127;
                        }
                        z = IWnnEngineService.DEBUG;
                    } else if (language < 0) {
                        Log.e(IWnnEngineService.TAG, "iWnnEngineService::setDictionary(): language < 0");
                        engineService.mErrorCode = -1;
                        z = IWnnEngineService.DEBUG;
                    } else {
                        boolean isLanguageChanged = language != engine.getLanguage() ? true : IWnnEngineService.DEBUG;
                        boolean isDictionaryChanged = (isLanguageChanged || dictionary != engine.getDictionary() || engineService.mEngineStatus == 1) ? true : IWnnEngineService.DEBUG;
                        if (!isDictionaryChanged) {
                            engineService.mConfigurationFile = configurationFile;
                            if (!isLanguageChanged || engineService.mIsFlexibleCharsetInit || engineService.mFlexibleSearch != flexibleSearch || engineService.mTenKeyType != tenKeyType) {
                                if (!flexibleSearch) {
                                    flexible = 1;
                                } else {
                                    flexible = 0;
                                }
                                if (!tenKeyType) {
                                    keyType = 0;
                                } else {
                                    keyType = 1;
                                }
                                engine.setFlexibleCharset(flexible, keyType);
                                engineService.mFlexibleSearch = flexibleSearch;
                                engineService.mTenKeyType = tenKeyType;
                                engineService.mIsFlexibleCharsetInit = IWnnEngineService.DEBUG;
                            }
                            engine.setEmojiFilter(emojiFilter);
                            engine.setDecoEmojiFilter(decoEmojiFilter);
                            engine.setEmailAddressFilter(emailFilter);
                            engine.setConvertedCandidateEnabled(convertCandidates);
                            engine.setEnableLearnNumber(learnNumber);
                            if (dictionary != 11 || dictionary == 10) {
                                engineService.mEngineStatus = 6;
                            } else {
                                engineService.mEngineStatus = 2;
                            }
                            z = true;
                        } else {
                            if (!engineService.mCachedDicSet.contains(Integer.valueOf(language)) && 3 <= engineService.mCachedDicSet.size()) {
                                init(packageName, engineService.mPassword, 1);
                            }
                            if (!engine.setDictionary(language, dictionary, hashCode(), configurationFile, engineService.mConnectedPackage, engineService.mPassword)) {
                                engineService.mErrorCode = -1;
                                z = IWnnEngineService.DEBUG;
                            } else {
                                engineService.mCachedDicSet.add(Integer.valueOf(language));
                                engineService.mConfigurationFile = configurationFile;
                                if (!isLanguageChanged) {
                                    if (!flexibleSearch) {
                                    }
                                    if (!tenKeyType) {
                                    }
                                    engine.setFlexibleCharset(flexible, keyType);
                                    engineService.mFlexibleSearch = flexibleSearch;
                                    engineService.mTenKeyType = tenKeyType;
                                    engineService.mIsFlexibleCharsetInit = IWnnEngineService.DEBUG;
                                    engine.setEmojiFilter(emojiFilter);
                                    engine.setDecoEmojiFilter(decoEmojiFilter);
                                    engine.setEmailAddressFilter(emailFilter);
                                    engine.setConvertedCandidateEnabled(convertCandidates);
                                    engine.setEnableLearnNumber(learnNumber);
                                    if (dictionary != 11) {
                                        engineService.mEngineStatus = 6;
                                        z = true;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    z = IWnnEngineService.DEBUG;
                }
            }
            return z;
        }

        @Override
        public boolean setGijiFilter(String packageName, int[] type) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (!IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[4][engineService.mEngineStatus]) {
                    if (engineService.mEngineStatus == 1) {
                        engineService.mErrorCode = -126;
                    } else {
                        engineService.mErrorCode = -127;
                    }
                    return IWnnEngineService.DEBUG;
                }
                if (type == null) {
                    return IWnnEngineService.DEBUG;
                }
                boolean result = engineService.mEngine.setGijiFilter(type);
                engineService.mErrorCode = result ? 0 : -1;
                return result;
            }
        }

        @Override
        public boolean undo(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[18][engineService.mEngineStatus]) {
                    boolean result = engineService.mEngine.undo(1);
                    engineService.mErrorCode = result ? 0 : -1;
                    engineService.mEngineStatus = 3;
                    return result;
                }
                if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return IWnnEngineService.DEBUG;
            }
        }

        @Override
        public void startInput(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[19][engineService.mEngineStatus]) {
                        Message message = IWnnEngineService.this.mHandler.obtainMessage(1);
                        Bundle bundle = new Bundle();
                        bundle.putString("packageName", packageName);
                        message.setData(bundle);
                        IWnnEngineService.this.mHandler.sendMessageDelayed(message, 0L);
                        Message message2 = IWnnEngineService.this.mHandler.obtainMessage(2);
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("packageName", packageName);
                        message2.setData(bundle2);
                        IWnnEngineService.this.mHandler.sendMessageDelayed(message2, 0L);
                    }
                    IWnnEngineService.this.callCheckDecoEmoji(packageName);
                }
            }
        }

        @Override
        public boolean isGijiDic(String packageName, int index) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService == null) {
                    return IWnnEngineService.DEBUG;
                }
                if (IWnnEngineService.ENGINE_STATUS_CHECK_TABLE[20][engineService.mEngineStatus]) {
                    boolean result = engineService.mEngine.isGijiDic(index);
                    engineService.mErrorCode = result ? 0 : -1;
                    return result;
                }
                if (engineService.mEngineStatus == 1) {
                    engineService.mErrorCode = -126;
                } else {
                    engineService.mErrorCode = -127;
                }
                return IWnnEngineService.DEBUG;
            }
        }

        @Override
        public int getStatus(String packageName) {
            int i;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    i = engineService.mEngineStatus;
                } else {
                    i = 1;
                }
            }
            return i;
        }

        @Override
        public int getDictionaryType(String packageName) {
            int dictionary;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    dictionary = engineService.mEngine.getDictionary();
                } else {
                    dictionary = -1;
                }
            }
            return dictionary;
        }

        @Override
        public int getErrorCode(String packageName) {
            int i;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    i = engineService.mErrorCode;
                } else {
                    i = -127;
                }
            }
            return i;
        }

        @Override
        public void disconnect(String packageName) {
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                if (engineService != null) {
                    IWnnEngineService.this.clearServiceInfo(packageName);
                }
            }
        }

        @Override
        public boolean isAlive(String packageName) {
            boolean z;
            synchronized (this) {
                EngineService engineService = IWnnEngineService.this.getEngineService(packageName);
                z = engineService == null ? IWnnEngineService.DEBUG : true;
            }
            return z;
        }
    };

    private static class EngineService {
        private HashSet<Integer> mCachedDicSet;
        private String mConfigurationFile;
        private String mConnectedPackage;
        private iWnnEngine mEngine;
        private int mEngineStatus;
        private int mErrorCode;
        private boolean mFlexibleSearch;
        private boolean mIsConverting;
        private boolean mIsFlexibleCharsetInit;
        private String mPassword;
        private int mSegment;
        private boolean mTenKeyType;
        private long mUsedTime;
        private ArrayList<WnnWord> mWnnWordArray;

        private EngineService() {
            this.mEngine = iWnnEngine.getEngineForService(LoggingEvents.EXTRA_CALLING_APP_NAME);
            this.mWnnWordArray = new ArrayList<>();
            this.mIsConverting = IWnnEngineService.DEBUG;
            this.mSegment = 0;
            this.mEngineStatus = 1;
            this.mIsFlexibleCharsetInit = true;
            this.mFlexibleSearch = IWnnEngineService.DEBUG;
            this.mTenKeyType = IWnnEngineService.DEBUG;
            this.mErrorCode = 0;
            this.mConnectedPackage = LoggingEvents.EXTRA_CALLING_APP_NAME;
            this.mPassword = LoggingEvents.EXTRA_CALLING_APP_NAME;
            this.mUsedTime = 0L;
            this.mConfigurationFile = LoggingEvents.EXTRA_CALLING_APP_NAME;
            this.mCachedDicSet = new HashSet<>();
        }

        static int access$1708(EngineService x0) {
            int i = x0.mSegment;
            x0.mSegment = i + 1;
            return i;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCurrentService = this;
        this.mContext = this;
        this.mFilesDirPath = IWnnIME.getFilesDirPath(this);
        createEngineService();
    }

    @Override
    public void onDestroy() {
        stopDecoEmojiUpdating();
        synchronized (this) {
            for (int i = 0; i < mEngineServiceArray.size(); i++) {
                if (mEngineServiceArray.get(i).mEngine != null) {
                    mEngineServiceArray.get(i).mEngine.close();
                }
            }
        }
        mCurrentService = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        DecoEmojiUtil.setConvertFunctionEnabled(DEBUG);
        EmojiAssist assist = EmojiAssist.getInstance();
        if (assist != null) {
            int functype = assist.getEmojiFunctionType();
            if (functype != 0) {
                DecoEmojiUtil.setConvertFunctionEnabled(true);
                decoEmojiBindStart();
            }
        }
        IWnnIME.copyPresetDecoEmojiGijiDictionary();
        return this.mEngineService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (this.mIsBind) {
            unbindService(this.mServiceConn);
            this.mIsBind = DEBUG;
        }
        stopDecoEmojiUpdating();
        synchronized (this) {
            for (int i = 0; i < mEngineServiceArray.size(); i++) {
                if (mEngineServiceArray.get(i).mEngine != null) {
                    mEngineServiceArray.get(i).mEngine.close();
                }
            }
            createEngineService();
        }
        return super.onUnbind(intent);
    }

    public static IWnnEngineService getCurrentService() {
        return mCurrentService;
    }

    private void decoEmojiBindStart() {
        Intent intent = new Intent();
        intent.setClassName("jp.co.omronsoft.android.decoemojimanager", DecoEmojiUtil.DECOEMOJIMANAGER_CLASSNAME);
        boolean success = bindService(intent, this.mServiceConn, 1);
        if (success) {
            this.mIsBind = true;
        }
    }

    private void stopDecoEmojiUpdating() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
    }

    private void callCheckDecoEmoji(String componentName) {
    }

    private EngineService getEngineService(String packageName) {
        if (packageName == null || packageName.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
            return null;
        }
        for (int i = 0; i < mEngineServiceArray.size(); i++) {
            if (mEngineServiceArray.get(i).mConnectedPackage.equals(packageName)) {
                mEngineServiceArray.get(i).mUsedTime = System.currentTimeMillis();
                return mEngineServiceArray.get(i);
            }
        }
        for (int i2 = 0; i2 < mEngineServiceArray.size(); i2++) {
            if (mEngineServiceArray.get(i2).mConnectedPackage.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                mEngineServiceArray.get(i2).mConnectedPackage = packageName;
                mEngineServiceArray.get(i2).mUsedTime = System.currentTimeMillis();
                mEngineServiceArray.get(i2).mEngine.setServiceConnectedName(packageName);
                return mEngineServiceArray.get(i2);
            }
        }
        return null;
    }

    private void clearServiceInfo(String packageName) {
        for (int i = 0; i < mEngineServiceArray.size(); i++) {
            if (mEngineServiceArray.get(i).mConnectedPackage.equals(packageName)) {
                mEngineServiceArray.get(i).mEngine.close();
                mEngineServiceArray.get(i).mEngine.setServiceConnectedName(LoggingEvents.EXTRA_CALLING_APP_NAME);
                mEngineServiceArray.get(i).mWnnWordArray = new ArrayList();
                mEngineServiceArray.get(i).mIsConverting = DEBUG;
                mEngineServiceArray.get(i).mSegment = 0;
                mEngineServiceArray.get(i).mEngineStatus = 1;
                mEngineServiceArray.get(i).mIsFlexibleCharsetInit = true;
                mEngineServiceArray.get(i).mFlexibleSearch = DEBUG;
                mEngineServiceArray.get(i).mTenKeyType = DEBUG;
                mEngineServiceArray.get(i).mErrorCode = 0;
                mEngineServiceArray.get(i).mConnectedPackage = LoggingEvents.EXTRA_CALLING_APP_NAME;
                mEngineServiceArray.get(i).mPassword = LoggingEvents.EXTRA_CALLING_APP_NAME;
                mEngineServiceArray.get(i).mUsedTime = 0L;
                mEngineServiceArray.get(i).mConfigurationFile = LoggingEvents.EXTRA_CALLING_APP_NAME;
                mEngineServiceArray.get(i).mCachedDicSet.clear();
                mEngineServiceArray.get(i).mEngine.clearOperationQueue();
                return;
            }
        }
    }

    private void createEngineService() {
        mEngineServiceArray.clear();
        iWnnEngine.clearServiceEngine();
        if (IWnnCore.hasLibraryForService()) {
            for (int i = 0; i < 3; i++) {
                EngineService engineService = new EngineService();
                mEngineServiceArray.add(engineService);
            }
        }
    }
}
