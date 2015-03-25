package net.mobitexter.mobiframework.commons.core.utils;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

/**
 * Created by devashish on 22/12/14.
 */
public class FileUtils {


    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);

        if (TextUtils.isEmpty(extension) && url.contains(".")) {
            extension = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
        }

        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }

        return type;
    }
}
