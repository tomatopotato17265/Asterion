package dev.tomatopotato.asterion.servers

import dev.tomatopotato.asterion.net.AsterionJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArchonParsingTest {

    @Test
    fun parsesServerListResponse() {
        val json = """
            {
              "servers": [{
                "server_id": "abc123",
                "name": "Test SMP",
                "owner_id": "user1",
                "net": { "ip": "1.2.3.4", "port": 25565, "domain": "test.modrinth.gg" },
                "game": "Minecraft",
                "status": "available",
                "suspension_reason": null,
                "loader": "Fabric",
                "loader_version": "0.16.9",
                "mc_version": "1.21.4",
                "backup_quota": 10,
                "used_backup_quota": 2,
                "datacenter": "us-east",
                "notices": [],
                "is_medal": false,
                "current_user_permissions": 4095
              }],
              "pagination": { "current_page": 1, "page_size": 20, "total_pages": 1, "total_items": 1 },
              "users": { "user1": { "id": "user1", "username": "tomato" } }
            }
        """.trimIndent()

        val parsed = AsterionJson.decodeFromString<ServerListResponse>(json)
        val server = parsed.servers.single()

        assertEquals("abc123", server.serverId)
        assertEquals(ServerStatus.Available, server.status)
        assertEquals(ServerLoader.Fabric, server.loader)
        assertNull(server.suspensionReason)
        assertEquals("test.modrinth.gg", server.address)
        assertEquals("tomato", parsed.users["user1"]?.username)
    }

    @Test
    fun keepsNonDefaultPortInAddress() {
        val json = """{"server_id":"s","net":{"ip":null,"port":25570,"domain":"a.b"}}"""
        assertEquals("a.b:25570", AsterionJson.decodeFromString<ArchonServer>(json).address)
    }

    @Test
    fun toleratesUnknownFieldsAndMissingOnes() {
        val json = """{"server_id":"s","some_new_field":{"nested":true}}"""
        val server = AsterionJson.decodeFromString<ArchonServer>(json)

        assertEquals("s", server.serverId)
        assertEquals(ServerStatus.Unknown, server.status)
        assertNull(server.loader)
        assertNull(server.address)
    }

    @Test
    fun unrecognisedEnumValuesFallBackInsteadOfThrowing() {
        val json = """{"server_id":"s","status":"hibernating","loader":"Sponge"}"""
        val server = AsterionJson.decodeFromString<ArchonServer>(json)

        assertEquals(ServerStatus.Unknown, server.status)
        assertEquals(ServerLoader.Unknown, server.loader)
    }

    @Test
    fun exposesSftpDetailsFromListResponse() {
        val json = """
            {"server_id":"s","sftp_host":"node-1.modrinth.com:22","sftp_username":"srv_abc",
             "sftp_password":"hunter2"}
        """.trimIndent()
        val server = AsterionJson.decodeFromString<ArchonServer>(json)
        assertEquals("node-1.modrinth.com:22", server.sftpHost)
        assertEquals("sftp://srv_abc@node-1.modrinth.com:22", server.sftpUrl)
    }

    @Test
    fun sftpUrlNullWhenDetailsMissing() {
        assertNull(AsterionJson.decodeFromString<ArchonServer>("""{"server_id":"s"}""").sftpUrl)
    }

    @Test
    fun parsesInstalledContentAndPicksDisplayName() {
        val json = """
            {"modloader":"fabric","modloader_version":"0.16.9","game_version":"1.21.4",
             "addons":[
               {"id":"a1","filename":"sodium-fabric.jar","filesize":900,"disabled":false,
                "kind":"mod","from_modpack":false,"has_update":null,"name":"Sodium","project_id":"AANobbMI"},
               {"id":"a2","filename":"random-thing.jar","filesize":10,"disabled":true,
                "kind":"mod","from_modpack":true,"has_update":"newver","name":null,"project_id":null}
             ]}
        """.trimIndent()
        val parsed = AsterionJson.decodeFromString<AddonsResponse>(json)
        assertEquals(2, parsed.addons.size)
        assertEquals("Sodium", parsed.addons[0].displayName)
        assertEquals("random-thing.jar", parsed.addons[1].displayName)
        assertEquals(true, parsed.addons[1].disabled)
    }

    @Test
    fun parsesServerFullAndFindsActiveWorld() {
        val json = """
            {"id":"s","name":"SMP","worlds":[
              {"id":"w1","name":"old","is_active":false},
              {"id":"w2","name":"current","is_active":true,
               "content":{"modloader":"fabric","game_version":"1.21.4"}}
            ]}
        """.trimIndent()
        val full = AsterionJson.decodeFromString<ServerFull>(json)
        assertEquals("w2", full.activeWorld?.id)
        assertEquals("1.21.4", full.activeWorld?.content?.gameVersion)
    }

    @Test
    fun serverFullFallsBackToFirstWorldWhenNoneActive() {
        val json = """{"id":"s","worlds":[{"id":"w1","name":"only"}]}"""
        assertEquals("w1", AsterionJson.decodeFromString<ServerFull>(json).activeWorld?.id)
    }
}
