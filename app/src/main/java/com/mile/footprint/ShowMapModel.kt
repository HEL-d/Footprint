package com.mile.footprint

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.google.maps.android.data.LineString
import com.mile.footprint.Utils.LatLngRealmObject
import com.mile.footprint.Utils.StateDistance
import io.realm.Realm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ShowMapModel : ComponentActivity() {
    private var googleMap: GoogleMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
           ShowMapNow()
        }
    }



    @Composable
    private fun ShowMapNow() {
        val currentDate = intent.getStringExtra("date")



        AndroidView(factory = {
            MapView(it).apply {
                onCreate(null)
                getMapAsync { map ->
                    googleMap = map
                 //   val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                  //  val currentDate = dateFormat.format(Date())

                    // Load and display the route for the current date
                    loadAndDisplayRoute(currentDate!!)

                }
            }
        }, modifier = Modifier.fillMaxSize())



    }

 /*   private fun getStateDistancesForDate(currentDate: String) :  List<Pair<String, Double>> {
        val realm = Realm.getDefaultInstance()
        val stateDistances = realm.where(StateDistance::class.java)
            .equalTo("date", currentDate)
            .findAll()

        // Convert Realm result into a list of state names and distances
        val result = stateDistances.map { Pair(it.stateName, it.distanceInMiles) }
        realm.close()
        return result
    }*/



/*    private fun displayStateDistances() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val stateDistances = getStateDistancesForDate(currentDate)






        if (stateDistances.isNotEmpty()) {
            val message = stateDistances.joinToString(separator = "\n") { "${it.first}: ${it.second} miles" }
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(applicationContext, "No state distances for today", Toast.LENGTH_LONG).show()
        }
    }*/


    private fun loadAndDisplayRoute(currentDate: String) {
        val savedRoute = getRoute(currentDate)
        savedRoute.let { route ->
            displayRouteOnMap(route!!)
        }

    }



    private fun getRoute(currentDate: String): List<LatLng>? {
        val realm = io.realm.Realm.getDefaultInstance()
        val userRoute = realm.where(LatLngRealmObject::class.java).equalTo("date", currentDate).findFirst()
        val route = userRoute?.routejson?.let {
            Gson().fromJson(it, Array<LatLng>::class.java).toList()
        }
        Log.d("RouteCheck", "Retrieved Route JSON: ${userRoute?.routejson}")
        Log.d("RouteCheck", "Deserialized Route: $route")
        realm.close()
        return route
    }



    private fun displayRouteOnMap(route: List<LatLng>) {



        googleMap?.let { map ->
            // Simplify the route with Douglas-Peucker
            val simplifiedRoute = douglasPeucker(route, tolerance = 0.0001)

            // Add intermediate points to make the circles closer
            val denseRoute = generateDenseRoute(simplifiedRoute, density = 10) // 10 intermediate points

            denseRoute.forEach { latLng ->
                val circleOptions = CircleOptions().apply {
                    center(latLng)
                    radius(10.0)
                    fillColor(android.graphics.Color.BLUE)
                    strokeColor(android.graphics.Color.WHITE)
                    strokeWidth(2f)
                }
                map.addCircle(circleOptions)
            }

            // Move the camera to the start of the simplified route
            if (denseRoute.isNotEmpty()) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(denseRoute[0], 17f))
            } else {
                Toast.makeText(this, "route null", Toast.LENGTH_LONG).show()
            }
        }












    /*    googleMap?.let { map ->
            if (route.isNotEmpty()) {
                // Iterate through the route and add a circle at each LatLng point
                route.forEach { latLng ->
                    val circleOptions = CircleOptions().apply {
                        center(latLng)                        // Set the center of the circle at the route point
                        radius(10.0)                          // Radius in meters
                        fillColor(android.graphics.Color.BLUE) // Circle fill color
                        strokeColor(android.graphics.Color.WHITE) // Circle border color
                        strokeWidth(2f)                        // Circle border width
                    }
                    map.addCircle(circleOptions)
                }

                // Move the camera to the start of the route
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(route[0], 15f))
            } else {
                Toast.makeText(this, "route null", Toast.LENGTH_LONG).show()
            }
        }*/


        /*googleMap?.let { map ->
            // Simplify the route with a tolerance of 50 meters (adjust as needed)
            val simplifiedRoute = douglasPeucker(route, tolerance = 0.0001)

            // Now use the simplified route instead of the original
            simplifiedRoute.forEach { latLng ->
                val circleOptions = CircleOptions().apply {
                    center(latLng)
                    radius(10.0)
                    fillColor(android.graphics.Color.BLUE)
                    strokeColor(android.graphics.Color.WHITE)
                    strokeWidth(2f)
                }
                map.addCircle(circleOptions)
            }

            // Move the camera to the start of the simplified route
            if (simplifiedRoute.isNotEmpty()) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(simplifiedRoute[0], 17f))
            } else {
                Toast.makeText(this, "route null", Toast.LENGTH_LONG).show()
            }
        }
*/























    }


    fun generateDenseRoute(route: List<LatLng>, density: Int): List<LatLng> {
        val denseRoute = mutableListOf<LatLng>()
        for (i in 0 until route.size - 1) {
            val start = route[i]
            val end = route[i + 1]
            denseRoute.add(start)

            for (j in 1..density) {
                val fraction = j / (density + 1).toDouble()
                val interpolatedPoint = LatLng(
                    start.latitude + fraction * (end.latitude - start.latitude),
                    start.longitude + fraction * (end.longitude - start.longitude)
                )
                denseRoute.add(interpolatedPoint)
            }
        }
        denseRoute.add(route.last()) // Add the last point
        return denseRoute
    }






    fun douglasPeucker(route: List<LatLng>, tolerance: Double): List<LatLng> {
        if (route.size < 3) return route

        val firstPoint = route.first()
        val lastPoint = route.last()

        var maxDistance = 0.0
        var index = 0

        for (i in 1 until route.size - 1) {
            val distance = perpendicularDistance(route[i], firstPoint, lastPoint)
            if (distance > maxDistance) {
                maxDistance = distance
                index = i
            }
        }

        if (maxDistance > tolerance) {
            val leftSegment = douglasPeucker(route.subList(0, index + 1), tolerance)
            val rightSegment = douglasPeucker(route.subList(index, route.size), tolerance)

            return leftSegment.dropLast(1) + rightSegment
        } else {
            return listOf(firstPoint, lastPoint)
        }
    }

    // Calculate perpendicular distance from point to line segment
    private fun perpendicularDistance(point: LatLng, start: LatLng, end: LatLng): Double {
        val dx = end.longitude - start.longitude
        val dy = end.latitude - start.latitude

        val mag = dx * dx + dy * dy
        var u = ((point.longitude - start.longitude) * dx + (point.latitude - start.latitude) * dy) / mag

        if (u > 1) u = 1.0
        else if (u < 0) u = 0.0

        val x = start.longitude + u * dx
        val y = start.latitude + u * dy

        val dx2 = point.longitude - x
        val dy2 = point.latitude - y

        return Math.sqrt(dx2 * dx2 + dy2 * dy2)
    }





}






