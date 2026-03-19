package com.android.photos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.widget.ShareActionProvider;
import com.android.gallery3d.common.ApiHelper;
import java.util.ArrayList;

public class SelectionManager {
    private Activity mActivity;
    private NfcAdapter mNfcAdapter;
    private SelectedUriSource mUriSource;
    private Intent mShareIntent = new Intent();
    private int mSelectedTotalCount = 0;
    private int mSelectedShareableCount = 0;
    private int mSelectedShareableImageCount = 0;
    private int mSelectedShareableVideoCount = 0;
    private int mSelectedDeletableCount = 0;
    private int mSelectedEditableCount = 0;
    private int mSelectedCroppableCount = 0;
    private int mSelectedSetableCount = 0;
    private int mSelectedTrimmableCount = 0;
    private int mSelectedMuteableCount = 0;
    private ArrayList<Uri> mCachedShareableUris = null;

    public interface SelectedUriSource {
        ArrayList<Uri> getSelectedShareableUris();
    }

    public SelectionManager(Activity activity) {
        this.mActivity = activity;
        if (ApiHelper.AT_LEAST_16) {
            this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this.mActivity);
            this.mNfcAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent nfcEvent) {
                    if (SelectionManager.this.mCachedShareableUris == null) {
                        return null;
                    }
                    return (Uri[]) SelectionManager.this.mCachedShareableUris.toArray(new Uri[SelectionManager.this.mCachedShareableUris.size()]);
                }
            }, this.mActivity);
        }
    }

    public void setSelectedUriSource(SelectedUriSource selectedUriSource) {
        this.mUriSource = selectedUriSource;
    }

    public void onItemSelectedStateChanged(ShareActionProvider shareActionProvider, int i, int i2, boolean z) {
        int i3;
        if (!z) {
            i3 = -1;
        } else {
            i3 = 1;
        }
        this.mSelectedTotalCount += i3;
        this.mCachedShareableUris = null;
        if ((i2 & 1) > 0) {
            this.mSelectedDeletableCount += i3;
        }
        if ((i2 & 512) > 0) {
            this.mSelectedEditableCount += i3;
        }
        if ((i2 & 8) > 0) {
            this.mSelectedCroppableCount += i3;
        }
        if ((i2 & 32) > 0) {
            this.mSelectedSetableCount += i3;
        }
        if ((i2 & 2048) > 0) {
            this.mSelectedTrimmableCount += i3;
        }
        if ((65536 & i2) > 0) {
            this.mSelectedMuteableCount += i3;
        }
        if ((i2 & 4) > 0) {
            this.mSelectedShareableCount += i3;
            if (i == 1) {
                this.mSelectedShareableImageCount += i3;
            } else if (i == 3) {
                this.mSelectedShareableVideoCount += i3;
            }
        }
        this.mShareIntent.removeExtra("android.intent.extra.STREAM");
        if (this.mSelectedShareableCount != 0) {
            if (this.mSelectedShareableCount >= 1) {
                this.mCachedShareableUris = this.mUriSource.getSelectedShareableUris();
                if (this.mCachedShareableUris.size() == 0) {
                    this.mShareIntent.setAction(null).setType(null);
                } else {
                    if (this.mSelectedShareableImageCount == this.mSelectedShareableCount) {
                        this.mShareIntent.setType("image/*");
                    } else if (this.mSelectedShareableVideoCount == this.mSelectedShareableCount) {
                        this.mShareIntent.setType("video/*");
                    } else {
                        this.mShareIntent.setType("*/*");
                    }
                    if (this.mCachedShareableUris.size() == 1) {
                        this.mShareIntent.setAction("android.intent.action.SEND");
                        this.mShareIntent.putExtra("android.intent.extra.STREAM", this.mCachedShareableUris.get(0));
                    } else {
                        this.mShareIntent.setAction("android.intent.action.SEND_MULTIPLE");
                        this.mShareIntent.putExtra("android.intent.extra.STREAM", this.mCachedShareableUris);
                    }
                }
            }
        } else {
            this.mShareIntent.setAction(null).setType(null);
        }
        shareActionProvider.setShareIntent(this.mShareIntent);
    }

    public int getSupportedOperations() {
        if (this.mSelectedTotalCount == 0) {
            return 0;
        }
        if (this.mSelectedTotalCount == 1) {
            i = this.mSelectedCroppableCount == 1 ? 8 : 0;
            if (this.mSelectedEditableCount == 1) {
                i |= 512;
            }
            if (this.mSelectedSetableCount == 1) {
                i |= 32;
            }
            if (this.mSelectedTrimmableCount == 1) {
                i |= 2048;
            }
            if (this.mSelectedMuteableCount == 1) {
                i |= 65536;
            }
        }
        if (this.mSelectedDeletableCount == this.mSelectedTotalCount) {
            i |= 1;
        }
        if (this.mSelectedShareableCount > 0) {
            return i | 4;
        }
        return i;
    }

    public void onClearSelection() {
        this.mSelectedTotalCount = 0;
        this.mSelectedShareableCount = 0;
        this.mSelectedShareableImageCount = 0;
        this.mSelectedShareableVideoCount = 0;
        this.mSelectedDeletableCount = 0;
        this.mSelectedEditableCount = 0;
        this.mSelectedCroppableCount = 0;
        this.mSelectedSetableCount = 0;
        this.mSelectedTrimmableCount = 0;
        this.mSelectedMuteableCount = 0;
        this.mCachedShareableUris = null;
        this.mShareIntent.removeExtra("android.intent.extra.STREAM");
        this.mShareIntent.setAction(null).setType(null);
    }
}
