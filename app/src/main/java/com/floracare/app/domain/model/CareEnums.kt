package com.floracare.app.domain.model

enum class CareTaskType { WATER, FERTILIZE, MIST, ROTATE, REPOT, PRUNE }

enum class CareTaskSource { ADAPTIVE, MANUAL }

enum class LightNeed { LOW, MEDIUM, HIGH, DIRECT_SUN }

enum class HumidityNeed { LOW, MEDIUM, HIGH }

enum class Toxicity { NONE, MILD, TOXIC }

enum class LocationTag { INDOOR, OUTDOOR, GREENHOUSE }

enum class SoilMoistureNote { DRY, MOIST, DAMP }
