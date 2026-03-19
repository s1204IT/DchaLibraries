package android.support.v7.preference;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

final class CollapsiblePreferenceGroupController {
    private final Context mContext;
    private boolean mHasExpandablePreference = false;
    private final PreferenceGroupAdapter mPreferenceGroupAdapter;

    CollapsiblePreferenceGroupController(PreferenceGroup preferenceGroup, PreferenceGroupAdapter preferenceGroupAdapter) {
        this.mPreferenceGroupAdapter = preferenceGroupAdapter;
        this.mContext = preferenceGroup.getContext();
    }

    public List<Preference> createVisiblePreferencesList(PreferenceGroup group) {
        return createInnerVisiblePreferencesList(group);
    }

    private List<Preference> createInnerVisiblePreferencesList(PreferenceGroup group) {
        this.mHasExpandablePreference = false;
        int visiblePreferenceCount = 0;
        boolean hasExpandablePreference = group.getInitialExpandedChildrenCount() != Integer.MAX_VALUE;
        List<Preference> visiblePreferences = new ArrayList<>();
        List<Preference> collapsedPreferences = new ArrayList<>();
        int groupSize = group.getPreferenceCount();
        for (int i = 0; i < groupSize; i++) {
            Preference preference = group.getPreference(i);
            if (preference.isVisible()) {
                if (!hasExpandablePreference || visiblePreferenceCount < group.getInitialExpandedChildrenCount()) {
                    visiblePreferences.add(preference);
                } else {
                    collapsedPreferences.add(preference);
                }
                if (!(preference instanceof PreferenceGroup)) {
                    visiblePreferenceCount++;
                } else {
                    PreferenceGroup innerGroup = (PreferenceGroup) preference;
                    if (innerGroup.isOnSameScreenAsChildren()) {
                        List<Preference> innerList = createInnerVisiblePreferencesList(innerGroup);
                        if (hasExpandablePreference && this.mHasExpandablePreference) {
                            throw new IllegalArgumentException("Nested expand buttons are not supported!");
                        }
                        for (Preference inner : innerList) {
                            if (!hasExpandablePreference || visiblePreferenceCount < group.getInitialExpandedChildrenCount()) {
                                visiblePreferences.add(inner);
                            } else {
                                collapsedPreferences.add(inner);
                            }
                            visiblePreferenceCount++;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        if (hasExpandablePreference && visiblePreferenceCount > group.getInitialExpandedChildrenCount()) {
            ExpandButton expandButton = createExpandButton(group, collapsedPreferences);
            visiblePreferences.add(expandButton);
        }
        this.mHasExpandablePreference |= hasExpandablePreference;
        return visiblePreferences;
    }

    public boolean onPreferenceVisibilityChange(Preference preference) {
        if (this.mHasExpandablePreference) {
            this.mPreferenceGroupAdapter.onPreferenceHierarchyChange(preference);
            return true;
        }
        return false;
    }

    private ExpandButton createExpandButton(final PreferenceGroup group, List<Preference> collapsedPreferences) {
        ExpandButton preference = new ExpandButton(this.mContext, collapsedPreferences, group.getId());
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference2) {
                group.setInitialExpandedChildrenCount(Preference.DEFAULT_ORDER);
                CollapsiblePreferenceGroupController.this.mPreferenceGroupAdapter.onPreferenceHierarchyChange(preference2);
                return true;
            }
        });
        return preference;
    }

    static class ExpandButton extends Preference {
        private long mId;

        ExpandButton(Context context, List<Preference> collapsedPreferences, long parentId) {
            super(context);
            initLayout();
            setSummary(collapsedPreferences);
            this.mId = 1000000 + parentId;
        }

        private void initLayout() {
            setLayoutResource(R.layout.expand_button);
            setIcon(R.drawable.ic_arrow_down_24dp);
            setTitle(R.string.expand_button_title);
            setOrder(999);
        }

        private void setSummary(List<Preference> collapsedPreferences) {
            CharSequence summary = null;
            List<PreferenceGroup> parents = new ArrayList<>();
            for (Preference preference : collapsedPreferences) {
                CharSequence title = preference.getTitle();
                if ((preference instanceof PreferenceGroup) && !TextUtils.isEmpty(title)) {
                    parents.add((PreferenceGroup) preference);
                }
                if (parents.contains(preference.getParent())) {
                    if (preference instanceof PreferenceGroup) {
                        parents.add((PreferenceGroup) preference);
                    }
                } else if (!TextUtils.isEmpty(title)) {
                    if (summary == null) {
                        summary = title;
                    } else {
                        summary = getContext().getString(R.string.summary_collapsed_preference_list, summary, title);
                    }
                }
            }
            setSummary(summary);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            holder.setDividerAllowedAbove(false);
        }

        @Override
        public long getId() {
            return this.mId;
        }
    }
}
