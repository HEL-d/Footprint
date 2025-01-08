package com.mile.footprint.Utils

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.UUID

open class StateDistance : RealmObject() {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()  // Unique ID for each entry
    var date: String = ""  // Date for tracking
    var stateName: String = ""
    var distanceInMiles: Double = 0.0
}

