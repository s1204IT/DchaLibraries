package android.webkit;

import android.annotation.SystemApi;
import java.util.Map;

public class WebStorage {

    @Deprecated
    public interface QuotaUpdater {
        void updateQuota(long j);
    }

    public static class Origin {
        private String mOrigin;
        private long mQuota;
        private long mUsage;

        @SystemApi
        protected Origin(String str, long j, long j2) {
            this.mOrigin = null;
            this.mQuota = 0L;
            this.mUsage = 0L;
            this.mOrigin = str;
            this.mQuota = j;
            this.mUsage = j2;
        }

        public String getOrigin() {
            return this.mOrigin;
        }

        public long getQuota() {
            return this.mQuota;
        }

        public long getUsage() {
            return this.mUsage;
        }
    }

    public void getOrigins(ValueCallback<Map> valueCallback) {
    }

    public void getUsageForOrigin(String str, ValueCallback<Long> valueCallback) {
    }

    public void getQuotaForOrigin(String str, ValueCallback<Long> valueCallback) {
    }

    @Deprecated
    public void setQuotaForOrigin(String str, long j) {
    }

    public void deleteOrigin(String str) {
    }

    public void deleteAllData() {
    }

    public static WebStorage getInstance() {
        return WebViewFactory.getProvider().getWebStorage();
    }

    @SystemApi
    public WebStorage() {
    }
}
