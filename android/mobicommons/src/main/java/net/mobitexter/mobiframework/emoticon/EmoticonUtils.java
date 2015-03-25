package net.mobitexter.mobiframework.emoticon;

import android.content.Context;
import android.text.Spannable;

import com.rockerhieu.emojicon.EmojiconHandler;

import net.mobitexter.mobiframework.commons.core.utils.Utils;

/**
 * Created by devashish on 26/1/15.
 */
public class EmoticonUtils {
    public static final Spannable.Factory spannableFactory = Spannable.Factory
            .getInstance();

    public static Spannable getSmiledText(Context context, CharSequence text) {
        Spannable spannable = spannableFactory.newSpannable(text);
        EmojiconHandler.addEmojis(context, spannable, Utils.dpToPx(28));
        return spannable;
    }
}
