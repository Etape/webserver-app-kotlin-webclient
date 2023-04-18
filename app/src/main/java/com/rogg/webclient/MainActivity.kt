package com.rogg.webclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    var adapter:ViewholderMessage?=null
    var start : Button?=null
    var recycler:RecyclerView?=null
    var temp:Long=0
    var stop=false
    companion object {
        var messages: ArrayList<Message> = ArrayList()
        var lastmessage="Welcome to our discussion server"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        messages.clear()
        start=findViewById(R.id.start)
        recycler=findViewById(R.id.recycler)
        adapter= ViewholderMessage(messages)
        recycler!!.adapter=adapter
        var ip=findViewById<EditText>(R.id.server_ip)
        var text=findViewById<TextView>(R.id.listentext)
        var thread_listen=Thread(Runnable {
            while(stop){
                listen()
                try {
                    Thread.sleep(1000)
                }
                catch (e:Exception){
                    Log.i("webClient","Listenning stopped")
                }
            }
        })
        recycler!!.layoutManager=LinearLayoutManager(this)
        start!!.setOnClickListener(View.OnClickListener {
            if (ip.text.isEmpty() || !Patterns.IP_ADDRESS.matcher(ip.text.toString()).matches()){
                ip.setBackgroundColor(Color.RED)
                text.text="Invalid IP address"
                text.setTextColor(Color.RED)
            }
            else{
                text.text="Listenning to server address"
                text.setTextColor(Color.BLACK)
                FuelManager.instance.basePath = "http://"+ip.text.toString()+":1337"
                stop=!stop
                if (stop){
                    start!!.setBackgroundColor(Color.RED)
                    start!!.text="STOP LISTENNING"
                    thread_listen.start()
                }
                else{
                    start!!.text="START LISTENNING"
                    start!!.setBackgroundColor(Color.GREEN)
                    thread_listen.interrupt()
                }
            }
        })
    }

    fun decode_request(body:String):Message{
        //responseBody="Message#"+mess!!.text.toString()+"#To#"+"*"+"#time#" + (System.currentTimeMillis()/1000).toString()
        var message = Message()
        var splits=body.split("#")
        if(splits.size>2){
            message.message=splits[1]
            message.to=splits[3]
            message.time= splits[splits.size-1].toLong()
        }
        return message
    }

    class Message{
        var message:String=""
        var to:String=""
        var time:Long=0
    }

    fun listen(){
        "/".httpGet().responseString { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                    Log.i("Client_Http","Result Error Message :"+ex)
                }
                is Result.Success -> {
                    val data = result.get()
                    if (!data.equals(lastmessage)){
                        val mmes=decode_request(result.get())
                        if (mmes.to.contains("ID0001") || mmes.to.contains("*")){
                            messages.add(mmes)
                            createNotification(mmes)
                            adapter!!.notifyDataSetChanged()
                        }
                        lastmessage=data
                    }
                    Log.i("Client_Http","Result Message :"+data)
                }
            }
        }
    }

    private fun createNotification(message: Message) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val CHANNEL_ID = "Client_wifi_channel_01"
        var notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Client_wifi_channel"
            val descriptionText = "Messages_channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(this, this::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wifi_black_24dp)
                .setContentTitle("New Message")
                .setContentText(getDateTime(message.time)+" : "+message.message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        notificationManager.notify(0, builder.build())
    }

    private fun getDateTime(s: Long): String? {
        try {
            val sdf = SimpleDateFormat("kk:mm")
            val netDate = Date(s * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }


}
