from core.desktop_skill_catalog import SKILL_CATEGORIES, SKILLS
from core.speech_presets import SPEECH_PRESETS, edge_tts_style


def test_catalog_has_200_distinct_categorized_workflows():
    assert len(SKILLS) == 200
    assert len({skill.id for skill in SKILLS}) == 200
    assert set(SKILL_CATEGORIES) == {
        "Coding",
        "Gaming & performance",
        "Apps & system",
        "Files & organization",
        "Learning & productivity",
    }
    assert all(skill.title and skill.prompt for skill in SKILLS)


def test_voice_presets_provide_valid_edge_tts_styles():
    assert len(SPEECH_PRESETS) == 4
    assert edge_tts_style("STUDIO") == ("+0%", "+0Hz")
    assert edge_tts_style("CALM") == ("-12%", "-2Hz")
    assert edge_tts_style("BRIGHT") == ("+6%", "+2Hz")
    assert edge_tts_style("CINEMATIC") == ("-8%", "-4Hz")
    assert edge_tts_style("unknown") == edge_tts_style("STUDIO")
