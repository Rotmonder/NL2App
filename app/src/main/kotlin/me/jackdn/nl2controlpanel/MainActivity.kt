package me.jackdn.nl2controlpanel

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import me.jackdn.nl2telemetry.TelemetryClient
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.InetSocketAddress


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @Suppress("unused")
    fun onClickConnect(@Suppress("UNUSED_PARAMETER") view: View) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
            connect()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), INTERNET_PERMISSION_REQUEST)
        }
    }

    fun connect() {
        doAsync {
            try {
                val address = InetSocketAddress(connect_host.text.toString(), connect_port.text.toString().toInt())
                TelemetryClient(address).close()
                uiThread {
                    startActivity(intentFor<PanelActivity>(EXTRA_ADDRESS to address))
                }
            } catch (exception: IOException) {
                uiThread {
                    toast("Connection failed")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != INTERNET_PERMISSION_REQUEST)
            return

        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connect()
        }
    }
}
