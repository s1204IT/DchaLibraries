package com.mediatek.phone.ext;

public interface IPhoneMiscExt {
    String[] addCurrentNetworkToSelectionListIndex(String[] strArr);

    CharSequence[] addCurrentNetworkToSelectionListValue(CharSequence[] charSequenceArr);

    int getCurrentNetworkIndex(int i);

    int getSelectedIndex(int i);

    boolean onPreferenceChange(int i);

    boolean publishBinderDirectly();

    String[] removeAskFirstFromSelectionListIndex(String[] strArr);

    CharSequence[] removeAskFirstFromSelectionListValue(CharSequence[] charSequenceArr);
}
