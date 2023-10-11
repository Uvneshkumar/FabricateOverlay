package tk.zwander.fabricateoverlaysample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import tk.zwander.fabricateoverlay.FabricatedOverlay
import tk.zwander.fabricateoverlay.FabricatedOverlayEntry
import tk.zwander.fabricateoverlay.OverlayAPI
import tk.zwander.fabricateoverlay.ShizukuUtils
import tk.zwander.fabricateoverlaysample.ui.pages.AppListPage
import tk.zwander.fabricateoverlaysample.ui.pages.CurrentOverlayEntriesListPage
import tk.zwander.fabricateoverlaysample.ui.pages.HomePage

@SuppressLint("PrivateApi")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HiddenApiBypass.setHiddenApiExemptions("L")

        if (!ShizukuUtils.shizukuAvailable) {
            showShizukuDialog()
            return
        }

        if (ShizukuUtils.hasShizukuPermission(this)) {
            init()
        } else {
            ShizukuUtils.requestShizukuPermission(this) { granted ->
                if (granted) {
                    init()
                } else {
                    showShizukuDialog()
                }
            }
        }
    }

    private fun showShizukuDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.shizuku_not_set_up)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener {
                    findViewById<TextView>(
                        Class.forName("com.android.internal.R\$id").getField("message").getInt(null)
                    )
                        ?.movementMethod = LinkMovementMethod()
                }
            }
            .show()
    }

    private fun init() {
        setContent {
            MaterialTheme(
                colors = darkColors()
            ) {
                Surface {
                    var appInfoArg by remember {
                        mutableStateOf<ApplicationInfo?>(null)
                    }
                    val navController = rememberNavController()
                    val activity = LocalContext.current as Activity

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            activity.setTitle(R.string.overlays)

                            HomePage(navController)
                        }
                        composable("app_list") {
                            activity.setTitle(R.string.apps)

                            AppListPage(navController)
                        }
                        composable("app_others") {
                            startActivity(Intent(this@MainActivity, OthersActivity::class.java))
                        }
                        composable(
                            route = "list_overlays"
                        ) {
                            navController.previousBackStackEntry?.arguments?.getParcelable<ApplicationInfo>(
                                "appInfo"
                            )?.let {
                                appInfoArg = it
                            }

                            activity.title = appInfoArg?.loadLabel(activity.packageManager)

                            CurrentOverlayEntriesListPage(
                                navController,
                                appInfoArg!!
                            )
                        }
                    }
                }
            }
        }

        val launcherPackage = "com.android.launcher3"
        val systemUIPackage = "com.android.systemui"
        val sourcePackage = OverlayAPI.servicePackage ?: "com.android.shell"

        val listOfOverlays = listOf(
            FabricatedOverlay(
                launcherPackage.overlay(),
                launcherPackage,
                sourcePackage
            ).apply {
                listOf(
                    FabricatedOverlayEntry(
                        "$launcherPackage:dimen/all_apps_search_bar_bottom_padding",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, 50)
                    )
                ).forEach { overlay ->
                    entries[overlay.resourceName] = overlay
                }
            },
            FabricatedOverlay(
                systemUIPackage.overlay(),
                systemUIPackage,
                sourcePackage
            ).apply {
                listOf(
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/keyguard_large_clock_top_margin",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, -300)
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/keyguard_affordance_vertical_offset",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, -300)
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/qs_top_brightness_margin_bottom",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, 20)
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/biometric_dialog_border_padding",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, 0)
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/biometric_dialog_corner_size",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, 16)
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/global_actions_translate",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, 0)
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:color/material_dynamic_primary90",
                        TypedValue.TYPE_INT_COLOR_ARGB8,
                        getParsedColor("0xffffffff")
                    ),
                    FabricatedOverlayEntry(
                        "$systemUIPackage:dimen/status_bar_system_icon_spacing",
                        TypedValue.TYPE_DIMENSION,
                        getParsedDimen(TypedValue.COMPLEX_UNIT_DIP, 0)
                    )
                ).forEach { overlay ->
                    entries[overlay.resourceName] = overlay
                }
            }
        )

        OverlayAPI.getInstance(this) { api ->
            listOfOverlays.forEach { overlay ->
                api.registerFabricatedOverlay(overlay)
                api.setEnabled(
                    FabricatedOverlay.generateOverlayIdentifier(
                        overlay.overlayName,
                        overlay.sourcePackage
                    ), true, 0
                )
            }
        }

        OverlayAPI.getInstance(this) { api ->
            val listOfFOs = api.getAllOverlays(-2).mapNotNull { (key, value) ->
                val filtered = value.filter { item ->
                    item.isFabricated && item.overlayName?.contains(packageName) == true
                }
                if (filtered.isEmpty()) null
                else (packageManager.run {
                    try {
                        getApplicationInfo(key, 0).loadLabel(this)
                    } catch (nameNotFoundException: PackageManager.NameNotFoundException) {
                        //package has been uninstalled before uninstalling related overlays
                        key
                    }
                }.toString() to filtered)
            }.toMap().toSortedMap { o1, o2 -> o1.compareTo(o2, true) }
            listOfFOs.forEach { (t, u) ->
                u.filter { it.overlayName?.endsWith(".overlay") == false }.forEach {
                    api.setEnabled(
                        FabricatedOverlay.generateOverlayIdentifier(
                            it.overlayName, it.packageName
                        ), it.isEnabled, 0
                    )
                }
            }
        }

        val saturation = Prefs.saturationValue
        Runtime.getRuntime().exec("su -c service call SurfaceFlinger 1022 f $saturation").waitFor()
        Runtime.getRuntime().exec("su -c setprop persist.sys.sf.color_saturation $saturation")
            .waitFor()
    }

    fun String.overlay(): String {
        return "$packageName.$this.overlay"
    }

    fun getParsedColor(value: String): Int {
        return Integer.parseUnsignedInt(value.substring(2), 16)
    }

    fun getParsedDimen(type: Int, value: Int): Int {
        return TypedValue::class.java
            .getMethod("createComplexDimension", Int::class.java, Int::class.java)
            .invoke(null, value, type) as Int
    }
}