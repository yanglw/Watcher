package me.peace.watcher.util

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.core.content.ContextCompat.startActivity

import android.content.pm.ResolveInfo

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*


object Utils {
    fun topPage(context: Context?):Array<String>?{
        val top = top(context)
        val packageName:String=top?.packageName?:""
        val className:String=top?.className?:""
        val pid = pid(context,packageName)
        val currentMemory = appMemory(context,pid)
        val systemFreeMemory = systemFreeMemory(context)
        val version = appVersion(context,packageName)
        return arrayOf<String>(pid.toString(),packageName,className,currentMemory,systemFreeMemory,version)
    }

    private fun top(context: Context?): ComponentName? {
        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        if (runningTasks.isNotEmpty()){
            return runningTasks[0]?.topActivity
        }
        return null
    }

    private fun pid(context:Context?, packageName:String):Int{
        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        var pid = -1
        runningAppProcesses.filter {
            it.processName == packageName
        }.forEach{
           pid = it.pid
        }
        return pid
    }

    private fun appMemory(context:Context?, pid:Int):String{
        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processMemoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
        return Formatter.formatFileSize(context,processMemoryInfo[0].totalPss.toLong() * 1024)
    }

    private fun systemFreeMemory(context: Context?):String{
        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return Formatter.formatFileSize(context,memoryInfo.availMem)
    }

    private fun appVersion(context: Context?,packageName:String):String{
        val packageManager = context?.packageManager
        val packageInfo = packageManager?.getPackageInfo(packageName, 0)
        return packageInfo?.versionName?:""
    }

    fun ip(): String? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val element: NetworkInterface = en.nextElement()
                val ip: Enumeration<InetAddress> = element.inetAddresses
                while (ip.hasMoreElements()) {
                    val inetAddress: InetAddress = ip.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress().toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return ""
    }
}