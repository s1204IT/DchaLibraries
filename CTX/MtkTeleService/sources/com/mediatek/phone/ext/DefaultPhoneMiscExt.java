package com.mediatek.phone.ext;

public class DefaultPhoneMiscExt implements IPhoneMiscExt {
    @Override
    public boolean publishBinderDirectly() {
        return false;
    }

    @Override
    public String[] removeAskFirstFromSelectionListIndex(String[] strArr) {
        return strArr;
    }

    @Override
    public CharSequence[] removeAskFirstFromSelectionListValue(CharSequence[] charSequenceArr) {
        return charSequenceArr;
    }

    @Override
    public int getSelectedIndex(int i) {
        return i;
    }

    @Override
    public String[] addCurrentNetworkToSelectionListIndex(String[] strArr) {
        return strArr;
    }

    @Override
    public CharSequence[] addCurrentNetworkToSelectionListValue(CharSequence[] charSequenceArr) {
        return charSequenceArr;
    }

    @Override
    public boolean onPreferenceChange(int i) {
        return false;
    }

    @Override
    public int getCurrentNetworkIndex(int i) {
        return i;
    }
}
