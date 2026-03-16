package jp.co.omronsoft.iwnnime.ml;

import android.content.Intent;

public class MushroomControl {
    private static MushroomControl mMushroomControl;
    private CharSequence mMushroomResult;
    private boolean mResultType;

    public MushroomControl() {
        mMushroomControl = null;
        this.mMushroomResult = null;
    }

    public static synchronized MushroomControl getInstance() {
        if (mMushroomControl == null) {
            mMushroomControl = new MushroomControl();
        }
        return mMushroomControl;
    }

    public void startMushroomLauncher(CharSequence oldString, Boolean type) {
        this.mMushroomResult = null;
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            Intent intent = new Intent();
            intent.setClass(wnn, MushroomPlus.class);
            intent.addFlags(402653184);
            intent.putExtra(MushroomPlus.MUSHROOM_REPLACE_KEY, oldString);
            intent.putExtra(MushroomPlus.GET_STRING_TYPE, type);
            wnn.startActivity(intent);
        }
    }

    public CharSequence getResultString() {
        CharSequence result = this.mMushroomResult;
        if (this.mMushroomResult != null) {
            this.mMushroomResult = null;
        }
        return result;
    }

    public void setResultString(CharSequence result) {
        this.mMushroomResult = result;
    }

    public Boolean getResultType() {
        boolean type = this.mResultType;
        this.mResultType = false;
        return Boolean.valueOf(type);
    }

    public void setResultType(Boolean type) {
        this.mResultType = type.booleanValue();
    }
}
