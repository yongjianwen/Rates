package com.example.rates

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.rates.services.BackgroundService
import com.example.rates.utils.LogUtils
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    init {
        instance = this
    }

    companion object {
        private var instance: MainActivity? = null
        const val TAG = "MainActivity"

        fun appContext(): Context {
            return instance!!.applicationContext
        }

        fun res(): Resources {
            return appContext().resources
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        if (!foregroundServiceRunning()) {
//            val serviceIntent = Intent(this, BackgroundService::class.java)
//            startForegroundService(serviceIntent)
//        }

        val cimbTextView: TextView = findViewById(R.id.cimb_textview)
        val otherTextView: TextView = findViewById(R.id.other_textview)

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val cimbUrl = URL(res().getString(R.string.cimb_url))
            val otherUrl = URL(res().getString(R.string.other_url))

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

            handler.post {
                var cimbRates: Float = Float.NaN
                var otherRates: Float = Float.NaN

                // step 1: get CIMB rates
                val cimbContainer =
                    Jsoup.parse(cimbResp).select(res().getString(R.string.cimb_selector))
//                LogUtils.logLong(TAG, cimbContainer.toString())
                var cimbRatesStr = cimbContainer.select("input").first()?.attr("value")
                if (cimbRatesStr != null) {
                    try {
                        cimbRatesStr = cimbRatesStr.substring(1, cimbRatesStr.length - 1)
                        cimbRates = cimbRatesStr.toFloat()
                        cimbTextView.text = cimbRates.toString()
                    } catch (e: Exception) {
                        LogUtils.logLong(TAG, e.toString(), "ERROR")
                        cimbTextView.text = res().getString(R.string.error_text)
                    }
                } else {
                    cimbTextView.text = res().getString(R.string.error_text)
                }

                // step 2: get other rates
                // following is for xe.com; removed due to the response returned may contain obsolete data
                /*val otherContainer =
                    Jsoup.parse(otherResp)
                        .select(res().getString(R.string.other_selector))
                LogUtils.logLong(TAG, otherContainer.toString())
                var otherRatesStr =
                    otherContainer.select("tbody").select("tr").first()?.select("td")?.last()
                        ?.text()
                if (otherRatesStr != null) {
                    try {
                        otherRatesStr = otherRatesStr.split(' ')[0]
                        otherRates = otherRatesStr.toFloat()
                        otherTextView.text = otherRates.toString()
                    } catch (e: Exception) {
                        LogUtils.logLong(TAG, e.toString(), "ERROR")
                        otherTextView.text = res().getString(R.string.error_text)
                    }
                } else {
                    otherTextView.text = res().getString(R.string.error_text)
                }*/

                // following is for bloomberg.com
                val otherContainer =
                    Jsoup.parse(otherResp)
                        .select(res().getString(R.string.other_selector))
                LogUtils.logLong(TAG, otherContainer.toString())
                val otherRatesStr =
                    otherContainer.last()?.text()
                if (otherRatesStr != null) {
                    try {
                        otherRates = otherRatesStr.toFloat()
                        otherTextView.text = otherRates.toString()
                    } catch (e: Exception) {
                        LogUtils.logLong(TAG, e.toString(), "ERROR")
                        otherTextView.text = res().getString(R.string.error_text)
                    }
                } else {
                    otherTextView.text = res().getString(R.string.error_text)
                }

                // step 3: calculate diff
                displayDiff(cimbRates, otherRates)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        Log.d("test", "onPause")
        Log.d("test", foregroundServiceRunning().toString())
        if (!foregroundServiceRunning()) {
            val serviceIntent = Intent(this, BackgroundService::class.java)
            serviceIntent.action = "START"
            startForegroundService(serviceIntent)
//            startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d("test", "onResume")
        val serviceIntent = Intent(this, BackgroundService::class.java)
        serviceIntent.action = "STOP"
//        startForegroundService(serviceIntent)
//        startService(serviceIntent)
        stopService(serviceIntent)
    }

    private fun displayDiff(num1: Float, num2: Float) {
        val diffTextView: TextView = findViewById(R.id.diff_textview)
        if (!num1.isNaN() && !num2.isNaN()) {
            // round to 4 decimal places
            diffTextView.text = (((num1 - num2) * 10000.0).roundToInt() / 10000.0).toString()
        } else {
            diffTextView.text = res().getString(R.string.error_text)
        }
    }

    private fun foregroundServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (BackgroundService::class.java.name.equals(service.service.className)) {
                return true
            }
        }
        return false
    }
}