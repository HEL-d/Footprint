package com.mile.footprint

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.mile.footprint.Utils.LocationService
import com.mile.footprint.ui.theme.FootprintTheme
import java.util.Calendar
import java.util.TimeZone





class MainActivity : ComponentActivity() {

    private var selectedHour: Int = 7
    private var selectedMinute: Int = 0
    private var selectedTimeZone: TimeZone = TimeZone.getDefault()
    private lateinit var locationRequest : LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var settingsClient: SettingsClient
    lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    lateinit var singlepermissionlauncher : ActivityResultLauncher<String>
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var _distanceInMiles = mutableStateOf(0.0)
    val distanceInMiles: State<Double> get() = _distanceInMiles
    private var _statename = mutableStateOf("Unknown")
    val statename : State<String>get() = _statename

    private var _locationState = mutableStateOf(LatLng(0.0, 0.0))
    val locationstate : State<LatLng>get() = _locationState


    private var locationService: LocationService? = null
    private var isBound = false
    var backgroundserivcelocation : Int = PackageManager.PERMISSION_DENIED
    var wasBackgroundPermissionGranted : Boolean = false


    // ServiceConnection to manage the connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocalBinder
            locationService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000L).setMinUpdateIntervalMillis(5000L)
            .build()
        locationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        super.onCreate(savedInstanceState)
        setContent {
            FootprintTheme {
               MainScreen(distanceInMiles,statename,locationstate)
            }
        }







        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val notificationpermission = ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)
        val foregroundservicelocation = ContextCompat.checkSelfPermission(this,Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        backgroundserivcelocation = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
         wasBackgroundPermissionGranted = isBackgroundLocationGranted()


        permissionLauncher = registerForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val hasfinelocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val hascoarselocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            val hasnotificationpermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            val hasforegroundsericelocation = permissions[Manifest.permission.FOREGROUND_SERVICE_LOCATION] ?: false
            val hasbackgroundlocationpermission = permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (hasfinelocation && hascoarselocation   && hasnotificationpermission && hasforegroundsericelocation) {
                    if (!hasbackgroundlocationpermission){
                        requestBackgroundLocationPermission()
                    } else {
                        checkLocationSettings()
                    }



                } else {
///
                }

            } else if(Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
                if (hasfinelocation && hascoarselocation && hasnotificationpermission) {
                    if (!hasbackgroundlocationpermission) {
                        requestBackgroundLocationPermission()
                    } else {
                        checkLocationSettings()
                    }
                } else {
///
                }


            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (hasfinelocation && hascoarselocation) {
                    if (!hasbackgroundlocationpermission) {
                        requestBackgroundLocationPermission()
                    } else {
                        checkLocationSettings()
                    }






                } else {
///
                }






            } else  {
                if (hasfinelocation && hascoarselocation ) {
                    checkLocationSettings()
                } else {
///
                }
            }



        }


        locationSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                showTimeZoneDialog()
            } else {
                Toast.makeText(this, "Location services must be enabled.", Toast.LENGTH_SHORT).show()
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED  && notificationpermission
                == PackageManager.PERMISSION_GRANTED && foregroundservicelocation == PackageManager.PERMISSION_GRANTED
            ) {

                checkLocationSettings()



            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION


                    )
                )
            }

        } else if(Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU){
            if (fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED   && notificationpermission == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }


        } else {
            if (fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
        }










    }

    private fun requestBackgroundLocationPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }


    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, locationReceiver, IntentFilter("LocationUpdate"), ContextCompat.RECEIVER_EXPORTED)
          if(!isBound && LocationService.isServiceRunning) {
              val intent = Intent(this, LocationService::class.java)
              bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
              Toast.makeText(this,"BindingStart",Toast.LENGTH_SHORT).show()
          }

      //  registerReceiver(locationReceiver, IntentFilter("LocationUpdate"), RECEIVER_NOT_EXPORTED)
    }



    override fun onResume() {
        super.onResume()
      val isGranted = isBackgroundLocationGranted()

     if(!wasBackgroundPermissionGranted && isGranted) {
         onBackgroundPermissionGranted()
     }
        wasBackgroundPermissionGranted = isGranted

    }

    private fun onBackgroundPermissionGranted() {
        checkLocationSettings()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        unregisterReceiver(locationReceiver)
    }


     fun checkLocationSettings() {
        val task = settingsClient.checkLocationSettings(locationSettingsRequest)
        task.addOnSuccessListener {
            if(!LocationService.isServiceRunning){
                showTimeZoneDialog()
            }

        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error
                }
            } else {
                // Handle failure case
                Toast.makeText(this, "Enable location services to proceed.", Toast.LENGTH_SHORT).show()
            }
        }




    }


     val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val latLng = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.extras?.getParcelable("latLng", LatLng::class.java)
                } else {
                    intent.getParcelableExtra("latLng")
                }

                _locationState.value = latLng!!
                val distance = it.getDoubleExtra("distanceInMiles", 0.0)
                _distanceInMiles.value = distance
                val name = it.getStringExtra("State")
                if(name != null){
                    _statename.value = name
                } else {
                    _statename.value = "Unknown"
                }








            }
        }
    }




    private fun startLocationUpdates() {
        val intent = Intent(this, LocationService::class.java)
        startForegroundService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Toast.makeText(this,"Service started",Toast.LENGTH_SHORT).show()








        // Location updates are now handled by the LocationService
        // so this function doesn't need to do much.
    }


    private fun isBackgroundLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }



    private fun loadUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        selectedHour = sharedPreferences.getInt("selectedHour", 7)
        selectedMinute = sharedPreferences.getInt("selectedMinute", 0)
        val timeZoneId = sharedPreferences.getString("selectedTimeZone", TimeZone.getDefault().id)
        selectedTimeZone = TimeZone.getTimeZone(timeZoneId)
    }


    private fun saveUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
     val saved =  sharedPreferences.edit().apply {
            putInt("selectedHour", selectedHour)
            putInt("selectedMinute", selectedMinute)
            putString("selectedTimeZone", selectedTimeZone.id)

        }.commit()
        if (saved){
             startLocationUpdates()
         } else {
             Toast.makeText(this,"Failed to save",Toast.LENGTH_SHORT).show()
         }



    }





    private fun showTimeZoneDialog() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)
   val timepickerdialog =  TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            // Save the selected time
            this.selectedHour = selectedHour
            this.selectedMinute = selectedMinute

            // After time is selected, show a dialog to select the timezone
            showTimeZoneSelectionDialog()
        }, hour, minute, true)

        timepickerdialog.setOnCancelListener {
              startLocationUpdates()

        }
        timepickerdialog.show()
    }





    private fun showTimeZoneSelectionDialog() {
        // Get a list of available time zones
        val timeZones = TimeZone.getAvailableIDs()
        var selectedTimeZone: TimeZone? = null

      val alertdialog =   AlertDialog.Builder(this)
            .setTitle("Select Time Zone")
            .setSingleChoiceItems(timeZones, -1) { _, which ->
                // Save the selected time zone
                selectedTimeZone = TimeZone.getTimeZone(timeZones[which])
            }
            .setPositiveButton("OK") { dialog, _ ->
                if (selectedTimeZone != null) {
                    // Assign the selected time zone to the class variable
                    this.selectedTimeZone = selectedTimeZone as TimeZone
                    // Save the preferences after user selects a time zone
                    saveUserPreferences()
                }
                dialog.dismiss()
            }
                .setNegativeButton("Cancel") { dialog, _ ->
                startLocationUpdates()
                dialog.dismiss()
            } .create()


        alertdialog.setOnCancelListener {
            startLocationUpdates()
        }


        alertdialog.show()



    }

    fun saveToDatabase(){
        Toast.makeText(this,"Disabled",Toast.LENGTH_SHORT).show()
    //    locationService?.pseudosavestate()
    }








}



