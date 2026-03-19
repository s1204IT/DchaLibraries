package com.android.settings.dream;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import com.android.settings.R;
import com.android.settings.dream.CurrentDreamPicker;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.CandidateInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CurrentDreamPicker extends RadioButtonPickerFragment {
    private DreamBackend mBackend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.current_dream_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 47;
    }

    @Override
    protected boolean setDefaultKey(String str) {
        Map<String, ComponentName> dreamComponentsMap = getDreamComponentsMap();
        if (dreamComponentsMap.get(str) != null) {
            this.mBackend.setActiveDream(dreamComponentsMap.get(str));
            return true;
        }
        return false;
    }

    @Override
    protected String getDefaultKey() {
        return this.mBackend.getActiveDream().flattenToString();
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        return (List) this.mBackend.getDreamInfos().stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return new CurrentDreamPicker.DreamCandidateInfo((DreamBackend.DreamInfo) obj);
            }
        }).collect(Collectors.toList());
    }

    @Override
    protected void onSelectionPerformed(boolean z) {
        super.onSelectionPerformed(z);
        getActivity().finish();
    }

    private Map<String, ComponentName> getDreamComponentsMap() {
        final HashMap map = new HashMap();
        this.mBackend.getDreamInfos().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                CurrentDreamPicker.lambda$getDreamComponentsMap$0(map, (DreamBackend.DreamInfo) obj);
            }
        });
        return map;
    }

    static void lambda$getDreamComponentsMap$0(Map map, DreamBackend.DreamInfo dreamInfo) {
    }

    private static final class DreamCandidateInfo extends CandidateInfo {
        private final Drawable icon;
        private final String key;
        private final CharSequence name;

        DreamCandidateInfo(DreamBackend.DreamInfo dreamInfo) {
            super(true);
            this.name = dreamInfo.caption;
            this.icon = dreamInfo.icon;
            this.key = dreamInfo.componentName.flattenToString();
        }

        @Override
        public CharSequence loadLabel() {
            return this.name;
        }

        @Override
        public Drawable loadIcon() {
            return this.icon;
        }

        @Override
        public String getKey() {
            return this.key;
        }
    }
}
