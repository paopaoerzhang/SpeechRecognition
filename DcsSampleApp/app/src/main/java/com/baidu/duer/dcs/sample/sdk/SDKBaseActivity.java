/*
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.baidu.duer.dcs.sample.sdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.duer.dcs.api.DcsSdkBuilder;
import com.baidu.duer.dcs.api.IConnectionStatusListener;
import com.baidu.duer.dcs.api.IDcsSdk;
import com.baidu.duer.dcs.api.IDialogStateListener;
import com.baidu.duer.dcs.api.IDirectiveIntercepter;
import com.baidu.duer.dcs.api.IFinishedDirectiveListener;
import com.baidu.duer.dcs.api.IMessageSender;
import com.baidu.duer.dcs.api.IVoiceRequestListener;
import com.baidu.duer.dcs.api.config.DcsConfig;
import com.baidu.duer.dcs.api.config.DefaultSdkConfigProvider;
import com.baidu.duer.dcs.api.config.SdkConfigProvider;
import com.baidu.duer.dcs.api.player.ITTSPositionInfoListener;
import com.baidu.duer.dcs.api.recorder.AudioRecordImpl;
import com.baidu.duer.dcs.api.recorder.BaseAudioRecorder;
import com.baidu.duer.dcs.api.wakeup.BaseWakeup;
import com.baidu.duer.dcs.api.wakeup.IWakeupAgent;
import com.baidu.duer.dcs.api.wakeup.IWakeupProvider;
import com.baidu.duer.dcs.ces.event.ContentData;
import com.baidu.duer.dcs.componentapi.AbsDcsClient;
import com.baidu.duer.dcs.componentapi.SimpleResponseListener;
import com.baidu.duer.dcs.devicemodule.custominteraction.CustomUserInteractionDeviceModule;
import com.baidu.duer.dcs.devicemodule.form.Form;
import com.baidu.duer.dcs.devicemodule.playbackcontroller.PlaybackControllerDeviceModule;
import com.baidu.duer.dcs.framework.DcsSdkImpl;
import com.baidu.duer.dcs.framework.ILoginListener;
import com.baidu.duer.dcs.framework.InternalApi;
import com.baidu.duer.dcs.framework.internalapi.IDirectiveReceivedListener;
import com.baidu.duer.dcs.framework.internalapi.IErrorListener;
import com.baidu.duer.dcs.framework.location.Location;
import com.baidu.duer.dcs.location.ILocation;
import com.baidu.duer.dcs.location.LocationImpl;
import com.baidu.duer.dcs.oauth.api.code.OauthCodeImpl;
import com.baidu.duer.dcs.router.ICES;
import com.baidu.duer.dcs.router.IFlow;
import com.baidu.duer.dcs.sample.BuildConfig;
import com.baidu.duer.dcs.sample.R;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.ScreenDeviceModule;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.extend.card.ScreenExtendDeviceModule;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.extend.card.message.RenderAudioListPlayload;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.extend.card.message.RenderPlayerInfoPayload;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.message.HtmlPayload;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.message.RenderCardPayload;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.message.RenderHintPayload;
import com.baidu.duer.dcs.sample.sdk.devicemodule.screen.message.RenderVoiceInputTextPayload;
import com.baidu.duer.dcs.systeminterface.IOauth;
import com.baidu.duer.dcs.util.AsrType;
import com.baidu.duer.dcs.util.DcsErrorCode;
import com.baidu.duer.dcs.util.HttpProxy;
import com.baidu.duer.dcs.util.api.IDcsRequestBodySentListener;
import com.baidu.duer.dcs.util.dispatcher.DialogRequestIdHandler;
import com.baidu.duer.dcs.util.message.DcsRequestBody;
import com.baidu.duer.dcs.util.message.Directive;
import com.baidu.duer.dcs.util.message.Payload;
import com.baidu.duer.dcs.util.util.CommonUtil;
import com.baidu.duer.dcs.util.util.LogUtil;
import com.baidu.duer.dcs.util.util.NetWorkUtil;
import com.baidu.duer.dcs.util.util.StandbyDeviceIdUtil;
import com.baidu.duer.dcs.widget.DcsWebView;
import com.baidu.duer.kitt.KittWakeUpServiceImpl;
import com.baidu.duer.kitt.WakeUpConfig;
import com.baidu.duer.kitt.WakeUpWord;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 录音权限问题，需要自己处理
 */
