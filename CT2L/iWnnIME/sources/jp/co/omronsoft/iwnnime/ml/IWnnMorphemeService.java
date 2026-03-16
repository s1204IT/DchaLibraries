package jp.co.omronsoft.iwnnime.ml;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import java.lang.reflect.Array;
import java.util.ArrayList;
import jp.co.omronsoft.iwnnime.ml.IMorphemeService;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnCore;

public class IWnnMorphemeService extends Service {
    private static final String CONF_FILE = "/system/lib/lib_dic_morphem_ja_JP.conf.so";
    private static final int INPUT_MAX = 50;
    private IWnnCore mCore;
    private IMorphemeService.Stub mMorphemeService = new IMorphemeService.Stub() {
        @Override
        public Bundle splitWord(String input, int readingsMax) {
            Bundle b;
            synchronized (this) {
                ArrayList<String>[] resultLists = new ArrayList[readingsMax + 1];
                for (int i = 0; i < readingsMax + 1; i++) {
                    resultLists[i] = new ArrayList<>();
                }
                ArrayList<Short> wordClassList = new ArrayList<>();
                int[] splitResult = new int[2];
                String[][] segment = (String[][]) Array.newInstance((Class<?>) String.class, readingsMax + 1, 2);
                int processLength = 0;
                int inputLength = input.length();
                while (processLength < inputLength) {
                    IWnnMorphemeService.this.mCore.splitWord(input.substring(processLength, Math.min(processLength + 50, inputLength)), splitResult);
                    int segmentCount = splitResult[0];
                    int length = splitResult[1];
                    if (segmentCount <= 0) {
                        break;
                    }
                    processLength += length;
                    for (int i2 = 0; i2 < segmentCount; i2++) {
                        IWnnMorphemeService.this.mCore.getMorphemeText(i2, segment);
                        for (int j = 0; j < readingsMax + 1; j++) {
                            resultLists[j].add(segment[j][0]);
                            resultLists[j].add(segment[j][1]);
                        }
                        short wordClass = IWnnMorphemeService.this.mCore.getMorphemePartOfSpeech(i2);
                        wordClassList.add(Short.valueOf(wordClass));
                        wordClassList.add(Short.valueOf(wordClass));
                    }
                }
                b = new Bundle();
                int size = resultLists[0].size();
                b.putStringArray("strings", (String[]) resultLists[0].toArray(new String[size]));
                for (int i3 = 1; i3 < readingsMax + 1; i3++) {
                    b.putStringArray("readings" + i3, (String[]) resultLists[i3].toArray(new String[size]));
                }
                short[] wordClassValues = new short[size];
                for (int i4 = 0; i4 < size; i4++) {
                    wordClassValues[i4] = wordClassList.get(i4).shortValue();
                }
                b.putShortArray("wordclasses", wordClassValues);
            }
            return b;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        this.mCore = new IWnnCore();
        String fileDirPath = IWnnIME.getFilesDirPath(this);
        this.mCore.init(fileDirPath);
        this.mCore.setDictionary(0, 0, CONF_FILE, true, fileDirPath);
    }

    @Override
    public void onDestroy() {
        this.mCore.unmountDictionary();
        this.mCore.destroyWnnInfo();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mMorphemeService;
    }
}
