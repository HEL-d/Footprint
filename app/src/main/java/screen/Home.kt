package screen


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.mile.footprint.MainActivity
import com.mile.footprint.MapActivity
import com.mile.footprint.Utils.LocationService


import com.mile.footprint.ui.theme.robotomonoFontFamily
import java.math.BigDecimal
import java.math.RoundingMode


@Composable
fun HomeScreen(
    distanceInMiles: State<Double>,
    statename: State<String>,
    locationstate: State<LatLng>
) {



    val context = LocalContext.current as MainActivity










            Column(modifier = Modifier.fillMaxSize(),
       verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally) {


       Text(
           modifier = Modifier.padding(top = 25.dp).clickable {


           },
           text = "Footprint",
           style =  androidx.compose.ui.text.TextStyle(fontSize = 19.sp, fontFamily = robotomonoFontFamily, fontWeight = FontWeight.Medium)



       )
       Text(
           text =   BigDecimal(distanceInMiles.value).setScale(2, RoundingMode.HALF_DOWN).toString() +"mi today",
           modifier = Modifier.padding(top = 40.dp),
           style =  androidx.compose.ui.text.TextStyle(fontSize = 26.sp,fontFamily = robotomonoFontFamily, fontWeight = FontWeight.Bold)
       )
       Text(
           text = statename.value,
           modifier = Modifier.padding(top = 25.dp),
           style =  androidx.compose.ui.text.TextStyle(fontSize = 17.sp,fontFamily = robotomonoFontFamily, fontWeight = FontWeight.Light)
       )

       Text(
           text = "Tracking : ON",
           modifier = Modifier.padding(top = 25.dp),
           style =  androidx.compose.ui.text.TextStyle(fontSize = 17.sp,fontFamily = robotomonoFontFamily, fontWeight = FontWeight.Light),

       )

       Button(
           onClick = {
               val latLng = locationstate.value
               val intent = Intent(context, MapActivity::class.java)
               intent.putExtra("latitude", latLng.latitude)
               intent.putExtra("longitude", latLng.longitude)
               context.startActivity(intent)
                     },
           modifier = Modifier
               .fillMaxWidth()
               .padding(top = 60.dp, start = 12.dp, end = 12.dp)
               .height(50.dp),
           enabled = true,
           colors = ButtonDefaults.buttonColors(
               containerColor = Color.Blue,
               contentColor = Color.White
           ),
           elevation = ButtonDefaults.buttonElevation(10.dp),
           shape = RoundedCornerShape(12.dp),


           ) {
           Text(
               text = "View Live Status",
               fontSize = 16.sp,
               fontFamily = robotomonoFontFamily,
               fontWeight = FontWeight.SemiBold,
           )


       }





   }


    BackHandler {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }



}









