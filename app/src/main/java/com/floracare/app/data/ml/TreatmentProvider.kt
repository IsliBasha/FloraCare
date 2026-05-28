package com.floracare.app.data.ml

/**
 * Maps a disease label returned by [DiseaseClassifier] to a plain-English
 * treatment suggestion. Uses keyword regex so suggestions survive minor wording
 * differences between model versions. Falls back to a generic message for
 * unrecognised labels.
 *
 * Exposed as an `object` — pure, no DI required.
 */
object TreatmentProvider {

    private val rules: List<Pair<Regex, String>> = listOf(
        Regex("healthy", RegexOption.IGNORE_CASE) to
            "Your plant looks healthy! Keep up the current care routine.",
        Regex("leaf.?spot|cercospora|alternaria|septoria", RegexOption.IGNORE_CASE) to
            "Remove affected leaves. Improve air circulation and avoid wetting foliage. " +
                "Apply a copper-based fungicide if the infection is spreading.",
        Regex("fungal|blight|rust|mildew|mold|rot", RegexOption.IGNORE_CASE) to
            "Remove and dispose of infected tissue. Reduce humidity and improve airflow. " +
                "Treat with a systemic fungicide as needed.",
        Regex("bacterial|fire blight|canker|soft rot", RegexOption.IGNORE_CASE) to
            "Prune infected parts with sterilised tools. Avoid overhead watering. " +
                "Apply a copper-based bactericide for severe cases.",
        Regex("nutrient|deficiency|chlorosis|yellowing", RegexOption.IGNORE_CASE) to
            "Apply a balanced slow-release fertiliser. Check soil pH — most houseplants " +
                "prefer 6.0–7.0. Consider repotting if the potting mix is exhausted.",
        Regex("pest|aphid|mite|scale|mealybug|whitefly|thrip", RegexOption.IGNORE_CASE) to
            "Wipe leaves with neem oil solution or insecticidal soap. Isolate the plant " +
                "to prevent spread. Repeat every 5–7 days until clear.",
        Regex("overwater|root rot|soggy|droopy", RegexOption.IGNORE_CASE) to
            "Allow soil to dry out completely before the next watering. Remove any mushy " +
                "roots and repot in fresh well-draining mix.",
        Regex("underwater|drought|wilt|dry", RegexOption.IGNORE_CASE) to
            "Water thoroughly until it drains from the bottom. Check that pot drainage holes " +
                "are clear. Mist if ambient humidity is very low.",
        Regex("sunburn|scorch|bleach", RegexOption.IGNORE_CASE) to
            "Move the plant away from direct harsh sunlight. Damaged leaves will not recover " +
                "but healthy new growth will emerge.",
    )

    private const val FALLBACK =
        "Take a clearer photo for a better result, or consult a plant specialist " +
            "for an accurate diagnosis and treatment plan."

    fun suggest(label: String): String =
        rules.firstOrNull { (regex, _) -> regex.containsMatchIn(label) }?.second ?: FALLBACK
}
