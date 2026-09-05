package com.appconsole

import android.content.*
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val bg=Color.rgb(11,13,18); private val panel=Color.rgb(18,21,28); private val line=Color.rgb(42,47,58)
    private val fg=Color.rgb(235,238,245); private val muted=Color.rgb(145,153,168); private val green=Color.rgb(74,222,128)
    private lateinit var root:LinearLayout; private lateinit var console:TextView; private var logProcess:Process?=null
    private val executor=Executors.newSingleThreadExecutor()
    override fun onCreate(b:Bundle?){super.onCreate(b);window.statusBarColor=bg;window.navigationBarColor=bg;showHome()}
    override fun onDestroy(){logProcess?.destroy();executor.shutdownNow();super.onDestroy()}
    private fun tv(s:String,size:Float=14f,color:Int=fg)=TextView(this).apply{text=s;textSize=size;setTextColor(color);setPadding(16,8,16,8)}
    private fun showHome(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setBackgroundColor(bg)}
        root.addView(TextView(this).apply{text="+";textSize=72f;setTextColor(fg);gravity=Gravity.CENTER;setOnClickListener{pickApp()}},LinearLayout.LayoutParams(150,150))
        root.addView(tv("Select an app",16f,muted));setContentView(root)
    }
    private fun pickApp(){
        val apps=packageManager.getInstalledApplications(0).filter{it.packageName!=packageName && packageManager.getLaunchIntentForPackage(it.packageName)!=null}.sortedBy{packageManager.getApplicationLabel(it).toString().lowercase()}
        AlertDialog.Builder(this).setTitle("Open application").setItems(apps.map{packageManager.getApplicationLabel(it).toString()}.toTypedArray()){_,i->openConsole(apps[i])}.setNegativeButton("Cancel",null).show()
    }
    private fun openConsole(app:ApplicationInfo){
        root.removeAllViews();root.orientation=LinearLayout.HORIZONTAL;root.setBackgroundColor(bg)
        val appPanel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.BLACK)}
        appPanel.addView(tv("APP  •  ${packageManager.getApplicationLabel(app)}",13f,muted).apply{setBackgroundColor(panel)},LinearLayout.LayoutParams(-1,48))
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        info.addView(ImageView(this).apply{setImageDrawable(packageManager.getApplicationIcon(app))},LinearLayout.LayoutParams(80,80))
        info.addView(tv(packageManager.getApplicationLabel(app).toString(),18f,fg).apply{gravity=Gravity.CENTER})
        info.addView(tv("Opening in system split-screen…",12f,muted).apply{gravity=Gravity.CENTER})
        appPanel.addView(info,LinearLayout.LayoutParams(-1,0,1f))
        val con=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(panel)}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(panel)}
        bar.addView(tv("CONSOLE",13f,green),LinearLayout.LayoutParams(0,52,1f))
        bar.addView(TextView(this).apply{text="⧉";textSize=24f;gravity=Gravity.CENTER;setTextColor(fg);setOnClickListener{copyLogs()}},LinearLayout.LayoutParams(56,52))
        bar.addView(TextView(this).apply{text="×";textSize=28f;gravity=Gravity.CENTER;setTextColor(fg);setOnClickListener{logProcess?.destroy();showHome()}},LinearLayout.LayoutParams(56,52));con.addView(bar)
        con.addView(View(this).apply{setBackgroundColor(line)},LinearLayout.LayoutParams(-1,1))
        console=tv("[AppConsole] Preparing log stream…\n",12f,Color.rgb(190,198,212)).apply{typeface=android.graphics.Typeface.MONOSPACE;gravity=Gravity.TOP}
        con.addView(ScrollView(this).apply{addView(console);setBackgroundColor(Color.rgb(10,12,16))},LinearLayout.LayoutParams(-1,0,1f))
        root.addView(appPanel,LinearLayout.LayoutParams(0,-1,1f));root.addView(con,LinearLayout.LayoutParams(0,-1,1f))
        launchAdjacent(app)
    }
    private fun launchAdjacent(app:ApplicationInfo){
        val i=packageManager.getLaunchIntentForPackage(app.packageName)
        if(i==null){append("[AppConsole] This app has no launcher activity.");return}
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        try{startActivity(i);append("[AppConsole] Launched ${app.packageName}. If your device supports split-screen, it will appear beside AppConsole.")}catch(e:Exception){append("[AppConsole] Launch failed: ${e.message}")}
        startLogs(app.packageName)
    }
    private fun startLogs(pkg:String){
        executor.execute{
            try{
                val pid=Runtime.getRuntime().exec(arrayOf("pidof",pkg)).inputStream.bufferedReader().readText().trim().split(" ").firstOrNull()?.toIntOrNull() ?: -1
                if(pid<1)throw IllegalStateException("target process is not running yet")
                logProcess=Runtime.getRuntime().exec(arrayOf("logcat","-v","time","--pid",pid.toString()))
                val r=BufferedReader(InputStreamReader(logProcess!!.inputStream));var s:String?
                while(r.readLine().also{s=it}!=null){val x=s!!;runOnUiThread{append(x)}}
            }catch(e:Exception){runOnUiThread{append("[logcat unavailable] ${e.message}\nAndroid normally blocks third-party apps from reading another app's private logcat stream. Full cross-app logs require ADB/root/system privileges.")}}
        }
    }
    private fun append(s:String){if(::console.isInitialized)console.append("$s\n")}
    private fun copyLogs(){val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager;cm.setPrimaryClip(ClipData.newPlainText("AppConsole logs",console.text));Toast.makeText(this,"Logs copied",Toast.LENGTH_SHORT).show()}
}
