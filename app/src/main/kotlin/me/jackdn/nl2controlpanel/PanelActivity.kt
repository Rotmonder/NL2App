package me.jackdn.nl2controlpanel

import android.app.Activity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_panel.*
import me.jackdn.nl2telemetry.TelemetryClient
import me.jackdn.nl2telemetry.packet.incoming.PacketIntPair
import me.jackdn.nl2telemetry.packet.outgoing.PacketGetCurrentCoasterAndNearestStation
import me.jackdn.nl2telemetry.packet.outgoing.station.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.net.InetSocketAddress

class PanelActivity : Activity() {
    var coaster = 0
    var station = 0
    var client: TelemetryClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel)

        gates_open.repeatingListener {
            val client = client
            client?.send(PacketSetGates(client.getRandomRequestId(), coaster, station, true))
        }

        gates_close.repeatingListener {
            val client = client
            client?.send(PacketSetGates(client.getRandomRequestId(), coaster, station, false))
        }

        restraints_open.repeatingListener {
            val client = client
            client?.send(PacketSetHarness(client.getRandomRequestId(), coaster, station, true))
        }

        restraints_close.repeatingListener {
            val client = client
            client?.send(PacketSetHarness(client.getRandomRequestId(), coaster, station, false))
        }

        platform_open.repeatingListener {
            val client = client
            client?.send(PacketSetPlatform(client.getRandomRequestId(), coaster, station, false))
        }

        platform_close.repeatingListener {
            val client = client
            client?.send(PacketSetPlatform(client.getRandomRequestId(), coaster, station, true))
        }

        flyercar_open.repeatingListener {
            val client = client
            client?.send(PacketSetFlyerCar(client.getRandomRequestId(), coaster, station, true))
        }

        flyercar_close.repeatingListener {
            val client = client
            client?.send(PacketSetFlyerCar(client.getRandomRequestId(), coaster, station, false))
        }

        dispatch_a.repeatingListener {
            if (dispatch_b.isPressed) {
                val client = client
                client?.send(PacketDispatch(client.getRandomRequestId(), coaster, station))
            }
        }

        doAsync {
            val client = TelemetryClient(intent.extras[EXTRA_ADDRESS] as InetSocketAddress)
            val version = client.version
            uiThread {
                toast("Connected to NoLimits v$version")
            }
            this@PanelActivity.client = client
        }
    }

    @Suppress("unused")
    fun onClickSetStation(@Suppress("UNUSED_PARAMETER") view: View) {
        doAsync {
            val client = client
            val result = client?.request<PacketIntPair>(PacketGetCurrentCoasterAndNearestStation(client.getRandomRequestId())) ?: return@doAsync
            coaster = result.int1
            station = result.int2
        }
    }

    override fun onDestroy() {
        client?.close()
        super.onDestroy()
    }
}