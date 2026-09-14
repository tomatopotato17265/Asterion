package dev.tomatopotato.asterion.servers

import dev.tomatopotato.asterion.net.AsterionJson
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentDecodingTest {
    @Test
    fun nullAddonsList() {
        val r = AsterionJson.decodeFromString<AddonsResponse>(
            """{"modloader":"fabric","modloader_version":null,"game_version":"1.21.11","modpack":null,"addons":null}""",
        )
        assertEquals(0, r.addons.size)
    }

    @Test
    fun addonWithNullableScalarsAndNestedObjects() {
        val r = AsterionJson.decodeFromString<AddonsResponse>(
            """{"addons":[{"id":"a","filename":"x.jar","filesize":12,"btime":"2026-01-01T00:00:00Z",
               "disabled":false,"kind":"mod","from_modpack":false,"status":"installed",
               "pack_client_retained":false,"pack_client_depends":false,"has_update":null,
               "name":null,"project_id":null,"version":{"id":"v","name":null},"owner":null,"icon_url":null}]}""",
        )
        assertEquals("x.jar", r.addons.single().displayName)
    }

    @Test
    fun addonWithoutIdUsesKindAndFilenameAsKey() {
        val r = AsterionJson.decodeFromString<AddonsResponse>(
            """{"addons":[
                {"filename":"sodium.jar","kind":"mod","disabled":false},
                {"filename":"sodium.jar","kind":"datapack","disabled":true}
            ]}""",
        )
        assertEquals(listOf("mod:sodium.jar", "datapack:sodium.jar"), r.addons.map { it.key })
        assertEquals(null, r.addons.first().id)
    }

    @Test
    fun serverFullWithNullContentAndExtraFields() {
        val r = AsterionJson.decodeFromString<ServerFull>(
            """{"id":"s","name":"n","subdomain":"tomatopotato","specs":{"cpu":2,"memory_mb":4096,"storage_mb":1,"swap_mb":0},
               "sftp_username":"u","sftp_password":"p","tags":[],"location":{"status":"unassigned"},
               "worlds":[{"id":"w","name":"World","created_at":"x","is_active":true,
                 "download_method":{"method_type":"direct_node_download"},"backups":[],"content":null,
                 "readiness":{}}]}""",
        )
        assertEquals("w", r.activeWorld?.id)
        assertEquals(2.0, r.specs?.cpu)
        assertEquals(4096L, r.specs?.memoryMb)
        assertEquals(1L, r.specs?.storageMb)
    }
}
