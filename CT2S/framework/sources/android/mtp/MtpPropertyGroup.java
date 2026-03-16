package android.mtp;

import android.content.IContentProvider;
import android.database.Cursor;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import java.util.ArrayList;

class MtpPropertyGroup {
    private static final String FORMAT_WHERE = "format=?";
    private static final String ID_FORMAT_WHERE = "_id=? AND format=?";
    private static final String ID_WHERE = "_id=?";
    private static final String PARENT_FORMAT_WHERE = "parent=? AND format=?";
    private static final String PARENT_WHERE = "parent=?";
    private static final String TAG = "MtpPropertyGroup";
    private String[] mColumns;
    private final MtpDatabase mDatabase;
    private final String mPackageName;
    private final Property[] mProperties;
    private final IContentProvider mProvider;
    private final Uri mUri;
    private final String mVolumeName;

    private native String format_date_time(long j);

    private class Property {
        int code;
        int column;
        int type;

        Property(int code, int type, int column) {
            this.code = code;
            this.type = type;
            this.column = column;
        }
    }

    public MtpPropertyGroup(MtpDatabase database, IContentProvider provider, String packageName, String volume, int[] properties) {
        this.mDatabase = database;
        this.mProvider = provider;
        this.mPackageName = packageName;
        this.mVolumeName = volume;
        this.mUri = MediaStore.Files.getMtpObjectsUri(volume);
        int count = properties.length;
        ArrayList<String> columns = new ArrayList<>(count);
        columns.add("_id");
        this.mProperties = new Property[count];
        for (int i = 0; i < count; i++) {
            this.mProperties[i] = createProperty(properties[i], columns);
        }
        int count2 = columns.size();
        this.mColumns = new String[count2];
        for (int i2 = 0; i2 < count2; i2++) {
            this.mColumns[i2] = columns.get(i2);
        }
    }

    private Property createProperty(int code, ArrayList<String> columns) {
        int type;
        String column = null;
        switch (code) {
            case MtpConstants.PROPERTY_STORAGE_ID:
                column = MediaStore.Files.FileColumns.STORAGE_ID;
                type = 6;
                break;
            case MtpConstants.PROPERTY_OBJECT_FORMAT:
                column = MediaStore.Files.FileColumns.FORMAT;
                type = 4;
                break;
            case MtpConstants.PROPERTY_PROTECTION_STATUS:
                type = 4;
                break;
            case MtpConstants.PROPERTY_OBJECT_SIZE:
                column = "_size";
                type = 8;
                break;
            case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                column = "_data";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DATE_MODIFIED:
                column = "date_modified";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_PARENT_OBJECT:
                column = "parent";
                type = 6;
                break;
            case MtpConstants.PROPERTY_PERSISTENT_UID:
                column = MediaStore.Files.FileColumns.STORAGE_ID;
                type = 10;
                break;
            case MtpConstants.PROPERTY_NAME:
                column = "title";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ARTIST:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DESCRIPTION:
                column = "description";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DATE_ADDED:
                column = "date_added";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DURATION:
                column = "duration";
                type = 6;
                break;
            case MtpConstants.PROPERTY_TRACK:
                column = MediaStore.Audio.AudioColumns.TRACK;
                type = 4;
                break;
            case MtpConstants.PROPERTY_GENRE:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_COMPOSER:
                column = MediaStore.Audio.AudioColumns.COMPOSER;
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE:
                column = MediaStore.Audio.AudioColumns.YEAR;
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ALBUM_NAME:
                type = 65535;
                break;
            case MtpConstants.PROPERTY_ALBUM_ARTIST:
                column = MediaStore.Audio.AudioColumns.ALBUM_ARTIST;
                type = 65535;
                break;
            case MtpConstants.PROPERTY_DISPLAY_NAME:
                column = "_display_name";
                type = 65535;
                break;
            case MtpConstants.PROPERTY_BITRATE_TYPE:
            case MtpConstants.PROPERTY_NUMBER_OF_CHANNELS:
                type = 4;
                break;
            case MtpConstants.PROPERTY_SAMPLE_RATE:
            case MtpConstants.PROPERTY_AUDIO_WAVE_CODEC:
            case MtpConstants.PROPERTY_AUDIO_BITRATE:
                type = 6;
                break;
            default:
                type = 0;
                Log.e(TAG, "unsupported property " + code);
                break;
        }
        if (column != null) {
            columns.add(column);
            return new Property(code, type, columns.size() - 1);
        }
        return new Property(code, type, -1);
    }

