package com.rogg.webclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result

import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    var address:Int by Delegates.observable(0) { property, oldValue, newValue ->
        if (newValue < 255) {
            var root = "http://192.168.43." + newValue
            Log.i("ClientHttp_scan","address : " +root)
            listen_scan(root, device_description)
        } else {
            searching.visibility=View.GONE
            Toast.makeText(this, "Ekhosport server not found", Toast.LENGTH_LONG)
        }
    }
    var adapter:ViewholderMessage?=null
    var start : Button?=null
    var recycler:RecyclerView?=null
    lateinit var searching:RelativeLayout
    var device_description=""
    var temp:Long=0
    var stop=false
    var id_dev="1001012"
    companion object {
        var messages: ArrayList<Message> = ArrayList()
        var lastmessage="Welcome to our discussion server"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        messages.clear()
        start=findViewById(R.id.start)
        searching=findViewById(R.id.searching)
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
        var ip_dev=getIPAddress(true).replace(".","_")
        var name_dev=Build.BRAND+" "+Build.MODEL
        var name_dev2=Build.MODEL
        device_description=name_dev+":"+id_dev+":"+ip_dev+":"+name_dev2
        var root = "http://192.168.43.105"
        listen_scan(root, device_description)

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
    fun listen_scan(root:String,description: String){
        FuelManager.instance.basePath = root+":1337/scan"
        "/"+description.httpGet().responseString { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    address += 1
                }
                is Result.Success -> {
                    val data = result.get()
                    if (data.equals("Scanned")){
                        FuelManager.instance.reset()
                        findViewById<EditText>(R.id.server_ip).setText(root.split("//")[1])
                        findViewById<EditText>(R.id.server_ip).keyListener=null
                        searching.visibility=View.GONE

                    }
                }
            }
        }
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
                        if (mmes.to.contains(id_dev) || mmes.to.contains("*")){
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

    fun getIPAddress( useIPv4: Boolean):String {
        try {
            var interfaces:List<NetworkInterface>  = Collections.list(NetworkInterface.getNetworkInterfaces());
            for ( intf: NetworkInterface in interfaces) {
                val addrs :List<InetAddress>  = Collections.list(intf.getInetAddresses());
                for (addr : InetAddress in addrs) {
                    if (!addr.isLoopbackAddress()) {
                        val sAddr = addr.getHostAddress()
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr.indexOf(':')<0

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return if (delim<0)  sAddr.toUpperCase() else sAddr.substring(0, delim).toUpperCase()
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) { } // for now eat exceptions
        return ""
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
