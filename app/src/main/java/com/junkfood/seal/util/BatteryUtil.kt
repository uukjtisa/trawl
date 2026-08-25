package com.junkfood.seal.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.junkfood.seal.R

object BatteryUtil {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        // The ONLY correct, Google-documented way to check the Doze/App-Standby battery
        // optimization whitelist is PowerManager.isIgnoringBatteryOptimizations() — see
        // https://developer.android.com/training/monitoring-device-state/doze-standby
        // ("An app can check whether it is currently on the exemption list by calling
        // isIgnoringBatteryOptimizations()"). Do not add any other fallback here: a previous
        // version of this function also checked the "android:run_any_in_background" AppOps op
        // as a fallback, assuming a MODE_ALLOWED result meant the app was exempt from battery
        // optimizations. That assumption was wrong — RUN_ANY_IN_BACKGROUND is a *different*,
        // unrelated app-op (background data/network execution), and it defaults to
        // MODE_ALLOWED for most apps regardless of whether battery optimization/Battery Saver
        // is actually restricting the app. That meant this function could incorrectly report
        // "true" (already ignoring optimizations) even while the device was still in Battery
        // Saver mode with the app NOT whitelisted — which silently suppressed the
        // "please disable battery optimization" dialog for exactly the users who needed to see
        // it. There is also no similarly reliable per-OEM "battery saver mode" API to check
        // instead — OEM battery-saver toggles (MIUI/HyperOS "Battery saver" per-app setting,
        // One UI "Sleeping apps", etc.) are proprietary and unqueryable, which is exactly why
        // BatteryUtil.launchBatterySettings() below routes the user to each OEM's own
        // settings screen to fix it manually instead of trying to detect it programmatically.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("oppo") || manufacturer.contains("oneplus") -> Manufacturer.OPPO
            manufacturer.contains("realme") -> Manufacturer.REALME
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> Manufacturer.XIAOMI
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("motorola") || manufacturer.contains("moto") -> Manufacturer.MOTOROLA
            manufacturer.contains("nothing") -> Manufacturer.NOTHING
            else -> Manufacturer.OTHER
        }
    }


    /**
     * Every battery screen worth trying, most specific first.
     *
     * WHY A LIST AND NOT ONE INTENT. `resolveActivity` answers "does this component exist", NOT
     * "may I start it". On Huawei those differ: `com.huawei.systemmanager`'s startup-manager
     * activity resolves happily and then throws
     *
     *     SecurityException: Permission Denial ... requires
     *     com.huawei.permission.external_app_settings.USE_COMPONENT
     *
     * on `startActivity`, because that permission is signature-level and no third-party app can
     * hold it. The old code picked exactly that intent and the button died on every Huawei
     * device. The same trap exists on Xiaomi, Oppo and Vivo, whose OEM screens are guarded the
     * same way -- so the answer is not "special-case Huawei", it is to stop believing
     * resolvability and try each candidate until one starts.
     *
     * The list always ends with Android's own screens, which are public API and cannot be
     * permission-denied, so it can never come back empty-handed.
     */
    @SuppressLint("BatteryLife")
    fun buildBatterySettingsIntents(context: Context): List<Intent> {
        val pkg = context.packageName
        val candidates = mutableListOf<Intent>()

        // FIRST, always: Android's own one-tap grant dialog. It is public API, so no ROM can
        // permission-deny it, and it hands back exactly the whitelist entry this dialog is
        // asking about without anyone visiting a settings screen.
        //
        // This used to be second, behind the OEM screen. On Huawei that meant every user watched
        // the OEM intent fail with a SecurityException before landing on the thing that works in
        // one tap. The OEM screens are still here, below, because on some ROMs the AOSP whitelist
        // is genuinely not sufficient -- but they are the fallback, not the opening move.
        val ignoreIntent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
            }
        if (isIntentResolvable(context, ignoreIntent)) candidates += ignoreIntent

        when (getManufacturer()) {
            Manufacturer.OPPO, Manufacturer.REALME -> tryOppoBatteryIntent(context, pkg)
            Manufacturer.XIAOMI -> tryXiaomiBatteryIntent(context, pkg)
            Manufacturer.VIVO -> tryVivoBatteryIntent(context, pkg)
            Manufacturer.HUAWEI -> tryHuaweiBatteryIntent(context, pkg)
            Manufacturer.SAMSUNG -> trySamsungBatteryIntent(context, pkg)
            else -> null
        }?.let { candidates += it }

        val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (isIntentResolvable(context, settingsIntent)) candidates += settingsIntent

        candidates +=
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
            }

        return candidates
    }

    /**
     * Opens the first battery screen that actually starts.
     *
     * [launch] is expected to throw when the target refuses -- SecurityException for a guarded
     * OEM component, ActivityNotFoundException for one that has been removed -- which is exactly
     * the signal to move to the next candidate. Returns false only if every one failed, so the
     * caller can say something useful instead of appearing to do nothing.
     */
    fun launchBatterySettings(context: Context, launch: (Intent) -> Unit): Boolean {
        for (intent in buildBatterySettingsIntents(context)) {
            val ok =
                runCatching { launch(intent) }
                    .onFailure {
                        TrawlLog.w(
                            "battery: " + (intent.component?.className ?: intent.action) +
                                " refused (" + it.javaClass.simpleName + ")"
                        )
                    }
                    .isSuccess
            if (ok) return true
        }
        TrawlLog.e("battery: every candidate screen refused to open")
        return false
    }

    private fun tryOppoBatteryIntent(context: Context, pkg: String): Intent? {
        val colorosSettings = Intent().apply {
            setClassName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaueze.PowerUsageDetailActivity"
            )
            putExtra("package_name", pkg)
        }
        if (isIntentResolvable(context, colorosSettings)) return colorosSettings

        val heytapSettings = Intent().apply {
            setClassName(
                "com.heytap.manager",
                "com.heytap.manager.PowerUsageDetailActivity"
            )
            putExtra("package_name", pkg)
        }
        if (isIntentResolvable(context, heytapSettings)) return heytapSettings

        val colorosCenter = Intent().apply {
            setClassName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppDetailActivity"
            )
            putExtra("package_name", pkg)
        }
        if (isIntentResolvable(context, colorosCenter)) return colorosCenter

        val colorosManager = Intent().apply {
            setClassName(
                "com.coloros.oppomanager",
                "com.coloros.oppomanager.appdetail.AppDetailActivity"
            )
            putExtra("packageName", pkg)
        }
        if (isIntentResolvable(context, colorosManager)) return colorosManager

        return null
    }

    @SuppressLint("BatteryLife")
    private fun tryXiaomiBatteryIntent(context: Context, pkg: String): Intent? {
        // 1. Standard Android ACTION — on MIUI/HyperOS this triggers the OEM-native system
        //    dialog that directly adds the app to "No restrictions". Most reliable across
        //    all MIUI versions and HyperOS 1/2 (Android 13–17+).
        val standardIgnore = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$pkg")
        }
        if (isIntentResolvable(context, standardIgnore)) return standardIgnore

        // 2. HyperOS / MIUI 16+ — unified power management app detail page
        val hyperOsPower = Intent().apply {
            setClassName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HybridAppManageActivity"
            )
            putExtra("package_name", pkg)
        }
        if (isIntentResolvable(context, hyperOsPower)) return hyperOsPower

        // 3. MIUI 12–15 — per-app battery use detail page
        val miuiBattery = Intent().apply {
            setClassName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.PowerUseDetailActivity"
            )
            putExtra("package_name", pkg)
        }
        if (isIntentResolvable(context, miuiBattery)) return miuiBattery

        // 4. Older MIUI — permissions editor (has battery section)
        val miuiPermissions = Intent().apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", pkg)
        }
        if (isIntentResolvable(context, miuiPermissions)) return miuiPermissions

        // 5. Last resort — Security Center main screen (user navigates manually)
        val miuiSecurity = Intent().apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.securitycenter.MainActivity"
            )
        }
        if (isIntentResolvable(context, miuiSecurity)) return miuiSecurity

        return null
    }

    private fun tryVivoBatteryIntent(context: Context, pkg: String): Intent? {
        val vivoPerm = Intent().apply {
            setClassName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"
            )
            putExtra("packagename", pkg)
        }
        if (isIntentResolvable(context, vivoPerm)) return vivoPerm

        val vivoManager = Intent().apply {
            setClassName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        }
        if (isIntentResolvable(context, vivoManager)) return vivoManager

        return null
    }

    private fun tryHuaweiBatteryIntent(context: Context, pkg: String): Intent? {
        val huaweiMgr = Intent().apply {
            setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        }
        if (isIntentResolvable(context, huaweiMgr)) return huaweiMgr

        val huaweiProtection = Intent().apply {
            setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        }
        if (isIntentResolvable(context, huaweiProtection)) return huaweiProtection

        return null
    }

    private fun trySamsungBatteryIntent(context: Context, pkg: String): Intent? {
        val batteryIntent = Intent().apply {
            setClassName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            )
            putExtra("showtype", "package")
            putExtra("package", pkg)
        }
        if (isIntentResolvable(context, batteryIntent)) return batteryIntent

        return null
    }

    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    intent,
                    android.content.pm.PackageManager.ResolveInfoFlags.of(0L)
                ) != null
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(intent, 0) != null
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getBatterySettingsDescResId(manufacturer: Manufacturer): Int {
        return when (manufacturer) {
            Manufacturer.OPPO, Manufacturer.REALME -> R.string.battery_settings_desc_oppo
            Manufacturer.XIAOMI -> R.string.battery_settings_desc_xiaomi
            Manufacturer.VIVO -> R.string.battery_settings_desc_vivo
            Manufacturer.HUAWEI -> R.string.battery_settings_desc_huawei
            // Samsung's One UI has its own "Put unused apps to sleep" / "Deep sleeping apps"
            // system that is separate from stock Android battery optimization — disabling the
            // latter alone is not enough on One UI, same class of problem as the other OEMs.
            Manufacturer.SAMSUNG -> R.string.battery_settings_desc_samsung
            else -> R.string.battery_settings_desc
        }
    }

    enum class Manufacturer {
        OPPO, REALME, XIAOMI, VIVO, HUAWEI, SAMSUNG, MOTOROLA, NOTHING, OTHER
    }
}
