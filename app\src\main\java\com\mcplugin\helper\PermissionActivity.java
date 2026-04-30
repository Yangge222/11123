package com.mcplugin.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 启动页：引导用户授予悬浮窗权限
 */
public class PermissionActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        Button btnGrant = findViewById(R.id.btn_grant);
        Button btnStart = findViewById(R.id.btn_start);
        TextView tvStatus = findViewById(R.id.tv_status);

        updateStatus(tvStatus, btnStart);

        btnGrant.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY);
            }
        });

        btnStart.setOnClickListener(v -> {
            if (hasOverlayPermission()) {
                startFloatingService();
            } else {
                Toast.makeText(this, "请先授予悬浮窗权限！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvStatus = findViewById(R.id.tv_status);
        Button btnStart = findViewById(R.id.btn_start);
        updateStatus(tvStatus, btnStart);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            TextView tvStatus = findViewById(R.id.tv_status);
            Button btnStart = findViewById(R.id.btn_start);
            updateStatus(tvStatus, btnStart);
        }
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void updateStatus(TextView tvStatus, Button btnStart) {
        if (hasOverlayPermission()) {
            tvStatus.setText("✅ 悬浮窗权限已授予");
            tvStatus.setTextColor(0xFF4CAF50);
            btnStart.setEnabled(true);
            btnStart.setAlpha(1f);
        } else {
            tvStatus.setText("❌ 悬浮窗权限未授予");
            tvStatus.setTextColor(0xFFF44336);
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.5f);
        }
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "悬浮球已启动！", Toast.LENGTH_SHORT).show();
        finish(); // 关闭引导页，回到游戏
    }
}
