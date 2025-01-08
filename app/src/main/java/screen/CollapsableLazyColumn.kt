package screen

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mile.footprint.ShowMapModel
import com.mile.footprint.Utils.StateDistance
import com.mile.footprint.ui.theme.robotomonoFontFamily
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.KProperty


@Composable
fun CollapsableLazyColumn(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sections by remember { mutableStateOf(emptyList<CollapsableSection>()) }
    var filteredSections by remember { mutableStateOf(emptyList<CollapsableSection>()) }
    var searchText by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 10
    val coroutineScope = rememberCoroutineScope()
    var isLastPage by remember { mutableStateOf(false) }  // Track if it's the last page
    var collapsedState by remember { mutableStateOf(MutableList(sections.size) { true }) }


    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                exportDataToCSV(context, filteredSections)
            } else {
                Toast.makeText(context, "Storage permission denied", Toast.LENGTH_LONG).show()
            }
        }
    )




    // Initial data load
    LaunchedEffect(Unit) {
        fetchPaginatedData(currentPage, pageSize, coroutineScope) { newSections, hasMore ->
            sections = newSections
            filteredSections = newSections // Initially show all sections
            collapsedState = MutableList(newSections.size) { true }
            isLastPage = !hasMore
        }
    }

    // Filter sections based on the search text
    LaunchedEffect(searchText) {
        filteredSections = if (searchText.isBlank()) {
            sections // If no search text, show all sections
        } else {
            sections.filter { it.title.contains(searchText, ignoreCase = true) } // Filter by title (date)
        }
    }






    Column(modifier = modifier.fillMaxSize()) {
        // Search Bar
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search by Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // No need for WRITE_EXTERNAL_STORAGE permission, directly export using MediaStore
                    exportDataToCSV(context, filteredSections)
                } else {
                    // For Android 9 and below, check if WRITE_EXTERNAL_STORAGE is granted
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        // Permission is granted, export the data
                        exportDataToCSV(context, filteredSections)
                    } else {
                        // Request the storage permission for Android 9 and below
                        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                      },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(40.dp),
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(10.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Export to CSV")
        }






        LazyColumn(modifier) {
            if (filteredSections.isNotEmpty()) {
                filteredSections.forEachIndexed { i, dataItem ->
                    val collapsed = collapsedState.getOrNull(i) ?: true

                    item(key = "header_$i") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    collapsedState = collapsedState.toMutableList().also {
                                        it[i] = !collapsed
                                    }
                                }
                        ) {
                            Icon(
                                Icons.Default.run {
                                    if (collapsed) KeyboardArrowDown else KeyboardArrowUp
                                },
                                contentDescription = "",
                                tint = Color.LightGray,
                            )
                            Text(
                                dataItem.title,
                                fontFamily = robotomonoFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp,
                                modifier = Modifier
                                    .padding(vertical = 17.dp)
                                    .weight(1f)
                            )
                        }

                        HorizontalDivider()
                    }

                    if (!collapsed) {
                        items(dataItem.rows) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        val date = dataItem.title.substringBeforeLast("-").trim()
                                        val intent = Intent(context,ShowMapModel::class.java)
                                        intent.putExtra("date",date)
                                        context.startActivity(intent)

                                       }
                            ) {
                                Spacer(modifier = Modifier.size(20.dp))
                                Text(
                                    row,
                                    fontFamily = robotomonoFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 17.sp,
                                    modifier = Modifier.padding(vertical = 17.dp)
                                )
                            }
                            HorizontalDivider()
                        }

                        if (!isLastPage) {
                            item {
                                Button(
                                    onClick = {
                                        currentPage++
                                        fetchPaginatedData(currentPage, pageSize, coroutineScope) { newSections, hasMore ->
                                            sections = newSections
                                            collapsedState = collapsedState.toMutableList().also {
                                                it.addAll(List(newSections.size - it.size) { true })
                                            }
                                            isLastPage = !hasMore
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                ) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            } else {
                // Show message when there is no data
                item {
                    Text(
                        text = "No data available.",
                        modifier = Modifier.padding(16.dp),
                        fontFamily = robotomonoFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun fetchPaginatedData(
    page: Int,
    pageSize: Int,
    coroutineScope: CoroutineScope,
    onFetched: (List<CollapsableSection>, Boolean) -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        val newSections = mutableListOf<CollapsableSection>()
        val realm = Realm.getDefaultInstance()
        var hasMore = false
        realm.executeTransaction { realmInstance ->
            val results = realmInstance.where(StateDistance::class.java)
                .sort("date")
                .findAll()

            val totalResults = results.size
            val startIndex = page * pageSize
            val endIndex = ((page + 1) * pageSize).coerceAtMost(totalResults)

            Log.d("FetchPaginatedData", "Total results: $totalResults, Start index: $startIndex, End index: $endIndex")

            if (startIndex < totalResults) {
                val paginatedResults = results.subList(startIndex, endIndex)

                if (paginatedResults.isNotEmpty()) {
                    val groupedByDate = paginatedResults.groupBy { it.date }
                    groupedByDate.forEach { (date, stateDistances) ->
                        Log.d("Grouped Data", "Date: $date, State Distances: $stateDistances")

                        if (date.isNotBlank()) {
                            val totalDistance = stateDistances.sumOf { it.distanceInMiles }
                            val decimaldistance = BigDecimal(totalDistance).setScale(2,RoundingMode.HALF_DOWN).toDouble()
                            val maxIntegerLength = stateDistances.maxOf { it.distanceInMiles.toInt().toString().length }
                            val rows = stateDistances.map {
                                val decimalstate= BigDecimal(it.distanceInMiles).setScale(2,RoundingMode.HALF_DOWN).toDouble()
                                val integerPartLength = decimalstate.toInt().toString().length
                                val padding = " ".repeat(maxIntegerLength - integerPartLength)
                                "$padding$decimalstate ${it.stateName.take(2).uppercase()} - ${it.stateName}"
                            }

                            if (rows.isNotEmpty()) {
                                newSections.add(CollapsableSection(title =  "$date - ($decimaldistance MI)", rows = rows))
                            }
                        }
                    }
                }
            }
            hasMore = endIndex < totalResults
        }
        realm.close()

        withContext(Dispatchers.Main) {
            onFetched(newSections, hasMore)
        }
    }
}

fun exportDataToCSV(context: Context, sections: List<CollapsableSection>) {
    val csvBuilder = StringBuilder()
    csvBuilder.append("Date,State,Miles\n") // Add header row

    sections.forEach { section ->
        // Write the date row (first row per date)
        csvBuilder.append("${section.title.substringBeforeLast("-")},,\n") // Date in the first column, empty for State and Miles columns

        // Write each state and miles under the date
        section.rows.forEach { row ->
            // Split the row into components based on space and dash
            val parts = row.split(" ", limit = 2)
            if (parts.size == 2) {
                val miles = parts[0].trim()  // The first part is the distance
                val stateInfo = parts[1].trim() // The second part is the state information ("XX - StateName")

                // Write empty date (since it's grouped), and state with miles
                csvBuilder.append(",$stateInfo,$miles\n")
            }
        }

        // Add a line break after each date section for clarity
        csvBuilder.append("\n")
    }


    val fileName = "MyRouteData_${System.currentTimeMillis()}.csv"
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore API for Android 10 and above
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvBuilder.toString().toByteArray())
                    outputStream.flush()
                }
                Toast.makeText(context, "Exported Successfully $fileName", Toast.LENGTH_LONG).show()
            } ?: throw IOException("Failed to create CSV file")

        } else {
            // For Android 9 and below, use legacy external storage access
            val filePath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(filePath).use { fileOutputStream ->
                OutputStreamWriter(fileOutputStream).use { writer ->
                    writer.write(csvBuilder.toString())
                }
            }
            Toast.makeText(context, "Exported Successfully $fileName", Toast.LENGTH_LONG).show()
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to export data", Toast.LENGTH_LONG).show()
    }
}



data class CollapsableSection(val title: String, val rows: List<String>)