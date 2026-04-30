package com.mcplugin.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

/**
 * 悬浮窗服务：圆形悬浮球 + 展开功能面板
 */
public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "mc_helper_channel";
    private WindowManager windowManager;
    private View floatBallView;
    private View panelView;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams panelParams;
    private boolean isPanelVisible = false;

    // 各功能开关状态
    private final boolean[] switchStates = new boolean[20];

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatBall();
        createPanel();
    }

    // ───── 通知频道 ─────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "MC 辅助悬浮窗", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持悬浮窗在前台运行");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MC 辅助工具运行中")
                .setContentText("点击展开控制面板")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // ───── 圆形悬浮球 ─────
    private void createFloatBall() {
        floatBallView = new View(this);
        floatBallView.setBackgroundDrawable(makeBallDrawable());
        floatBallView.setAlpha(0.85f);

        int ballSize = dp2px(56);
        ballParams = new WindowManager.LayoutParams(
                ballSize, ballSize,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.x = 20;
        ballParams.y = 300;

        floatBallView.setOnTouchListener(new BallTouchListener());
        windowManager.addView(floatBallView, ballParams);
    }

    private android.graphics.drawable.GradientDrawable makeBallDrawable() {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(0xFF1B5E20); // 深绿色，MC风格
        gd.setStroke(dp2px(3), 0xFF76FF03);
        return gd;
    }

    // ───── 功能面板 ─────
    private void createPanel() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xEE0D1B0A);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp2px(12), dp2px(12), dp2px(12), dp2px(16));

        // 标题栏
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚔ MC 辅助工具");
        tvTitle.setTextColor(0xFF76FF03);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView btnClose = new TextView(this);
        btnClose.setText("✕");
        btnClose.setTextColor(0xFFFF5252);
        btnClose.setTextSize(18);
        btnClose.setPadding(dp2px(8), dp2px(4), dp2px(4), dp2px(4));
        btnClose.setOnClickListener(v -> hidePanel());

        titleBar.addView(tvTitle);
        titleBar.addView(btnClose);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF76FF03);
        LinearLayout.LayoutParams divLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp2px(1));
        divLP.setMargins(0, dp2px(6), 0, dp2px(10));
        divider.setLayoutParams(divLP);

        container.addView(titleBar);
        container.addView(divider);

        // ── 四大功能分类 ──
        addCategory(container, "⚔ 战斗", new String[][]{
                {"自动攻击", "0"},
                {"暴击辅助", "1"},
                {"自动格挡", "2"},
                {"范围伤害", "3"},
                {"一击必杀 (仅测试)", "4"}
        });

        addCategory(container, "🏃 移动", new String[][]{
                {"飞行模式", "5"},
                {"疾跑加速", "6"},
                {"无限跳跃", "7"},
                {"无摔落伤害", "8"}
        });

        addCategory(container, "👁 视觉", new String[][]{
                {"透视 (X-Ray)", "9"},
                {"夜视模式", "10"},
                {"高亮实体", "11"},
                {"全图迷雾去除", "12"}
        });

        addCategory(container, "🛠 辅助", new String[][]{
                {"自动采集", "13"},
                {"自动搭桥", "14"},
                {"防踢检测绕过", "15"},
                {"反AFK (防挂机)", "16"},
                {"自动钓鱼", "17"},
                {"物品自动整理", "18"}
        });

        scrollView.addView(container);

        int panelW = dp2px(260);
        int panelH = dp2px(400);
        panelParams = new WindowManager.LayoutParams(
                panelW, panelH,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 80;
        panelParams.y = 200;

        panelView = scrollView;
        panelView.setVisibility(View.GONE);
        windowManager.addView(panelView, panelParams);
    }

    private void addCategory(LinearLayout parent, String title, String[][] items) {
        // 分类标题
        TextView tvCat = new TextView(this);
        tvCat.setText(title);
        tvCat.setTextColor(0xFFFFD600);
        tvCat.setTextSize(14);
        tvCat.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams catLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        catLP.setMargins(0, dp2px(8), 0, dp2px(4));
        tvCat.setLayoutParams(catLP);

        // 分类背景
        android.graphics.drawable.GradientDrawable catBg = new android.graphics.drawable.GradientDrawable();
        catBg.setColor(0xFF1A3320);
        catBg.setCornerRadius(dp2px(4));
        tvCat.setBackground(catBg);
        tvCat.setPadding(dp2px(10), dp2px(6), dp2px(10), dp2px(6));
        parent.addView(tvCat);

        // 功能开关列表
        for (String[] item : items) {
            addSwitchRow(parent, item[0], Integer.parseInt(item[1]));
        }
    }

    private void addSwitchRow(LinearLayout parent, String label, int idx) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp2px(40));
        rowLP.setMargins(dp2px(4), 0, dp2px(4), 0);
        row.setLayoutParams(rowLP);
        row.setPadding(dp2px(6), 0, dp2px(6), 0);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFFCCFFCC);
        tv.setTextSize(13);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Switch sw = new Switch(this);
        sw.setChecked(switchStates[idx]);
        int finalIdx = idx;
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchStates[finalIdx] = isChecked;
            Toast.makeText(this, label + (isChecked ? " 已开启" : " 已关闭"),
                    Toast.LENGTH_SHORT).show();
        });

        // 分隔线
        View sep = new View(this);
        sep.setBackgroundColor(0x33AAFFAA);

        row.addView(tv);
        row.addView(sw);

        FrameLayout wrapper = new FrameLayout(this);
        LinearLayout.LayoutParams wLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapper.setLayoutParams(wLP);
        wrapper.addView(row);

        View sepLine = new View(this);
        LinearLayout.LayoutParams sepLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sepLine.setLayoutParams(sepLP);
        sepLine.setBackgroundColor(0x22AAFFAA);

        parent.addView(wrapper);
        parent.addView(sepLine);
    }

    // ───── 显示 / 隐藏面板 ─────
    private void togglePanel() {
        if (isPanelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        panelView.setVisibility(View.VISIBLE);
        isPanelVisible = true;
    }

    private void hidePanel() {
        panelView.setVisibility(View.GONE);
        isPanelVisible = false;
    }

    // ───── 悬浮球拖动监听 ─────
    private class BallTouchListener implements View.OnTouchListener {
        private int initX, initY;
        private float initTouchX, initTouchY;
        private long downTime;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initX = ballParams.x;
                    initY = ballParams.y;
                    initTouchX = event.getRawX();
                    initTouchY = event.getRawY();
                    downTime = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    ballParams.x = initX + (int)(event.getRawX() - initTouchX);
                    ballParams.y = initY + (int)(event.getRawY() - initTouchY);
                    windowManager.updateViewLayout(floatBallView, ballParams);
                    return true;
                case MotionEvent.ACTION_UP:
                    long elapsed = System.currentTimeMillis() - downTime;
                    float dx = Math.abs(event.getRawX() - initTouchX);
                    float dy = Math.abs(event.getRawY() - initTouchY);
                    if (elapsed < 250 && dx < 10 && dy < 10) {
                        togglePanel();
                    }
                    return true;
            }
            return false;
        }
    }

    // ───── 工具方法 ─────
    private int dp2px(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatBallView != null) windowManager.removeView(floatBallView);
        if (panelView != null) windowManager.removeView(panelView);
    }
}
