"""Desktop speech styles shared by UI selection and the Edge TTS playback path."""

SPEECH_PRESETS = {
    "STUDIO": ("Studio", "+0%", "+0Hz"),
    "CALM": ("Calm", "-12%", "-2Hz"),
    "BRIGHT": ("Bright", "+6%", "+2Hz"),
    "CINEMATIC": ("Cinematic", "-8%", "-4Hz"),
}


def edge_tts_style(preset: str) -> tuple[str, str]:
    selected = SPEECH_PRESETS.get(preset, SPEECH_PRESETS["STUDIO"])
    return selected[1], selected[2]
