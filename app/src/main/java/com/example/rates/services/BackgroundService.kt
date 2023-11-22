package com.example.rates.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.example.rates.MainActivity
import com.example.rates.R
import com.example.rates.utils.LogUtils
import org.jsoup.Jsoup
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class BackgroundService : Service() {

    private lateinit var yong: Thread

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val handler = Handler()
//        val runnable = object : Runnable {
//            override fun run() {
//                handler.postDelayed(this, 3000)
//                Log.d("hello: ", "test")
//                Toast.makeText(MainActivity.appContext(), "hello", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        handler.post(runnable)

        var cimbRates: Float = Float.NaN
        var otherRates: Float = Float.NaN


        val CHANNEL_ID = "Foreground Service ID"
        val notificationChannel =
            NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            notificationChannel
        )
        val builder = Notification.Builder(this, CHANNEL_ID)
        val notification = builder
            .setContentText("Primary: " + cimbRates.toString() + ", Secondary: " + otherRates.toString())
            .setContentTitle("Service enabled").setSmallIcon(
                R.drawable.ic_launcher_background
            )

//        if (intent!!.action.equals("START")) {
        Log.d("test", "START")
        startForeground(1001, notification.build())
//        } else {
//            Log.d("test", "STOP")
//
//            startForeground(1001, notification.build())
//            stopForeground(STOP_FOREGROUND_REMOVE)
//            stopSelf()
////            return START_NOT_STICKY
//        }

        yong = Thread(Runnable {
            try {
                while (true) {
                    val cimbUrl = URL(MainActivity.res().getString(R.string.cimb_url))
                    val otherUrl = URL(MainActivity.res().getString(R.string.other_url))

                    val cimbUrlConnection = cimbUrl.openConnection() as HttpsURLConnection
                    val otherUrlConnection = otherUrl.openConnection() as HttpsURLConnection

                    val cimbResp: String
                    val otherResp: String

                    try {
                        cimbResp = cimbUrlConnection.inputStream.bufferedReader().readText()
                        otherResp = otherUrlConnection.inputStream.bufferedReader().readText()
                    } finally {
                        cimbUrlConnection.disconnect()
                        otherUrlConnection.disconnect()
                    }

                    // step 1: get CIMB rates
                    val cimbContainer =
                        Jsoup.parse(cimbResp)
                            .select(MainActivity.res().getString(R.string.cimb_selector))
//                LogUtils.logLong(TAG, cimbContainer.toString())
                    var cimbRatesStr = cimbContainer.select("input").first()?.attr("value")
                    if (cimbRatesStr != null) {
                        try {
                            cimbRatesStr = cimbRatesStr.substring(1, cimbRatesStr.length - 1)
                            cimbRates = cimbRatesStr.toFloat()
                        } catch (e: Exception) {
                            LogUtils.logLong(MainActivity.TAG, e.toString(), "ERROR")
                            cimbRates = -1f
                        }
                    } else {
                        cimbRates = -1f
                    }

                    // step 2: get other rates
                    // following is for bloomberg.com
                    val otherContainer =
                        Jsoup.parse(otherResp)
                            .select(MainActivity.res().getString(R.string.other_selector))
                    LogUtils.logLong(MainActivity.TAG, otherContainer.toString())
                    val otherRatesStr =
                        otherContainer.last()?.text()
                    if (otherRatesStr != null) {
                        try {
                            otherRates = otherRatesStr.toFloat()
                        } catch (e: Exception) {
                            LogUtils.logLong(MainActivity.TAG, e.toString(), "ERROR")
                            otherRates = -1f
                        }
                    } else {
                        otherRates = -1f
                    }

                    Log.d("hello: ", "test: " + cimbRates.toString())
                    builder.setContentText("Primary: " + cimbRates.toString() + ", Secondary: " + otherRates.toString())
                    val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(1001, builder.build())

                    Thread.sleep(1000 * 60 * 5) // 5 mins
                }
            } catch (e: InterruptedException) {
                Log.d("test: ", "interrupted")
            }
//            Toast.makeText(MainActivity.appContext(), "hello", Toast.LENGTH_SHORT).show()
        })

        yong.start()

        return super.onStartCommand(intent, flags, startId)
//        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("test", "onDestroy")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        yong.interrupt()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}