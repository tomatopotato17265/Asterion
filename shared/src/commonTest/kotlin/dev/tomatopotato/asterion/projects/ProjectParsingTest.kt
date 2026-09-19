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
              "updated": "2026-09-01T12:00:00.000000Z"
            }]
        """.trimIndent()

        val project = AsterionJson.decodeFromString<List<ModrinthProject>>(json).single()

        assertEquals("AANobbMI", project.id)
        assertEquals("Sodium", project.title)
        assertEquals("The fastest rendering optimization mod", project.description)
        assertEquals("https://cdn.modrinth.com/data/AANobbMI/icon.png", project.iconUrl)
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
