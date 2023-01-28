package com.example.videocon;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;
import android.util.Log;
import android.widget.EditText;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Fill the App ID of your project generated on Agora Console.
    private final String appId = "9d2498880e934632b38b0a68fa2f1622";
    // Fill the channel name.
    private String channelName = "demo";
    // Fill the temp token generated on Agora Console.
    private String token = "";
    // An integer that identifies the local user.
    private int uid = 10;
    private boolean isJoined = false;

    private RtcEngine agoraEngine;
    //SurfaceView to render local video in a Container.
    private SurfaceView localSurfaceView;
    //SurfaceView to render Remote video in a Container.
    private SurfaceView remoteSurfaceView;

    private int tokenRole; // The token role: Broadcaster or Audience
    private String serverUrl = "https://agora-token-service-production-8e40.up.railway.app";
    private int tokenExpireTime = 600; // Expire time in Seconds.
    private EditText editChannelName; // To read the channel name from the UI.

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS =
            {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            };

    private boolean checkSelfPermission()
    {
        if (ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) !=  PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) !=  PackageManager.PERMISSION_GRANTED)
        {
            return false;
        }
        return true;
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        // Listen for the event that the token is about to expire
        @Override
        public void onTokenPrivilegeWillExpire(String token) {
            Log.i("i", "Token Will expire");
            fetchToken(uid, channelName, tokenRole);
            super.onTokenPrivilegeWillExpire(token);
        }

        @Override
        // Listen for the remote host joining the channel to get the uid of the host.
        public void onUserJoined(int uid, int elapsed) {
            showMessage("Remote user joined " + uid);

            // Set the remote video view
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            isJoined = true;
            showMessage("Joined Channel " + channel);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            showMessage("Remote user offline " + uid + " " + reason);
            runOnUiThread(() -> remoteSurfaceView.setVisibility(View.GONE));
        }
    };

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.small_video_container);
        remoteSurfaceView = new SurfaceView(getBaseContext());
        remoteSurfaceView.setZOrderMediaOverlay(true);
        container.addView(remoteSurfaceView);
        agoraEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        // Display RemoteSurfaceView.
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }

    public void joinChannel(View view) {
        channelName = editChannelName.getText().toString();
        if (channelName.length() == 0) {
            showMessage("Type a channel name");
            return;
        } else if (!serverUrl.contains("http")) {
            showMessage("Invalid token server URL");
            return;
        }

        if (checkSelfPermission()) {
            tokenRole = Constants.CLIENT_ROLE_BROADCASTER;
            // Display LocalSurfaceView.
            setupLocalVideo();
            localSurfaceView.setVisibility(View.VISIBLE);
            fetchToken(uid, channelName, tokenRole);
        } else {
            showMessage("Permissions was not granted");
        }
    }


    private void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.large_video_container);
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = new SurfaceView(getBaseContext());
        container.addView(localSurfaceView);
        // Pass the SurfaceView object to Agora so that it renders the local video.
        agoraEngine.setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    boolean localInLargeContainer = true;
    public void swapViews(View view){
        localInLargeContainer = !localInLargeContainer;

        FrameLayout largeContainer = findViewById(R.id.large_video_container);
        FrameLayout smallContainer = findViewById(R.id.small_video_container);

        largeContainer.removeAllViews();
        smallContainer.removeAllViews();

        largeContainer.addView(localInLargeContainer ? localSurfaceView : remoteSurfaceView);
        smallContainer.addView(!localInLargeContainer ? localSurfaceView : remoteSurfaceView);
    }

    public void leaveChannel(View view) {
        if (!isJoined) {
            showMessage("Join a channel first");
        } else {
            agoraEngine.leaveChannel();
            showMessage("You left the channel");
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
            isJoined = false;
        }
    }

    private void setupVideoSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = RtcEngine.create(config);
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine.enableVideo();
        } catch (Exception e) {
            showMessage(e.toString());
        }
    }

    void showMessage(String message) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
        setupVideoSDKEngine();
        editChannelName = (EditText) findViewById(R.id.editChannelName);

    }

    // Fetch the <Vg k="VSDK" /> token
    private void fetchToken(int uid, String channelName, int tokenRole) {
        // Prepare the Url
        String URLString = serverUrl + "/rtc/" + channelName + "/" + tokenRole + "/"
                + "uid" + "/" + uid + "/?expiry=" + tokenExpireTime;

        OkHttpClient client = new OkHttpClient();

        // Instantiate the RequestQueue.
        Request request = new Request.Builder()
                .url(URLString)
                .header("Content-Type", "application/json; charset=UTF-8")
                .get()
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("IOException", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Gson gson = new Gson();
                    String result = response.body().string();
                    Map map = gson.fromJson(result, Map.class);
                    String _token = map.get("rtcToken").toString();
                    setToken(_token);
                    Log.i("Token Received", token);
                }
            }
        });
    }

    void setToken(String newValue) {
        token = newValue;
        if (!isJoined) { // Join a channel
            ChannelMediaOptions options = new ChannelMediaOptions();

            // For a Video call, set the channel profile as COMMUNICATION.
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            // Start local preview.
            agoraEngine.startPreview();

            // Join the channel with a token.
            agoraEngine.joinChannel(token, channelName, uid, options);
        } else { // Already joined, renew the token by calling renewToken
            agoraEngine.renewToken(token);
            showMessage("Token renewed");
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        agoraEngine.stopPreview();
        agoraEngine.leaveChannel();

        // Destroy the engine in a sub-thread to avoid congestion
        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

}