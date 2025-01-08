package com.mile.footprint

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.android.gms.maps.model.LatLng
import screen.Historyscreen
import screen.HomeScreen
import screen.Mapscreen



@Composable
 fun BottomNavGraph(
    navController: NavHostController,
    distanceInMiles: State<Double>,
    statename: State<String>,
    locationstate: State<LatLng>
) {
     NavHost(navController = navController, startDestination = BottomBarscreen.Home.route) {
         composable(route = BottomBarscreen.Home.route){
             HomeScreen(distanceInMiles,statename,locationstate)
         }
         composable(route = BottomBarscreen.Maps.route){
            Mapscreen()
         }
         composable(route = BottomBarscreen.History.route){
           Historyscreen()
         }
     }
}