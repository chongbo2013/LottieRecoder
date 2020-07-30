package com.yy.lottierecoder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.utils.Utils;

import com.yy.lottierecoder.encoders.YYTemplateRender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 将lottie渲染成视频MV
 */
public class MainActivity extends AppCompatActivity {
    Button btn_recoder;
    RelativeLayout rl_dialog_progress;
    TextView tv_progress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.NOT_SCALE = true;
        btn_recoder = findViewById(R.id.btn_recoder);
        rl_dialog_progress=findViewById(R.id.rl_dialog_progress);
        tv_progress=findViewById(R.id.tv_progress);
        btn_recoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });



    }
    final Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(tv_progress==null||rl_dialog_progress==null)
                return;
            String progressStr="处理中"+msg.what+"%";
            tv_progress.setText(progressStr);

        }
    };
    private void recoder() {
        rl_dialog_progress.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {

                YYTemplateRender yyTemplateRender = new YYTemplateRender(MainActivity.this, new YYTemplateRender.IRenderProgressListem() {
                    @Override
                    public void onProgress(float progress) {
                        handler.sendEmptyMessage((int) (progress*100f));
                    }

                    @Override
                    public void onError() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"编码错误",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void success() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    rl_dialog_progress.setVisibility(View.GONE);
                                    if(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/lottie.mp4").exists()){
                                        startActivity(new Intent(MainActivity.this,VideoPlayerActivity.class));
                                    }
                                }
                            });
                    }
                }, null, null, Environment.getExternalStorageDirectory().getAbsolutePath() + "/lottie.mp4");

                yyTemplateRender.start();
            }
        }).start();
    }
}
