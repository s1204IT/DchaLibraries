package org.gsma.joyn.capability;

import android.net.Uri;

public class CapabilitiesLog {
    public static final String CAPABILITY_BURN_AFTER_READING = "burn_after_reading";
    public static final String CAPABILITY_EXTENSIONS = "capability_extensions";
    public static final String CAPABILITY_FILE_TRANSFER = "capability_file_transfer";
    public static final String CAPABILITY_GEOLOC_PUSH = "capability_geoloc_push";
    public static final String CAPABILITY_IMAGE_SHARE = "capability_image_share";
    public static final String CAPABILITY_IM_SESSION = "capability_im_session";
    public static final String CAPABILITY_IP_VIDEO_CALL = "capability_ip_video_call";
    public static final String CAPABILITY_IP_VOICE_CALL = "capability_ip_voice_call";
    public static final String CAPABILITY_IR94_DUPLEX_MODE = "capability_IR94_duplex_mode";
    public static final String CAPABILITY_IR94_VIDEO_CALL = "capability_IR94_video_call";
    public static final String CAPABILITY_IR94_VOICE_CALL = "capability_IR94_voice_call";
    public static final String CAPABILITY_VIDEO_SHARE = "capability_video_share";
    public static final String CONTACT_NUMBER = "contact_number";
    public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.capabilities/capabilities");
    public static final String ID = "_id";
    public static final int NOT_SUPPORTED = 0;
    public static final int SUPPORTED = 1;
}
