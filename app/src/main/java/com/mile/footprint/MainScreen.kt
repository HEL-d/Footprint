package com.mile.footprint

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.mile.footprint.ui.theme.robotomonoFontFamily

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    distanceInMiles: State<Double>,
    statename: State<String>,
    locationstate: State<LatLng>
) {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BottomBar(navController = navController) }){
        BottomNavGraph(navController = navController,distanceInMiles,statename,locationstate)
    }






}


@Composable
fun BottomBar(navController: NavHostController){
    val screens = listOf(BottomBarscreen.Home, BottomBarscreen.Maps, BottomBarscreen.History)
    val navbackstackEntry by navController.currentBackStackEntryAsState()
    val currentdestination = navbackstackEntry?.destination

   NavigationBar {
      screens.forEach {
          AddItems(
              screens = it,
              currrentdestination = currentdestination,
              navController = navController
          )
      }
   }

}


@Composable
fun RowScope.AddItems(
    screens:BottomBarscreen,
    currrentdestination : NavDestination?,
    navController: NavHostController
){

   NavigationBarItem(
      label = {
          Text(text = screens.title, style =  androidx.compose.ui.text.TextStyle(fontFamily = robotomonoFontFamily, fontWeight = FontWeight.Normal))
      },
       icon = {
           Icon(imageVector = screens.icon, contentDescription = "Navigation Icon")
       } ,
       selected = currrentdestination?.hierarchy?.any {
           it.route == screens.route
       } == true ,

       onClick = {
           navController.navigate(screens.route){
               popUpTo(navController.graph.findStartDestination().id)
               launchSingleTop = true

           }
       }

   )




}
