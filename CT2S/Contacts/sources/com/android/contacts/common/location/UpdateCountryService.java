package com.android.contacts.common.location;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.IOException;
import java.util.List;

public class UpdateCountryService extends IntentService {
    private static final String TAG = UpdateCountryService.class.getSimpleName();

    public UpdateCountryService() {
        super(TAG);
    }

    public static void updateCountry(Context context, Location location) {
        Intent serviceIntent = new Intent(context, (Class<?>) UpdateCountryService.class);
        serviceIntent.setAction("saveCountry");
        serviceIntent.putExtra("location", location);
        context.startService(serviceIntent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "onHandleIntent: could not handle null intent");
            return;
        }
        if ("saveCountry".equals(intent.getAction())) {
            Location location = (Location) intent.getParcelableExtra("location");
            String country = getCountryFromLocation(getApplicationContext(), location);
            if (country != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("preference_time_updated", System.currentTimeMillis());
                editor.putString("preference_current_country", country);
                editor.commit();
            }
        }
    }

    private String getCountryFromLocation(Context context, Location location) {
        Geocoder geocoder = new Geocoder(context);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses == null || addresses.size() <= 0) {
                return null;
            }
            String country = addresses.get(0).getCountryCode();
            return country;
        } catch (IOException e) {
            Log.w(TAG, "Exception occurred when getting geocoded country from location");
            return null;
        }
    }
}
