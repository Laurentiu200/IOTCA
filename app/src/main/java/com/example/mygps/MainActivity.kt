package com.example.mygps

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    //database variables
    var firebaseDatabase: FirebaseDatabase? = null
    var databaseReference: DatabaseReference? = null

    // status view
    var status: TextView? = null

    //arming button
    var arming_btn: Button? = null

    //latitude and longitude view
    var lat_view: TextView? = null
    var long_view: TextView? = null

    //latitude and logitude coordinates
    var curr_lat: Double ?= null
    var curr_long: Double ?= null

    //gps location view
    var gps_link = "https://www.youtube.com/watch?v=xvFZjo5PgG0&t=2s&ab_channel=Duran"
    var gps_view: TextView? = null

    // valid location view
    var val_loc: TextView? = null

    // angle of inclination view
    var inclination: TextView? = null

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private fun getdata(data_to_retrieve: String) {
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference(data_to_retrieve)
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(String::class.java)
                if(data_to_retrieve == "armed")
                {
                    if(value == "true")
                    {
                        status?.text = "Armed"
                        arming_btn?.text = "Disarm the GPS"
                    }
                    else
                    {
                        status?.text = "Disarmed"
                        arming_btn?.text = "Arm the GPS"
                    }
                }
                if(data_to_retrieve == "lat")
                {
                    curr_lat = String.format("%.6f", value.toString().toDouble()).toDouble()
                    if(curr_lat!! >= 0)
                        lat_view?.text = value.toString() + " N"
                    else
                        lat_view?.text = value.toString().substring(1) + " S"
                }
                if(data_to_retrieve == "long")
                {
                    curr_long = String.format("%.6f", value.toString().toDouble()).toDouble()
                    if(curr_long!! >= 0)
                        long_view?.text = value.toString() + " E"
                    else
                        long_view?.text = value.toString().substring(1) + " W"
                }
                if(data_to_retrieve == "valid_gps")
                {
                    if(value == "true")
                    {
                        val_loc?.text = "Yes"
                        gps_view?.text = gps_link
                    }
                    else
                    {
                        val_loc?.text = "No"
                        gps_view?.text = "Waiting for satellites"
                    }
                }
                if(data_to_retrieve == "angle")
                {
                    inclination?.text = value
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveDatatoFirebase(field_to_write_to: String, value: String) {
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference(field_to_write_to)
        databaseReference!!.setValue(value)
    }

    private fun saveDatatoFirebaseArmedValues() {
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseDatabase!!.getReference("armed_values/lat")!!.setValue(curr_lat)
        firebaseDatabase!!.getReference("armed_values/long")!!.setValue(curr_long)
        firebaseDatabase!!.getReference("armed_values/angle")!!.setValue(inclination!!.text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.StatusView)
        arming_btn = findViewById(R.id.arming_button)
        lat_view = findViewById(R.id.latView)
        long_view = findViewById(R.id.longView)
        val_loc = findViewById(R.id.valid_loc_txt)
        gps_view = findViewById(R.id.textView3)
        inclination = findViewById(R.id.inclination_txt)

        getdata("armed")
        getdata("lat")
        getdata("long")
        getdata("valid_gps")
        getdata("angle")

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
                    if(status?.text == "Armed")
                    {
                        saveDatatoFirebase("armed", "false")
                    }
                    else
                    {
                        saveDatatoFirebase("armed", "true")
                        saveDatatoFirebaseArmedValues()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for MyGPS")
            .setSubtitle("Log in using your smartphone biometric credential")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unavailable")
        builder.setMessage("This action cannot be performed due to connection errors")

        val button = findViewById<Button>(R.id.arming_button)

        button.setOnClickListener {
            if(!(findViewById<Button>(R.id.arming_button).text.equals("Unavailable"))) {
                biometricPrompt.authenticate(promptInfo)
            }
            else
            {
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                getdata("armed")
                getdata("lat")
                getdata("long")
                getdata("valid_gps")
                getdata("angle")
            }

            // lost network connection
            override fun onLost(network: Network) {
                super.onLost(network)
                status?.text = "Lost Network"
                arming_btn?.text = "Unavailable"
            }
        }

        val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)

    }
}
