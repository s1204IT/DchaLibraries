package com.android.bips.ipp;

import android.text.TextUtils;
import com.android.bips.R;
import com.android.bips.jni.BackendConstants;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JobStatus {
    private static final Map<String, Integer> sBlockReasonsMap = new HashMap();
    private final Set<String> mBlockedReasons;
    private int mId;
    private String mJobResult;
    private String mJobState;

    static {
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__DOOR_OPEN, Integer.valueOf(R.string.printer_door_open));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__JAMMED, Integer.valueOf(R.string.printer_jammed));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__OUT_OF_PAPER, Integer.valueOf(R.string.printer_out_of_paper));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__SERVICE_REQUEST, Integer.valueOf(R.string.printer_check));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__OUT_OF_INK, Integer.valueOf(R.string.printer_out_of_ink));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__OUT_OF_TONER, Integer.valueOf(R.string.printer_out_of_toner));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__LOW_ON_INK, Integer.valueOf(R.string.printer_low_on_ink));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__REALLY_LOW_ON_INK, Integer.valueOf(R.string.printer_low_on_ink));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__LOW_ON_TONER, Integer.valueOf(R.string.printer_low_on_toner));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__BUSY, Integer.valueOf(R.string.printer_busy));
        sBlockReasonsMap.put(BackendConstants.BLOCKED_REASON__OFFLINE, Integer.valueOf(R.string.printer_offline));
    }

    public JobStatus() {
        this.mId = -1;
        this.mBlockedReasons = new LinkedHashSet();
    }

    private JobStatus(JobStatus jobStatus) {
        this.mId = jobStatus.mId;
        this.mJobState = jobStatus.mJobState;
        this.mJobResult = jobStatus.mJobResult;
        this.mBlockedReasons = jobStatus.mBlockedReasons;
    }

    public int getBlockedReasonId() {
        for (String str : this.mBlockedReasons) {
            if (sBlockReasonsMap.containsKey(str)) {
                return sBlockReasonsMap.get(str).intValue();
            }
        }
        return 0;
    }

    public String getJobState() {
        return this.mJobState;
    }

    public String getJobResult() {
        return this.mJobResult;
    }

    public int getId() {
        return this.mId;
    }

    boolean isJobDone() {
        return !TextUtils.isEmpty(this.mJobResult);
    }

    public String toString() {
        return "JobStatus{id=" + this.mId + ", jobState=" + this.mJobState + ", jobResult=" + this.mJobResult + ", blockedReasons=" + this.mBlockedReasons + "}";
    }

    static class Builder {
        final JobStatus mPrototype;

        Builder() {
            this.mPrototype = new JobStatus();
        }

        Builder(JobStatus jobStatus) {
            this.mPrototype = new JobStatus();
        }

        public Builder setId(int i) {
            this.mPrototype.mId = i;
            return this;
        }

        Builder setJobState(String str) {
            this.mPrototype.mJobState = str;
            return this;
        }

        Builder setJobResult(String str) {
            this.mPrototype.mJobResult = str;
            return this;
        }

        Builder clearBlockedReasons() {
            this.mPrototype.mBlockedReasons.clear();
            return this;
        }

        Builder addBlockedReason(String str) {
            this.mPrototype.mBlockedReasons.add(str);
            return this;
        }

        public JobStatus build() {
            return new JobStatus();
        }
    }
}
