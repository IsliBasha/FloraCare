package com.floracare.app.ui.feature.plantdetail

import com.floracare.app.domain.model.CareTask
import com.floracare.app.domain.model.CareTaskSource
import com.floracare.app.domain.model.CareTaskType
import com.floracare.app.domain.model.HumidityNeed
import com.floracare.app.domain.model.LightNeed
import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.domain.model.Species
import com.floracare.app.domain.model.Toxicity
import com.floracare.app.domain.model.WeatherSnapshot
import com.floracare.app.domain.usecase.ComputeNextCareTaskUseCase
import com.floracare.app.domain.usecase.ReenrichPlantSpeciesUseCase
import com.floracare.app.domain.usecase.SpeciesLookupUseCase
import com.floracare.app.test.FakeDiagnosisRepository
import com.floracare.app.test.FakePlantRepository
import com.floracare.app.test.FakeSpeciesRepository
import com.floracare.app.test.FakeWeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Shape tests for the weather-reason badge surfaced via [UpcomingTask.reasonLabel].
 * The compute use-case has its own pure-function tests; here we only verify the VM
 * threads weather + plant + species into the right reason and maps it to the
 * expected UI label.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var plants: FakePlantRepository
    private lateinit var weather: FakeWeatherRepository
    private lateinit var reenrich: ReenrichPlantSpeciesUseCase
    private val computeNextTask = ComputeNextCareTaskUseCase()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        plants = FakePlantRepository()
        weather = FakeWeatherRepository()
        reenrich = ReenrichPlantSpeciesUseCase(plants, SpeciesLookupUseCase(FakeSpeciesRepository()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.keepSubscribed(vm: PlantDetailViewModel, block: () -> Unit) {
        val job = launch { vm.state.collect { /* drain */ } }
        try {
            block()
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `outdoor plant with heavy recent rain surfaces 'delayed by recent rain' on the WATER row`() =
        runTest(dispatcher) {
            val now = Clock.System.now()
            seedPlant(location = LocationTag.OUTDOOR)
            seedSpecies()
            seedWaterTask(now + 10.days)
            seedWeather(
                weatherAt(now - 4.hours, rain = 8f),
                weatherAt(now - 28.hours, rain = 6f),
            )
            val vm = newViewModel()

            keepSubscribed(vm) {
                val ready = vm.state.value as PlantDetailUiState.Ready
                val water = ready.upcoming.firstOrNull { it.type == CareTaskType.WATER }
                assertNotNull("expected a WATER upcoming task", water)
                assertEquals("delayed by recent rain", water!!.reasonLabel)
            }
        }

    @Test
    fun `indoor plant in same conditions does not surface RAIN`() = runTest(dispatcher) {
        val now = Clock.System.now()
        seedPlant(location = LocationTag.INDOOR)
        seedSpecies()
        seedWaterTask(now + 10.days)
        seedWeather(
            weatherAt(now - 4.hours, rain = 8f),
            weatherAt(now - 28.hours, rain = 6f),
        )
        val vm = newViewModel()

        keepSubscribed(vm) {
            val ready = vm.state.value as PlantDetailUiState.Ready
            val water = ready.upcoming.first { it.type == CareTaskType.WATER }
            assertNull("indoor plants ignore rainfall", water.reasonLabel)
        }
    }

    @Test
    fun `prolonged heat surfaces 'hastened by heat'`() = runTest(dispatcher) {
        val now = Clock.System.now()
        seedPlant()
        seedSpecies()
        seedWaterTask(now + 8.days)
        seedWeather(
            weatherAt(now - 4.hours, temp = 30f),
            weatherAt(now - 28.hours, temp = 31f),
        )
        val vm = newViewModel()

        keepSubscribed(vm) {
            val ready = vm.state.value as PlantDetailUiState.Ready
            val water = ready.upcoming.first { it.type == CareTaskType.WATER }
            assertEquals("hastened by heat", water.reasonLabel)
        }
    }

    @Test
    fun `calm conditions leave reasonLabel null`() = runTest(dispatcher) {
        val now = Clock.System.now()
        seedPlant()
        seedSpecies()
        seedWaterTask(now + 7.days)
        seedWeather(
            weatherAt(now - 4.hours, temp = 21f, humidity = 55f, rain = 0f),
            weatherAt(now - 28.hours, temp = 22f, humidity = 60f, rain = 0f),
        )
        val vm = newViewModel()

        keepSubscribed(vm) {
            val ready = vm.state.value as PlantDetailUiState.Ready
            val water = ready.upcoming.first { it.type == CareTaskType.WATER }
            assertNull("no modifiers fired → no badge", water.reasonLabel)
        }
    }

    @Test
    fun `WATER task without species shows null reasonLabel and does not crash`() = runTest(dispatcher) {
        val now = Clock.System.now()
        seedPlant(speciesId = null)
        // No species inserted.
        seedWaterTask(now + 7.days)
        seedWeather(weatherAt(now - 4.hours, temp = 30f, rain = 8f))
        val vm = newViewModel()

        keepSubscribed(vm) {
            val ready = vm.state.value as PlantDetailUiState.Ready
            val water = ready.upcoming.first { it.type == CareTaskType.WATER }
            assertNull(water.reasonLabel)
        }
    }

    @Test
    fun `non-WATER tasks never carry a reasonLabel`() = runTest(dispatcher) {
        val now = Clock.System.now()
        seedPlant(location = LocationTag.OUTDOOR)
        seedSpecies()
        plants.openTasks.value = listOf(
            CareTask(
                id = "ct-fert",
                plantId = PLANT_ID,
                type = CareTaskType.FERTILIZE,
                scheduledAt = now + 3.days,
                source = CareTaskSource.ADAPTIVE,
            ),
        )
        seedWeather(weatherAt(now - 4.hours, rain = 8f))
        val vm = newViewModel()

        keepSubscribed(vm) {
            val ready = vm.state.value as PlantDetailUiState.Ready
            assertTrue(ready.upcoming.all { it.reasonLabel == null })
        }
    }

    private fun newViewModel() = PlantDetailViewModel(
        plantId = PLANT_ID,
        repo = plants,
        reenrich = reenrich,
        weather = weather,
        computeNextTask = computeNextTask,
        diagnosisRepo = FakeDiagnosisRepository(),
    )

    private fun seedPlant(
        location: LocationTag = LocationTag.INDOOR,
        speciesId: String? = SPECIES_ID,
    ) {
        plants.plants.value = listOf(
            Plant(
                id = PLANT_ID,
                nickname = "Mona",
                speciesId = speciesId,
                locationTag = location,
                acquiredAt = Instant.parse("2026-01-01T00:00:00Z"),
                coverPhotoUri = null,
                notes = "",
            ),
        )
    }

    private fun seedSpecies(provider: String = Species.PROVIDER_PERENUAL) {
        plants.species.value = listOf(
            Species(
                id = SPECIES_ID,
                scientificName = "Monstera deliciosa",
                commonName = "Monstera",
                waterFrequencyDays = 10,
                lightNeed = LightNeed.MEDIUM,
                humidityNeed = HumidityNeed.MEDIUM,
                temperatureRangeC = 18f..28f,
                toxicity = Toxicity.MILD,
                careNotes = "",
                provider = provider,
            ),
        )
    }

    private fun seedWaterTask(scheduledAt: Instant) {
        plants.openTasks.value = listOf(
            CareTask(
                id = "ct-water",
                plantId = PLANT_ID,
                type = CareTaskType.WATER,
                scheduledAt = scheduledAt,
                source = CareTaskSource.ADAPTIVE,
            ),
        )
    }

    private fun seedWeather(vararg snapshots: WeatherSnapshot) {
        weather.snapshots.value = snapshots.toList()
    }

    private fun weatherAt(
        instant: Instant,
        temp: Float = 22f,
        humidity: Float = 50f,
        rain: Float = 0f,
    ): WeatherSnapshot = WeatherSnapshot(
        id = "w-${instant.toEpochMilliseconds()}",
        lat = 0.0,
        lon = 0.0,
        recordedAt = instant,
        tempC = temp,
        humidityPct = humidity,
        rainMm = rain,
        uvIndex = 0f,
    )

    companion object {
        private const val PLANT_ID = "pl-1"
        private const val SPECIES_ID = "sp-monstera"
    }
}
