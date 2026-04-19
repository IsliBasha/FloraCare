package com.floracare.app.data.local

import androidx.room.TypeConverter
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.SoilMoistureNote
import com.floracare.app.domain.model.Toxicity
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilliseconds()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @TypeConverter fun careTypeToString(v: CareTaskType?): String? = v?.name
    @TypeConverter fun stringToCareType(v: String?): CareTaskType? = v?.let(CareTaskType::valueOf)

    @TypeConverter fun sourceToString(v: CareTaskSource?): String? = v?.name
    @TypeConverter fun stringToSource(v: String?): CareTaskSource? = v?.let(CareTaskSource::valueOf)

    @TypeConverter fun lightToString(v: LightNeed?): String? = v?.name
    @TypeConverter fun stringToLight(v: String?): LightNeed? = v?.let(LightNeed::valueOf)

    @TypeConverter fun humidityToString(v: HumidityNeed?): String? = v?.name
    @TypeConverter fun stringToHumidity(v: String?): HumidityNeed? = v?.let(HumidityNeed::valueOf)

    @TypeConverter fun toxicityToString(v: Toxicity?): String? = v?.name
    @TypeConverter fun stringToToxicity(v: String?): Toxicity? = v?.let(Toxicity::valueOf)

    @TypeConverter fun locationToString(v: LocationTag?): String? = v?.name
    @TypeConverter fun stringToLocation(v: String?): LocationTag? = v?.let(LocationTag::valueOf)

    @TypeConverter fun soilToString(v: SoilMoistureNote?): String? = v?.name
    @TypeConverter fun stringToSoil(v: String?): SoilMoistureNote? = v?.let(SoilMoistureNote::valueOf)
}
