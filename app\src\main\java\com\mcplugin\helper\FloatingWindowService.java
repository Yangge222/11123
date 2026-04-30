package com.mcplugin.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    // 当前选中的分类索引
    private int currentCategory = 0;
    
    // 各功能开关状态
    private final boolean[] switchStates = new boolean[30];

    // 分类名称
    private final String[] categoryNames = {"战斗类", "世界类", "渲染类", "其他类"};
    
    // 所有功能数据
    private final String[][][] allFeatures = {
        // 战斗类
        {
            {"杀戮光环", "0"}, {"刀刀暴击", "1"}, {"上帝隐身", "2"}, {"环绕目标", "3"},
            {"攻击翻倍", "4"}, {"防止击退", "5"}, {"无限耐久", "6"}, {"攻击特效", "7"}
        },
        // 世界类
        {
            {"强制飞行", "8"}, {"穿墙", "9"}, {"无视摔伤", "10"}, {"点击传送", "11"},
            {"无视踢出", "12"}, {"强制成员", "13"}, {"掉落物刷盒子", "14"}, {"容器刷盒子", "15"}
        },
        // 渲染类
        {
            {"矿物标记", "16"}, {"实体标记", "17"}, {"容器标记", "18"}, {"运动标记", "19"},
            {"人物旋转", "20"}
        },
        // 其他类
        {
            {"攻击自己", "21"}, {"一键刷屏", "22"}
        }
    };

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
                    CHANNEL_ID, "草方块盒子悬浮窗", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持悬浮窗在前台运行");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("草方块盒子运行中")
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

    private android.graphics.drawable.Drawable makeBallDrawable() {
        // 优先使用自定义图标
        try {
            android.graphics.drawable.Drawable d = getResources().getDrawable(R.drawable.float_ball);
            if (d != null) return d;
        } catch (Exception e) {
            // 兜底绿色圆形
        }
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(0xFF1B5E20);
        gd.setStroke(dp2px(3), 0xFF76FF03);
        return gd;
    }

    // ───── 功能面板 ─────
    private LinearLayout categoryContainer;
    private LinearLayout contentContainer;
    private TextView[] categoryTabs = new TextView[4];

    private void createPanel() {
        // 主容器 - 黑色背景
        FrameLayout mainContainer = new FrameLayout(this);
        mainContainer.setBackgroundColor(0xFF000000);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(0xFF000000);

        // ── 彩虹标题 ──
        TextView titleView = new TextView(this);
        titleView.setText("草方块盒子2.3.6");
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, dp2px(16), 0, dp2px(12));
        
        // 彩虹渐变文字效果
        android.graphics.LinearGradient shader = new android.graphics.LinearGradient(
                0, 0, dp2px(200), 0,
                new int[]{0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00, 
                         0xFF0000FF, 0xFF4B0082, 0xFF9400D3},
                null, android.graphics.Shader.TileMode.CLAMP);
        titleView.getPaint().setShader(shader);
        titleView.setTextColor(0xFFFFFFFF);
        
        rootLayout.addView(titleView);

        // ── 分类标签栏（青色背景）──
        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(0xFF00CED1); // 青色
        tabBar.setPadding(0, dp2px(8), 0, dp2px(8));
        
        for (int i = 0; i < 4; i++) {
            final int index = i;
            TextView tab = new TextView(this);
            tab.setText(categoryNames[i]);
            tab.setTextSize(14);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp2px(8), dp2px(4), dp2px(8), dp2px(4));
            tab.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            // 选中状态样式
            updateTabStyle(tab, i == currentCategory);
            
            tab.setOnClickListener(v -> {
                currentCategory = index;
                updateAllTabStyles();
                refreshContent();
            });
            
            categoryTabs[i] = tab;
            tabBar.addView(tab);
        }
        rootLayout.addView(tabBar);

        // ── 内容区域（可滚动）──
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF000000);
        
        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dp2px(12), dp2px(12), dp2px(12), dp2px(16));
        contentContainer.setBackgroundColor(0xFF000000);
        
        scrollView.addView(contentContainer);
        rootLayout.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // ── 关闭按钮 ──
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕ 关闭");
        closeBtn.setTextColor(0xFFFF5252);
        closeBtn.setTextSize(14);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(0, dp2px(8), 0, dp2px(8));
        closeBtn.setOnClickListener(v -> hidePanel());
        rootLayout.addView(closeBtn);

        mainContainer.addView(rootLayout);

        // 初始化显示第一个分类
        refreshContent();

        int panelW = dp2px(300);
        int panelH = dp2px(450);
        panelParams = new WindowManager.LayoutParams(
                panelW, panelH,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 50;
        panelParams.y = 150;

        panelView = mainContainer;
        panelView.setVisibility(View.GONE);
        windowManager.addView(panelView, panelParams);
    }

    private void updateTabStyle(TextView tab, boolean selected) {
        if (selected) {
            tab.setTextColor(0xFFFFFFFF);
            tab.setBackgroundColor(0xFF008B8B); // 深青色
        } else {
            tab.setTextColor(0xFFE0FFFF);
            tab.setBackgroundColor(0x00000000);
        }
    }

    private void updateAllTabStyles() {
        for (int i = 0; i < 4; i++) {
            updateTabStyle(categoryTabs[i], i == currentCategory);
        }
    }

    private void refreshContent() {
        contentContainer.removeAllViews();
        
        String[][] features = allFeatures[currentCategory];
        
        // 两列布局
        for (int i = 0; i < features.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp2px(48)));
            
            // 左侧功能
            addFeatureItem(row, features[i]);
            
            // 右侧功能（如果有）
            if (i + 1 < features.length) {
                addFeatureItem(row, features[i + 1]);
            } else {
                // 占位
                View placeholder = new View(this);
                placeholder.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
                row.addView(placeholder);
            }
            
            contentContainer.addView(row);
        }
    }

    private void addFeatureItem(LinearLayout parent, String[] feature) {
        String name = feature[0];
        int idx = Integer.parseInt(feature[1]);
        
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        item.setPadding(dp2px(4), 0, dp2px(4), 0);
        
        // 功能名称
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(0xFFFFFFFF);
        nameView.setTextSize(14);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        
        // 圆形开关按钮
        FrameLayout switchContainer = new FrameLayout(this);
        switchContainer.setLayoutParams(new LinearLayout.LayoutParams(dp2px(44), dp2px(28)));
        
        // 开关背景（轨道）
        View track = new View(this);
        GradientDrawable trackDrawable = new GradientDrawable();
        trackDrawable.setShape(GradientDrawable.RECTANGLE);
        trackDrawable.setCornerRadius(dp2px(14));
        trackDrawable.setColor(switchStates[idx] ? 0xFF4CAF50 : 0xFF757575);
        track.setBackground(trackDrawable);
        track.setLayoutParams(new FrameLayout.LayoutParams(dp2px(40), dp2px(20), Gravity.CENTER));
        
        // 圆形滑块
        View thumb = new View(this);
        GradientDrawable thumbDrawable = new GradientDrawable();
        thumbDrawable.setShape(GradientDrawable.OVAL);
        thumbDrawable.setColor(0xFFFFFFFF);
        thumb.setBackground(thumbDrawable);
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp2px(24), dp2px(24), Gravity.CENTER_VERTICAL);
        thumbParams.leftMargin = switchStates[idx] ? dp2px(18) : dp2px(2);
        thumb.setLayoutParams(thumbParams);
        
        switchContainer.addView(track);
        switchContainer.addView(thumb);
        
        // 点击切换
        final int finalIdx = idx;
        final String finalName = name;
        switchContainer.setOnClickListener(v -> {
            switchStates[finalIdx] = !switchStates[finalIdx];
            
            // 更新UI
            trackDrawable.setColor(switchStates[finalIdx] ? 0xFF4CAF50 : 0xFF757575);
            thumbParams.leftMargin = switchStates[finalIdx] ? dp2px(18) : dp2px(2);
            thumb.setLayoutParams(thumbParams);
            
            Toast.makeText(this, finalName + (switchStates[finalIdx] ? " 已开启" : " 已关闭"),
                    Toast.LENGTH_SHORT).show();
        });
        
        item.addView(nameView);
        item.addView(switchContainer);
        parent.addView(item);
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
