# R8 rules for release (#105). Only rules proven necessary; no package-wide keeps.
#
# Needed none so far (verified by scripts/e2e-demo.sh on the R8 build, #105):
# - Pleos navi-data AAR ships consumer rules keeping ai.pleos.playground.navi.data/constants (kotlinx.serialization
#   targets of NaviHelper). NaviHelper-3.0.5.aar has no proguard.txt, but the app only calls its public API.
# - feature/drive Movement.kt reflects only platform classes (android.car.*, CarUxRestrictions); R8 never renames them.
# - R8 reported no missing classes (build/outputs/mapping/release/missing_rules.txt absent), so no -dontwarn.
