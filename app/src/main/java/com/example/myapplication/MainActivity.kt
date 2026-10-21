package com.example.myapplication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // Standard SPP (Serial Port Profile) UUID a Bluetooth Serial kommunikációhoz
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private var motor1speed = 0     // 0=STOP, 1=ELŐRE, 2=HÁTRA
    private var motor2speed = 0     // 0=STOP, 1=ELŐRE, 2=HÁTRA


    private lateinit var btnConnect: Button     //Kapcsolódás
    private lateinit var btnLF: Button          //Bal lánctalp előre
    private lateinit var btnLB: Button          //Bal lánctalp hátra
    private lateinit var btnRF: Button          //Jobb lánctalp előre
    private lateinit var btnRB: Button          //Jobb lánctalp hátra
    private lateinit var tvStatus: TextView     //Kapcsolat státusz
    private lateinit var tvGunAngle: TextView   //Löveg dőlés
    private lateinit var tvTurret: TextView     //Torony forgása
    private lateinit var seekBarGun: SeekBar    //Löveg döntés
    private lateinit var seekBarTurret: SeekBar //Torony forgatás

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Elemek bekötése
        btnConnect = findViewById(R.id.btnConnect)
        btnLF = findViewById(R.id.btnLF)
        btnLB = findViewById(R.id.btnLB)
        btnRF = findViewById(R.id.btnRF)
        btnRB = findViewById(R.id.btnRB)

        seekBarGun = findViewById(R.id.seekBarGun)
        seekBarTurret = findViewById(R.id.seekBarTurret)

        tvStatus = findViewById(R.id.tvStatus)
        tvGunAngle = findViewById(R.id.tvGunAngle)
        tvTurret = findViewById(R.id.tvTurret)





        btnConnect.setOnClickListener {
            connectToEsp32("ESP32") // Az ESP32-ben megadott SerialBT.begin("ESP32") név
        }


        @SuppressLint("ClickableViewAccessibility")
        btnLF.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    motor1speed = 1 // Előre
                    sendControlArray()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    motor1speed = 0 // STOP
                    sendControlArray()
                    true
                }
                else -> false
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        btnLB.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    motor1speed = 2 // Hátra
                    sendControlArray()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    motor1speed = 0 // STOP
                    sendControlArray()
                    true
                }
                else -> false
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        btnRF.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    motor2speed = 1 // Előre
                    sendControlArray()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    motor2speed = 0 // STOP
                    sendControlArray()
                    true
                }
                else -> false
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        btnRB.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    motor2speed = 2 // Hátra
                    sendControlArray()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    motor2speed = 0 // STOP
                    sendControlArray()
                    true
                }
                else -> false
            }
        }

        seekBarGun.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvGunAngle.text = "Löveg dőlés: $progress"
                if (fromUser) {
                    sendControlArray()

                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarTurret.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTurret.text = "Torony forgás: $progress"
                if (fromUser) {
                    sendControlArray()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    // Bluetooth Csatlakozás Logikája
    @SuppressLint("MissingPermission")
    private fun connectToEsp32(deviceName: String) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "A telefon nem támogatja a Bluetooth-t!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Kapcsold be a Bluetooth-t!", Toast.LENGTH_SHORT).show()
            return
        }

        // Megkeressük az "ESP32" nevű eszközt a párosított eszközök között
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val espDevice = pairedDevices?.firstOrNull { it.name == deviceName }

        if (espDevice != null) {    // Bluetooth kapcsolat építése külön szálon
            tvStatus.text = "Csatlakozás folyamatban..."

            Thread {
                try {
                    bluetoothSocket = espDevice.createRfcommSocketToServiceRecord(sppUuid)
                    bluetoothSocket?.connect()
                    outputStream = bluetoothSocket?.outputStream

                    runOnUiThread {
                        tvStatus.text = "Sikeresen csatlakozva!"
                        tvStatus.setTextColor(android.graphics.Color.GREEN)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    try {
                        bluetoothSocket?.close()
                    } catch (closeException: IOException) {
                        closeException.printStackTrace()
                    }
                    runOnUiThread {
                        tvStatus.text = "Sikertelen csatlakozás!"
                        tvStatus.setTextColor(android.graphics.Color.RED)
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Az 'ESP32' eszköz nem található! Párosítsd a telefon beállításaiban!", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendControlArray() {
        // 4 elemű tömb: [Turret, Gun, Motor1, Motor2]
        val message = byteArrayOf(
            seekBarTurret.progress.toByte(),  //Torony szervó
            seekBarGun.progress.toByte(),     //Löveg szervó
            motor1speed.toByte(),  //Motor 1 (0=STOP, 1=ELŐRE, 2=HÁTRA)
            motor2speed.toByte()   //Motor 2 (0=STOP, 1=ELŐRE, 2=HÁTRA)
        )

        Thread {
            try {
                outputStream?.write(message)
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}