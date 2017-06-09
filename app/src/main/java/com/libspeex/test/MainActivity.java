package com.libspeex.test;

import android.Manifest;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.libspeex.test.helper.AudioPlayer;
import com.libspeex.test.helper.AudioRecorder;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.thinkcore.storage.TFilePath;
import com.thinkcore.utils.log.TLog;

import java.io.File;

import rx.functions.Action1;

public class MainActivity extends Activity implements View.OnClickListener {
    private AudioRecorder mAudioRecorder;
    private Long mVolumeStartTime;
    private String mVolumeFilePath;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.Button_start_record).setOnClickListener(this);
        findViewById(R.id.Button_stop_record).setOnClickListener(this);
        findViewById(R.id.Button_start_audio).setOnClickListener(this);
        findViewById(R.id.Button_stop_audio).setOnClickListener(this);

        AudioPlayer.getInstance().initConfig();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.Button_start_record) {

            RxPermissions.getInstance(this).request(Manifest.permission.RECORD_AUDIO)
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean granted) {
                            if (granted) {
                                TFilePath filePath = new TFilePath();
                                mVolumeStartTime = System.currentTimeMillis();
                                mVolumeFilePath = filePath.getExternalCacheDir() + File.separator
                                        + mVolumeStartTime + AudioRecorder.Field_Suffix;

                                if (mAudioRecorder != null)
                                    mAudioRecorder.stopRecord(new AudioRecorder.RecorderListener() {

                                        @Override
                                        public void onStop(String filePath) {
                                            TLog.i("", filePath);
                                        }
                                    });
                                mAudioRecorder = new AudioRecorder(mVolumeFilePath);
                                mAudioRecorder.startRecord();
                            } else {
                                Toast.makeText(MainActivity.this, "没有权限", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else if (view.getId() == R.id.Button_stop_record) {
            if (mAudioRecorder == null)
                return;

            mAudioRecorder.stopRecord(new AudioRecorder.RecorderListener() {

                @Override
                public void onStop(String filePath) {
                    TLog.i("", filePath);
                }
            });
        } else if (view.getId() == R.id.Button_start_audio) {
            // TFilePath filePath = new TFilePath();
            // mVolumeFilePath = filePath.getExternalCacheDir() + File.separator
            // + 11 + AudioRecorder.Field_Suffix;
            // if (TFileUtils.isFileExit(mVolumeFilePath))
            try {
                AudioPlayer.getInstance().startPlay(mVolumeFilePath, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (view.getId() == R.id.Button_stop_audio) {
            AudioPlayer.getInstance().stopPlayer();
        }
    }
}
