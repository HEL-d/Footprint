package com.mile.footprint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mile.footprint.ui.theme.FootprintTheme


class MapActivity : ComponentActivity() {

    private var googleMap: GoogleMap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FootprintTheme {
                ShowMap()
            }

        }


    }


  /*  private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val latLng = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.extras?.getParcelable("latLng", LatLng::class.java)
                } else {
                    intent.getParcelableExtra("latLng")
                }
                val distanceInMiles = it.getDoubleExtra("distanceInMiles", 0.0)
                latLng?.let { location ->
                    googleMap?.let { map ->
                        map.addCircle(
                            CircleOptions().center(location).radius(50.0)
                                .strokeColor(android.graphics.Color.WHITE).strokeWidth(5f)
                                .fillColor(android.graphics.Color.BLUE)
                        )
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                        Toast.makeText(
                            this@MapActivity,
                            "Distance: $distanceInMiles miles",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }*/


    @Composable
    fun ShowMap() {
        val context = LocalContext.current
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val latLng = LatLng(latitude, longitude)

        AndroidView(factory = {
            MapView(it).apply {
                onCreate(null)
                getMapAsync { googlMap ->
                    googleMap = googlMap
                }
            }


        }, modifier = Modifier.fillMaxSize(), update = {
            it.getMapAsync { map ->
                googleMap = map
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                googleMap?.clear()
                googleMap?.addMarker(MarkerOptions().position(latLng).title("Current Location"))



            }
        })


    }
}





























