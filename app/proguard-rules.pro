# R8 full mode is the default since AGP 8.0 (android.enableR8.fullMode is not disabled).
# Library keep rules come from their consumer-rules; add app-specific rules here.

# Glance draws widgets in a WorkManager job. WorkManager creates its input merger by reflection,
# and R8 (AGP 9.1) drops OverwritingInputMerger's no-argument constructor, so the job never runs
# and every widget stays on its loading screen. Keep it.
-keep class * extends androidx.work.InputMerger { public <init>(); }
