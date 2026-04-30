package com.caofangkuaihezi.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_OVERLAY = 1001
    private var currentTab = "battle"
    private val switchStates = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查悬浮窗权限
        checkOverlayPermission()

        setupTabs()
        setupFeatures()
        showFeatures("battle")
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                findViewById<LinearLayout>(R.id.permission_container).visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.content_container).visibility = View.GONE
            } else {
                startFloatingService()
                findViewById<LinearLayout>(R.id.permission_container).visibility = View.GONE
                findViewById<LinearLayout>(R.id.content_container).visibility = View.VISIBLE
            }
        }
    }

    fun requestOverlayPermission(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            checkOverlayPermission()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun setupTabs() {
        findViewById<TextView>(R.id.tab_battle).setOnClickListener { switchTab("battle") }
        findViewById<TextView>(R.id.tab_world).setOnClickListener { switchTab("world") }
        findViewById<TextView>(R.id.tab_render).setOnClickListener { switchTab("render") }
        findViewById<TextView>(R.id.tab_other).setOnClickListener { switchTab("other") }
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        val tabs = listOf(
            R.id.tab_battle to "battle",
            R.id.tab_world to "world",
            R.id.tab_render to "render",
            R.id.tab_other to "other"
        )

        tabs.forEach { (id, name) ->
            val tabView = findViewById<TextView>(id)
            if (name == tab) {
                tabView.setBackgroundResource(R.drawable.tab_selected)
            } else {
                tabView.setBackgroundResource(R.drawable.tab_unselected)
            }
        }
        showFeatures(tab)
    }

    private fun showFeatures(tab: String) {
        findViewById<LinearLayout>(R.id.battle_features).visibility = if (tab == "battle") View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.world_features).visibility = if (tab == "world") View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.render_features).visibility = if (tab == "render") View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.other_features).visibility = if (tab == "other") View.VISIBLE else View.GONE
    }

    private fun setupFeatures() {
        setupFeature(R.id.feature_autopunch, "自动攻击", "battle_autopunch")
        setupFeature(R.id.feature_crit, "暴击辅助", "battle_crit")
        setupFeature(R.id.feature_autoblock, "自动格挡", "battle_autoblock")
        setupFeature(R.id.feature_range, "范围伤害", "battle_range")
        setupFeature(R.id.feature_fly, "飞行模式", "world_fly")
        setupFeature(R.id.feature_noclip, "穿墙模式", "world_noclip")
        setupFeature(R.id.feature_speed, "加速移动", "world_speed")
        setupFeature(R.id.feature_esp, "人物透视", "render_esp")
        setupFeature(R.id.feature_xray, "矿物透视", "render_xray")
        setupFeature(R.id.feature_nametag, "显示ID", "render_nametag")
        setupFeature(R.id.feature_inventory, "背包整理", "other_inventory")
        setupFeature(R.id.feature_potion, "药水控制", "other_potion")
    }

    private fun setupFeature(viewId: Int, name: String, key: String) {
        val view = findViewById<LinearLayout>(viewId)
        val nameView = view.findViewById<TextView>(R.id.feature_name)
        val switchView = view.findViewById<ImageView>(R.id.switch_button)
        nameView.text = name
        switchStates[key] = false
        switchView.setOnClickListener {
            val isOn = !(switchStates[key] ?: false)
            switchStates[key] = isOn
            if (isOn) {
                switchView.setBackgroundResource(R.drawable.circle_switch_on)
            } else {
                switchView.setBackgroundResource(R.drawable.circle_switch_off)
            }
            Toast.makeText(this, if (isOn) "$name 已开启" else "$name 已关闭", Toast.LENGTH_SHORT).show()
        }
    }
}
