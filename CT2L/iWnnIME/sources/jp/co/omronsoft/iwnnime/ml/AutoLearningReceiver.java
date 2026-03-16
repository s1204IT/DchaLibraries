package jp.co.omronsoft.iwnnime.ml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.lang.reflect.Array;
import java.util.ArrayList;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnCore;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class AutoLearningReceiver extends BroadcastReceiver {
    private static final String ACTION_AUTO_LEARNING = "jp.co.omronsoft.iwnnime.ml.AUTO_LEARNING";
    private static final String ACTION_CLEAR_LEARNING = "jp.co.omronsoft.iwnnime.ml.CLEAR_LEARNING";
    private static final String AUTO_LEARNING_READINGS_KEYCODE = "readings";
    private static final String AUTO_LEARNING_STRING_KEYCODE = "string";
    private static final String CONF_FILE = "/system/lib/lib_dic_morphem_ja_JP.conf.so";
    private static final int INPUT_MAX = 50;
    private static final int MAX_DICTIONARY_WORD_LIST = 500;

    @Override
    public void onReceive(Context context, Intent intent) {
        IWnnImeBase ime;
        String input;
        iWnnEngine engine = iWnnEngine.getEngine();
        if (intent.getAction().equals(ACTION_AUTO_LEARNING)) {
            IWnnCore mCore = new IWnnCore();
            String fileDirPath = IWnnIME.getFilesDirPath(context);
            mCore.init(fileDirPath);
            mCore.setDictionary(0, 0, CONF_FILE, true, fileDirPath);
            Bundle bundle = intent.getExtras();
            if (bundle != null && (input = bundle.getString(AUTO_LEARNING_STRING_KEYCODE)) != null) {
                int readingsMax = bundle.getInt(AUTO_LEARNING_READINGS_KEYCODE, 1);
                ArrayList<String>[] resultLists = new ArrayList[readingsMax + 1];
                for (int i = 0; i < readingsMax + 1; i++) {
                    resultLists[i] = new ArrayList<>();
                }
                int[] splitResult = new int[2];
                String[][] segment = (String[][]) Array.newInstance((Class<?>) String.class, readingsMax + 1, 2);
                int processLength = 0;
                int inputLength = input.length();
                while (processLength < inputLength) {
                    mCore.splitWord(input.substring(processLength, Math.min(processLength + 50, inputLength)), splitResult);
                    int segmentCount = splitResult[0];
                    int length = splitResult[1];
                    if (segmentCount <= 0) {
                        break;
                    }
                    processLength += length;
                    for (int i2 = 0; i2 < segmentCount; i2++) {
                        mCore.getMorphemeText(i2, segment);
                        if (!resultLists[0].contains(segment[0][0])) {
                            for (int j = 0; j < readingsMax + 1; j++) {
                                resultLists[j].add(segment[j][0]);
                                resultLists[j].add(segment[j][1]);
                            }
                        }
                    }
                }
                engine.setDictionary(0, 45, hashCode());
                if (engine.createAutoLearningDictionary(45)) {
                    int count = 0;
                    boolean connect = false;
                    for (int i3 = 0; i3 < resultLists[0].size() && 500 > count; i3++) {
                        String candidate = resultLists[0].get(i3);
                        if (i3 % 2 == 0) {
                            if (candidate == null) {
                                connect = false;
                            } else {
                                String stroke = resultLists[1].get(i3);
                                if (stroke != null) {
                                    WnnWord word = new WnnWord(candidate, stroke);
                                    word.attribute |= 32768;
                                    if (connect) {
                                        word.attribute |= 8192;
                                    }
                                    if (engine.addWord(word) == 0) {
                                        count++;
                                        connect = true;
                                    }
                                }
                            }
                        } else if (candidate != null) {
                            connect = false;
                        }
                    }
                    for (int i4 = 0; i4 < resultLists[0].size() && 500 > count; i4 += 2) {
                        String candidate2 = resultLists[0].get(i4);
                        if (candidate2 != null) {
                            for (int j2 = 2; j2 < readingsMax + 1; j2++) {
                                String stroke2 = resultLists[j2].get(i4);
                                if (stroke2 != null) {
                                    WnnWord word2 = new WnnWord(candidate2, stroke2);
                                    word2.attribute |= 32768;
                                    if (engine.addWord(word2) == 0) {
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                    if (count > 0) {
                        engine.saveAutoLearningDictionary(45);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        } else if (intent.getAction().equals(ACTION_CLEAR_LEARNING)) {
            engine.setDictionary(0, 45, hashCode());
            engine.deleteAutoLearningDictionary(45);
        }
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            CandidatesManager manager = wnn.getCurrentCandidatesManager();
            if ((manager == null || !manager.isSymbolMode()) && (ime = wnn.getCurrentIWnnIme()) != null) {
                ime.clearCommitInfo();
                ime.initializeScreen();
            }
        }
    }
}
