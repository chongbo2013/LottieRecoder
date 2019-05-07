package com.yy.lottierecoder;
import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.airbnb.lottie.utils.Utils;

/**
 * 将lottie渲染成视频MV
 */
public class MainActivity extends AppCompatActivity {
    TextureView textureView;
    OffscreenAfterEffectView offscreenAfterEffectView;
    Button btn_refresh,btn_recoder;
    float progress=0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.NOT_SCALE=true;
        textureView=findViewById(R.id.textureView);
        btn_refresh=findViewById(R.id.btn_refresh);
        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress+=0.1f;
                offscreenAfterEffectView.setProgress(progress);
                if(progress>=1.0f)
                    progress=0.0f;
            }
        });
        btn_recoder=findViewById(R.id.btn_recoder);
        btn_recoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        offscreenAfterEffectView=new OffscreenAfterEffectView(this);
        offscreenAfterEffectView.load();
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                offscreenAfterEffectView.setSurface(new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }
}
