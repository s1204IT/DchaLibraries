package com.android.gallery3d.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.android.gallery3d.common.BlobCache;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReverseGeocoder {
    private static Address sCurrentAddress;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private BlobCache mGeoCache;
    private Geocoder mGeocoder;

    public static class SetLatLong {
        public double mMaxLatLongitude;
        public double mMaxLonLatitude;
        public double mMinLatLongitude;
        public double mMinLonLatitude;
        public double mMinLatLatitude = 90.0d;
        public double mMaxLatLatitude = -90.0d;
        public double mMinLonLongitude = 180.0d;
        public double mMaxLonLongitude = -180.0d;
    }

    public ReverseGeocoder(Context context) {
        this.mContext = context;
        this.mGeocoder = new Geocoder(this.mContext);
        this.mGeoCache = CacheManager.getCache(context, "rev_geocoding", 1000, 512000, 0);
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
    }

    public String computeAddress(SetLatLong setLatLong, boolean z) {
        double d;
        String str;
        String str2;
        String str3;
        LocationManager locationManager;
        Location lastKnownLocation;
        double d2 = setLatLong.mMinLatLatitude;
        double d3 = setLatLong.mMinLatLongitude;
        double d4 = setLatLong.mMaxLatLatitude;
        double d5 = setLatLong.mMaxLatLongitude;
        if (Math.abs(setLatLong.mMaxLatLatitude - setLatLong.mMinLatLatitude) < Math.abs(setLatLong.mMaxLonLongitude - setLatLong.mMinLonLongitude)) {
            d2 = setLatLong.mMinLonLatitude;
            d3 = setLatLong.mMinLonLongitude;
            d4 = setLatLong.mMaxLonLatitude;
            d5 = setLatLong.mMaxLonLongitude;
        }
        double d6 = d2;
        double d7 = d3;
        double d8 = d4;
        double d9 = d5;
        Address addressLookupAddress = lookupAddress(d6, d7, z);
        Address addressLookupAddress2 = lookupAddress(d8, d9, z);
        Address address = addressLookupAddress == null ? addressLookupAddress2 : addressLookupAddress;
        Address address2 = addressLookupAddress2 == null ? address : addressLookupAddress2;
        if (address == null || address2 == null) {
            return null;
        }
        LocationManager locationManager2 = (LocationManager) this.mContext.getSystemService("location");
        List<String> allProviders = locationManager2.getAllProviders();
        Location location = null;
        int i = 0;
        while (i < allProviders.size()) {
            String str4 = allProviders.get(i);
            if (str4 == null) {
                locationManager = locationManager2;
                lastKnownLocation = null;
            } else {
                try {
                    lastKnownLocation = locationManager2.getLastKnownLocation(str4);
                    locationManager = locationManager2;
                } catch (SecurityException e) {
                    locationManager = locationManager2;
                    Log.d("Gallery2/ReverseGeocoder", "<computeAddress> SecurityException", e);
                }
            }
            location = lastKnownLocation;
            if (location != null) {
                break;
            }
            i++;
            locationManager2 = locationManager;
        }
        String strCheckNull = "";
        String strCheckNull2 = "";
        String country = Locale.getDefault().getCountry();
        if (location != null) {
            d = d8;
            Address addressLookupAddress3 = lookupAddress(location.getLatitude(), location.getLongitude(), z);
            if (addressLookupAddress3 == null) {
                addressLookupAddress3 = sCurrentAddress;
            } else {
                sCurrentAddress = addressLookupAddress3;
            }
            if (addressLookupAddress3 != null && addressLookupAddress3.getCountryCode() != null) {
                strCheckNull = checkNull(addressLookupAddress3.getLocality());
                country = checkNull(addressLookupAddress3.getCountryCode());
                strCheckNull2 = checkNull(addressLookupAddress3.getAdminArea());
            }
        } else {
            d = d8;
        }
        String str5 = strCheckNull2;
        String str6 = country;
        String strCheckNull3 = checkNull(address.getLocality());
        String strCheckNull4 = checkNull(address2.getLocality());
        String strCheckNull5 = checkNull(address.getAdminArea());
        String strCheckNull6 = checkNull(address2.getAdminArea());
        String strCheckNull7 = checkNull(address.getCountryCode());
        String strCheckNull8 = checkNull(address2.getCountryCode());
        if (strCheckNull.equals(strCheckNull3) || strCheckNull.equals(strCheckNull4)) {
            if (strCheckNull.equals(strCheckNull3)) {
                if (strCheckNull4.length() == 0) {
                    if (!str6.equals(strCheckNull8)) {
                        strCheckNull4 = strCheckNull6 + " " + strCheckNull8;
                    } else {
                        strCheckNull4 = strCheckNull6;
                    }
                }
                String str7 = strCheckNull4;
                strCheckNull4 = strCheckNull3;
                strCheckNull3 = str7;
            } else {
                if (strCheckNull3.length() == 0) {
                    if (!str6.equals(strCheckNull7)) {
                        strCheckNull3 = strCheckNull5 + " " + strCheckNull7;
                    } else {
                        strCheckNull3 = strCheckNull5;
                    }
                }
                strCheckNull5 = strCheckNull6;
                strCheckNull7 = strCheckNull8;
            }
            String str8 = strCheckNull4;
            String strValueIfEqual = valueIfEqual(address.getAddressLine(0), address2.getAddressLine(0));
            if (strValueIfEqual != null && !"null".equals(strValueIfEqual)) {
                if (!strCheckNull.equals(strCheckNull3)) {
                    return strValueIfEqual + " - " + strCheckNull3;
                }
                return strValueIfEqual;
            }
            String strValueIfEqual2 = valueIfEqual(address.getThoroughfare(), address2.getThoroughfare());
            if (strValueIfEqual2 != null && !"null".equals(strValueIfEqual2)) {
                return strValueIfEqual2;
            }
            str = strCheckNull5;
            str2 = strCheckNull7;
            strCheckNull3 = str8;
            strCheckNull4 = strCheckNull3;
        } else {
            str2 = strCheckNull7;
            strCheckNull7 = strCheckNull8;
            str = strCheckNull6;
        }
        String strValueIfEqual3 = valueIfEqual(strCheckNull3, strCheckNull4);
        if (strValueIfEqual3 != null) {
            str3 = strCheckNull7;
            if (!"".equals(strValueIfEqual3)) {
                if (strCheckNull5 != null && strCheckNull5.length() > 0) {
                    if (!str2.equals(str6)) {
                        return strValueIfEqual3 + ", " + strCheckNull5 + " " + str2;
                    }
                    return strValueIfEqual3 + ", " + strCheckNull5;
                }
                return strValueIfEqual3;
            }
        } else {
            str3 = strCheckNull7;
        }
        if (str5.equals(strCheckNull5) && str5.equals(str)) {
            if ("".equals(strCheckNull3)) {
                strCheckNull3 = strCheckNull4;
            }
            if ("".equals(strCheckNull4)) {
                strCheckNull4 = strCheckNull3;
            }
            if (!"".equals(strCheckNull3)) {
                if (strCheckNull3.equals(strCheckNull4)) {
                    return strCheckNull3 + ", " + str5;
                }
                return strCheckNull3 + " - " + strCheckNull4;
            }
        }
        String str9 = str;
        String str10 = str3;
        Location.distanceBetween(d6, d7, d, d9, new float[1]);
        if (((int) GalleryUtils.toMile(r3[0])) < 20) {
            String localityAdminForAddress = getLocalityAdminForAddress(address, true);
            if (localityAdminForAddress != null) {
                return localityAdminForAddress;
            }
            String localityAdminForAddress2 = getLocalityAdminForAddress(address2, true);
            if (localityAdminForAddress2 != null) {
                return localityAdminForAddress2;
            }
        }
        String strValueIfEqual4 = valueIfEqual(strCheckNull5, str9);
        if (strValueIfEqual4 != null && !"".equals(strValueIfEqual4)) {
            if (!str2.equals(str6) && str2 != null && str2.length() > 0) {
                return strValueIfEqual4 + " " + str2;
            }
            return strValueIfEqual4;
        }
        String strValueIfEqual5 = valueIfEqual(str2, str10);
        if (strValueIfEqual5 != null && !"".equals(strValueIfEqual5)) {
            return strValueIfEqual5;
        }
        String countryName = address.getCountryName();
        String countryName2 = address2.getCountryName();
        if (countryName == null) {
            countryName = str2;
        }
        if (countryName2 == null) {
            countryName2 = str10;
        }
        if (countryName == null || countryName2 == null) {
            return null;
        }
        if (countryName.length() > 8 || countryName2.length() > 8) {
            return str2 + " - " + str10;
        }
        return countryName + " - " + countryName2;
    }

    private String checkNull(String str) {
        if (str == null || str.equals("null")) {
            return "";
        }
        return str;
    }

    private String getLocalityAdminForAddress(Address address, boolean z) {
        if (address == null) {
            return "";
        }
        String locality = address.getLocality();
        if (locality != null && !"null".equals(locality)) {
            String adminArea = address.getAdminArea();
            if (adminArea != null && adminArea.length() > 0) {
                return locality + ", " + adminArea;
            }
            return locality;
        }
        return null;
    }

    public Address lookupAddress(double d, double d2, boolean z) {
        byte[] bArrLookup;
        Locale locale;
        long j = (long) ((((d + 90.0d) * 2.0d * 90.0d) + d2 + 180.0d) * 6378137.0d);
        if (z) {
            try {
                if (this.mGeoCache != null) {
                    bArrLookup = this.mGeoCache.lookup(j);
                } else {
                    bArrLookup = null;
                }
            } catch (Exception e) {
                return null;
            }
        }
        NetworkInfo activeNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        int i = 0;
        if (bArrLookup != null && bArrLookup.length != 0) {
            DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bArrLookup));
            String utf = readUTF(dataInputStream);
            String utf2 = readUTF(dataInputStream);
            String utf3 = readUTF(dataInputStream);
            if (utf == null) {
                locale = null;
            } else if (utf2 == null) {
                locale = new Locale(utf);
            } else if (utf3 == null) {
                locale = new Locale(utf, utf2);
            } else {
                locale = new Locale(utf, utf2, utf3);
            }
            if (!locale.getLanguage().equals(Locale.getDefault().getLanguage())) {
                dataInputStream.close();
                return lookupAddress(d, d2, false);
            }
            Address address = new Address(locale);
            address.setThoroughfare(readUTF(dataInputStream));
            int i2 = dataInputStream.readInt();
            while (i < i2) {
                address.setAddressLine(i, readUTF(dataInputStream));
                i++;
            }
            address.setFeatureName(readUTF(dataInputStream));
            address.setLocality(readUTF(dataInputStream));
            address.setAdminArea(readUTF(dataInputStream));
            address.setSubAdminArea(readUTF(dataInputStream));
            address.setCountryName(readUTF(dataInputStream));
            address.setCountryCode(readUTF(dataInputStream));
            address.setPostalCode(readUTF(dataInputStream));
            address.setPhone(readUTF(dataInputStream));
            address.setUrl(readUTF(dataInputStream));
            dataInputStream.close();
            return address;
        }
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            List<Address> fromLocation = this.mGeocoder.getFromLocation(d, d2, 1);
            if (fromLocation.isEmpty()) {
                return null;
            }
            Address address2 = fromLocation.get(0);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            Locale locale2 = address2.getLocale();
            writeUTF(dataOutputStream, locale2.getLanguage());
            writeUTF(dataOutputStream, locale2.getCountry());
            writeUTF(dataOutputStream, locale2.getVariant());
            writeUTF(dataOutputStream, address2.getThoroughfare());
            int maxAddressLineIndex = address2.getMaxAddressLineIndex();
            dataOutputStream.writeInt(maxAddressLineIndex);
            while (i < maxAddressLineIndex) {
                writeUTF(dataOutputStream, address2.getAddressLine(i));
                i++;
            }
            writeUTF(dataOutputStream, address2.getFeatureName());
            writeUTF(dataOutputStream, address2.getLocality());
            writeUTF(dataOutputStream, address2.getAdminArea());
            writeUTF(dataOutputStream, address2.getSubAdminArea());
            writeUTF(dataOutputStream, address2.getCountryName());
            writeUTF(dataOutputStream, address2.getCountryCode());
            writeUTF(dataOutputStream, address2.getPostalCode());
            writeUTF(dataOutputStream, address2.getPhone());
            writeUTF(dataOutputStream, address2.getUrl());
            dataOutputStream.flush();
            if (this.mGeoCache != null) {
                this.mGeoCache.insert(j, byteArrayOutputStream.toByteArray());
            }
            dataOutputStream.close();
            return address2;
        }
        return null;
    }

    private String valueIfEqual(String str, String str2) {
        if (str == null || str2 == null || !str.equalsIgnoreCase(str2)) {
            return null;
        }
        return str;
    }

    public static final void writeUTF(DataOutputStream dataOutputStream, String str) throws IOException {
        if (str == null) {
            dataOutputStream.writeUTF("");
        } else {
            dataOutputStream.writeUTF(str);
        }
    }

    public static final String readUTF(DataInputStream dataInputStream) throws IOException {
        String utf = dataInputStream.readUTF();
        if (utf.length() == 0) {
            return null;
        }
        return utf;
    }
}
