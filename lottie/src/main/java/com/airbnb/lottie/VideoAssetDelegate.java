package com.airbnb.lottie;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

/**
 * Delegate to handle the loading of bitmaps that are not packaged in the assets of your app.
 *
 * @see LottieDrawable#setVideoAssetDelegate(VideoAssetDelegate)
 */
public interface VideoAssetDelegate {
  @Nullable Bitmap fetchBitmap(LottieVideoAsset videoAsset,int time);
}
