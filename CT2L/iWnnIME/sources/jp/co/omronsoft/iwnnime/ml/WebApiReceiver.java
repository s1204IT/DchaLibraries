package jp.co.omronsoft.iwnnime.ml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class WebApiReceiver extends BroadcastReceiver {
    private static final String WEBAPI_CANDIDATE_KEYCODE = "candidate";
    private static final String WEBAPI_ERROR_CODE_KEYCODE = "error_code";
    private static final String WEBAPI_ERROR_KEYCODE = "error";
    private static final String WEBAPI_HINSHI_KEYCODE = "hinshi";
    private static final String WEBAPI_PACKAGE_KEYCODE = "package";
    private static final String WEBAPI_YOMI_KEYCODE = "yomi_key";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String yomi = bundle.getString(WEBAPI_YOMI_KEYCODE);
            iWnnEngine engine = iWnnEngine.getEngine();
            if (yomi != null && yomi.equals(engine.getSendingYomiToWebApi())) {
                boolean error = bundle.getBoolean(WEBAPI_ERROR_KEYCODE);
                IWnnIME wnn = IWnnIME.getCurrentIme();
                if (wnn != null) {
                    engine.setWebApiResult(bundle.getString(WEBAPI_PACKAGE_KEYCODE), !error);
                    if (error) {
                        String errorCode = bundle.getString(WEBAPI_ERROR_CODE_KEYCODE);
                        wnn.onEvent(new IWnnImeEvent(IWnnImeEvent.RESULT_WEBAPI_NG, errorCode));
                    } else {
                        String[] candidate = bundle.getStringArray(WEBAPI_CANDIDATE_KEYCODE);
                        short[] hinshi = bundle.getShortArray(WEBAPI_HINSHI_KEYCODE);
                        engine.setWebApiCandidates(yomi, candidate, hinshi);
                    }
                    if (engine.isWebApiAllReceived()) {
                        if (engine.isWebApiSuccessReceived()) {
                            wnn.onEvent(new IWnnImeEvent(IWnnImeEvent.RESULT_WEBAPI_OK));
                        }
                        engine.onDoneGettingCandidates();
                    }
                }
            }
        }
    }
}
