package com.furflix.app.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.furflix.app.ui.theme.AppIcon

object IconManager {

    /**
     * Полностью переключает activity-alias на newIcon и перезапускает
     * приложение управляемым образом.
     *
     * История фиксов (для контекста):
     * 1. Первая версия полагалась на onStop() — ненадёжно, система может
     *    убить процесс раньше, чем выполнится код.
     * 2. Вторая версия делала всё синхронно + killProcess — сама смена
     *    алиасов заработала стабильно, но лапка иконки рисовалась
     *    неправильно (артефакт маскирования на MIUI).
     * 3. Третья версия добавила AlarmManager для релонча ПОСЛЕ killProcess —
     *    но по логам выяснилось, что MIUI (и Android 10+ в целом) блокирует
     *    "Background activity launch" когда PendingIntent пытается запустить
     *    Activity в момент, когда процесс уже не в foreground:
     *    "Background activity launch blocked! ... realCallerStartMode:
     *    MODE_BACKGROUND_ACTIVITY_START_DENIED". Сама смена алиасов при
     *    этом происходила успешно (лаунчер обновлял ярлык), но релаунч
     *    не срабатывал — отсюда "иконка на ярлыке сменилась/мигнула,
     *    а приложение не переоткрылось".
     *
     * Решение (текущее): требует передать текущую Activity, пока она ещё
     * в foreground (т.е. вызывается прямо из UI, до killProcess). Мы:
     *  1) синхронно меняем activity-alias (как и раньше),
     *  2) запускаем НОВЫЙ таск через startActivity с явным component на
     *     MainActivity, пока наш процесс ещё активен и имеет право
     *     стартовать Activity (в отличие от отложенного через AlarmManager),
     *  3) и только потом убиваем процесс — уже после того как новый таск
     *     гарантированно запущен и виден пользователю.
     * Это тот же паттерн, что использует стандартный ActivityManager при
     * "Restart app" в других icon-changer приложениях: релаунч должен
     * происходить, пока запускающий процесс ещё активен на экране.
     */
    fun applyIconAndRestart(activity: Activity, newIcon: AppIcon) {
        val appContext = activity.applicationContext
        val packageManager = appContext.packageManager

        // Шаг 1: включаем новый alias
        safeSetEnabled(
            packageManager,
            ComponentName(appContext, newIcon.aliasName),
            enable = true
        )

        // Шаг 2: синхронно выключаем все остальные алиасы
        AppIcon.entries
            .filter { it.aliasName != newIcon.aliasName }
            .forEach { icon ->
                safeSetEnabled(
                    packageManager,
                    ComponentName(appContext, icon.aliasName),
                    enable = false
                )
            }

        // Шаг 3: запускаем новый таск, пока мы ещё в foreground —
        // это ключевое отличие от предыдущей версии с AlarmManager.
        // FLAG_ACTIVITY_NEW_TASK + CLEAR_TASK гарантируют чистый старт
        // без старого back stack, содержащего активность со старым alias.
        val restartIntent = Intent(appContext, com.furflix.app.MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        activity.startActivity(restartIntent)

        // Шаг 4: убиваем старый процесс. Новый таск уже запущен на шаге 3,
        // так что пользователь не увидит "пустого" момента — только смену
        // экрана на новый инстанс приложения с уже применённой иконкой.
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun safeSetEnabled(
        packageManager: PackageManager,
        component: ComponentName,
        enable: Boolean
    ) {
        try {
            val targetState = if (enable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                component,
                targetState,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            // Не даём сбою одного алиаса сломать переключение остальных.
        }
    }
}