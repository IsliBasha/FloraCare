package com.floracare.app.ui.feature.editplant

import com.floracare.app.domain.model.LocationTag
import com.floracare.app.domain.model.Plant
import com.floracare.app.test.FakePlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditPlantViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val acquired = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private fun seededPlant(
        id: String = "pl-1",
        nickname: String = "Mona",
        notes: String = "lives by the window",
        location: LocationTag = LocationTag.INDOOR,
        coverPhotoUri: String? = null,
    ) = Plant(
        id = id,
        nickname = nickname,
        speciesId = "sp-monstera",
        locationTag = location,
        acquiredAt = acquired,
        coverPhotoUri = coverPhotoUri,
        notes = notes,
    )

    private fun vm(
        plantId: String = "pl-1",
        seeded: List<Plant> = listOf(seededPlant(id = plantId)),
    ): Pair<EditPlantViewModel, FakePlantRepository> {
        val repo = FakePlantRepository().apply { plants.value = seeded }
        return EditPlantViewModel(plantId, repo) to repo
    }

    @Test
    fun `initial Loading transitions to Ready with persisted fields`() = runTest {
        val (vm, _) = vm()
        val s = vm.state.value
        assertTrue(s is EditPlantUiState.Ready)
        s as EditPlantUiState.Ready
        assertEquals("pl-1", s.plantId)
        assertEquals("Mona", s.nickname)
        assertEquals("lives by the window", s.notes)
        assertEquals(LocationTag.INDOOR, s.locationTag)
        assertNull(s.coverPhotoUri)
        assertFalse(s.saving)
        assertNull(s.nicknameError)
    }

    @Test
    fun `unknown plant id resolves to NotFound`() = runTest {
        val (vm, _) = vm(plantId = "pl-missing", seeded = emptyList())
        assertEquals(EditPlantUiState.NotFound, vm.state.value)
    }

    @Test
    fun `Set events update the draft without writing to repo`() = runTest {
        val (vm, repo) = vm()
        vm.onEvent(EditPlantEvent.SetNickname("Mona Lisa"))
        vm.onEvent(EditPlantEvent.SetNotes("repotted"))
        vm.onEvent(EditPlantEvent.SetLocation(LocationTag.GREENHOUSE))
        vm.onEvent(EditPlantEvent.SetCoverPhotoUri("content://photo/42"))

        val s = vm.state.value as EditPlantUiState.Ready
        assertEquals("Mona Lisa", s.nickname)
        assertEquals("repotted", s.notes)
        assertEquals(LocationTag.GREENHOUSE, s.locationTag)
        assertEquals("content://photo/42", s.coverPhotoUri)
        assertTrue(repo.upsertedPlants.isEmpty())
        assertTrue(repo.archivedCalls.isEmpty())
    }

    @Test
    fun `Save with blank nickname surfaces validation and skips upsert`() = runTest {
        val (vm, repo) = vm()
        vm.onEvent(EditPlantEvent.SetNickname("   "))
        vm.onEvent(EditPlantEvent.Save)

        val s = vm.state.value as EditPlantUiState.Ready
        assertNotNull(s.nicknameError)
        assertTrue(repo.upsertedPlants.isEmpty())
    }

    @Test
    fun `Save with valid changes upserts once preserving acquiredAt and species`() = runTest {
        val (vm, repo) = vm()
        vm.onEvent(EditPlantEvent.SetNickname("  Mona Lisa  "))
        vm.onEvent(EditPlantEvent.SetNotes("repotted in spring"))
        vm.onEvent(EditPlantEvent.SetLocation(LocationTag.OUTDOOR))
        vm.onEvent(EditPlantEvent.SetCoverPhotoUri("content://photo/7"))
        vm.onEvent(EditPlantEvent.Save)

        assertEquals(EditPlantUiState.Saved, vm.state.value)
        assertEquals(1, repo.upsertedPlants.size)
        val saved = repo.upsertedPlants.single()
        assertEquals("pl-1", saved.id)
        assertEquals("Mona Lisa", saved.nickname)
        assertEquals("repotted in spring", saved.notes)
        assertEquals(LocationTag.OUTDOOR, saved.locationTag)
        assertEquals("content://photo/7", saved.coverPhotoUri)
        assertEquals("sp-monstera", saved.speciesId)
        assertEquals(acquired, saved.acquiredAt)
        assertFalse(saved.archived)
    }

    @Test
    fun `Archive calls archivePlant with true and transitions to Saved`() = runTest {
        val (vm, repo) = vm()
        vm.onEvent(EditPlantEvent.Archive)

        assertEquals(listOf("pl-1" to true), repo.archivedCalls)
        assertEquals(EditPlantUiState.Saved, vm.state.value)
    }

    @Test
    fun `UnarchiveLast after Archive calls archivePlant with false`() = runTest {
        val (vm, repo) = vm()
        vm.onEvent(EditPlantEvent.Archive)
        vm.onEvent(EditPlantEvent.UnarchiveLast)

        assertEquals(
            listOf("pl-1" to true, "pl-1" to false),
            repo.archivedCalls,
        )
    }
}
