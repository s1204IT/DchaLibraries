package android.mtp;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.mtp.MtpStorageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import java.util.ArrayList;

class MtpPropertyGroup {
    private static final String PATH_WHERE = "_data=?";
    private static final String TAG = MtpPropertyGroup.class.getSimpleName();
    private String[] mColumns;
    private final Property[] mProperties;
    private final ContentProviderClient mProvider;
    private final Uri mUri;
    private final String mVolumeName;

    private native String format_date_time(long j);

    private class Property {
        int code;
        int column;
        int type;

        Property(int i, int i2, int i3) {
            this.code = i;
            this.type = i2;
            this.column = i3;
        }
    }

    public MtpPropertyGroup(ContentProviderClient contentProviderClient, String str, int[] iArr) {
        this.mProvider = contentProviderClient;
        this.mVolumeName = str;
        this.mUri = MediaStore.Files.getMtpObjectsUri(str);
        int length = iArr.length;
        ArrayList<String> arrayList = new ArrayList<>(length);
        arrayList.add("_id");
        this.mProperties = new Property[length];
        for (int i = 0; i < length; i++) {
            this.mProperties[i] = createProperty(iArr[i], arrayList);
        }
        int size = arrayList.size();
        this.mColumns = new String[size];
        for (int i2 = 0; i2 < size; i2++) {
            this.mColumns[i2] = arrayList.get(i2);
        }
    }

    private Property createProperty(int i, ArrayList<String> arrayList) {
        int i2 = 4;
        int i3 = 65535;
        String str = null;
        switch (i) {
            case MtpConstants.PROPERTY_STORAGE_ID:
            case MtpConstants.PROPERTY_PARENT_OBJECT:
            case MtpConstants.PROPERTY_SAMPLE_RATE:
            case MtpConstants.PROPERTY_AUDIO_WAVE_CODEC:
            case MtpConstants.PROPERTY_AUDIO_BITRATE:
                i3 = 6;
                break;
            case MtpConstants.PROPERTY_OBJECT_FORMAT:
            case MtpConstants.PROPERTY_PROTECTION_STATUS:
            case MtpConstants.PROPERTY_BITRATE_TYPE:
            case MtpConstants.PROPERTY_NUMBER_OF_CHANNELS:
                i3 = i2;
                break;
            case MtpConstants.PROPERTY_OBJECT_SIZE:
                i2 = 8;
                i3 = i2;
                break;
            case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
            case MtpConstants.PROPERTY_DATE_MODIFIED:
            case MtpConstants.PROPERTY_NAME:
            case MtpConstants.PROPERTY_ARTIST:
            case MtpConstants.PROPERTY_DATE_ADDED:
            case MtpConstants.PROPERTY_GENRE:
            case MtpConstants.PROPERTY_ALBUM_NAME:
            case MtpConstants.PROPERTY_DISPLAY_NAME:
                break;
            case MtpConstants.PROPERTY_PERSISTENT_UID:
                i2 = 10;
                i3 = i2;
                break;
            case MtpConstants.PROPERTY_DESCRIPTION:
                str = "description";
                break;
            case MtpConstants.PROPERTY_DURATION:
                str = "duration";
                i3 = 6;
                break;
            case MtpConstants.PROPERTY_TRACK:
                str = MediaStore.Audio.AudioColumns.TRACK;
                i3 = i2;
                break;
            case MtpConstants.PROPERTY_COMPOSER:
                str = MediaStore.Audio.AudioColumns.COMPOSER;
                break;
            case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE:
                str = MediaStore.Audio.AudioColumns.YEAR;
                break;
            case MtpConstants.PROPERTY_ALBUM_ARTIST:
                str = MediaStore.Audio.AudioColumns.ALBUM_ARTIST;
                break;
            default:
                i2 = 0;
                Log.e(TAG, "unsupported property " + i);
                i3 = i2;
                break;
        }
        if (str != null) {
            arrayList.add(str);
            return new Property(i, i3, arrayList.size() - 1);
        }
        return new Property(i, i3, -1);
    }

