package com.floracare.app.data.ml

/**
 * Crosswalk from the houseplant ViT model's 47 output labels (see
 * `assets/ml/houseplant_vit_labels.txt`, sourced verbatim from the model's own
 * `config.json`) to scientific names, because
 * [com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase] resolves and
 * persists species by scientific name, not by whatever label a classifier
 * emits.
 *
 * Most source labels already carry a parenthesized scientific name — that
 * value is extracted, with two kinds of correction applied against real
 * botanical nomenclature rather than trusting the source label spelling:
 *  - Spelling/genus fixes, e.g. the source label "Sanseviera" is a common
 *    misspelling of the real genus `Sansevieria`.
 *  - Labels whose parenthetical is itself just another common name rather
 *    than a real scientific name, e.g. "Pothos (Ivy arum)" — "Ivy arum" is
 *    not Latin, so it is mapped to the real binomial `Epipremnum aureum`
 *    instead of being propagated as though it were scientific.
 *  - "spp." suffixes (e.g. source label "Begonia spp.") are stripped down to
 *    the bare genus — "spp." means "unspecified species in this genus", not
 *    a real binomial, so keeping it would fabricate false precision.
 *
 * A second group of labels is genus-only or common-name-only with no species
 * given by the source model at all. Where the genus/common name has one
 * overwhelmingly common houseplant identity, that specific binomial is used
 * (`Aloe Vera` -> `Aloe vera`; `Venus Flytrap` -> `Dionaea muscipula`, the
 * only species in its genus). Where the genus contains many commonly-kept
 * houseplant species and picking one would be a real guess (`Calathea`,
 * `Dracaena`, `Schefflera`, `Tradescantia`, `Kalanchoe`, `Chrysanthemum`,
 * `Ctenanthe`), or the common name doesn't match a real genus at all
 * (`Orchid` spans the whole family Orchidaceae, not a genus; `Peace lily` is
 * the common name for genus `Spathiphyllum`; `Tulip` is the common name for
 * genus `Tulipa`), the mapping intentionally stops at genus or family level.
 * This is a real, documented limitation of the source model, not something
 * to paper over with a guessed species:
 * [com.floracare.app.domain.usecase.ResolveOrCreateSpeciesUseCase] already
 * has a synth-fallback path for names it can't resolve via Perenual, so a
 * genus- or family-level string degrades gracefully rather than silently
 * asserting false precision.
 */
object HouseplantAliasMap {

    fun scientificNameFor(vitLabel: String): String? = ALIASES[vitLabel]

    private val ALIASES: Map<String, String> = mapOf(
        "African Violet (Saintpaulia ionantha)" to "Saintpaulia ionantha",
        "Aloe Vera" to "Aloe vera",
        "Anthurium (Anthurium andraeanum)" to "Anthurium andraeanum",
        "Areca Palm (Dypsis lutescens)" to "Dypsis lutescens",
        "Asparagus Fern (Asparagus setaceus)" to "Asparagus setaceus",
        "Begonia (Begonia spp.)" to "Begonia",
        "Bird of Paradise (Strelitzia reginae)" to "Strelitzia reginae",
        "Birds Nest Fern (Asplenium nidus)" to "Asplenium nidus",
        "Boston Fern (Nephrolepis exaltata)" to "Nephrolepis exaltata",
        "Calathea" to "Calathea",
        "Cast Iron Plant (Aspidistra elatior)" to "Aspidistra elatior",
        "Chinese Money Plant (Pilea peperomioides)" to "Pilea peperomioides",
        "Chinese evergreen (Aglaonema)" to "Aglaonema",
        "Christmas Cactus (Schlumbergera bridgesii)" to "Schlumbergera bridgesii",
        "Chrysanthemum" to "Chrysanthemum",
        "Ctenanthe" to "Ctenanthe",
        "Daffodils (Narcissus spp.)" to "Narcissus",
        "Dracaena" to "Dracaena",
        "Dumb Cane (Dieffenbachia spp.)" to "Dieffenbachia",
        "Elephant Ear (Alocasia spp.)" to "Alocasia",
        "English Ivy (Hedera helix)" to "Hedera helix",
        "Hyacinth (Hyacinthus orientalis)" to "Hyacinthus orientalis",
        "Iron Cross begonia (Begonia masoniana)" to "Begonia masoniana",
        "Jade plant (Crassula ovata)" to "Crassula ovata",
        "Kalanchoe" to "Kalanchoe",
        "Lilium (Hemerocallis)" to "Hemerocallis",
        "Lily of the valley (Convallaria majalis)" to "Convallaria majalis",
        "Money Tree (Pachira aquatica)" to "Pachira aquatica",
        "Monstera Deliciosa (Monstera deliciosa)" to "Monstera deliciosa",
        "Orchid" to "Orchidaceae",
        "Parlor Palm (Chamaedorea elegans)" to "Chamaedorea elegans",
        "Peace lily" to "Spathiphyllum",
        "Poinsettia (Euphorbia pulcherrima)" to "Euphorbia pulcherrima",
        "Polka Dot Plant (Hypoestes phyllostachya)" to "Hypoestes phyllostachya",
        "Ponytail Palm (Beaucarnea recurvata)" to "Beaucarnea recurvata",
        "Pothos (Ivy arum)" to "Epipremnum aureum",
        "Prayer Plant (Maranta leuconeura)" to "Maranta leuconeura",
        "Rattlesnake Plant (Calathea lancifolia)" to "Calathea lancifolia",
        "Rubber Plant (Ficus elastica)" to "Ficus elastica",
        "Sago Palm (Cycas revoluta)" to "Cycas revoluta",
        "Schefflera" to "Schefflera",
        "Snake plant (Sanseviera)" to "Sansevieria",
        "Tradescantia" to "Tradescantia",
        "Tulip" to "Tulipa",
        "Venus Flytrap" to "Dionaea muscipula",
        "Yucca" to "Yucca",
        "ZZ Plant (Zamioculcas zamiifolia)" to "Zamioculcas zamiifolia",
    )
}
