package com.rogg.webclient

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import java.util.*
import android.support.v4.os.HandlerCompat.postDelayed
import com.rogg.webclient.R.id.async


class BackgroundRunning : Service() {
    // Binder given to clients.
    /** Method for clients.  */
    companion object {
        val d:Long =0
    }
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        onTaskRemoved(intent)
        FuelManager.instance.basePath = "http://192.168.43.3:1337"
        //time=System.currentTimeMillis()/1000
        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }
    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

}