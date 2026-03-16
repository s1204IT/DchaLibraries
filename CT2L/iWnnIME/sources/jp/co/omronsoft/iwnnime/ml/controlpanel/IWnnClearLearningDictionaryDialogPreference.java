package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.io.File;
import java.io.IOException;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.iwnn.IWnnSymbolEngine;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class IWnnClearLearningDictionaryDialogPreference extends DialogPreference {
    private static final String DELETE_DICTIONALY_COMMAND = "rm -R ";
    private static final String DICTIONALY_PATH = "/dicset/master/";
    private static final String DUMMY_STRING = "dummy";
    protected Context mContext;

    public IWnnClearLearningDictionaryDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(LoggingEvents.EXTRA_CALLING_APP_NAME);
        this.mContext = context;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String filesDirPath = IWnnIME.getFilesDirPath(this.mContext);
            StringBuffer pathStrBuf = new StringBuffer(filesDirPath);
            pathStrBuf.append(DICTIONALY_PATH);
            String filePath = pathStrBuf.toString();
            File dir = new File(filePath);
            File[] filelist = dir.listFiles();
            iWnnEngine engine = iWnnEngine.getEngine();
            String databasePath = this.mContext.getDatabasePath(DUMMY_STRING).getPath();
            int pos = databasePath.indexOf(DUMMY_STRING);
            String databasePath2 = databasePath.substring(0, pos);
            StringBuffer cmdStrBuf = new StringBuffer(DELETE_DICTIONALY_COMMAND);
            cmdStrBuf.append(databasePath2);
            try {
                Runtime localRuntime = Runtime.getRuntime();
                localRuntime.exec(cmdStrBuf.toString());
            } catch (IOException e) {
                Log.e("iWnn", "Database delete failed!");
            }
            IWnnIME wnn = IWnnIME.getCurrentIme();
            if (wnn != null && wnn.getCurrentIWnnIme() != null) {
                wnn.getCurrentIWnnIme().resetHistories();
            } else {
                IWnnSymbolEngine symbolEngine = new IWnnSymbolEngine(this.mContext, LanguageManager.getChosenLocale(0).toString());
                symbolEngine.resetHistories();
            }
            engine.close();
            if (filelist != null) {
                for (File file : filelist) {
                    String filename = file.getName();
                    deleteLearningDic(engine, filePath + filename);
                }
            }
            engine.init(filesDirPath);
            Toast.makeText(this.mContext, R.string.ti_dialog_clear_learning_dictionary_done_txt, 0).show();
        }
    }

    private void deleteLearningDic(iWnnEngine engine, String path) {
        if (iWnnEngine.isClearLearningDictionary(path)) {
            engine.deleteDictionaryFile(path);
        }
    }
}
