package com.mile.footprint

import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarscreen(
  val route :String,
  val title:String,
  val icon:ImageVector
 ) {
   object Home : BottomBarscreen(
    route = "home",
    title = "Home",
    icon = Icons.Default.Home
   )
 object Maps : BottomBarscreen(
  route = "map",
  title = "Record",
  icon = Icons.Default.DateRange
 )
 object History : BottomBarscreen(
  route = "history",
  title = "Setting",
  icon = Icons.Default.Settings
 )



}


