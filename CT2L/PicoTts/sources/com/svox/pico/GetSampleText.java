package com.svox.pico;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class GetSampleText extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int result = 0;
        Intent returnData = new Intent();
        Intent i = getIntent();
        String language = i.getExtras().getString("language");
        String country = i.getExtras().getString("country");
        i.getExtras().getString("variant");
        if (language.equals("eng")) {
            if (country.equals("GBR")) {
                returnData.putExtra("sampleText", getString(R.string.eng_gbr_sample));
            } else {
                returnData.putExtra("sampleText", getString(R.string.eng_usa_sample));
            }
        } else if (language.equals("fra")) {
            returnData.putExtra("sampleText", getString(R.string.fra_fra_sample));
        } else if (language.equals("ita")) {
            returnData.putExtra("sampleText", getString(R.string.ita_ita_sample));
        } else if (language.equals("deu")) {
            returnData.putExtra("sampleText", getString(R.string.deu_deu_sample));
        } else if (language.equals("spa")) {
            returnData.putExtra("sampleText", getString(R.string.spa_esp_sample));
        } else {
            result = -2;
            returnData.putExtra("sampleText", "");
        }
        setResult(result, returnData);
        finish();
    }
}
