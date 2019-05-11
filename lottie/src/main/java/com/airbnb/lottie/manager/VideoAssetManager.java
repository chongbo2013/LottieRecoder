package com.airbnb.lottie.manager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieVideoAsset;
import com.airbnb.lottie.VideoAssetDelegate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
public class VideoAssetManager {
  private static final Object bitmapHashLock = new Object();

  private final Context context;
  private String imagesFolder;
  @Nullable private VideoAssetDelegate delegate;
  private final Map<String, LottieVideoAsset> imageAssets;

  public VideoAssetManager(Drawable.Callback callback, String imagesFolder,
                           VideoAssetDelegate delegate, Map<String, LottieVideoAsset> imageAssets) {
    this.imagesFolder = imagesFolder;
    if (!TextUtils.isEmpty(imagesFolder) &&
        this.imagesFolder.charAt(this.imagesFolder.length() - 1) != '/') {
      this.imagesFolder += '/';
    }

    if (!(callback instanceof View)) {
      Log.w(L.TAG, "LottieDrawable must be inside of a view for images to work.");
      this.imageAssets = new HashMap<>();
      context = null;
      return;
    }

    context = ((View) callback).getContext();
    this.imageAssets = imageAssets;
    setDelegate(delegate);
  }

  public void setDelegate(@Nullable VideoAssetDelegate assetDelegate) {
    this.delegate = assetDelegate;
  }



  @Nullable public Bitmap bitmapForId(String id,int time) {
    LottieVideoAsset asset = imageAssets.get(id);
    if (asset == null) {
      return null;
    }
    Bitmap bitmap=null;
    String filename = asset.getFileName();
    File outFileName=new File(getLottieCacheFile(context,filename));
    if(outFileName.exists()){
      //如果文件存在 初始化视频信息
      asset.init(outFileName.getAbsolutePath());

      //根据时间，解码视频成Bitmap
      if (delegate != null) {//外部解码
        bitmap = delegate.fetchBitmap(asset,time);
        return bitmap;
      }
      //内部解码


      return bitmap;
    }
    //拷贝到SD卡
    InputStream is;
    try {
      if (TextUtils.isEmpty(imagesFolder)) {
        throw new IllegalStateException("You must set an images folder before loading an image." +
            " Set it with LottieComposition#setImagesFolder or LottieDrawable#setImagesFolder");
      }
      is = context.getAssets().open(imagesFolder + filename);
    } catch (IOException e) {
      Log.w(L.TAG, "Unable to open asset.", e);
      return null;
    }

    try {
      FileOutputStream fos = new FileOutputStream(outFileName.getAbsolutePath());
      byte[] buffer = new byte[1024];
      int byteCount;
      while ((byteCount = is.read(buffer)) != -1) {
        fos.write(buffer, 0, byteCount);
      }
      fos.flush();
      is.close();
      fos.close();
    }catch (Exception e){
      e.printStackTrace();
    }

    if(outFileName.exists()){
      //如果文件存在 初始化视频信息
      asset.init(outFileName.getAbsolutePath());
      //根据时间，解码视频成Bitmap
      if (delegate != null) {//外部解码
        bitmap = delegate.fetchBitmap(asset,time);
        return bitmap;
      }
      //内部解码


      return bitmap;
    }
    return null;
  }

  public boolean hasSameContext(Context context) {
    return context == null && this.context == null || this.context.equals(context);
  }
  public static String getLottieCacheFile(Context context,String fileName) {
    String directoryPath;
    // 判断外部存储是否可用，如果不可用则使用内部存储路径
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      directoryPath = context.getExternalCacheDir().getAbsolutePath();
    } else { // 使用内部存储缓存目录
      directoryPath = context.getCacheDir().getAbsolutePath();
    }
    String path = directoryPath + File.separator + fileName;
    File file = new File(path);
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    return path;
  }
}
