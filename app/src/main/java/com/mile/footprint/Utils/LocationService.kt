package com.mile.footprint.Utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import com.mile.footprint.MapActivity
import com.mile.footprint.R
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


class LocationService: Service() {



   @Volatile private var isSavingProgress = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val userRoute = mutableStateListOf<LatLng>()
    private var locationCallback: LocationCallback? = null
    private var lastLocation: LatLng? = null
    private var currentDayStartMillis: Long = 0
    private var totalDistanceInMiles: Double = 0.0
    private var selectedHour: Int = 7
    private var selectedMinute: Int = 0
    private lateinit var selectedTimeZone: TimeZone
    private val stateDistanceMap : ConcurrentHashMap<String, Double> = ConcurrentHashMap()
    private var currentState: String? = null
    var date:String? = null
    private var unknown:String = "Unknown"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
      private val resetScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val binder = LocalBinder()

    companion object {
        var isServiceRunning = false
        private const val minDistanceChange = 20.0
        private const val AcccuracyThreshold = 20.0
    }



    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        loadUserPreferences()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        resetScope.launch {
            while (isActive){
                resetDailyTrackingIfNeeded()
                delay(TimeUnit.MINUTES.toMillis(30))

            }
        }


        locationCallback = object: LocationCallback(){
             override fun onLocationResult(p0: LocationResult) {
                    if(isSavingProgress) return
                p0.locations.forEach{ loco ->
                    val latlang = LatLng(loco.latitude,loco.longitude)
                    if (lastLocation != null){
                        val distanceToLast = lastLocation?.let { SphericalUtil.computeDistanceBetween(it,latlang) } ?: 0.0
                        if (distanceToLast >= minDistanceChange){
                            userRoute.add(latlang)
                            val distanceInMeter = SphericalUtil.computeLength(userRoute)
                            totalDistanceInMiles = distanceInMeter / 1609.34
                            Toast.makeText(this@LocationService,totalDistanceInMiles.toString(),Toast.LENGTH_SHORT).show()
                            sendLocationUpdate(latlang, totalDistanceInMiles)
                         //   updateNotification()
                            reverseGeocodeLocation(latlang)
                            if (currentState != null && currentState != unknown) {
                               synchronized(stateDistanceMap) {
                                   val stablestate = normalizeState(currentState!!)
                                   stateDistanceMap[stablestate] = stateDistanceMap.getOrDefault(stablestate, 0.0) + (distanceToLast / 1609.34)
                               }
                            }



                        } else {
                          sendLocationUpdate(latlang,totalDistanceInMiles)
                          //  updateNotification()
                        }
                    }
                    lastLocation = latlang
                    






                }




            }
        }



    }

    private fun GetDate() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val formatDate = yesterday.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))
        date = formatDate


    }


    private fun reverseGeocodeLocation(location: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val state = addresses.firstOrNull()?.adminArea ?: unknown
                    val normalstate = normalizeState(state)

                    if(normalstate != currentState){
                        if (currentState != null && currentState != unknown){
                            val previousDistance = stateDistanceMap.getOrDefault(currentState!!,0.0)
                            stateDistanceMap[currentState!!] = previousDistance

                        }
                        currentState = normalstate
                        stateDistanceMap.putIfAbsent(currentState!!,0.0)


                    }

                }

                override fun onError(errorMessage: String?) {
                    fetchStatefromCoordinate(location.latitude,location.longitude){ fetchedstate ->
                        val normstate = normalizeState(fetchedstate)

                        if(normstate != currentState ){
                            if (currentState != null && currentState != unknown){
                                val previousDistance = stateDistanceMap.getOrDefault(currentState!!,0.0)
                                stateDistanceMap[currentState!!] = previousDistance

                            }

                            currentState = fetchedstate
                            stateDistanceMap.putIfAbsent(currentState!!,0.0)


                        }

                    }




                }
            })
        } else {


            try {
                val adresss = geocoder.getFromLocation(location.latitude,location.longitude,1)
                val state2 = adresss?.firstOrNull()?.adminArea ?: unknown
                val simstate = normalizeState(state2)
                if(simstate != currentState){
                    if (currentState != null && currentState != unknown){
                        val previousDistance = stateDistanceMap.getOrDefault(currentState!!,0.0)
                        stateDistanceMap[currentState!!] = previousDistance

                    }

                    currentState = simstate
                    stateDistanceMap.putIfAbsent(currentState!!,0.0)


                }




            } catch (e:Exception) {
                fetchStatefromCoordinate(location.latitude,location.longitude){ fetchedstate ->
                    val timstate = normalizeState(fetchedstate)


                    if(timstate != currentState){
                        if (currentState != null && currentState != unknown){
                            val previousDistance = stateDistanceMap.getOrDefault(currentState!!,0.0)
                            stateDistanceMap[currentState!!] = previousDistance

                        }

                        currentState = timstate
                        stateDistanceMap.putIfAbsent(currentState!!,0.0)


                    }

                }






            }





        }







    }




    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        startfootpriUpdate()
        return START_STICKY
    }



    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback!!)
        serviceJob.cancel()
        isServiceRunning = false
    }



    private fun getCurrentMillisInSelectedTimeZone(): Long {
       val zonedatetime = ZonedDateTime.now(ZoneId.of(selectedTimeZone.id))

        return zonedatetime.toInstant().toEpochMilli()


    }





   fun resetDailyTrackingIfNeeded() {
       GetDate()
       val currentMillisInSelectedTimeZone = getCurrentMillisInSelectedTimeZone()
       if (currentMillisInSelectedTimeZone >= currentDayStartMillis + TimeUnit.DAYS.toMillis(1)){
           Handler(Looper.getMainLooper()).postDelayed({
               saveRoute(date, userRoute)
           },4000)


       }

      /*  val currentMillisInSelectedTimeZone = getCurrentMillisInSelectedTimeZone()
        if (currentMillisInSelectedTimeZone >= currentDayStartMillis + TimeUnit.DAYS.toMillis(1)) {
            withContext(Dispatchers.Main){
                Toast.makeText(this@LocationService,"saving to database",Toast.LENGTH_SHORT).show()
            }
             saveRoute(date, userRoute)

         }*/
     }


    fun pseudosavestate(){
        GetDate()
        Handler(Looper.getMainLooper()).postDelayed({
            saveRoute(date, userRoute)
        },4000)
    }









    private fun calculateNextDayStartMillis() {

        val zoneDatetime = ZonedDateTime.of(LocalDate.now(selectedTimeZone.toZoneId()),LocalTime.of(selectedHour,selectedMinute),selectedTimeZone.toZoneId())


        currentDayStartMillis = zoneDatetime.toInstant().toEpochMilli()





    /*    val currentMillisInSelectedTimeZone = getCurrentMillisInSelectedTimeZone()
        val calendar = Calendar.getInstance(selectedTimeZone)
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
        calendar.set(Calendar.MINUTE, selectedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (currentMillisInSelectedTimeZone > calendar.timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val nextDayMillis = calendar.timeInMillis
            val nextDay = Date(nextDayMillis)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormat.format(nextDay)
         //   Toast.makeText(this, "Next day: $formattedDate", Toast.LENGTH_LONG).show()
        } else {
           ///
        }
        currentDayStartMillis = calendar.timeInMillis*/

    }



    private fun loadUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        selectedHour = sharedPreferences.getInt("selectedHour", 7)
        selectedMinute = sharedPreferences.getInt("selectedMinute", 0)
        val timeZoneId = sharedPreferences.getString("selectedTimeZone", TimeZone.getDefault().id)
        selectedTimeZone = TimeZone.getTimeZone(timeZoneId)
        Toast.makeText(this@LocationService,"$selectedHour,$selectedMinute",Toast.LENGTH_LONG).show()
        calculateNextDayStartMillis()
    }



    @SuppressLint("MissingPermission")
    private fun startfootpriUpdate() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY,60000L).setMinUpdateIntervalMillis(10000L)
            .build(),
            locationCallback!!,
            Looper.getMainLooper())










    }






     fun saveRoute(date: String?, userRoute: SnapshotStateList<LatLng>) {
       serviceScope.launch {
             isSavingProgress = true
           val realm = Realm.getDefaultInstance()
           val routeJson = Gson().toJson(userRoute)
           realm.executeTransaction { realmInstance ->
               val latLngRealmObject = LatLngRealmObject().apply {
                   this.date = date!!
                   this.routejson = routeJson
               }
               realmInstance.copyToRealmOrUpdate(latLngRealmObject)

           }
           realm.close()

           savewholeState(currentState,date)  // suspend function
           isSavingProgress = false


       }



    }


     suspend fun savewholeState(currentState: String?, date: String?) {
          val realm = Realm.getDefaultInstance()

           val validstatedistanceMap = stateDistanceMap.filter { (stateName,distance) ->
                   stateName != unknown && distance > 0.0

           }


              realm.executeTransaction { realmtrans ->
                  validstatedistanceMap.forEach { (statename, distance) ->
                      val stateDistance = StateDistance().apply {
                          this.date = date!!
                          this.stateName = statename
                          this.distanceInMiles = distance

                      }
                      realmtrans.copyToRealmOrUpdate(stateDistance)
                  }
              }
              realm.close()
              withContext(Dispatchers.Main) {
                  totalDistanceInMiles = 0.0
                  stateDistanceMap.clear()
                  lastLocation = null
                  userRoute.clear()
                  currentDayStartMillis += TimeUnit.DAYS.toMillis(1)
                   Toast.makeText(this@LocationService,"Cleared",Toast.LENGTH_SHORT).show()

              }
     }





    fun fetchStatefromCoordinate(latitude: Double, longitude: Double, callback: (String) -> Unit) {
        val apiKey = "AIzaSyD-X5U1mQ1p6sOTBAA0K8l5Kd1ilFBc_kk"
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$latitude,$longitude&key=$apiKey"


        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                    callback(unknown)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string().let { responsebody ->
                    try {
                        val jsonObject = JSONObject(responsebody)
                        val result = jsonObject.getJSONArray("results")
                        if(result.length() > 0) {
                            val adressComponent = result.getJSONObject(0).getJSONArray("address_components")
                            for( i in 0 until adressComponent.length()) {
                                val component = adressComponent.getJSONObject(i)
                                val types = component.getJSONArray("types")
                                if(types.toString().contains("administrative_area_level_1")){
                                    val state = component.getString("long_name")
                                    callback(state)
                                    return
                                }
                            }

                        }

                    } catch (e:JSONException){

                    }
                }

                callback(unknown)

            }


        })



    }



    fun normalizeState(state: String): String {
        return state.trim().lowercase(Locale.getDefault())
    }





























      private fun createNotification(): Notification {
        val notificationChannelId = "LocationServiceChannel"
          val channel = NotificationChannel(
              notificationChannelId,
              "Location Service",
              NotificationManager.IMPORTANCE_LOW
          )
          val manager = getSystemService(NotificationManager::class.java)
          manager.createNotificationChannel(channel)
          return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Tracking Route")
            .setContentText("Tracking : ON")
              .setOngoing(true)
            .setSmallIcon(R.drawable.baseline_location_on_24)
              .setPriority(NotificationCompat.PRIORITY_LOW)
              .build()
    }



    private fun updateNotification() {
     //   val decimalstate= BigDecimal(distanceInMiles).setScale(2,RoundingMode.HALF_DOWN).toDouble()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelId = "LocationServiceChannel"
        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Tracking Route")
            .setContentText("miles loading..") // Update content text here
            .setSmallIcon(R.drawable.baseline_location_on_24) // Ensure you have this icon in your resources
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MapActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        notificationManager.notify(1, notification)

    }


    private fun sendLocationUpdate(latLng: LatLng, distanceInMiles: Double) {
        val intent = Intent("LocationUpdate").apply {
            putExtra("latLng", latLng)
            putExtra("distanceInMiles", distanceInMiles)
            putExtra("State",currentState)
        }
        sendBroadcast(intent)
    }















}





