package com.example.mygps


import android.content.Intent
import android.graphics.Paint
import android.net.*
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
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
    var gps_link = "http://35.173.198.116"
    var gps_view: TextView? = null

    // valid location view
    var val_loc: TextView? = null

    // angle of inclination view
    var inclination: TextView? = null

    // topic to which we subscribe
    private var topic = "trouble"

    //biometrics variable
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    //function to retrieve the desired data from the realtime database
    private fun getdata(data_to_retrieve: String) {

        //firebase realtime database references
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference(data_to_retrieve)

        //we add an event listener to verify when the data is changed
        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                //get the value of the asked item
                val value = snapshot.getValue(String::class.java)

                //in case the data to retrieve is the status
                if(data_to_retrieve == "armed")
                {
                    //in case the device is armed
                    if(value == "true")
                    {
                        status?.text = "Armed"
                        arming_btn?.text = "Disarm the GPS"
                    }
                    else
                    //in case the device is disarmed
                    {
                        status?.text = "Disarmed"
                        arming_btn?.text = "Arm the GPS"
                    }
                }
                //in case the data to retrieve is the latitude
                if(data_to_retrieve == "lat")
                {
                    //we make the string a double with exactly 6 decimal values
                    curr_lat = String.format("%.6f", value.toString().toDouble()).toDouble()

                    //we add if is north
                    if(curr_lat!! >= 0)
                        lat_view?.text = value.toString() + " N"
                    else
                        //we add if is south + eliminate the minus
                        lat_view?.text = value.toString().substring(1) + " S"
                }

                //in case the data to retrieve is the longitude
                if(data_to_retrieve == "long")
                {
                    //we make the string a double with exactly 6 decimal values
                    curr_long = String.format("%.6f", value.toString().toDouble()).toDouble()

                    //we add if is east
                    if(curr_long!! >= 0)
                        long_view?.text = value.toString() + " E"
                    else
                        //we add if is west + eliminate the minus
                        long_view?.text = value.toString().substring(1) + " W"
                }

                //in case the data to retrieve is the valid location status
                if(data_to_retrieve == "valid_gps")
                {
                    //in case the device location is valid
                    if(value == "true")
                    {
                        val_loc?.text = "Yes"
                        gps_view?.text = "Go to location"
                        gps_view?.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                    }
                    else
                    //in case the device is not valid
                    {
                        val_loc?.text = "No"
                        gps_view?.text = "Waiting for satellites..."
                        gps_view?.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                    }
                }

                //in case the data to retrieve is the angle of the device
                if(data_to_retrieve == "angle")
                {
                    inclination?.text = value
                }
            }

            //error getting the data
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Fail to get data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    //save the selected item to firebase
    private fun saveDatatoFirebase(field_to_write_to: String, value: String) {
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase!!.getReference(field_to_write_to)
        databaseReference!!.setValue(value)
    }

    //save the last armed configuration to firebase
    private fun saveDatatoFirebaseArmedValues() {
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseDatabase!!.getReference("armed_values/lat")!!.setValue(curr_lat)
        firebaseDatabase!!.getReference("armed_values/long")!!.setValue(curr_long)
        firebaseDatabase!!.getReference("armed_values/angle")!!.setValue(inclination!!.text)
    }

    //subscribe to a topic
    private fun subscribeToNotificationTopic(topic: String) {
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
    }

    //main app
    override fun onCreate(savedInstanceState: Bundle?) {

        //default view -> everything is Unavailable
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //subscribe to the topic
        subscribeToNotificationTopic(topic)

        // Request notification permissions
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // TODO: Inform user that that your app will not show notifications.
            }
        }

        //find each view in the page
        status = findViewById(R.id.StatusView)
        arming_btn = findViewById(R.id.arming_button)
        lat_view = findViewById(R.id.latView)
        long_view = findViewById(R.id.longView)
        val_loc = findViewById(R.id.valid_loc_txt)
        gps_view = findViewById(R.id.textView3)
        inclination = findViewById(R.id.inclination_txt)

        //make the gps a link in case we are not waiting for satellites
        gps_view?.setOnClickListener {
            if(gps_view?.text != "Waiting for satellites...")
            {
                val viewIntent = Intent(
                    "android.intent.action.VIEW",
                    Uri.parse(gps_link)
                )
                startActivity(viewIntent)
            }
        }

        //get for each view live data
        getdata("armed")
        getdata("lat")
        getdata("long")
        getdata("valid_gps")
        getdata("angle")

        /*
            We prepare a biometric login for the arm/disarm button
            The builder and biometric set-up is down in the building part
         */
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                /*
                    Authentication error case = the device is unable to authenticate due other issues other than
                        invalid credentials, like: missing light for the AI, missing fingerprint sensor, etc. and also timeout
                    PS: this error will show if every method fails (every method in the cascade) due to these reasons
                            ex: AI - missing lights, Fingerprint - missing sensor, Password - timeout reached for inputting the password
                */
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT
                    )
                        .show()
                }

                // Authentication Succeeded = one of the authentication methods was given valid credentials
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT
                    )
                        .show()

                    //we verify if the device is armed and disarm it
                    if (status?.text == "Armed") {
                        saveDatatoFirebase("armed", "false")
                    }
                    else
                    //we verify if the device is disarmed and arm it + save the current data
                    {

                        saveDatatoFirebase("armed", "true")
                        saveDatatoFirebaseArmedValues()
                    }
                }

                // Authentication Failed = one of the authentication methods was given valid credentials
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            })

        /*
            The builder part = here we build our biometric authenticator using the previous configuration
            We set the title + subtitle
            We choose how to authenticate: in this case a cascade was chosen
                Facial Recognition (AI) -> Fingerprint recognition (Fingerprint sensor) -> Password (or any other kind of device credential, like pattern)
            We set to automatically verify skipping the "confirm" button
         */
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for MyGPS")
            .setSubtitle("Log in using your smartphone biometric credential")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        // we set an Alert Dialog to display in case of a connection error
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unavailable")
        builder.setMessage("This action cannot be performed due to connection errors")

        //we find the button view (this is using val, necessary to the next part)
        val button = findViewById<Button>(R.id.arming_button)

        //we set a event listener on our button to verify if it was clicked
        button.setOnClickListener {

            // it was clicked and is not unavailable so, start the authentication process
            if (!(findViewById<Button>(R.id.arming_button).text.equals("Unavailable"))) {
                biometricPrompt.authenticate(promptInfo)
            }
            else
            //it was clicked but is unavailable so, print the Alert Dialog
            {
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            }
        }

        // we create a network request on almost any kind of protocol and internet capability
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        // we create a connectivity manager to verify if we are currently connected to the internet
        val networkCallback = object : ConnectivityManager.NetworkCallback() {

            // lost network connection, print error messages but keep last updated position variables
            override fun onLost(network: Network) {
                super.onLost(network)
                status?.text = "Lost Network"
                arming_btn?.text = "Unavailable"
            }

            // network is available for use, get every data needed again
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                getdata("armed")
                getdata("lat")
                getdata("long")
                getdata("valid_gps")
                getdata("angle")
            }
        }

        //we initialize our connectivity manager with the up here configuration
        val connectivityManager =
            getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)

    }
}
