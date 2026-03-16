package com.svox.pico;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class CheckVoiceData extends Activity {
    private static final String PICO_LINGWARE_PATH = Environment.getExternalStorageDirectory() + "/svox/";
    private static final String[] dataFiles = {"de-DE_gl0_sg.bin", "de-DE_ta.bin", "en-GB_kh0_sg.bin", "en-GB_ta.bin", "en-US_lh0_sg.bin", "en-US_ta.bin", "es-ES_ta.bin", "es-ES_zl0_sg.bin", "fr-FR_nk0_sg.bin", "fr-FR_ta.bin", "it-IT_cm0_sg.bin", "it-IT_ta.bin"};
    private static final String[] dataFilesInfo = {"deu-DEU", "deu-DEU", "eng-GBR", "eng-GBR", "eng-USA", "eng-USA", "spa-ESP", "spa-ESP", "fra-FRA", "fra-FRA", "ita-ITA", "ita-ITA"};
    private static final String[] supportedLanguages = {"deu-DEU", "eng-GBR", "eng-USA", "spa-ESP", "fra-FRA", "ita-ITA"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ArrayList<String> langCountryVars;
        super.onCreate(savedInstanceState);
        int result = 1;
        boolean foundMatch = false;
        ArrayList<String> available = new ArrayList<>();
        ArrayList<String> unavailable = new ArrayList<>();
        HashMap<String, Boolean> languageCountry = new HashMap<>();
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && (langCountryVars = bundle.getStringArrayList("checkVoiceDataFor")) != null) {
            for (int i = 0; i < langCountryVars.size(); i++) {
                if (langCountryVars.get(i).length() > 0) {
                    languageCountry.put(langCountryVars.get(i), true);
                }
            }
        }
        for (int i2 = 0; i2 < supportedLanguages.length; i2++) {
            if (languageCountry.size() < 1 || languageCountry.containsKey(supportedLanguages[i2])) {
                if (!fileExists(dataFiles[i2 * 2]) || !fileExists(dataFiles[(i2 * 2) + 1])) {
                    result = -2;
                    unavailable.add(supportedLanguages[i2]);
                } else {
                    available.add(supportedLanguages[i2]);
                    foundMatch = true;
                }
            }
        }
        if (languageCountry.size() > 0 && !foundMatch) {
            result = 0;
        }
        Intent returnData = new Intent();
        returnData.putExtra("dataRoot", PICO_LINGWARE_PATH);
        returnData.putExtra("dataFiles", dataFiles);
        returnData.putExtra("dataFilesInfo", dataFilesInfo);
        returnData.putStringArrayListExtra("availableVoices", available);
        returnData.putStringArrayListExtra("unavailableVoices", unavailable);
        setResult(result, returnData);
        finish();
    }

    private boolean fileExists(String filename) {
        File tempFile = new File(PICO_LINGWARE_PATH + filename);
        File tempFileSys = new File("/system/tts/lang_pico/" + filename);
        return tempFile.exists() || tempFileSys.exists();
    }
}
