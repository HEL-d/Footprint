package com.mile.footprint.Utils

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class LatLngRealmObject(
@PrimaryKey var date : String = "",
            var routejson : String = ""


 ):RealmObject()

