package ai.familystop.feature.recommend

import ai.familystop.core.DataSource
import ai.familystop.core.Facility
import ai.familystop.core.StopNeed
import ai.familystop.core.StopPlace
import org.json.JSONArray

/**
 * 시연 시설 JSON(`app/src/main/assets/demo/facilities.json`)을 파싱한다. 앱 셸이 assets를 읽어
 * 이 함수로 [FacilitySource]를 만든다(feature는 Context 불필요, ADR-0001).
 * 실제 시설은 `"source": "REAL"`로 같은 파일에 추가한다.
 */
fun parseFacilities(json: String): List<Facility> {
    val arr = JSONArray(json)
    return List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        val provides = o.getJSONArray("provides")
        Facility(
            place = StopPlace(
                o.getString("name"),
                o.getDouble("lat"),
                o.getDouble("lon"),
                o.optString("id").ifEmpty {
                    null
                }
            ),
            provides = List(provides.length()) { StopNeed.valueOf(provides.getString(it)) }.toSet(),
            source = DataSource.valueOf(o.getString("source")),
            detourKm = o.getDouble("detourKm"),
            extraMinutes = o.getInt("extraMinutes")
        )
    }
}
