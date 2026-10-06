package com.brahma.connect.core

data class MobileSkill(
    val id: String,
    val category: String,
    val title: String,
    val prompt: String,
)

object MobileSkillCatalog {
    private val languages = listOf(
        "Python", "Kotlin", "Java", "TypeScript", "JavaScript",
        "C#", "C++", "Rust", "Go", "Swift",
        "Dart", "Ruby", "PHP", "SQL", "Bash",
        "PowerShell", "Lua", "R", "Scala", "HTML and CSS",
    )

    private val codeWorkflows = listOf(
        "Build a minimal, idiomatic implementation. Clarify requirements, show the code, and explain how to run it.",
        "Debug the supplied code from its exact error and reproduction steps. Explain the root cause, then give the smallest correction and tests to run; never claim unrun tests passed.",
        "Review the supplied code for correctness, edge cases, and security. Rank actionable findings and cite the affected functions or lines.",
        "Create focused tests for normal cases, boundaries, and failure paths. Use the language's standard test conventions and show how to run them.",
        "Optimize the supplied code against a stated performance or memory goal. Identify the bottleneck first and explain trade-offs; never claim unmeasured speedups.",
    )

    private val gameTargets = listOf("Unity games", "Unreal Engine games", "Godot games", "PC games", "Android games")
    private val gameWorkflows = listOf(
        "Profile frame-time symptoms and propose a measurement-first graphics and CPU optimization plan.",
        "Diagnose stutter, shader compilation, asset streaming, or loading delays from the user's logs and hardware details.",
        "Tune resolution, frame pacing, graphics quality, and thermal limits for the user's stated device and target frame rate.",
        "Review game code or configuration for rendering, memory, networking, and battery bottlenecks without inventing benchmark results.",
        "Build a reproducible before-and-after test checklist and safe rollback plan for performance changes.",
    )

    private val systemTargets = listOf("Windows background processes", "Android apps", "desktop programs", "development environments", "startup services")
    private val systemWorkflows = listOf(
        "Interpret user-provided resource usage and logs; separate likely bottlenecks from guesses.",
        "Create a low-risk optimization plan that preserves required services and explains how to undo every change.",
        "Prioritize startup and background-work candidates using evidence, impact, and dependency risk.",
        "Diagnose CPU, memory, disk, GPU, network, and thermal symptoms with platform-appropriate tools.",
        "Write a maintenance checklist with measurable baselines and safe stop conditions; never terminate or disable a process without explicit confirmation.",
    )

    private val fileTargets = listOf("Downloads folders", "source-code repositories", "photo libraries", "project workspaces", "archive collections")
    private val fileWorkflows = listOf(
        "Design a preview-first sorting plan based on file type, date, project, and duplicate checks.",
        "Propose a clear naming and folder convention that preserves original names and makes rollback possible.",
        "Identify likely duplicates or stale files from a supplied listing; do not infer file contents from names alone.",
        "Create a cleanup shortlist and backup checklist; never delete, overwrite, or move files without explicit approval.",
        "Organize a mixed file listing into a table of proposed destination folders and explain uncertain classifications.",
    )

    private val productivityTargets = listOf("learning a technical topic", "planning a software project", "writing clear documentation", "reviewing a design", "solving a practical problem")
    private val productivityWorkflows = listOf(
        "Ask only essential clarifying questions, then produce a concrete step-by-step plan with risks and success criteria.",
        "Explain the topic at beginner, intermediate, and advanced levels, then add a short self-check.",
        "Turn the user's notes into an organized draft while preserving facts and marking assumptions.",
        "Compare practical options in a concise decision table with costs, benefits, risks, and a recommendation.",
        "Create an actionable checklist, identify dependencies, and suggest the smallest useful next step.",
    )

    val all: List<MobileSkill> = buildList(200) {
        languages.forEachIndexed { languageIndex, language ->
            codeWorkflows.forEachIndexed { index, workflow ->
                add(
                    MobileSkill(
                        id = "code-$languageIndex-${language.lowercase().replace(Regex("[^a-z0-9]+"), "-")}-${index + 1}",
                        category = "Coding",
                        title = "$language: ${listOf("Build", "Debug", "Review", "Test", "Optimize")[index]}",
                        prompt = "$workflow Use $language idiomatically. State assumptions, avoid fabricating libraries or benchmark results, and ask before destructive changes.",
                    ),
                )
            }
        }
        addMatrix("Gaming & performance", gameTargets, gameWorkflows)
        addMatrix("Apps & system", systemTargets, systemWorkflows)
        addMatrix("Files & organization", fileTargets, fileWorkflows)
        addMatrix("Learning & productivity", productivityTargets, productivityWorkflows)
    }

    fun categories(): List<String> = all.map(MobileSkill::category).distinct()

    private fun MutableList<MobileSkill>.addMatrix(category: String, targets: List<String>, workflows: List<String>) {
        targets.forEach { target ->
            workflows.forEachIndexed { index, workflow ->
                add(
                    MobileSkill(
                        id = "${category.lowercase().replace(Regex("[^a-z0-9]+"), "-")}-${target.lowercase().replace(Regex("[^a-z0-9]+"), "-")}-${index + 1}",
                        category = category,
                        title = "$target: ${listOf("Analyze", "Diagnose", "Plan", "Optimize", "Verify")[index]}",
                        prompt = "Help with $target. $workflow Ask for relevant device or project details when needed. Explain uncertainty and keep user approval and reversibility explicit.",
                    ),
                )
            }
        }
    }
}
