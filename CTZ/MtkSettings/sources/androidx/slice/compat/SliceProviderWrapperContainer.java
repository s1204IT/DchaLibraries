package androidx.slice.compat;

import android.annotation.TargetApi;
import android.app.slice.Slice;
import android.app.slice.SliceProvider;
import android.app.slice.SliceSpec;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import androidx.slice.SliceConvert;
import java.util.Collection;
import java.util.Set;

@TargetApi(28)
public class SliceProviderWrapperContainer {

    public static class SliceProviderWrapper extends SliceProvider {
        private androidx.slice.SliceProvider mSliceProvider;

        public SliceProviderWrapper(androidx.slice.SliceProvider provider, String[] autoGrantPermissions) {
            super(autoGrantPermissions);
            this.mSliceProvider = provider;
        }

        @Override
        public void attachInfo(Context context, ProviderInfo info) {
            this.mSliceProvider.attachInfo(context, info);
            super.attachInfo(context, info);
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Slice onBindSlice(Uri sliceUri, Set<SliceSpec> supportedVersions) {
            androidx.slice.SliceProvider.setSpecs(SliceConvert.wrap(supportedVersions));
            try {
                return SliceConvert.unwrap(this.mSliceProvider.onBindSlice(sliceUri));
            } finally {
                androidx.slice.SliceProvider.setSpecs(null);
            }
        }

        @Override
        public void onSlicePinned(Uri sliceUri) {
            this.mSliceProvider.onSlicePinned(sliceUri);
            this.mSliceProvider.handleSlicePinned(sliceUri);
        }

        @Override
        public void onSliceUnpinned(Uri sliceUri) {
            this.mSliceProvider.onSliceUnpinned(sliceUri);
            this.mSliceProvider.handleSliceUnpinned(sliceUri);
        }

        @Override
        public Collection<Uri> onGetSliceDescendants(Uri uri) {
            return this.mSliceProvider.onGetSliceDescendants(uri);
        }

        @Override
        public Uri onMapIntentToUri(Intent intent) {
            return this.mSliceProvider.onMapIntentToUri(intent);
        }
    }
}
