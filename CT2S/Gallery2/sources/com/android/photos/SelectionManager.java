package com.android.photos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.support.v4.app.NotificationCompat;
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
                public Uri[] createBeamUris(NfcEvent arg0) {
                    if (SelectionManager.this.mCachedShareableUris == null) {
                        return null;
                    }
                    return (Uri[]) SelectionManager.this.mCachedShareableUris.toArray(new Uri[SelectionManager.this.mCachedShareableUris.size()]);
                }
            }, this.mActivity);
        }
    }

    public void setSelectedUriSource(SelectedUriSource source) {
        this.mUriSource = source;
    }

    public void onItemSelectedStateChanged(ShareActionProvider share, int itemType, int itemSupportedOperations, boolean selected) {
        int increment = selected ? 1 : -1;
        this.mSelectedTotalCount += increment;
        this.mCachedShareableUris = null;
        if ((itemSupportedOperations & 1) > 0) {
            this.mSelectedDeletableCount += increment;
        }
        if ((itemSupportedOperations & NotificationCompat.FLAG_GROUP_SUMMARY) > 0) {
            this.mSelectedEditableCount += increment;
        }
        if ((itemSupportedOperations & 8) > 0) {
            this.mSelectedCroppableCount += increment;
        }
        if ((itemSupportedOperations & 32) > 0) {
            this.mSelectedSetableCount += increment;
        }
        if ((itemSupportedOperations & 2048) > 0) {
            this.mSelectedTrimmableCount += increment;
        }
        if ((65536 & itemSupportedOperations) > 0) {
            this.mSelectedMuteableCount += increment;
        }
        if ((itemSupportedOperations & 4) > 0) {
            this.mSelectedShareableCount += increment;
            if (itemType == 1) {
                this.mSelectedShareableImageCount += increment;
            } else if (itemType == 3) {
                this.mSelectedShareableVideoCount += increment;
            }
        }
        this.mShareIntent.removeExtra("android.intent.extra.STREAM");
        if (this.mSelectedShareableCount == 0) {
            this.mShareIntent.setAction(null).setType(null);
        } else if (this.mSelectedShareableCount >= 1) {
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
        share.setShareIntent(this.mShareIntent);
    }

    public int getSupportedOperations() {
        if (this.mSelectedTotalCount == 0) {
            return 0;
        }
        int supported = 0;
        if (this.mSelectedTotalCount == 1) {
            if (this.mSelectedCroppableCount == 1) {
                supported = 0 | 8;
            }
            if (this.mSelectedEditableCount == 1) {
                supported |= NotificationCompat.FLAG_GROUP_SUMMARY;
            }
            if (this.mSelectedSetableCount == 1) {
                supported |= 32;
            }
            if (this.mSelectedTrimmableCount == 1) {
                supported |= 2048;
            }
            if (this.mSelectedMuteableCount == 1) {
                supported |= 65536;
            }
        }
        if (this.mSelectedDeletableCount == this.mSelectedTotalCount) {
            supported |= 1;
        }
        if (this.mSelectedShareableCount > 0) {
            return supported | 4;
        }
        return supported;
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
