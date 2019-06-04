package com.lody.virtual.client.stub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.server.am.AttributeCache;

import mirror.com.android.internal.R_Hide;

/**
 * @author Lody
 */
public class WindowPreviewActivity extends Activity {

    private long startTime;
    private int mTargetUserId;
    private String mTargetPackageName;

    public static void previewActivity(int userId, ActivityInfo info) {
        Context context = VirtualCore.get().getContext();
        Intent windowBackgroundIntent = new Intent(context, WindowPreviewActivity.class);
        windowBackgroundIntent.putExtra("_VA_|user_id", userId);
        windowBackgroundIntent.putExtra("_VA_|activity_info", info);
        windowBackgroundIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        windowBackgroundIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(windowBackgroundIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        ActivityInfo info = intent.getParcelableExtra("_VA_|activity_info");
        int userId = intent.getIntExtra("_VA_|user_id", -1);
        if (info == null || userId == -1) {
            finish();
            return;
        }
        mTargetUserId = userId;
        mTargetPackageName = info.packageName;
        int theme = info.theme;
        if (theme == 0) {
            theme = info.applicationInfo.theme;
        }
        //是否使用了自定义背景
        boolean hasCustomBg = startWindowPreview(info, theme);
        if (!hasCustomBg) {
            showDefaultPreview(info);
        }
    }

    protected boolean startWindowPreview(ActivityInfo info, int theme){
        boolean hasCustomBg = false;
        AttributeCache.Entry windowExt = AttributeCache.instance().get(info.packageName, theme,
                R_Hide.styleable.Window.get());
        if (windowExt != null) {
            boolean fullscreen = windowExt.array.getBoolean(R_Hide.styleable.Window_windowFullscreen.get(), false);
            boolean translucent = windowExt.array.getBoolean(R_Hide.styleable.Window_windowIsTranslucent.get(), false);
            boolean disablePreview = windowExt.array.getBoolean(R_Hide.styleable.Window_windowDisablePreview.get(), false);

            if (!disablePreview) {
                if (fullscreen) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                Drawable drawable = null;
                try {
                    int index = R_Hide.styleable.Window_windowBackground.get();
                    int id = windowExt.array.getResourceId(index, 0);
                    hasCustomBg = id > 0x7F000000;
                    if (hasCustomBg) {
                        drawable = windowExt.array.getDrawable(index);
                    }
                } catch (Throwable e) {
                    // ignore
                }
                if (drawable == null) {
                    AttributeCache.Entry viewEnt = AttributeCache.instance().get(info.packageName, info.theme,
                            R_Hide.styleable.View.get());
                    if (viewEnt != null) {
                        try {
                            drawable = viewEnt.array.getDrawable(R_Hide.styleable.View_background.get());
                            int index = R_Hide.styleable.View_background.get();
                            int id = viewEnt.array.getResourceId(index, 0);
                            hasCustomBg = id > 0x7F000000;
                            if (hasCustomBg) {
                                drawable = viewEnt.array.getDrawable(index);
                            }
                        } catch (Throwable e) {
                            // ignore
                        }
                    }
                }
                if (drawable != null) {
                    getWindow().setBackgroundDrawable(drawable);
                } else {
                    hasCustomBg = false;
                }
            }
        }
        return hasCustomBg;
    }

    protected void showDefaultPreview(ActivityInfo info) {
        //系统默认：白屏，黑屏，透明
        setContentView(R.layout.activity_preview);
        TextView textTitle = findViewById(R.id.tv_titlle);
//                TextView textTip = findViewById(R.id.tv_tip);
        ImageView imageView = findViewById(R.id.img_icon);
        PackageManager pm = VirtualCore.getPM();
        try {
            textTitle.setText(info.applicationInfo.loadLabel(pm));
            imageView.setImageDrawable(info.applicationInfo.loadIcon(pm));
        } catch (Throwable e) {
            //加载异常的显示处理
        }
    }

    @Override
    public void onBackPressed() {
        long time = System.currentTimeMillis();
        if (time - startTime > 5000L) {
            //用户手动退出
            if (!TextUtils.isEmpty(mTargetPackageName)) {
                VActivityManager.get().killAppByPkg(mTargetPackageName, mTargetUserId);
            }
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing()) {
            finish();
        }
    }
}
