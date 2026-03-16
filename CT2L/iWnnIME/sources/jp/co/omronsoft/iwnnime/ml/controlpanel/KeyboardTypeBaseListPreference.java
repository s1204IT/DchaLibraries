package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RadioButton;
import java.util.ArrayList;
import java.util.Iterator;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnArrayAdapter;

public class KeyboardTypeBaseListPreference extends ListPreference {
    protected AdapterView.OnItemClickListener listener;
    protected String mCurrentSelectValue;
    protected WnnArrayAdapter<CharSequence> mWnnArrayAdapter;

    public KeyboardTypeBaseListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentSelectValue = null;
        this.mWnnArrayAdapter = null;
        this.listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RadioButton button;
                String preSelectValue = KeyboardTypeBaseListPreference.this.mCurrentSelectValue;
                CharSequence[] entryValues = KeyboardTypeBaseListPreference.this.getEntryValues();
                if (entryValues != null && position >= 0 && position < entryValues.length) {
                    KeyboardTypeBaseListPreference.this.mCurrentSelectValue = entryValues[position].toString();
                    if (KeyboardTypeBaseListPreference.this.mCurrentSelectValue.equals(preSelectValue)) {
                        return;
                    }
                }
                if (KeyboardTypeBaseListPreference.this.mWnnArrayAdapter != null) {
                    ArrayList<RadioButton> buttonList = KeyboardTypeBaseListPreference.this.mWnnArrayAdapter.getRadioButtonList();
                    Iterator<RadioButton> it = buttonList.iterator();
                    while (it.hasNext()) {
                        it.next().setChecked(false);
                    }
                }
                if ((view instanceof ViewGroup) && (button = (RadioButton) ((ViewGroup) view).findViewById(R.id.list_button_area)) != null) {
                    button.setChecked(true);
                }
            }
        };
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && callChangeListener(this.mCurrentSelectValue)) {
            setValue(this.mCurrentSelectValue);
        }
    }
}
