package me.jackdn.nl2controlpanel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import me.jackdn.nl2telemetry.TelemetryClient
import me.jackdn.nl2telemetry.packet.incoming.PacketTelemetry
import me.jackdn.nl2telemetry.packet.outgoing.PacketGetTelemetry
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
        val preferences = getPreferences(Context.MODE_PRIVATE)
        connect_host.setText(preferences.getString("host", ""))
        connect_port.setText(preferences.getInt("port", getString(R.string.default_port).toInt()).toString())
    }

    @Suppress("unused")
    fun onClickConnect(@Suppress("UNUSED_PARAMETER") view: View) {
        val host = connect_host.text.toString()
        val port = try {
            connect_port.text.toString().toInt()
        } catch (exception: NumberFormatException) {
            toast(R.string.invalid_port)
            connect_port.setText(R.string.default_port)
            return
        }

        val preferences = getPreferences(Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("host", host)
        editor.putInt("port", port)
        editor.apply()
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
                val client = TelemetryClient(address)
                val telemetry = client.request<PacketTelemetry>(PacketGetTelemetry(client.getRandomRequestId()))
                client.close()
                uiThread {
                    if (telemetry.playMode) {
                        startActivity(intentFor<PanelActivity>(EXTRA_ADDRESS to address))
                    } else {
                        toast(R.string.not_in_play_mode)
                    }
                }
            } catch (exception: IOException) {
                uiThread {
                    toast(R.string.connecting_failed)
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
