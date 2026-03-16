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

    public String computeAddress(SetLatLong set) {
        String otherCity;
        double setMinLatitude = set.mMinLatLatitude;
        double setMinLongitude = set.mMinLatLongitude;
        double setMaxLatitude = set.mMaxLatLatitude;
        double setMaxLongitude = set.mMaxLatLongitude;
        if (Math.abs(set.mMaxLatLatitude - set.mMinLatLatitude) < Math.abs(set.mMaxLonLongitude - set.mMinLonLongitude)) {
            setMinLatitude = set.mMinLonLatitude;
            setMinLongitude = set.mMinLonLongitude;
            setMaxLatitude = set.mMaxLonLatitude;
            setMaxLongitude = set.mMaxLonLongitude;
        }
        Address addr1 = lookupAddress(setMinLatitude, setMinLongitude, true);
        Address addr2 = lookupAddress(setMaxLatitude, setMaxLongitude, true);
        if (addr1 == null) {
            addr1 = addr2;
        }
        if (addr2 == null) {
            addr2 = addr1;
        }
        if (addr1 == null || addr2 == null) {
            return null;
        }
        LocationManager locationManager = (LocationManager) this.mContext.getSystemService("location");
        Location location = null;
        List<String> providers = locationManager.getAllProviders();
        for (int i = 0; i < providers.size(); i++) {
            String provider = providers.get(i);
            location = provider != null ? locationManager.getLastKnownLocation(provider) : null;
            if (location != null) {
                break;
            }
        }
        String currentCity = "";
        String currentAdminArea = "";
        String currentCountry = Locale.getDefault().getCountry();
        if (location != null) {
            Address currentAddress = lookupAddress(location.getLatitude(), location.getLongitude(), true);
            if (currentAddress == null) {
                currentAddress = sCurrentAddress;
            } else {
                sCurrentAddress = currentAddress;
            }
            if (currentAddress != null && currentAddress.getCountryCode() != null) {
                currentCity = checkNull(currentAddress.getLocality());
                currentCountry = checkNull(currentAddress.getCountryCode());
                currentAdminArea = checkNull(currentAddress.getAdminArea());
            }
        }
        String addr1Locality = checkNull(addr1.getLocality());
        String addr2Locality = checkNull(addr2.getLocality());
        String addr1AdminArea = checkNull(addr1.getAdminArea());
        String addr2AdminArea = checkNull(addr2.getAdminArea());
        String addr1CountryCode = checkNull(addr1.getCountryCode());
        String addr2CountryCode = checkNull(addr2.getCountryCode());
        if (currentCity.equals(addr1Locality) || currentCity.equals(addr2Locality)) {
            if (currentCity.equals(addr1Locality)) {
                otherCity = addr2Locality;
                if (otherCity.length() == 0) {
                    otherCity = addr2AdminArea;
                    if (!currentCountry.equals(addr2CountryCode)) {
                        otherCity = otherCity + " " + addr2CountryCode;
                    }
                }
                addr2Locality = addr1Locality;
                addr2AdminArea = addr1AdminArea;
                addr2CountryCode = addr1CountryCode;
            } else {
                otherCity = addr1Locality;
                if (otherCity.length() == 0) {
                    otherCity = addr1AdminArea;
                    if (!currentCountry.equals(addr1CountryCode)) {
                        otherCity = otherCity + " " + addr1CountryCode;
                    }
                }
                addr1Locality = addr2Locality;
                addr1AdminArea = addr2AdminArea;
                addr1CountryCode = addr2CountryCode;
            }
            String closestCommonLocation = valueIfEqual(addr1.getAddressLine(0), addr2.getAddressLine(0));
            if (closestCommonLocation != null && !"null".equals(closestCommonLocation)) {
                if (!currentCity.equals(otherCity)) {
                    return closestCommonLocation + " - " + otherCity;
                }
                return closestCommonLocation;
            }
            String closestCommonLocation2 = valueIfEqual(addr1.getThoroughfare(), addr2.getThoroughfare());
            if (closestCommonLocation2 != null && !"null".equals(closestCommonLocation2)) {
                return closestCommonLocation2;
            }
        }
        String closestCommonLocation3 = valueIfEqual(addr1Locality, addr2Locality);
        if (closestCommonLocation3 != null && !"".equals(closestCommonLocation3)) {
            String adminArea = addr1AdminArea;
            String countryCode = addr1CountryCode;
            if (adminArea != null && adminArea.length() > 0) {
                if (!countryCode.equals(currentCountry)) {
                    return closestCommonLocation3 + ", " + adminArea + " " + countryCode;
                }
                return closestCommonLocation3 + ", " + adminArea;
            }
            return closestCommonLocation3;
        }
        if (currentAdminArea.equals(addr1AdminArea) && currentAdminArea.equals(addr2AdminArea)) {
            if ("".equals(addr1Locality)) {
                addr1Locality = addr2Locality;
            }
            if ("".equals(addr2Locality)) {
                addr2Locality = addr1Locality;
            }
            if (!"".equals(addr1Locality)) {
                if (addr1Locality.equals(addr2Locality)) {
                    return addr1Locality + ", " + currentAdminArea;
                }
                return addr1Locality + " - " + addr2Locality;
            }
        }
        float[] distanceFloat = new float[1];
        Location.distanceBetween(setMinLatitude, setMinLongitude, setMaxLatitude, setMaxLongitude, distanceFloat);
        int distance = (int) GalleryUtils.toMile(distanceFloat[0]);
        if (distance < 20) {
            String closestCommonLocation4 = getLocalityAdminForAddress(addr1, true);
            if (closestCommonLocation4 == null) {
                String closestCommonLocation5 = getLocalityAdminForAddress(addr2, true);
                if (closestCommonLocation5 != null) {
                    return closestCommonLocation5;
                }
            } else {
                return closestCommonLocation4;
            }
        }
        String closestCommonLocation6 = valueIfEqual(addr1AdminArea, addr2AdminArea);
        if (closestCommonLocation6 != null && !"".equals(closestCommonLocation6)) {
            String countryCode2 = addr1CountryCode;
            if (!countryCode2.equals(currentCountry) && countryCode2 != null && countryCode2.length() > 0) {
                return closestCommonLocation6 + " " + countryCode2;
            }
            return closestCommonLocation6;
        }
        String closestCommonLocation7 = valueIfEqual(addr1CountryCode, addr2CountryCode);
        if (closestCommonLocation7 == null || "".equals(closestCommonLocation7)) {
            String addr1Country = addr1.getCountryName();
            String addr2Country = addr2.getCountryName();
            if (addr1Country == null) {
                addr1Country = addr1CountryCode;
            }
            if (addr2Country == null) {
                addr2Country = addr2CountryCode;
            }
            if (addr1Country == null || addr2Country == null) {
                return null;
            }
            if (addr1Country.length() > 8 || addr2Country.length() > 8) {
                return addr1CountryCode + " - " + addr2CountryCode;
            }
            return addr1Country + " - " + addr2Country;
        }
        return closestCommonLocation7;
    }

    private String checkNull(String locality) {
        if (locality == null || locality.equals("null")) {
            return "";
        }
        return locality;
    }

    private String getLocalityAdminForAddress(Address addr, boolean approxLocation) {
        if (addr == null) {
            return "";
        }
        String localityAdminStr = addr.getLocality();
        if (localityAdminStr != null && !"null".equals(localityAdminStr)) {
            if (approxLocation) {
            }
            String adminArea = addr.getAdminArea();
            if (adminArea != null && adminArea.length() > 0) {
                return localityAdminStr + ", " + adminArea;
            }
            return localityAdminStr;
        }
        return null;
    }

    public Address lookupAddress(double latitude, double longitude, boolean useCache) {
        long locationKey = (long) ((((90.0d + latitude) * 2.0d * 90.0d) + 180.0d + longitude) * 6378137.0d);
        byte[] cachedLocation = null;
        if (useCache) {
            try {
                if (this.mGeoCache != null) {
                    cachedLocation = this.mGeoCache.lookup(locationKey);
                }
            } catch (Exception e) {
                return null;
            }
        }
        NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (cachedLocation == null || cachedLocation.length == 0) {
            if (networkInfo == null || !networkInfo.isConnected()) {
                return null;
            }
            List<Address> addresses = this.mGeocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.isEmpty()) {
                return null;
            }
            Address address = addresses.get(0);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            Locale locale = address.getLocale();
            writeUTF(dos, locale.getLanguage());
            writeUTF(dos, locale.getCountry());
            writeUTF(dos, locale.getVariant());
            writeUTF(dos, address.getThoroughfare());
            int numAddressLines = address.getMaxAddressLineIndex();
            dos.writeInt(numAddressLines);
            for (int i = 0; i < numAddressLines; i++) {
                writeUTF(dos, address.getAddressLine(i));
            }
            writeUTF(dos, address.getFeatureName());
            writeUTF(dos, address.getLocality());
            writeUTF(dos, address.getAdminArea());
            writeUTF(dos, address.getSubAdminArea());
            writeUTF(dos, address.getCountryName());
            writeUTF(dos, address.getCountryCode());
            writeUTF(dos, address.getPostalCode());
            writeUTF(dos, address.getPhone());
            writeUTF(dos, address.getUrl());
            dos.flush();
            if (this.mGeoCache != null) {
                this.mGeoCache.insert(locationKey, bos.toByteArray());
            }
            dos.close();
            return address;
        }
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(cachedLocation));
        String language = readUTF(dis);
        String country = readUTF(dis);
        String variant = readUTF(dis);
        Locale locale2 = null;
        if (language != null) {
            if (country == null) {
                locale2 = new Locale(language);
            } else if (variant == null) {
                locale2 = new Locale(language, country);
            } else {
                locale2 = new Locale(language, country, variant);
            }
        }
        if (!locale2.getLanguage().equals(Locale.getDefault().getLanguage())) {
            dis.close();
            return lookupAddress(latitude, longitude, false);
        }
        Address address2 = new Address(locale2);
        address2.setThoroughfare(readUTF(dis));
        int numAddressLines2 = dis.readInt();
        for (int i2 = 0; i2 < numAddressLines2; i2++) {
            address2.setAddressLine(i2, readUTF(dis));
        }
        address2.setFeatureName(readUTF(dis));
        address2.setLocality(readUTF(dis));
        address2.setAdminArea(readUTF(dis));
        address2.setSubAdminArea(readUTF(dis));
        address2.setCountryName(readUTF(dis));
        address2.setCountryCode(readUTF(dis));
        address2.setPostalCode(readUTF(dis));
        address2.setPhone(readUTF(dis));
        address2.setUrl(readUTF(dis));
        dis.close();
        return address2;
    }

    private String valueIfEqual(String a, String b) {
        if (a == null || b == null || !a.equalsIgnoreCase(b)) {
            return null;
        }
        return a;
    }

    public static final void writeUTF(DataOutputStream dos, String string) throws IOException {
        if (string == null) {
            dos.writeUTF("");
        } else {
            dos.writeUTF(string);
        }
    }

    public static final String readUTF(DataInputStream dis) throws IOException {
        String retVal = dis.readUTF();
        if (retVal.length() == 0) {
            return null;
        }
        return retVal;
    }
}
