package com.example.video_project;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private static final String FILE_NAME = "video.mp4";
    private Uri vidFile;
    private final int REQUEST_CODE = 200;
    private final int START_POSITION = 0;
    private final String BASE_URL = " http://www.timeapi.org";
    private static Retrofit retrofit;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView) findViewById(R.id.video_view);
        vidFile = Uri.parse(
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + FILE_NAME);

        Log.d(TAG, "trying = " + (Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + FILE_NAME));

        if (savedInstanceState != null) {
            int pos = savedInstanceState.getInt("pos");
            startPlaying(pos);
        } else {

            if (!shouldAskPermissions())
                startPlaying(START_POSITION);
            else
                askPermissions();
        }
    }

    protected boolean shouldAskPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(23)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };

        requestPermissions(permissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startPlaying(START_POSITION);

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

        }
    }

    private void startPlaying(final int position) {

        videoView.setVideoURI(vidFile);
        if (position != START_POSITION)
            videoView.seekTo(position);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                getTime();
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
            }
        });
    }

    protected void getTime() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(new ToStringConverterFactory()).build();
        }

        ITimeInterface timeInterface = retrofit.create(ITimeInterface.class);

        Call<String> call = timeInterface.getTime();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response != null)
                    Toast.makeText(MainActivity.this, response.body().toString(), Toast.LENGTH_SHORT).show();
                startPlaying(START_POSITION);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                startPlaying(START_POSITION);
            }
        });
    }

    public class ToStringConverterFactory extends Converter.Factory {
        private final MediaType MEDIA_TYPE = MediaType.parse("text/plain");


        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (String.class.equals(type)) {
                return new Converter<ResponseBody, String>() {
                    @Override
                    public String convert(ResponseBody value) throws IOException {
                        return value.string();
                    }
                };
            }
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (videoView.isPlaying()) outState.putInt("pos", videoView.getCurrentPosition());
    }


    public interface ITimeInterface {
        @GET("/utc/now")
        Call<String> getTime();
    }
}
