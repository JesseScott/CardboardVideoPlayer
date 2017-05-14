package ca.ecuad.research.s3dcentre.cardboardvideoplayer.nkmip;

import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.google.vr.sdk.widgets.video.VrVideoView.Options;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends Activity {

    private static final String TAG = " -- S3D -- ";

    private static final String DEFAULT_ASSET = "nkmip.mp4";
    private static final String STATE_IS_PAUSED = "isPaused";
    private static final String STATE_PROGRESS_TIME = "progressTime";
    private static final String STATE_VIDEO_DURATION = "videoDuration";

    public static final int LOAD_VIDEO_STATUS_UNKNOWN = 0;
    public static final int LOAD_VIDEO_STATUS_SUCCESS = 1;
    public static final int LOAD_VIDEO_STATUS_ERROR = 2;
    private int loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

    private VideoLoaderTask backgroundVideoLoaderTask;
    private VrVideoView videoWidgetView;
    private boolean isPaused = false;
    private Options videoOptions = new Options();
    private Uri fileUri;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
        videoWidgetView.setEventListener(new ActivityEventListener());

        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, this.hashCode() + ".onNewIntent()");
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Log.i(TAG, "ACTION_VIEW Intent received");
            fileUri = intent.getData();
            if (fileUri == null) {
                Log.w(TAG, "No data uri specified. Use \"-d /path/filename\".");
            } else {
                Log.i(TAG, "Using file " + fileUri.toString());
            }

            videoOptions.inputFormat = intent.getIntExtra("inputFormat", Options.FORMAT_DEFAULT);
            videoOptions.inputType = intent.getIntExtra("inputType", Options.TYPE_MONO);
        } else {
            Log.i(TAG, "Intent is not ACTION_VIEW. Using the default video.");
            fileUri = null;
        }
        if (backgroundVideoLoaderTask != null) {
            backgroundVideoLoaderTask.cancel(true);
        }
        backgroundVideoLoaderTask = new VideoLoaderTask();
        backgroundVideoLoaderTask.execute(Pair.create(fileUri, videoOptions));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        videoWidgetView.seekTo(progressTime);

        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isPaused) {
            videoWidgetView.pauseVideo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoWidgetView.pauseRendering();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoWidgetView.resumeRendering();
    }

    @Override
    protected void onDestroy() {
        videoWidgetView.shutdown();
        super.onDestroy();
    }

    private void togglePause() {
        if (isPaused) {
            videoWidgetView.playVideo();
        } else {
            videoWidgetView.pauseVideo();
        }
        isPaused = !isPaused;
    }

    /*

     */
    private class ActivityEventListener extends VrVideoEventListener  {

        @Override
        public void onLoadSuccess() {
            Log.i(TAG, "Sucessfully loaded video ");
            loadVideoStatus = LOAD_VIDEO_STATUS_SUCCESS;
            videoWidgetView.playVideo();
        }

        @Override
        public void onLoadError(String errorMessage) {
            loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
            Toast.makeText(MainActivity.this, "Error loading video: " + errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error loading video: " + errorMessage);
        }

        @Override
        public void onClick() {
            togglePause();
        }


        @Override
        public void onCompletion() {
            videoWidgetView.seekTo(0);
        }
    }

    /*

     */
    class VideoLoaderTask extends AsyncTask<Pair<Uri, Options>, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Pair<Uri, Options>... fileInformation) {
            try {
                if (fileInformation == null || fileInformation.length < 1 || fileInformation[0] == null || fileInformation[0].first == null) {
                    Options options = new Options();
                    options.inputType = Options.TYPE_STEREO_OVER_UNDER;
                    videoWidgetView.loadVideoFromAsset(DEFAULT_ASSET, options);
                } else {
                    videoWidgetView.loadVideo(fileInformation[0].first, fileInformation[0].second);
                }
            } catch (IOException e) {
                videoWidgetView.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Error opening file. ", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Could not open video: " + e);
            }

            return true;
        }
    }

}

