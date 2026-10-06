"""Searchable desktop assistant workflow templates."""

from __future__ import annotations

from dataclasses import dataclass
import re


@dataclass(frozen=True)
class DesktopSkill:
    id: str
    category: str
    title: str
    prompt: str


_LANGUAGES = (
    "Python", "Kotlin", "Java", "TypeScript", "JavaScript",
    "C#", "C++", "Rust", "Go", "Swift",
    "Dart", "Ruby", "PHP", "SQL", "Bash",
    "PowerShell", "Lua", "R", "Scala", "HTML and CSS",
)
_CODE_TASKS = (
    ("Build", "Build a minimal, idiomatic implementation. Clarify requirements, show the code, and explain how to run it."),
    ("Debug", "Debug the supplied code from its exact error and reproduction steps. Explain the root cause, then give the smallest correction and tests to run; never claim unrun tests passed."),
    ("Review", "Review the supplied code for correctness, edge cases, and security. Rank actionable findings and cite the affected functions or lines."),
    ("Test", "Create focused tests for normal cases, boundaries, and failure paths. Use the language's standard test conventions and show how to run them."),
    ("Optimize", "Optimize the supplied code against a stated performance or memory goal. Identify the bottleneck first and explain trade-offs; never claim unmeasured speedups."),
)
_WORKFLOW_GROUPS = (
    (
        "Gaming & performance",
        ("Unity games", "Unreal Engine games", "Godot games", "PC games", "Android games"),
        (
            "Profile frame-time symptoms and propose a measurement-first graphics and CPU optimization plan.",
            "Diagnose stutter, shader compilation, asset streaming, or loading delays from the user's logs and hardware details.",
            "Tune resolution, frame pacing, graphics quality, and thermal limits for the user's stated device and target frame rate.",
            "Review game code or configuration for rendering, memory, networking, and battery bottlenecks without inventing benchmark results.",
            "Build a reproducible before-and-after test checklist and safe rollback plan for performance changes.",
        ),
    ),
    (
        "Apps & system",
        ("Windows background processes", "Android apps", "desktop programs", "development environments", "startup services"),
        (
            "Interpret user-provided resource usage and logs; separate likely bottlenecks from guesses.",
            "Create a low-risk optimization plan that preserves required services and explains how to undo every change.",
            "Prioritize startup and background-work candidates using evidence, impact, and dependency risk.",
            "Diagnose CPU, memory, disk, GPU, network, and thermal symptoms with platform-appropriate tools.",
            "Write a maintenance checklist with measurable baselines and safe stop conditions; never terminate or disable a process without explicit confirmation.",
        ),
    ),
    (
        "Files & organization",
        ("Downloads folders", "source-code repositories", "photo libraries", "project workspaces", "archive collections"),
        (
            "Design a preview-first sorting plan based on file type, date, project, and duplicate checks.",
            "Propose a clear naming and folder convention that preserves original names and makes rollback possible.",
            "Identify likely duplicates or stale files from a supplied listing; do not infer file contents from names alone.",
            "Create a cleanup shortlist and backup checklist; never delete, overwrite, or move files without explicit approval.",
            "Organize a mixed file listing into a table of proposed destination folders and explain uncertain classifications.",
        ),
    ),
    (
        "Learning & productivity",
        ("learning a technical topic", "planning a software project", "writing clear documentation", "reviewing a design", "solving a practical problem"),
        (
            "Ask only essential clarifying questions, then produce a concrete step-by-step plan with risks and success criteria.",
            "Explain the topic at beginner, intermediate, and advanced levels, then add a short self-check.",
            "Turn the user's notes into an organized draft while preserving facts and marking assumptions.",
            "Compare practical options in a concise decision table with costs, benefits, risks, and a recommendation.",
            "Create an actionable checklist, identify dependencies, and suggest the smallest useful next step.",
        ),
    ),
)


def _slug(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def build_skill_catalog() -> tuple[DesktopSkill, ...]:
    skills = [
        DesktopSkill(
            id=f"code-{language_index}-{_slug(language)}-{task_index}",
            category="Coding",
            title=f"{language}: {name}",
            prompt=(
                f"{description} Use {language} idiomatically. State assumptions, avoid "
                "fabricating libraries or benchmark results, and ask before destructive changes."
            ),
        )
        for language_index, language in enumerate(_LANGUAGES)
        for task_index, (name, description) in enumerate(_CODE_TASKS, start=1)
    ]

    task_names = ("Analyze", "Diagnose", "Plan", "Optimize", "Verify")
    for category, targets, workflows in _WORKFLOW_GROUPS:
        for target in targets:
            for task_index, (task_name, workflow) in enumerate(zip(task_names, workflows), start=1):
                skills.append(
                    DesktopSkill(
                        id=f"{_slug(category)}-{_slug(target)}-{task_index}",
                        category=category,
                        title=f"{target}: {task_name}",
                        prompt=(
                            f"Help with {target}. {workflow} Ask for relevant device or project "
                            "details when needed. Explain uncertainty and keep user approval and "
                            "reversibility explicit."
                        ),
                    )
                )
    return tuple(skills)


SKILLS = build_skill_catalog()
SKILL_CATEGORIES = tuple(dict.fromkeys(skill.category for skill in SKILLS))