public abstract class SDKBaseActivity extends AppCompatActivity implements
        View.OnClickListener {
    public static final String TAG = "DCS-SDK";
    // demo使用的CLIENT_ID，正式产品请用自己申请的CLIENT_ID、PID
    public static final String CLIENT_ID = BuildConfig.CLIENT_ID;
    public static final int PID = BuildConfig.PID ;
    public static final String APP_KEY = BuildConfig.APP_KEY;
    // 唤醒配置
    // 格式必须为：浮点数，用','分隔，每个模型对应3个灵敏度
    // 例如有2个模型,就需要6个灵敏度，0.35,0.35,0.40,0.45,0.45,0.55
    private static final String WAKEUP_RES_PATH = "snowboy/common.res";
    private static final String WAKEUP_UMDL_PATH = "snowboy/xiaoduxiaodu_all_11272017.umdl";
    private static final String WAKEUP_SENSITIVITY = "0.35,0.35,0.40";
    private static final String WAKEUP_HIGH_SENSITIVITY = "0.45,0.45,0.55";
    // 唤醒成功后是否需要播放提示音
    private static final boolean ENABLE_PLAY_WARNING = true;
    private static final int REQUEST_CODE = 123;
    protected EditText textInput;
    protected Button sendButton;
    protected IDcsSdk dcsSdk;
    protected ScreenDeviceModule screenDeviceModule;
    private Button nextButton;
    private Button preButton;
    private Button playButton;
    private Button voiceButton;
    private Button cancelVoiceButton;
    private boolean isPlaying;
    private TextView textViewWakeUpTip;
    private LinearLayout mTopLinearLayout;
    private DcsWebView dcsWebView;
    private ILocation location;
    // for dcs统计-demo
    private long duerResultT;
    // for dcs统计-demo
    protected TextView textViewRenderVoiceInputText;
    private IDialogStateListener dialogStateListener;
    private IDialogStateListener.DialogState currentDialogState = IDialogStateListener.DialogState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdk_main);
        setTitle(BuildConfig.APP_TITLE);
        initViews();
        initPermission();
        initSdk();
        sdkRun();
        initListener();
        initLocation();
    }

    protected void initListener() {
        // 设置各种监听器
        dcsSdk.addConnectionStatusListener(connectionStatusListener);
        // 错误
        getInternalApi().addErrorListener(errorListener);
        // event发送
        getInternalApi().addRequestBodySentListener(dcsRequestBodySentListener);
        // 对话状态
        initDialogStateListener();
        // 语音文本同步
        initTTSPositionInfoListener();
        // 唤醒
        initWakeUpAgentListener();
        // 所有指令透传，建议在各自的DeviceModule中处理
        addDirectiveReceivedListener();
        // 指令执行完毕回调
        initFinishedDirectiveListener();
        // 语音音量回调监听
        initVolumeListener();
        initVoiceErrorListener();
        initDirectiveIntercepter();
    }

    private void initLocation() {
        // 定位
        location = new LocationImpl(getApplicationContext());
        location.requestLocation(false);
        // 需要定位后赋值
        // 目前是写死的北京的
        getInternalApi().setLocationHandler(locationHandler);
    }

    protected Location.LocationHandler locationHandler = new Location.LocationHandler() {
        @Override
        public double getLongitude() {
            if (location == null) {
                return 0;
            }
            return location.getLocationInfo().longitude;
        }

        @Override
        public double getLatitude() {
            if (location == null) {
                return 0;
            }
            return location.getLocationInfo().latitude;
        }

        @Override
        public String getCity() {
            if (location == null) {
                return "";
            }
            return location.getLocationInfo().city;
        }

        @Override
        public Location.EGeoCoordinateSystem getGeoCoordinateSystem() {
            return Location.EGeoCoordinateSystem.BD09LL;
        }
    };
    private ScreenDeviceModule.IScreenListener screenListener = new ScreenDeviceModule.IScreenListener() {
        @Override
        public void onRenderVoiceInputText(RenderVoiceInputTextPayload payload) {
            handleRenderVoiceInputTextPayload(payload);
        }

        @Override
        public void onHtmlPayload(HtmlPayload htmlPayload) {
            handleHtmlPayload(htmlPayload);
        }

        @Override
        public void onRenderCard(RenderCardPayload renderCardPayload) {

        }

        @Override
        public void onRenderHint(RenderHintPayload renderHintPayload) {

        }
    };

    private IDcsRequestBodySentListener dcsRequestBodySentListener = new IDcsRequestBodySentListener() {

        @Override
        public void onDcsRequestBody(DcsRequestBody dcsRequestBody) {
            String eventName = dcsRequestBody.getEvent().getHeader().getName();
            Log.v(TAG, "eventName:" + eventName);
            if (eventName.equals("PlaybackStopped") || eventName.equals("PlaybackFinished")
                    || eventName.equals("PlaybackFailed")) {
                playButton.setText("等待音乐");
                isPlaying = false;
            } else if (eventName.equals("PlaybackPaused")) {
                playButton.setText("暂停中");
                isPlaying = false;
            } else if (eventName.equals("PlaybackStarted") || eventName.equals("PlaybackResumed")) {
                playButton.setText("播放中...");
                isPlaying = true;
            }
        }
    };
    private IErrorListener errorListener = new IErrorListener() {
        @Override
        public void onErrorCode(DcsErrorCode errorCode) {
            if (errorCode.error == DcsErrorCode.VOICE_REQUEST_EXCEPTION) {
                if (errorCode.subError == DcsErrorCode.NETWORK_UNAVAILABLE) {
                    Toast.makeText(SDKBaseActivity.this,
                            "网络不可用",
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(SDKBaseActivity.this,
                            getResources().getString(R.string.voice_err_msg),
                            Toast.LENGTH_SHORT)
                            .show();
                }

            } else if (errorCode.error == DcsErrorCode.LOGIN_FAILED) {
                // 未登录
                Toast.makeText(SDKBaseActivity.this,
                        "未登录",
                        Toast.LENGTH_SHORT)
                        .show();
            } else if (errorCode.subError == DcsErrorCode.UNAUTHORIZED_REQUEST) {
                // 以下仅针对 passport 登陆情况下的账号刷新，非 passport 刷新请参看文档。
            }
        }
    };

    private IConnectionStatusListener connectionStatusListener = new IConnectionStatusListener() {
        @Override
        public void onConnectStatus(ConnectionStatus connectionStatus) {
            Log.d(TAG, "onConnectionStatusChange: " + connectionStatus);

        }
    };

    /**
     * tts文字同步
     */
    private void initTTSPositionInfoListener() {
        getInternalApi().addTTSPositionInfoListener(new ITTSPositionInfoListener() {
            @Override
            public void onPositionInfo(long pos, long playTimeMs, long mark) {
            }
        });
    }

    /**
     * 语音音量回调监听
     */
    private void initVolumeListener() {
        getInternalApi().getDcsClient().addVolumeListener(new AbsDcsClient.IVolumeListener() {
            @Override
            public void onVolume(int volume, int percent) {
                Log.d(TAG, "volume  ----->" + volume);
                Log.d(TAG, "percent ----->" + percent);
            }
        });
    }

    /**
     * 语音错误回调监听
     */
    private void initVoiceErrorListener() {
        getInternalApi().getDcsClient().addVoiceErrorListener(new AbsDcsClient.IVoiceErrorListener() {
            @Override
            public void onVoiceError(int error, int subError) {
                Log.d(TAG, "onVoiceError:" + error + " " + subError);
            }
        });
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ArrayList<String> toApplyList = new ArrayList<>();
        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                    perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }
        }
        if (!toApplyList.isEmpty()) {
            String tmpList[] = new String[toApplyList.size()];
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。

    }

    @Override
    public void onBackPressed() {
        try {
            super.onBackPressed();
        } catch (Exception ignored) {
            // LEFT-DO-NOTHING
        }
    }

    protected void initSdk() {
        // 第一步初始化sdk
        // BaseAudioRecorder audioRecorder = new PcmAudioRecorderImpl(); pcm 输入方式
        BaseAudioRecorder audioRecorder = new AudioRecordImpl();
        IOauth oauth = getOauth();
        // 唤醒单独开启唤醒进程；  如果不需要将唤醒放入一个单独进程，可以使用KittWakeUpImpl
        final BaseWakeup wakeup = new KittWakeUpServiceImpl(audioRecorder);
        // 百度语音团队的离线asr和百度语音团队的唤醒，2个so库冲突，暂时不要用WakeupImpl实现的唤醒功能！！
//        final BaseWakeup wakeup = new WakeupImpl();
        final IWakeupProvider wakeupProvider = new IWakeupProvider() {
            @Override
            public WakeUpConfig wakeUpConfig() {
                // 添加多唤醒词和索引
                // 此处传入的index需要和Snowboy唤醒模型文件一致
                // 例：模型文件中有3个唤醒词，分别为不同语速的"小度小度"，index分别为1-3，则需要按照以下格式添加
                // 唤醒成功后，回调中会包含被唤醒的WakeUpWord
                List<WakeUpWord> wakeupWordList = new ArrayList<>();
                wakeupWordList.add(new WakeUpWord(1, "小度小度"));
                wakeupWordList.add(new WakeUpWord(2, "小度小度"));
                wakeupWordList.add(new WakeUpWord(3, "小度小度"));
                final List<String> umdlPaths = new ArrayList<>();
                umdlPaths.add(WAKEUP_UMDL_PATH);
                return new WakeUpConfig.Builder()
                        .resPath(WAKEUP_RES_PATH)
                        .umdlPath(umdlPaths)
                        .sensitivity(WAKEUP_SENSITIVITY)
                        .highSensitivity(WAKEUP_HIGH_SENSITIVITY)
                        .wakeUpWords(wakeupWordList)
                        .build();
            }

            @Override
            public boolean enableWarning() {
                return ENABLE_PLAY_WARNING;
            }

            @Override
            public String warningSource() {
                // 每次在播放唤醒提示音前调用该方法
                // assets目录下的以assets://开头
                // 文件为绝对路径
                return "assets://ding.wav";
            }

            @Override
            public float volume() {
                // 每次在播放唤醒提示音前调用该方法
                // [0-1]
                return 0.8f;
            }

            @Override
            public boolean wakeAlways() {
                return SDKBaseActivity.this.enableWakeUp();
            }

            @Override
            public BaseWakeup wakeupImpl() {
                return wakeup;
            }

            @Override
            public int audioType() {
                // 用户自定义类型
                return AudioManager.STREAM_MUSIC;
            }
        };


        // proxyIp 为代理IP
        // proxyPort  为代理port
        HttpProxy httpProxy = new HttpProxy("172.24.194.28", 8888);

        // SDK配置，ClientId、语音PID、代理等
        SdkConfigProvider sdkConfigProvider = getSdkConfigProvider();
        // 构造dcs sdk
        DcsSdkBuilder builder = new DcsSdkBuilder();
        dcsSdk = builder.withSdkConfig(sdkConfigProvider)
                .withWakeupProvider(wakeupProvider)
                .withOauth(oauth)
                .withAudioRecorder(audioRecorder)
                // 1.withDeviceId设置设备唯一ID
                // 2.强烈建议！！！！
                //   如果开发者清晰的知道自己设备的唯一id，可以按照自己的规则传入
                //   需要保证设置正确，保证唯一、刷机和升级后不变
                // 3.sdk提供的方法，但是不保证所有的设别都是唯一的
                //   StandbyDeviceIdUtil.getStandbyDeviceId()
                //   该方法的算法是MD5（android_id + imei + Mac地址）32位  +  32位UUID总共64位
                //   生成：首次按照上述算法生成ID，生成后依次存储apk内部->存储系统数据库->存储外部文件
                //   获取：存储apk内部->存储系统数据库->存储外部文件，都没有则重新生成
                .withDeviceId(StandbyDeviceIdUtil.getStandbyDeviceId())
                // 设置音乐播放器的实现，sdk 内部默认实现为MediaPlayerImpl
                // .withMediaPlayer(new MediaPlayerImpl(AudioManager.STREAM_MUSIC))
                .build();

        // 设置Oneshot
        getInternalApi().setSupportOneshot(false);
        // ！！！！临时配置需要在run之前设置！！！！
        // 临时配置开始
        // 暂时没有定的API接口，可以通过getInternalApi设置后使用
        // 设置唤醒参数后，初始化唤醒
        getInternalApi().initWakeUp();
//        getInternalApi().setOnPlayingWakeUpSensitivity(WAKEUP_ON_PLAYING_SENSITIVITY);
//        getInternalApi().setOnPlayingWakeUpHighSensitivity(WAKEUP_ON_PLAYING_HIGH_SENSITIVITY);
        getInternalApi().setAsrMode(getAsrMode());
        // 测试数据，具体bduss值
        // getInternalApi().setBDuss("");
        // 临时配置结束
        // dbp平台
        // getInternalApi().setDebugBot("f15be387-1348-b71b-2ae5-8f19f2375ea1");

        // 第二步：可以按需添加内置端能力和用户自定义端能力（需要继承BaseDeviceModule）
        // 屏幕展示
        IMessageSender messageSender = getInternalApi().getMessageSender();

        // 上屏
        screenDeviceModule = new ScreenDeviceModule(messageSender);
        screenDeviceModule.addScreenListener(screenListener);
        dcsSdk.putDeviceModule(screenDeviceModule);

        ScreenExtendDeviceModule screenExtendDeviceModule = new ScreenExtendDeviceModule(messageSender);
        screenExtendDeviceModule.addExtensionListener(mScreenExtensionListener);
        dcsSdk.putDeviceModule(screenExtendDeviceModule);

        // 在线返回文本的播报，eg:你好，返回你好的播报
        DialogRequestIdHandler dialogRequestIdHandler =
                ((DcsSdkImpl) dcsSdk).getProvider().getDialogRequestIdHandler();
        CustomUserInteractionDeviceModule customUserInteractionDeviceModule =
                new CustomUserInteractionDeviceModule(messageSender, dialogRequestIdHandler);
        dcsSdk.putDeviceModule(customUserInteractionDeviceModule);

        // 扩展自定义DeviceModule,eg...
        addOtherDeviceModule(dcsSdk, messageSender);
        // 获取设备列表
        // getInternalApi().getSmartHomeManager().getDeviceList(null, null);
    }


    protected void addOtherDeviceModule(IDcsSdk dcsSdk, IMessageSender messageSender) {

    }

    protected SdkConfigProvider getSdkConfigProvider() {
        return new DefaultSdkConfigProvider() {
            @Override
            public String clientId() {
                return CLIENT_ID;
            }

            @Override
            public int pid() {
                return PID;
            }

            @Override
            public String appKey() {
                return APP_KEY;
            }
        };
    }

    private String mRenderPlayerInfoToken = null;
    private String mPlayToken = null;
    private ScreenExtendDeviceModule.IScreenExtensionListener mScreenExtensionListener = new ScreenExtendDeviceModule
            .IScreenExtensionListener() {


        @Override
        public void onRenderPlayerInfo(RenderPlayerInfoPayload renderPlayerInfoPayload) {
            // handleRenderPlayerInfoPayload(renderPlayerInfoPayload);
        }

        @Override
        public void onRenderAudioList(RenderAudioListPlayload renderAudioListPlayload) {

        }
    };

    protected void sdkRun() {
        // 第三步，将sdk跑起来
        ((DcsSdkImpl) dcsSdk).getInternalApi().login(new ILoginListener() {
            @Override
            public void onSucceed(String accessToken) {
                dcsSdk.run(null);
                Toast.makeText(SDKBaseActivity.this.getApplicationContext(), "登录成功", Toast
                        .LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(String errorMessage) {
                Toast.makeText(SDKBaseActivity.this.getApplicationContext(), "登录失败", Toast
                        .LENGTH_SHORT).show();
                Log.e(TAG, "login onFailed. ");
                finish();
            }

            @Override
            public void onCancel() {
                Toast.makeText(SDKBaseActivity.this.getApplicationContext(), "登录被取消", Toast
                        .LENGTH_SHORT).show();
                Log.e(TAG, "login onCancel. ");
                finish();
            }
        });
    }

    private void initViews() {
        textViewWakeUpTip = (TextView) findViewById(R.id.id_tv_wakeup_tip);
        nextButton = (Button) findViewById(R.id.id_next_audio_btn);
        nextButton.setOnClickListener(this);
        preButton = (Button) findViewById(R.id.id_previous_audio);
        preButton.setOnClickListener(this);
        playButton = (Button) findViewById(R.id.id_audio_default_btn);
        playButton.setOnClickListener(this);
        textInput = (EditText) findViewById(R.id.textInput);
        sendButton = (Button) findViewById(R.id.sendBtn);
        sendButton.setOnClickListener(this);
        voiceButton = (Button) findViewById(R.id.voiceBtn);
        voiceButton.setOnClickListener(this);
        cancelVoiceButton = (Button) findViewById(R.id.cancelBtn);
        cancelVoiceButton.setOnClickListener(this);
        textViewRenderVoiceInputText = (TextView) findViewById(R.id.id_tv_RenderVoiceInputText);
        mTopLinearLayout = (LinearLayout) findViewById(R.id.topLinearLayout);
        dcsWebView = new DcsWebView(this.getApplicationContext());
        mTopLinearLayout.addView(dcsWebView);

        textViewWakeUpTip.setVisibility(enableWakeUp() ? View.VISIBLE : View.GONE);
        initDcsWebView();
    }

    private void initDcsWebView() {
        dcsWebView.setLoadListener(new DcsWebView.LoadListener() {
            @Override
            public void onPageStarted() {

            }

            @Override
            public void onPageFinished() {
                if (duerResultT > 0) {
                    // DCSStatisticsImpl.getInstance().reportView(duerResultT, System.currentTimeMillis());
                    Toast.makeText(SDKBaseActivity.this, (System.currentTimeMillis() - duerResultT)
                            + " ms", Toast.LENGTH_LONG).show();
                    duerResultT = 0;
                }
            }
        });
    }

    public InternalApi getInternalApi() {
        return ((DcsSdkImpl) dcsSdk).getInternalApi();
    }

    private IWakeupAgent.IWakeupAgentListener wakeupAgentListener = new IWakeupAgent.SimpleWakeUpAgentListener() {
        @Override
        public void onWakeupSucceed(WakeUpWord wakeUpWord) {
            Toast.makeText(SDKBaseActivity.this,
                    "唤醒成功",
                    Toast.LENGTH_LONG).show();
        }
    };

    private void initWakeUpAgentListener() {
        IWakeupAgent wakeupAgent = getInternalApi().getWakeupAgent();
        if (wakeupAgent != null) {
            wakeupAgent.addWakeupAgentListener(wakeupAgentListener);
        }
    }

    private void beginVoiceRequest(final boolean vad) {
        // 必须先调用cancel
        dcsSdk.getVoiceRequest().cancelVoiceRequest(new IVoiceRequestListener() {
            @Override
            public void onSucceed() {
                dcsSdk.getVoiceRequest().beginVoiceRequest(vad);
            }
        });
    }

    private void initDialogStateListener() {
        // 添加会话状态监听
        dialogStateListener = new IDialogStateListener() {
            @Override
            public void onDialogStateChanged(final DialogState dialogState) {
                currentDialogState = dialogState;
                Log.d(TAG, "onDialogStateChanged: " + dialogState);
                switch (dialogState) {
                    case IDLE:
                        voiceButton.setText(getResources().getString(R.string.stop_record));
                        break;
                    case LISTENING:
                        textViewRenderVoiceInputText.setText("");
                        voiceButton.setText(getResources().getString(R.string.start_record));
                        break;
                    case SPEAKING:
                        voiceButton.setText(getResources().getString(R.string.speaking));
                        break;
                    case THINKING:
                        voiceButton.setText(getResources().getString(R.string.think));
                        break;
                    default:
                        break;
                }
            }
        };
        dcsSdk.getVoiceRequest().addDialogStateListener(dialogStateListener);
    }

    private void addDirectiveReceivedListener() {
        getInternalApi().addDirectiveReceivedListener(new IDirectiveReceivedListener() {
            @Override
            public void onDirective(Directive directive) {
                if (directive == null) {
                    return;
                }
                if (directive.getName().equals("Play")) {
                    Payload mPayload = directive.getPayload();
                    if (mPayload instanceof com.baidu.duer.dcs.devicemodule.audioplayer.message.PlayPayload) {
                        com.baidu.duer.dcs.devicemodule.audioplayer.message.PlayPayload.Stream stream =
                                ((com.baidu.duer.dcs.devicemodule.audioplayer.message.PlayPayload) mPayload)
                                        .audioItem.stream;
                        if (stream != null) {
                            mPlayToken = ((com.baidu.duer.dcs.devicemodule.audioplayer.message.PlayPayload) mPayload)
                                    .audioItem.stream.token;
                            Log.i(TAG, "  directive mToken = " + mPlayToken);
                        }
                    }
                } else if (directive.getName().equals("RenderPlayerInfo")) {
                    Payload mPayload = directive.getPayload();
                    if (mPayload instanceof RenderPlayerInfoPayload) {
                        mRenderPlayerInfoToken = ((RenderPlayerInfoPayload) mPayload).getToken();
                    }
                }
            }
        });
    }

    private void initDirectiveIntercepter() {
        getInternalApi().setDirectiveIntercepter(new IDirectiveIntercepter() {
            @Override
            public boolean onInterceptDirective(Directive directive) {
                return false;
            }
        });
    }

    private void initFinishedDirectiveListener() {
        // 所有指令执行完毕的回调监听
        getInternalApi().addFinishedDirectiveListener(new IFinishedDirectiveListener() {
            @Override
            public void onFinishedDirective() {
                Log.d(TAG, "所有指令执行完毕");
            }
        });
    }


    private void handleHtmlPayload(HtmlPayload payload) {
        dcsWebView.loadUrl(payload.getUrl());
        duerResultT = System.currentTimeMillis();
    }

    private void handleRenderVoiceInputTextPayload(RenderVoiceInputTextPayload payload) {
        textViewRenderVoiceInputText.setText(payload.text);
        if (payload.type == RenderVoiceInputTextPayload.Type.FINAL) {
            LogUtil.dc("ASR-FINAL-RESULT", payload.text);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.id_next_audio_btn:
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }
                cancelVoiceRequest();
                if (TextUtils.isEmpty(mRenderPlayerInfoToken) || !mRenderPlayerInfoToken.equals(mPlayToken)) {
                    getInternalApi().sendCommandIssuedEvent(PlaybackControllerDeviceModule
                            .CommandIssued
                            .CommandIssuedNext);
                } else {
                    getInternalApi().stopSpeaker();
                    getInternalApi().postEvent(Form.nextButtonClicked(mRenderPlayerInfoToken),
                            new SimpleResponseListener() {
                                @Override
                                public void onSucceed(int statusCode) {
                                    if (statusCode == 204) {
                                        getInternalApi().resumeSpeaker();
                                    }
                                }

                                @Override
                                public void onFailed(DcsErrorCode dcsErrorCode) {
                                    getInternalApi().resumeSpeaker();
                                }
                            });
                }
                break;
            case R.id.id_previous_audio:
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }
                cancelVoiceRequest();
                if (TextUtils.isEmpty(mRenderPlayerInfoToken) || !mRenderPlayerInfoToken.equals(mPlayToken)) {
                    getInternalApi().sendCommandIssuedEvent(PlaybackControllerDeviceModule
                            .CommandIssued
                            .CommandIssuedPrevious);
                } else {
                    getInternalApi().stopSpeaker();
                    getInternalApi().postEvent(Form.previousButtonClicked(mRenderPlayerInfoToken),
                            new SimpleResponseListener() {
                                @Override
                                public void onSucceed(int statusCode) {
                                    if (statusCode == 204) {
                                        getInternalApi().resumeSpeaker();
                                    }
                                }

                                @Override
                                public void onFailed(DcsErrorCode dcsErrorCode) {
                                    getInternalApi().resumeSpeaker();
                                }
                            });
                }
                break;
            case R.id.id_audio_default_btn:
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }
                if (TextUtils.isEmpty(mRenderPlayerInfoToken) || !mRenderPlayerInfoToken.equals(mPlayToken)) {
                    if (isPlaying) {
                        getInternalApi().sendCommandIssuedEvent(PlaybackControllerDeviceModule
                                .CommandIssued
                                .CommandIssuedPause);

                    } else {
                        getInternalApi().sendCommandIssuedEvent(PlaybackControllerDeviceModule
                                .CommandIssued
                                .CommandIssuedPlay);
                    }
                    isPlaying = !isPlaying;
                } else {
                    getInternalApi().postEvent(Form.playPauseButtonClicked(mRenderPlayerInfoToken), null);
                }
                break;
            case R.id.sendBtn:
                String inputText = textInput.getText().toString().trim();
                if (TextUtils.isEmpty(inputText)) {
                    Toast.makeText(this, getResources().getString(R.string
                                    .inputed_text_cannot_be_empty),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // 清空并收起键盘
                textInput.getEditableText().clear();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context
                        .INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
                if (!NetWorkUtil.isNetworkConnected(this)) {
                    Toast.makeText(this,
                            getResources().getString(R.string.err_net_msg),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                getInternalApi().sendQuery(inputText);
                break;
            case R.id.voiceBtn:
                if (getAsrMode() == DcsConfig.ASR_MODE_ONLINE) {
                    if (!NetWorkUtil.isNetworkConnected(this)) {
                        Toast.makeText(this,
                                getResources().getString(R.string.err_net_msg),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (CommonUtil.isFastDoubleClick()) {
                    return;
                }
                // 为了解决频繁的点击 而服务器没有时间返回结果造成的不能点击的bug
                if (currentDialogState == IDialogStateListener.DialogState.LISTENING) {
                    dcsSdk.getVoiceRequest().endVoiceRequest(new IVoiceRequestListener() {
                        @Override
                        public void onSucceed() {

                        }
                    });
                } else {
                    beginVoiceRequest(getAsrType() == AsrType.AUTO);
                }
                break;
            case R.id.cancelBtn:
                // 取消识别，不再返回任何识别结果
                cancelVoiceRequest();
                break;
            default:
                break;
        }
    }

    private void cancelVoiceRequest() {
        dcsSdk.getVoiceRequest().cancelVoiceRequest(new IVoiceRequestListener() {
            @Override
            public void onSucceed() {
                Log.d(TAG, "cancelVoiceRequest onSucceed");
            }
        });
    }


    private int calculateVolume(byte[] buffer) {
        short[] audioData = new short[buffer.length / 2];
        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData);
        double sum = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < audioData.length; i++) {
            sum += audioData[i] * audioData[i];
        }
        // 平方和除以数据总长度，得到音量大小
        double mean = sum / (double) audioData.length;
        final double volume = 10 * Math.log10(mean);
        return (int) volume;
    }

    private void wakeUp() {
        getInternalApi().startWakeup();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        // 这里是为了展示如何使用下面的2个方法，如果不需要可以不用调用
        // 停止tts，音乐等有关播放.
        getInternalApi().pauseSpeaker();
        // 如果有唤醒，则停止唤醒
        getInternalApi().stopWakeup(null);
        // 取消识别，不返回结果
        cancelVoiceRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 这里是为了展示如何使用下面的2个方法，如果不需要可以不用调用
        Log.d(TAG, "onRestart");
        // 恢复tts，音乐等有关播放
        getInternalApi().resumeSpeaker();
        // 如果有唤醒，则恢复唤醒
        wakeUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // dcsWebView
        dcsWebView.setLoadListener(null);
        mTopLinearLayout.removeView(dcsWebView);
        dcsWebView.removeAllViews();
        dcsWebView.destroy();

        if (screenDeviceModule != null) {
            screenDeviceModule.removeScreenListener(screenListener);
        }
        screenListener = null;

        dcsSdk.getVoiceRequest().removeDialogStateListener(dialogStateListener);
        dialogStateListener = null;

        dcsSdk.removeConnectionStatusListener(connectionStatusListener);
        connectionStatusListener = null;

        getInternalApi().removeErrorListener(errorListener);
        errorListener = null;

        getInternalApi().removeRequestBodySentListener(dcsRequestBodySentListener);
        dcsRequestBodySentListener = null;

        getInternalApi().setLocationHandler(null);
        locationHandler = null;
        if (location != null) {
            location.release();
        }

        // 第3步，释放sdk
        dcsSdk.release();
    }

    protected IOauth getOauth() {
        return new OauthCodeImpl(CLIENT_ID, this);
    }

    // -------------------------abstract

    /**
     * 是否启用唤醒
     *
     * @return
     */
    public abstract boolean enableWakeUp();

    /**
     * asr的识别类型-在线or离线
     *
     * @return
     */
    public abstract int getAsrMode();

    /**
     * 识别模式
     *
     * @return
     */
    public abstract AsrType getAsrType();

    // -------------------------abstract

    public static final String EVENT_TYPE_CLICK = "click";
    public static final String EVENT_TYPE_SHOW = "show";
    public static final String STATISTICS_USER_DEFINED_400 = "400";

    public void reportDemo() {
        // 1、....  初始化sdk ....
        // 2、构造数据对象
        ContentData data = new ContentData();
        data.mBusinessFrom = "shouzhu";                                       // shouzhu,  业务分类为手助
        data.mEventType = EVENT_TYPE_CLICK;                                   // click/show,事件类型为点击或者展现
        data.mEventFlag = "http://duer.baidu.com/midpage/meishi?&id=23...";  // url 事件标识为url
        data.mEventValue = "禾绿回转寿";                                       // title 内容为标题
        JSONObject extensions = new JSONObject();
        try {
            extensions.put("自定义key", "value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        data.mExtension = extensions;
        // 请注意，自定义的字段不推介使用，因为云端会把它当成一串无结构的Json字符串，不会解析他，仅仅是字符串对待而已。
        // 强烈建议使用上面的4个字段。
        // 3、获取上报对象并且上报
        ICES statistics = ((DcsSdkImpl) dcsSdk).getInternalApi().getStatistics();
        statistics.onEvent(STATISTICS_USER_DEFINED_400, data.toJsonString());
    }


    public static final String EVENT_TYPE_MUSIC = "music";
    public static final String EVENT_TYPE_UNICAST = "unicast";
    public static final String EVENT_TYPE_BLUETOOTH = "bluetooth";
    public static final String STATISTICS_USER_DEFINED_401 = "401";

    public void reportFlowDemo() {
        // 1、....  初始化sdk ....
        // 2、构造数据对象
        final ContentData data = new ContentData();
        data.mBusinessFrom = "langang";                                       // langang,  业务分类为蓝港
        data.mEventType = EVENT_TYPE_MUSIC;                                   // music/unicast/bluetooth,事件类型为音乐有声，蓝牙
        data.mEventFlag = "告白气球";                                       // 歌曲名称
        data.mEventValue = "周杰伦";                                       // 歌手名称
        JSONObject extensions = new JSONObject();
        try {
            extensions.put("自定义key", "value"); // 自定义的
        } catch (JSONException e) {
            e.printStackTrace();
        }
        data.mExtension = extensions;
        // 请注意，自定义的字段不推介使用，因为云端会把它当成一串无结构的Json字符串，不会解析他，仅仅是字符串对待而已。
        // 强烈建议使用上面的4个字段。
        // 3、获取上报对象并且上报
        ICES statistics = ((DcsSdkImpl) dcsSdk).getInternalApi().getStatistics();
        final IFlow flow = statistics.beginFlow(STATISTICS_USER_DEFINED_401); // 音乐开始调该方法
        flow.setValueWithDuration(data.toJsonString());  // 音乐结束调这两个方法
        flow.end();
    }
}