    private String queryAudio(String str, String str2) throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQuery = this.mProvider.query(MediaStore.Audio.Media.getContentUri(this.mVolumeName), new String[]{str2}, PATH_WHERE, new String[]{str}, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        String string = cursorQuery.getString(0);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return string;
                    }
                } catch (Exception e) {
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    return "";
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return "";
        } catch (Exception e2) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private String queryGenre(String str) throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQuery = this.mProvider.query(MediaStore.Audio.Genres.getContentUri(this.mVolumeName), new String[]{"name"}, PATH_WHERE, new String[]{str}, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        String string = cursorQuery.getString(0);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return string;
                    }
                } catch (Exception e) {
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    return "";
                } catch (Throwable th) {
                    cursor = cursorQuery;
                    th = th;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return "";
        } catch (Exception e2) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public int getPropertyList(MtpStorageManager.MtpObject mtpObject, MtpPropertyList mtpPropertyList) throws Throwable {
        Cursor cursor;
        int i;
        int i2;
        long id;
        int id2 = mtpObject.getId();
        String string = mtpObject.getPath().toString();
        Property[] propertyArr = this.mProperties;
        int length = propertyArr.length;
        Cursor cursor2 = null;
        int i3 = 0;
        while (i3 < length) {
            Property property = propertyArr[i3];
            if (property.column != -1 && cursor2 == null) {
                try {
                    Cursor cursorQuery = this.mProvider.query(this.mUri, this.mColumns, PATH_WHERE, new String[]{string}, null, null);
                    if (cursorQuery != null) {
                        try {
                            if (!cursorQuery.moveToNext()) {
                                cursorQuery.close();
                                cursorQuery = null;
                            }
                        } catch (RemoteException e) {
                            cursor2 = cursorQuery;
                            Log.e(TAG, "Mediaprovider lookup failed");
                            cursor = cursor2;
                        }
                    }
                    cursor = cursorQuery;
                } catch (RemoteException e2) {
                }
            } else {
                cursor = cursor2;
            }
            switch (property.code) {
                case MtpConstants.PROPERTY_STORAGE_ID:
                    mtpPropertyList.append(id2, property.code, property.type, mtpObject.getStorageId());
                    break;
                case MtpConstants.PROPERTY_OBJECT_FORMAT:
                    mtpPropertyList.append(id2, property.code, property.type, mtpObject.getFormat());
                    break;
                case MtpConstants.PROPERTY_PROTECTION_STATUS:
                    mtpPropertyList.append(id2, property.code, property.type, 0L);
                    break;
                case MtpConstants.PROPERTY_OBJECT_SIZE:
                    mtpPropertyList.append(id2, property.code, property.type, mtpObject.getSize());
                    break;
                case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                case MtpConstants.PROPERTY_NAME:
                case MtpConstants.PROPERTY_DISPLAY_NAME:
                    mtpPropertyList.append(id2, property.code, mtpObject.getName());
                    break;
                case MtpConstants.PROPERTY_DATE_MODIFIED:
                case MtpConstants.PROPERTY_DATE_ADDED:
                    mtpPropertyList.append(id2, property.code, format_date_time(mtpObject.getModifiedTime()));
                    break;
                case MtpConstants.PROPERTY_PARENT_OBJECT:
                    int i4 = property.code;
                    int i5 = property.type;
                    if (mtpObject.getParent().isRoot()) {
                        id = 0;
                    } else {
                        id = mtpObject.getParent().getId();
                    }
                    mtpPropertyList.append(id2, i4, i5, id);
                    break;
                case MtpConstants.PROPERTY_PERSISTENT_UID:
                    mtpPropertyList.append(id2, property.code, property.type, mtpObject.getModifiedTime() + ((long) (mtpObject.getPath().toString().hashCode() << 32)));
                    break;
                case MtpConstants.PROPERTY_ARTIST:
                    mtpPropertyList.append(id2, property.code, queryAudio(string, "artist"));
                    break;
                case MtpConstants.PROPERTY_TRACK:
                    if (cursor != null) {
                        i = cursor.getInt(property.column);
                    } else {
                        i = 0;
                    }
                    mtpPropertyList.append(id2, property.code, 4, i % 1000);
                    break;
                case MtpConstants.PROPERTY_GENRE:
                    String strQueryGenre = queryGenre(string);
                    if (strQueryGenre != null) {
                        mtpPropertyList.append(id2, property.code, strQueryGenre);
                    }
                    break;
                case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE:
                    if (cursor != null) {
                        i2 = cursor.getInt(property.column);
                    } else {
                        i2 = 0;
                    }
                    mtpPropertyList.append(id2, property.code, Integer.toString(i2) + "0101T000000");
                    break;
                case MtpConstants.PROPERTY_ALBUM_NAME:
                    mtpPropertyList.append(id2, property.code, queryAudio(string, "album"));
                    break;
                case MtpConstants.PROPERTY_BITRATE_TYPE:
                case MtpConstants.PROPERTY_NUMBER_OF_CHANNELS:
                    mtpPropertyList.append(id2, property.code, 4, 0L);
                    break;
                case MtpConstants.PROPERTY_SAMPLE_RATE:
                case MtpConstants.PROPERTY_AUDIO_WAVE_CODEC:
                case MtpConstants.PROPERTY_AUDIO_BITRATE:
                    mtpPropertyList.append(id2, property.code, 6, 0L);
                    break;
                default:
                    int i6 = property.type;
                    if (i6 == 0) {
                        mtpPropertyList.append(id2, property.code, property.type, 0L);
                    } else if (i6 != 65535) {
                        mtpPropertyList.append(id2, property.code, property.type, cursor != null ? cursor.getLong(property.column) : 0L);
                    } else {
                        String string2 = "";
                        if (cursor != null) {
                            string2 = cursor.getString(property.column);
                        }
                        mtpPropertyList.append(id2, property.code, string2);
                    }
                    break;
            }
            i3++;
            cursor2 = cursor;
        }
        if (cursor2 != null) {
            cursor2.close();
            return MtpConstants.RESPONSE_OK;
        }
        return MtpConstants.RESPONSE_OK;
    }
}
