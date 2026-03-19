package android.text.style;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Parcel;
import android.provider.Browser;
import android.text.ParcelableSpan;
import android.util.Log;
import android.view.View;

public class URLSpan extends ClickableSpan implements ParcelableSpan {
    private final String mURL;

    public URLSpan(String str) {
        this.mURL = str;
    }

    public URLSpan(Parcel parcel) {
        this.mURL = parcel.readString();
    }

    public int getSpanTypeId() {
        return getSpanTypeIdInternal();
    }

    public int getSpanTypeIdInternal() {
        return 11;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        writeToParcelInternal(parcel, i);
    }

    public void writeToParcelInternal(Parcel parcel, int i) {
        parcel.writeString(this.mURL);
    }

    public String getURL() {
        return this.mURL;
    }

    @Override
    public void onClick(View view) {
        Uri uri = Uri.parse(getURL());
        Context context = view.getContext();
        Intent intent = new Intent("android.intent.action.VIEW", uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            if (BenesseExtension.getDchaState() == 0) {
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            Log.w("URLSpan", "Actvity was not found for intent, " + intent.toString());
        }
    }
}
