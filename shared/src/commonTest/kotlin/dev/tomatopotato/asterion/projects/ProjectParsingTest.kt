package dev.tomatopotato.asterion.projects

import dev.tomatopotato.asterion.net.AsterionJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectParsingTest {

    @Test
    fun parsesProjectListResponse() {
        val json = """
            [{
              "id": "AANobbMI",
              "slug": "sodium",
              "project_type": "mod",
              "title": "Sodium",
              "description": "The fastest rendering optimization mod",
              "icon_url": "https://cdn.modrinth.com/data/AANobbMI/icon.png",
              "downloads": 123,
              "followers": 45,
              "status": "approved",
              "published": "2025-01-01T00:00:00.000000Z",
              "updated": "2026-09-01T12:00:00.000000Z"
            }]
        """.trimIndent()

        val project = AsterionJson.decodeFromString<List<ModrinthProject>>(json).single()

        assertEquals("AANobbMI", project.id)
        assertEquals("Sodium", project.title)
        assertEquals("The fastest rendering optimization mod", project.description)
        assertEquals("https://cdn.modrinth.com/data/AANobbMI/icon.png", project.iconUrl)
        assertEquals("mod", project.projectType)
        assertEquals(123L, project.downloads)
        assertEquals(45L, project.followers)
        assertEquals("approved", project.status)
    }

    @Test
    fun presentsStatusAndTypeAsLabels() {
        val project = ModrinthProject(id = "p", title = "P", status = "under_review", projectType = "resourcepack")

        assertEquals("Under review", project.statusLabel)
        assertEquals("Resourcepack", project.projectTypeLabel)
    }

    @Test
    fun mapsEveryStatusToItsTone() {
        fun tone(status: String) = ModrinthProject(id = "p", title = "P", status = status).statusTone

        listOf("approved", "unlisted", "scheduled").forEach { assertEquals(ProjectStatusTone.Positive, tone(it), it) }
        listOf("archived", "private", "processing").forEach { assertEquals(ProjectStatusTone.Warning, tone(it), it) }
        listOf("rejected", "withheld").forEach { assertEquals(ProjectStatusTone.Negative, tone(it), it) }
        listOf("draft", "unknown", "", "something-new").forEach { assertEquals(ProjectStatusTone.Neutral, tone(it), it) }
    }

    @Test
    fun buildsWebUrlOnlyWhenSlugAndTypeArePresent() {
        assertEquals(
            "https://modrinth.com/mod/sodium",
            ModrinthProject(id = "p", title = "P", slug = "sodium", projectType = "mod").webUrl,
        )
        assertNull(ModrinthProject(id = "p", title = "P", slug = "sodium").webUrl)
        assertNull(ModrinthProject(id = "p", title = "P", projectType = "mod").webUrl)
    }

    @Test
    fun projectWithoutIconOrDescriptionStillParses() {
        val project = AsterionJson.decodeFromString<ModrinthProject>(
            """{"id":"p","title":"Bare","icon_url":null}""",
        )

        assertNull(project.iconUrl)
        assertEquals("", project.description)
    }

    @Test
    fun sortsMostRecentlyUpdatedFirst() {
        val projects = listOf(
            ModrinthProject(id = "old", title = "Old", updated = "2025-01-01T00:00:00Z"),
            ModrinthProject(id = "new", title = "New", updated = "2026-09-01T00:00:00Z"),
            ModrinthProject(id = "mid", title = "Mid", updated = "2026-01-01T00:00:00Z"),
        )

        assertEquals(listOf("new", "mid", "old"), projects.sortedByRecentlyUpdated().map { it.id })
    }
}
