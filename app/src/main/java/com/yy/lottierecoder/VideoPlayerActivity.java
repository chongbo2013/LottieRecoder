package com.yy.lottierecoder;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * @author ferrisXu
 * 创建日期：2019/5/8
 * 描述：
 */
public class VideoPlayerActivity  extends AppCompatActivity {
    VideoView videoView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         videoView=new VideoView(this);
        videoView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(videoView);

        videoView.setMediaController(new MediaController(this));//这样就有滑动条了

        videoView.setVideoURI(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/lottie.mp4"));//播放网络视频
        videoView.start();

    }
}
