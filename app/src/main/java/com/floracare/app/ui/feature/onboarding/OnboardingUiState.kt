package com.floracare.app.ui.feature.onboarding

/**
 * Three-step onboarding flow. `Ready` is a brief terminal screen shown after
 * the permission pane; the user advances from it into the plant list, at
 * which point the VM persists the completion flag.
 */
sealed interface OnboardingStep {
    data object Welcome : OnboardingStep
    data object Permissions : OnboardingStep
    data object Ready : OnboardingStep
}

/**
 * Single immutable snapshot of the onboarding UI. Permission fields remain
 * observable even after the step advances so a user who denies then returns
 * can see that state reflected in the Permissions pane.
 */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val notificationsGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val notificationsAsked: Boolean = false,
    val cameraAsked: Boolean = false,
    val locationAsked: Boolean = false,
    /** `true` once the user finishes onboarding and the completion flag has been persisted. */
    val finished: Boolean = false,
)

enum class OnboardingPermission {
    Notifications, Camera, Location
}
