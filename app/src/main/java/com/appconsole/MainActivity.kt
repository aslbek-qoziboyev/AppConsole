package com.appconsole

import android.app.*
import android.content.*
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(11,13,18); private val panel = Color.rgb(18,21,28); private val line = Color.rgb(42,47,58)
    private val fg = Color.rgb(235,238,245); private val muted = Color.rgb(145,153,168); private val green = Color.rgb(74,222,128)
    private lateinit var root: LinearLayout; private lateinit var console: TextView; private var selected: ApplicationInfo? = null
    private val executor = Executors.newSingleThreadExecutor(); private var logProcess: Process? = null

    override fun onCreate(b: Bundle?) { super.onCreate(b); window.statusBarColor=bg; window.navigationBarColor=bg; showHome() }
    override fun onDestroy(){ logProcess?.destroy(); executor.shutdownNow(); super.onDestroy() }
    private fun tv(s:String,size:Float=14f,color:Int=fg)=TextView(this).apply{ text=s;textSize=size;setTextColor(color);setPadding(16,8,16,8) }
    private fun showHome(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setBackgroundColor(bg)}
        val plus=TextView(this).apply{ text="+";textSize=72f;setTextColor(fg);gravity=Gravity.CENTER;setOnClickListener{pickApp()} }
        root.addView(plus,LinearLayout.LayoutParams(150,150)); root.addView(tv("Select an app",16f,muted),LinearLayout.LayoutParams(-2,-2)); setContentView(root)
    }
    private fun pickApp(){
        val apps=packageManager.getInstalledApplications(0).filter{it.packageName!=packageName && (it.flags and ApplicationInfo.FLAG_SYSTEM)==0}.sortedBy{packageManager.getApplicationLabel(it).toString().lowercase()}
        val names=apps.map{packageManager.getApplicationLabel(it).toString()}.toTypedArray()
        AlertDialog.Builder(this).setTitle("Open application").setItems(names){_,which->openConsole(apps[which])}.setNegativeButton("Cancel",null).show()
    }
    private fun openConsole(app:ApplicationInfo){ selected=app; root.removeAllViews(); root.orientation=LinearLayout.HORIZONTAL; root.gravity=Gravity.FILL; root.setBackgroundColor(bg)
        val appPanel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.BLACK)}
        val appTitle=tv("APP  •  ${packageManager.getApplicationLabel(app)}",13f,muted).apply{setBackgroundColor(panel)}; appPanel.addView(appTitle,LinearLayout.LayoutParams(-1,48))
        val frame=FrameLayout(this).apply{setBackgroundColor(Color.BLACK)}
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
        val icon=ImageView(this).apply{setImageDrawable(packageManager.getApplicationIcon(app))}; info.addView(icon,LinearLayout.LayoutParams(80,80))
        info.addView(tv(packageManager.getApplicationLabel(app).toString(),18f,fg).apply{gravity=Gravity.CENTER})
        info.addView(tv("Launching…",12f,muted).apply{gravity=Gravity.CENTER}); frame.addView(info,FrameLayout.LayoutParams(-1,-1)); appPanel.addView(frame,LinearLayout.LayoutParams(0,0,1f))
        val con=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(panel)}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(panel)}
        bar.addView(tv("CONSOLE",13f,green),LinearLayout.LayoutParams(0,52,1f))
        val copy=TextView(this).apply{text="⧉";textSize=24f;gravity=Gravity.CENTER;setTextColor(fg);setOnClickListener{copyLogs()}};bar.addView(copy,LinearLayout.LayoutParams(56,52))
        val exit=TextView(this).apply{text="×";textSize=28f;gravity=Gravity.CENTER;setTextColor(fg);setOnClickListener{logProcess?.destroy();showHome()}};bar.addView(exit,LinearLayout.LayoutParams(56,52));con.addView(bar)
        val sep=View(this).apply{setBackgroundColor(line)};con.addView(sep,LinearLayout.LayoutParams(-1,1)); console=tv("Starting log monitor…\n",12f,Color.rgb(190,198,212)).apply{setTypeface(null,android.graphics.Typeface.MONOSPACE);gravity=Gravity.TOP};val scroll=ScrollView(this).apply{addView(console);setBackgroundColor(Color.rgb(10,12,16))};con.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(appPanel,LinearLayout.LayoutParams(0,-1,1f)); root.addView(con,LinearLayout.LayoutParams(0,-1,1f)); launch(app)
    }
    private fun launch(app:ApplicationInfo){ val intent=packageManager.getLaunchIntentForPackage(app.packageName); if(intent!=null){intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);append("[AppConsole] launched ${app.packageName}")} else append("[AppConsole] Cannot launch this package") ; startLogs(app.packageName) }
    private fun startLogs(pkg:String){ executor.execute{ try{logProcess=Runtime.getRuntime().exec(arrayOf("logcat","-v","time","--pid",getPid(pkg).toString())); val r=BufferedReader(InputStreamReader(logProcess!!.inputStream)); var s:String?;while(r.readLine().also{s=it}!=null){val x=s!!;runOnUiThread{append(x)}} }catch(e:Exception){runOnUiThread{append("[logcat unavailable] ${e.message}\nOnly system/ADB/root builds can read another app's private log stream on modern Android.")}} } }
    private fun getPid(pkg:String):Int=try{Runtime.getRuntime().exec(arrayOf("pidof",pkg)).inputStream.bufferedReader().readText().trim().split(" ").firstOrNull()?.toInt()?:-1}catch(_:Exception){-1}
    private fun append(s:String){console.append("$s\n")}
    private fun copyLogs(){ val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager;cm.setPrimaryClip(ClipData.newPlainText("AppConsole logs",console.text));Toast.makeText(this,"Logs copied",Toast.LENGTH_SHORT).show() }
}
