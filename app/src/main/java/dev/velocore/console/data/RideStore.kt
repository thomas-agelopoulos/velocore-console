package dev.velocore.console.data

import dev.velocore.console.ride.RideSummary
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** A finished ride as kept on the bike. */
data class RideRecord(
    val startedAt: Long,
    val workoutId: String?,
    val name: String,
    val seconds: Int,
    val avgWatts: Int,
    val kilojoules: Int,
    val avgRpm: Int,
    val spark: List<Int>,
)

/** Rides saved as a small JSON file in the app's private storage. */
class RideStore(private val file: File) {

    @Synchronized
    fun load(): List<RideRecord> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val spark = o.optJSONArray("spark")
                RideRecord(
                    startedAt = o.getLong("startedAt"),
                    workoutId = if (o.isNull("workoutId")) null else o.optString("workoutId"),
                    name = o.getString("name"),
                    seconds = o.getInt("seconds"),
                    avgWatts = o.getInt("avgWatts"),
                    kilojoules = o.getInt("kilojoules"),
                    avgRpm = o.optInt("avgRpm"),
                    spark = if (spark == null) emptyList() else (0 until spark.length()).map { spark.getInt(it) },
                )
            }.sortedBy { it.startedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(summary: RideSummary): RideRecord {
        val record = RideRecord(
            startedAt = summary.startedAt,
            workoutId = summary.workoutId,
            name = summary.name,
            seconds = summary.seconds,
            avgWatts = summary.avgWatts,
            kilojoules = summary.kilojoules,
            avgRpm = summary.avgRpm,
            spark = downsample(summary.trace, SPARK_POINTS),
        )
        val all = load() + record
        val arr = JSONArray()
        all.forEach { r ->
            arr.put(
                JSONObject()
                    .put("startedAt", r.startedAt)
                    .put("workoutId", r.workoutId ?: JSONObject.NULL)
                    .put("name", r.name)
                    .put("seconds", r.seconds)
                    .put("avgWatts", r.avgWatts)
                    .put("kilojoules", r.kilojoules)
                    .put("avgRpm", r.avgRpm)
                    .put("spark", JSONArray(r.spark)),
            )
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
        return record
    }

    companion object {
        const val SPARK_POINTS = 88

        fun downsample(values: List<Int>, points: Int): List<Int> {
            if (values.size <= points) return values
            val step = values.size.toDouble() / points
            return (0 until points).map { k ->
                val from = (k * step).toInt()
                val to = ((k + 1) * step).toInt().coerceAtMost(values.size).coerceAtLeast(from + 1)
                values.subList(from, to).average().roundToInt()
            }
        }

        /** Minutes ridden on each day of the current week, Monday first. */
        fun weekMinutes(rides: List<RideRecord>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): IntArray {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val out = IntArray(7)
            rides.forEach { r ->
                val day = Instant.ofEpochMilli(r.startedAt).atZone(zone).toLocalDate()
                val idx = (day.toEpochDay() - monday.toEpochDay()).toInt()
                if (idx in 0..6) out[idx] += (r.seconds + 30) / 60
            }
            return out
        }
    }
}