    private String queryString(int id, String column) {
        Cursor c = null;
        try {
            c = this.mProvider.query(this.mPackageName, this.mUri, new String[]{"_id", column}, ID_WHERE, new String[]{Integer.toString(id)}, null, null);
            if (c == null || !c.moveToNext()) {
                if (c == null) {
                    return ProxyInfo.LOCAL_EXCL_LIST;
                }
                c.close();
                return ProxyInfo.LOCAL_EXCL_LIST;
            }
            String string = c.getString(1);
            if (c == null) {
                return string;
            }
            c.close();
            return string;
        } catch (Exception e) {
            if (c != null) {
                c.close();
            }
            return null;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private String queryAudio(int id, String column) {
        Cursor c = null;
        try {
            c = this.mProvider.query(this.mPackageName, MediaStore.Audio.Media.getContentUri(this.mVolumeName), new String[]{"_id", column}, ID_WHERE, new String[]{Integer.toString(id)}, null, null);
            if (c == null || !c.moveToNext()) {
                if (c == null) {
                    return ProxyInfo.LOCAL_EXCL_LIST;
                }
                c.close();
                return ProxyInfo.LOCAL_EXCL_LIST;
            }
            String string = c.getString(1);
            if (c == null) {
                return string;
            }
            c.close();
            return string;
        } catch (Exception e) {
            if (c != null) {
                c.close();
            }
            return null;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private String queryGenre(int id) {
        Cursor c = null;
        try {
            try {
                Uri uri = MediaStore.Audio.Genres.getContentUriForAudioId(this.mVolumeName, id);
                c = this.mProvider.query(this.mPackageName, uri, new String[]{"_id", "name"}, null, null, null, null);
                if (c == null || !c.moveToNext()) {
                    if (c == null) {
                        return ProxyInfo.LOCAL_EXCL_LIST;
                    }
                    c.close();
                    return ProxyInfo.LOCAL_EXCL_LIST;
                }
                String string = c.getString(1);
                if (c == null) {
                    return string;
                }
                c.close();
                return string;
            } catch (Exception e) {
                Log.e(TAG, "queryGenre exception", e);
                if (c != null) {
                    c.close();
                }
                return null;
            }
        } catch (Throwable th) {
            if (c != null) {
            }
            throw th;
        }
        if (c != null) {
            c.close();
        }
        throw th;
    }

    private Long queryLong(int id, String column) {
        Cursor c = null;
        try {
            c = this.mProvider.query(this.mPackageName, this.mUri, new String[]{"_id", column}, ID_WHERE, new String[]{Integer.toString(id)}, null, null);
            if (c != null && c.moveToNext()) {
                Long l = new Long(c.getLong(1));
                if (c == null) {
                    return l;
                }
                c.close();
                return l;
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
        return null;
    }

    private static String nameFromPath(String path) {
        int start = 0;
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash >= 0) {
            start = lastSlash + 1;
        }
        int end = path.length();
        if (end - start > 255) {
            end = start + 255;
        }
        return path.substring(start, end);
    }

    MtpPropertyList getPropertyList(int handle, int format, int depth) {
        String[] whereArgs;
        String where;
        if (depth > 1) {
            return new MtpPropertyList(0, MtpConstants.RESPONSE_SPECIFICATION_BY_DEPTH_UNSUPPORTED);
        }
        if (format == 0) {
            if (handle == -1) {
                where = null;
                whereArgs = null;
            } else {
                whereArgs = new String[]{Integer.toString(handle)};
                where = depth == 1 ? PARENT_WHERE : ID_WHERE;
            }
        } else if (handle == -1) {
            where = FORMAT_WHERE;
            whereArgs = new String[]{Integer.toString(format)};
        } else {
            whereArgs = new String[]{Integer.toString(handle), Integer.toString(format)};
            where = depth == 1 ? PARENT_FORMAT_WHERE : ID_FORMAT_WHERE;
        }
        Cursor c = null;
        try {
            if (depth > 0 || handle == -1) {
                c = this.mProvider.query(this.mPackageName, this.mUri, this.mColumns, where, whereArgs, null, null);
                if (c == null) {
                    MtpPropertyList mtpPropertyList = new MtpPropertyList(0, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                    if (c == null) {
                        return mtpPropertyList;
                    }
                    c.close();
                    return mtpPropertyList;
                }
            } else {
                try {
                    if (this.mColumns.length > 1) {
                    }
                } catch (RemoteException e) {
                    MtpPropertyList mtpPropertyList2 = new MtpPropertyList(0, 8194);
                    if (0 == 0) {
                        return mtpPropertyList2;
                    }
                    c.close();
                    return mtpPropertyList2;
                }
            }
            int count = c == null ? 1 : c.getCount();
            MtpPropertyList result = new MtpPropertyList(this.mProperties.length * count, MtpConstants.RESPONSE_OK);
            for (int objectIndex = 0; objectIndex < count; objectIndex++) {
                if (c != null) {
                    c.moveToNext();
                    handle = (int) c.getLong(0);
                }
                for (int propertyIndex = 0; propertyIndex < this.mProperties.length; propertyIndex++) {
                    Property property = this.mProperties[propertyIndex];
                    int propertyCode = property.code;
                    int column = property.column;
                    switch (propertyCode) {
                        case MtpConstants.PROPERTY_PROTECTION_STATUS:
                            result.append(handle, propertyCode, 4, 0L);
                            break;
                        case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                            String value = c.getString(column);
                            if (value != null) {
                                result.append(handle, propertyCode, nameFromPath(value));
                            } else {
                                result.setResult(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                            }
                            break;
                        case MtpConstants.PROPERTY_DATE_MODIFIED:
                        case MtpConstants.PROPERTY_DATE_ADDED:
                            result.append(handle, propertyCode, format_date_time(c.getInt(column)));
                            break;
                        case MtpConstants.PROPERTY_PERSISTENT_UID:
                            long puid = c.getLong(column);
                            result.append(handle, propertyCode, 10, (puid << 32) + ((long) handle));
                            break;
                        case MtpConstants.PROPERTY_NAME:
                            String name = c.getString(column);
                            if (name == null) {
                                name = queryString(handle, "name");
                            }
                            if (name == null && (name = queryString(handle, "_data")) != null) {
                                name = nameFromPath(name);
                            }
                            if (name != null) {
                                result.append(handle, propertyCode, name);
                            } else {
                                result.setResult(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                            }
                            break;
                        case MtpConstants.PROPERTY_ARTIST:
                            result.append(handle, propertyCode, queryAudio(handle, "artist"));
                            break;
                        case MtpConstants.PROPERTY_TRACK:
                            result.append(handle, propertyCode, 4, c.getInt(column) % 1000);
                            break;
                        case MtpConstants.PROPERTY_GENRE:
                            String genre = queryGenre(handle);
                            if (genre != null) {
                                result.append(handle, propertyCode, genre);
                            } else {
                                result.setResult(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                            }
                            break;
                        case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE:
                            int year = c.getInt(column);
                            String dateTime = Integer.toString(year) + "0101T000000";
                            result.append(handle, propertyCode, dateTime);
                            break;
                        case MtpConstants.PROPERTY_ALBUM_NAME:
                            result.append(handle, propertyCode, queryAudio(handle, "album"));
                            break;
                        case MtpConstants.PROPERTY_BITRATE_TYPE:
                        case MtpConstants.PROPERTY_NUMBER_OF_CHANNELS:
                            result.append(handle, propertyCode, 4, 0L);
                            break;
                        case MtpConstants.PROPERTY_SAMPLE_RATE:
                        case MtpConstants.PROPERTY_AUDIO_WAVE_CODEC:
                        case MtpConstants.PROPERTY_AUDIO_BITRATE:
                            result.append(handle, propertyCode, 6, 0L);
                            break;
                        default:
                            if (property.type == 65535) {
                                result.append(handle, propertyCode, c.getString(column));
                            } else if (property.type == 0) {
                                result.append(handle, propertyCode, property.type, 0L);
                            } else {
                                result.append(handle, propertyCode, property.type, c.getLong(column));
                            }
                            break;
                    }
                }
            }
            if (c == null) {
                return result;
            }
            c.close();
            return result;
        } catch (Throwable th) {
            if (0 != 0) {
                c.close();
            }
            throw th;
        }
    }
}
