package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.Context;
import android.icu.text.ListFormatter;
import android.provider.SearchIndexableResource;
import android.service.notification.ZenModeConfig;
import com.android.settings.R;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ZenModeSettings extends ZenModeSettingsBase {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.zen_mode_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("zen_mode_duration_settings");
            nonIndexableKeys.add("zen_mode_settings_button_container");
            return nonIndexableKeys;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ZenModeSettings.buildPreferenceControllers(context, null, null);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 76;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), getFragmentManager());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle, FragmentManager fragmentManager) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ZenModeBehaviorMsgEventReminderPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeBehaviorSoundPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeBehaviorCallsPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeBlockedEffectsPreferenceController(context, lifecycle));
        arrayList.add(new ZenModeDurationPreferenceController(context, lifecycle, fragmentManager));
        arrayList.add(new ZenModeAutomationPreferenceController(context));
        arrayList.add(new ZenModeButtonPreferenceController(context, lifecycle, fragmentManager));
        arrayList.add(new ZenModeSettingsFooterPreferenceController(context, lifecycle));
        return arrayList;
    }

    public static class SummaryBuilder {
        private static final int[] ALL_PRIORITY_CATEGORIES = {32, 64, 128, 4, 2, 1, 8, 16};
        private Context mContext;

        public SummaryBuilder(Context context) {
            this.mContext = context;
        }

        String getSoundSettingSummary(NotificationManager.Policy policy) {
            List<String> enabledCategories = getEnabledCategories(policy, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ZenModeSettings.SummaryBuilder.lambda$getSoundSettingSummary$0((Integer) obj);
                }
            });
            int size = enabledCategories.size();
            if (size == 0) {
                return this.mContext.getString(R.string.zen_sound_all_muted);
            }
            if (size == 1) {
                return this.mContext.getString(R.string.zen_sound_one_allowed, enabledCategories.get(0).toLowerCase());
            }
            if (size == 2) {
                return this.mContext.getString(R.string.zen_sound_two_allowed, enabledCategories.get(0).toLowerCase(), enabledCategories.get(1).toLowerCase());
            }
            if (size == 3) {
                return this.mContext.getString(R.string.zen_sound_three_allowed, enabledCategories.get(0).toLowerCase(), enabledCategories.get(1).toLowerCase(), enabledCategories.get(2).toLowerCase());
            }
            return this.mContext.getString(R.string.zen_sound_none_muted);
        }

        static boolean lambda$getSoundSettingSummary$0(Integer num) {
            return 32 == num.intValue() || 64 == num.intValue() || 128 == num.intValue();
        }

        String getCallsSettingSummary(NotificationManager.Policy policy) {
            List<String> enabledCategories = getEnabledCategories(policy, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ZenModeSettings.SummaryBuilder.lambda$getCallsSettingSummary$1((Integer) obj);
                }
            });
            int size = enabledCategories.size();
            if (size == 0) {
                return this.mContext.getString(R.string.zen_mode_no_exceptions);
            }
            if (size == 1) {
                return this.mContext.getString(R.string.zen_mode_calls_summary_one, enabledCategories.get(0).toLowerCase());
            }
            return this.mContext.getString(R.string.zen_mode_calls_summary_two, enabledCategories.get(0).toLowerCase(), enabledCategories.get(1).toLowerCase());
        }

        static boolean lambda$getCallsSettingSummary$1(Integer num) {
            return 8 == num.intValue() || 16 == num.intValue();
        }

        String getMsgEventReminderSettingSummary(NotificationManager.Policy policy) {
            List<String> enabledCategories = getEnabledCategories(policy, new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ZenModeSettings.SummaryBuilder.lambda$getMsgEventReminderSettingSummary$2((Integer) obj);
                }
            });
            int size = enabledCategories.size();
            if (size == 0) {
                return this.mContext.getString(R.string.zen_mode_no_exceptions);
            }
            if (size == 1) {
                return enabledCategories.get(0);
            }
            if (size == 2) {
                return this.mContext.getString(R.string.join_two_items, enabledCategories.get(0), enabledCategories.get(1).toLowerCase());
            }
            if (size == 3) {
                ArrayList arrayList = new ArrayList();
                arrayList.add(enabledCategories.get(0));
                arrayList.add(enabledCategories.get(1).toLowerCase());
                arrayList.add(enabledCategories.get(2).toLowerCase());
                return ListFormatter.getInstance().format(arrayList);
            }
            ArrayList arrayList2 = new ArrayList();
            arrayList2.add(enabledCategories.get(0));
            arrayList2.add(enabledCategories.get(1).toLowerCase());
            arrayList2.add(enabledCategories.get(2).toLowerCase());
            arrayList2.add(this.mContext.getString(R.string.zen_mode_other_options));
            return ListFormatter.getInstance().format(arrayList2);
        }

        static boolean lambda$getMsgEventReminderSettingSummary$2(Integer num) {
            return 2 == num.intValue() || 1 == num.intValue() || 4 == num.intValue();
        }

        String getSoundSummary() {
            if (NotificationManager.from(this.mContext).getZenMode() != 0) {
                String description = ZenModeConfig.getDescription(this.mContext, true, NotificationManager.from(this.mContext).getZenModeConfig(), false);
                return description == null ? this.mContext.getString(R.string.zen_mode_sound_summary_on) : this.mContext.getString(R.string.zen_mode_sound_summary_on_with_info, description);
            }
            int enabledAutomaticRulesCount = getEnabledAutomaticRulesCount();
            if (enabledAutomaticRulesCount > 0) {
                return this.mContext.getString(R.string.zen_mode_sound_summary_off_with_info, this.mContext.getResources().getQuantityString(R.plurals.zen_mode_sound_summary_summary_off_info, enabledAutomaticRulesCount, Integer.valueOf(enabledAutomaticRulesCount)));
            }
            return this.mContext.getString(R.string.zen_mode_sound_summary_off);
        }

        String getBlockedEffectsSummary(NotificationManager.Policy policy) {
            if (policy.suppressedVisualEffects == 0) {
                return this.mContext.getResources().getString(R.string.zen_mode_restrict_notifications_summary_muted);
            }
            if (NotificationManager.Policy.areAllVisualEffectsSuppressed(policy.suppressedVisualEffects)) {
                return this.mContext.getResources().getString(R.string.zen_mode_restrict_notifications_summary_hidden);
            }
            return this.mContext.getResources().getString(R.string.zen_mode_restrict_notifications_summary_custom);
        }

        String getAutomaticRulesSummary() {
            int enabledAutomaticRulesCount = getEnabledAutomaticRulesCount();
            return enabledAutomaticRulesCount == 0 ? this.mContext.getString(R.string.zen_mode_settings_summary_off) : this.mContext.getResources().getQuantityString(R.plurals.zen_mode_settings_summary_on, enabledAutomaticRulesCount, Integer.valueOf(enabledAutomaticRulesCount));
        }

        int getEnabledAutomaticRulesCount() {
            Map<String, AutomaticZenRule> automaticZenRules = NotificationManager.from(this.mContext).getAutomaticZenRules();
            int i = 0;
            if (automaticZenRules != null) {
                Iterator<Map.Entry<String, AutomaticZenRule>> it = automaticZenRules.entrySet().iterator();
                while (it.hasNext()) {
                    AutomaticZenRule value = it.next().getValue();
                    if (value != null && value.isEnabled()) {
                        i++;
                    }
                }
            }
            return i;
        }

        private List<String> getEnabledCategories(NotificationManager.Policy policy, Predicate<Integer> predicate) {
            ArrayList arrayList = new ArrayList();
            for (int i : ALL_PRIORITY_CATEGORIES) {
                if (predicate.test(Integer.valueOf(i)) && isCategoryEnabled(policy, i)) {
                    if (i == 32) {
                        arrayList.add(this.mContext.getString(R.string.zen_mode_alarms));
                    } else if (i == 64) {
                        arrayList.add(this.mContext.getString(R.string.zen_mode_media));
                    } else if (i == 128) {
                        arrayList.add(this.mContext.getString(R.string.zen_mode_system));
                    } else if (i == 4) {
                        if (policy.priorityMessageSenders == 0) {
                            arrayList.add(this.mContext.getString(R.string.zen_mode_all_messages));
                        } else {
                            arrayList.add(this.mContext.getString(R.string.zen_mode_selected_messages));
                        }
                    } else if (i == 2) {
                        arrayList.add(this.mContext.getString(R.string.zen_mode_events));
                    } else if (i == 1) {
                        arrayList.add(this.mContext.getString(R.string.zen_mode_reminders));
                    } else if (i == 8) {
                        if (policy.priorityCallSenders != 0) {
                            if (policy.priorityCallSenders == 1) {
                                arrayList.add(this.mContext.getString(R.string.zen_mode_contacts_callers));
                            } else {
                                arrayList.add(this.mContext.getString(R.string.zen_mode_starred_callers));
                            }
                        } else {
                            arrayList.add(this.mContext.getString(R.string.zen_mode_all_callers));
                        }
                    } else if (i == 16 && !arrayList.contains(this.mContext.getString(R.string.zen_mode_all_callers))) {
                        arrayList.add(this.mContext.getString(R.string.zen_mode_repeat_callers));
                    }
                }
            }
            return arrayList;
        }

        private boolean isCategoryEnabled(NotificationManager.Policy policy, int i) {
            return (policy.priorityCategories & i) != 0;
        }
    }
}
