package com.brahma.connect.core

enum class VisualTheme(
    val title: String,
    val primary: Long,
    val accent: Long,
    val background: Long,
    val surface: Long,
) {
    CYBER_CYAN("Cyber Cyan", 0xFFFFFFFF, 0xFF00E5FF, 0xFF020305, 0xFF0B0D12),
    PLASMA_VIOLET("Plasma Violet", 0xFFFFFFFF, 0xFFB388FF, 0xFF090510, 0xFF150D20),
    SOLAR_AMBER("Solar Ember", 0xFFFFF3E0, 0xFFFFAB40, 0xFF100A05, 0xFF1B120A),
    MATRIX_GREEN("Matrix Green", 0xFFE8F5E9, 0xFF69F0AE, 0xFF030A07, 0xFF0A1510),
    GLACIER_BLUE("Glacier Blue", 0xFFEAF7FF, 0xFF80D8FF, 0xFF041018, 0xFF0A1A24),
    CRIMSON_CORE("Crimson Core", 0xFFFFEBEE, 0xFFFF5252, 0xFF100507, 0xFF1A0B0E),
}

enum class StartupEffect(val title: String) {
    ORBITAL_IGNITION("Orbital ignition"),
    SINGULARITY("Singularity"),
    NEURAL_PULSE("Neural pulse"),
    QUANTUM_GATE("Quantum gate"),
}

enum class VoicePreset(
    val title: String,
    val pitch: Float,
    val rate: Float,
) {
    STUDIO("Studio", 1.0f, 1.0f),
    CALM("Calm", 0.9f, 0.88f),
    BRIGHT("Bright", 1.12f, 1.04f),
    CINEMATIC("Cinematic", 0.82f, 0.94f),
}
