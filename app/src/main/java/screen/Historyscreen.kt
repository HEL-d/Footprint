package screen

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mile.footprint.MainActivity
import com.mile.footprint.MapActivity
import com.mile.footprint.ShowMapModel
import com.mile.footprint.ui.theme.robotomonoFontFamily

@Composable
fun Historyscreen() {
    val context = LocalContext.current as MainActivity





    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .padding(top = 25.dp)
                .clickable {
                    context.startActivity(Intent(context, ShowMapModel::class.java))
                },
            text = "App Setting",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 19.sp,
                fontFamily = robotomonoFontFamily,
                fontWeight = FontWeight.Medium
            )

        )


        Text(
            modifier = Modifier
                .padding(top = 25.dp),
            text = "Save Route from here directly",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontFamily = robotomonoFontFamily,
                fontWeight = FontWeight.Light
            )

        )







        Button(
            onClick = {
               context.saveToDatabase()
             //   Toast.makeText(context,"Saving to database",Toast.LENGTH_LONG).show()
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
                text = "Save To Database",
                fontSize = 16.sp,
                fontFamily = robotomonoFontFamily,
                fontWeight = FontWeight.SemiBold,
            )


        }


    }

}