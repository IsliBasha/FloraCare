package com.floracare.app.data.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HouseplantAliasMapTest {

    // Verbatim 47 labels from the houseplant ViT model's config.json — do not
    // "clean up" this list, it must match the real model output exactly.
    private val allVitLabels = listOf(
        "African Violet (Saintpaulia ionantha)",
        "Aloe Vera",
        "Anthurium (Anthurium andraeanum)",
        "Areca Palm (Dypsis lutescens)",
        "Asparagus Fern (Asparagus setaceus)",
        "Begonia (Begonia spp.)",
        "Bird of Paradise (Strelitzia reginae)",
        "Birds Nest Fern (Asplenium nidus)",
        "Boston Fern (Nephrolepis exaltata)",
        "Calathea",
        "Cast Iron Plant (Aspidistra elatior)",
        "Chinese Money Plant (Pilea peperomioides)",
        "Chinese evergreen (Aglaonema)",
        "Christmas Cactus (Schlumbergera bridgesii)",
        "Chrysanthemum",
        "Ctenanthe",
        "Daffodils (Narcissus spp.)",
        "Dracaena",
        "Dumb Cane (Dieffenbachia spp.)",
        "Elephant Ear (Alocasia spp.)",
        "English Ivy (Hedera helix)",
        "Hyacinth (Hyacinthus orientalis)",
        "Iron Cross begonia (Begonia masoniana)",
        "Jade plant (Crassula ovata)",
        "Kalanchoe",
        "Lilium (Hemerocallis)",
        "Lily of the valley (Convallaria majalis)",
        "Money Tree (Pachira aquatica)",
        "Monstera Deliciosa (Monstera deliciosa)",
        "Orchid",
        "Parlor Palm (Chamaedorea elegans)",
        "Peace lily",
        "Poinsettia (Euphorbia pulcherrima)",
        "Polka Dot Plant (Hypoestes phyllostachya)",
        "Ponytail Palm (Beaucarnea recurvata)",
        "Pothos (Ivy arum)",
        "Prayer Plant (Maranta leuconeura)",
        "Rattlesnake Plant (Calathea lancifolia)",
        "Rubber Plant (Ficus elastica)",
        "Sago Palm (Cycas revoluta)",
        "Schefflera",
        "Snake plant (Sanseviera)",
        "Tradescantia",
        "Tulip",
        "Venus Flytrap",
        "Yucca",
        "ZZ Plant (Zamioculcas zamiifolia)",
    )

    @Test
    fun `every one of the 47 real model labels resolves to a non-null scientific name`() {
        allVitLabels.forEach { label ->
            assertNotNull(
                "expected a mapping for label: $label",
                HouseplantAliasMap.scientificNameFor(label),
            )
        }
    }

    @Test
    fun `there are exactly 47 known labels`() {
        assertEquals(47, allVitLabels.size)
        assertEquals(47, allVitLabels.toSet().size)
    }

    @Test
    fun `unknown label returns null`() {
        assertNull(HouseplantAliasMap.scientificNameFor("Not A Real Plant Label"))
    }

    @Test
    fun `blank label returns null`() {
        assertNull(HouseplantAliasMap.scientificNameFor(""))
    }

    @Test
    fun `extracts scientific name already present in parentheses`() {
        assertEquals(
            "Anthurium andraeanum",
            HouseplantAliasMap.scientificNameFor("Anthurium (Anthurium andraeanum)"),
        )
        assertEquals(
            "Monstera deliciosa",
            HouseplantAliasMap.scientificNameFor("Monstera Deliciosa (Monstera deliciosa)"),
        )
        assertEquals(
            "Strelitzia reginae",
            HouseplantAliasMap.scientificNameFor("Bird of Paradise (Strelitzia reginae)"),
        )
    }

    @Test
    fun `corrects the Sanseviera misspelling to the real genus Sansevieria`() {
        assertEquals(
            "Sansevieria",
            HouseplantAliasMap.scientificNameFor("Snake plant (Sanseviera)"),
        )
    }

    @Test
    fun `Pothos parenthetical is a common name not a scientific one, mapped to the real binomial`() {
        // "(Ivy arum)" is another common name, not Latin — must not be
        // propagated as though it were the scientific name.
        assertEquals(
            "Epipremnum aureum",
            HouseplantAliasMap.scientificNameFor("Pothos (Ivy arum)"),
        )
    }

    @Test
    fun `single-species genus common names map to the specific binomial`() {
        // Aloe vera is the unambiguous, overwhelmingly common houseplant Aloe.
        assertEquals("Aloe vera", HouseplantAliasMap.scientificNameFor("Aloe Vera"))
        // Dionaea is a monotypic genus — Venus Flytrap can only mean this species.
        assertEquals("Dionaea muscipula", HouseplantAliasMap.scientificNameFor("Venus Flytrap"))
    }

    @Test
    fun `genus-only mappings return the bare genus, not a fabricated species`() {
        assertEquals("Calathea", HouseplantAliasMap.scientificNameFor("Calathea"))
        assertEquals("Dracaena", HouseplantAliasMap.scientificNameFor("Dracaena"))
        assertEquals("Schefflera", HouseplantAliasMap.scientificNameFor("Schefflera"))
        assertEquals("Tradescantia", HouseplantAliasMap.scientificNameFor("Tradescantia"))
        assertEquals("Yucca", HouseplantAliasMap.scientificNameFor("Yucca"))
        assertEquals("Kalanchoe", HouseplantAliasMap.scientificNameFor("Kalanchoe"))
        assertEquals("Chrysanthemum", HouseplantAliasMap.scientificNameFor("Chrysanthemum"))
        assertEquals("Ctenanthe", HouseplantAliasMap.scientificNameFor("Ctenanthe"))
    }

    @Test
    fun `common names that differ from their scientific genus map to the real genus or family`() {
        // "Orchid" spans an entire family, not a single genus.
        assertEquals("Orchidaceae", HouseplantAliasMap.scientificNameFor("Orchid"))
        // "Peace lily" is the common name for genus Spathiphyllum.
        assertEquals("Spathiphyllum", HouseplantAliasMap.scientificNameFor("Peace lily"))
        // "Tulip" is the common name for genus Tulipa.
        assertEquals("Tulipa", HouseplantAliasMap.scientificNameFor("Tulip"))
    }

    @Test
    fun `spp suffix is stripped down to the bare genus`() {
        assertEquals("Begonia", HouseplantAliasMap.scientificNameFor("Begonia (Begonia spp.)"))
        assertEquals("Narcissus", HouseplantAliasMap.scientificNameFor("Daffodils (Narcissus spp.)"))
        assertEquals("Dieffenbachia", HouseplantAliasMap.scientificNameFor("Dumb Cane (Dieffenbachia spp.)"))
        assertEquals("Alocasia", HouseplantAliasMap.scientificNameFor("Elephant Ear (Alocasia spp.)"))
    }
}
