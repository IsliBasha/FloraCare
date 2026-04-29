# Product

## Register

product

## Users

Today: a single user — the author — using FloraCare to keep track of his own houseplants on a personal Android phone (Samsung SM-S938B, Android 16). The app is also actively presented as an academic project in the Polis University · Fakulteti Kërkim Zhvillim · App Programming Project context, so secondary "users" right now are mentors and demo-day reviewers reading the About panel.

Aspirational: indoor-plant hobbyists with roughly 3–30 plants who forget watering schedules, can't always remember species names, and want a quiet place to log photos and notice trouble. They are not professional gardeners and not crisis-driven shoppers — they are people who like their plants and dislike app feeds.

Context of use: short, intentional sessions on a phone, usually triggered by a daily reminder at 07:00 or by curiosity about a specific plant. Not a daily-engagement product. The app is open to do one thing, then closed.

## Product Purpose

FloraCare is a personal houseplant catalog with adaptive care reminders and (eventually) leaf-level diagnosis. It exists to answer two recurring questions for a hobbyist: *"which plant is this and what does it need?"* and *"is this one in trouble?"*

Success looks like: every plant the user owns is in the catalog with a real photo and a known species; daily reminders fire at the right time and adjust to local weather; when something is wrong, the user can point a camera at a leaf and get a useful answer. Failure looks like: another plant-app feed of tips, ads, and engagement loops the user has to dismiss.

Scope realism: this is an unfinished, single-developer, personal Android project. Several surfaces in the code are intentionally provisional. Critiques and design work should treat the following as in-progress rather than as trust-breaking gaps:

- **Diagnose screen** is currently a `PlaceholderScreen`. The disease-classifier feature is planned but not built. Onboarding copy and PlantDetail buttons reference it deliberately so the slot exists.
- **Journal screen** is also a `PlaceholderScreen`. A photo-timeline V0 is planned.
- **Weather adaptation reasons** ("delayed by recent rain") are computed but not yet surfaced on PlantDetail — this is the active next-session priority #1.
- **Widget mark-done / snooze actions** are not in the widget yet — the widget renders rows and deep-links only.
- **Restore-archived screen** is not built; archive uses an in-screen Snackbar Undo for now.
- **Locale toggle** (Albanian / English) is not built.

When in doubt, consult `~/.claude/projects/-home-lugat/memory/floracare_progress.md` and `floracare_scope.md` — those memories are the source of truth on what is and is not shipped.

## Brand Personality

Calm, herbarium, editorial. Three words that should describe every surface.

Voice: confident, low-frequency, reassuring without being precious. Talks like a careful field journal entry, not a marketing email. No exclamation points. No emoji in copy. Imperatives are gentle ("Pick a starting point") rather than urgent ("Get started now!"). Implementation details (cron times, fallback paths, classifier confidence percentages) do not appear in user-facing copy.

Emotional goal: the user opens FloraCare and feels the same thing as opening a hardback gardening book on a quiet Sunday — slow, considered, attentive. The app should not feel like it wants something from the user.

## Anti-references

- Plantix and similar consumer plant-care apps with ad-heavy feeds, push-engagement, daily streaks framed as guilt.
- Generic Material 3 demos: stacked cards with identical shapes/elevations, hero-metric layouts, gradient accents on every surface.
- "Wellness app" aesthetic: pastel gradients, oversized rounded corners, decorative blobs.
- Default shadcn / template SaaS dashboard look — uniform card grids with icon + heading + helper text repeated endlessly.
- Anything that screams "AI-generated indie app" — neon-on-dark, gradient text, glassmorphism, decorative motion.
- Engagement-loop UX: streak guilt copy, modal upsells, "you haven't watered in 3 days!" red badges. Reminders are a service, not a hook.

## Design Principles

1. **Quiet by default, editorial when it matters.** The list is text-and-photo. The detail view earns its display-serif heading and full-bleed hero. Most surfaces should feel like a notebook page; one surface per flow can feel like a magazine spread.
2. **Photos are the catalog.** Plant cover photos and species images are the primary visual content, not decoration. Anywhere a photo could be shown, it should be — and where photos are absent, the absence should feel like white space, not a flat colored block standing in for one.
3. **Plant-first, not metric-first.** Streak counts, water totals, and confidence percentages serve plants, not the reverse. Numbers earn their place by being actionable; otherwise they get cut or demoted to body text.
4. **Adaptation is shown, not implied.** When the care engine adjusts a schedule (rain delay, heat acceleration), the user must see why on the surface that owns that schedule. Hiding the work makes the app feel arbitrary.
5. **Match implementation honesty.** This is an unfinished personal project. Provisional surfaces (Diagnose placeholder, missing widget actions) should be honest about their state rather than pretending to be finished — but the polished surfaces (PlantDetail, Identify, EditPlant) should not be dragged down by them.

## Accessibility & Inclusion

Target: WCAG 2.1 AA on the surfaces that ship as finished. Specifically:

- Color contrast ≥ 4.5:1 for body text and ≥ 3:1 for large display type and UI components in both light and dark schemes. The current ForestDeep-on-PaperCream and PaperCream-on-CharcoalDark pairings should be measured before each surface ships.
- TalkBack pass: every actionable element has a `contentDescription`; the decorative pill on `PlantCard` and similar invented affordances are either removed or annotated.
- Touch targets ≥ 48dp.
- Reduced-motion respect: animated content (Onboarding `AnimatedContent` fade) checks the system setting; nothing critical is gated on motion.

No specific accommodations beyond the WCAG 2.1 AA baseline are currently required. If the project moves toward real users in the future, color-blind palette validation (the terracotta/forest pairing) and large-text reflow on the Dashboard sparkline are the most likely next concerns.
