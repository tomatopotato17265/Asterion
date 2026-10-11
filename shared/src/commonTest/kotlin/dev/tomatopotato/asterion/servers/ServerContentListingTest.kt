package dev.tomatopotato.asterion.servers

import kotlin.test.Test
import kotlin.test.assertEquals

class ServerContentListingTest {
    private val addons = listOf(
        Addon(filename = "sodium-0.6.jar", kind = "mod", name = "Sodium"),
        Addon(filename = "lithium-0.14.jar", kind = "mod", name = "lithium"),
        Addon(filename = "EssentialsX.jar", kind = "plugin"),
        Addon(filename = "terralith.zip", kind = "datapack", name = "Terralith"),
    )

    @Test
    fun listsDistinctKindsInOrder() {
        assertEquals(listOf("datapack", "mod", "plugin"), ServerContentListing.kinds(addons))
    }

    @Test
    fun sortsByNameIgnoringCase() {
        assertEquals(
            listOf("EssentialsX.jar", "lithium", "Sodium", "Terralith"),
            ServerContentListing.filter(addons, null, "").map { it.displayName },
        )
    }

    @Test
    fun filtersByKindAndSearchesNameAndFilename() {
        assertEquals(listOf("lithium", "Sodium"), ServerContentListing.filter(addons, "mod", "").map { it.displayName })
        assertEquals(listOf("Sodium"), ServerContentListing.filter(addons, null, " SOD ").map { it.displayName })
        assertEquals(listOf("Terralith"), ServerContentListing.filter(addons, null, ".zip").map { it.displayName })
        assertEquals(emptyList(), ServerContentListing.filter(addons, "plugin", "sodium"))
    }
}
