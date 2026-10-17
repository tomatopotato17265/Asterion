package dev.tomatopotato.asterion.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// the sealed value was produced by modrinth's own `sealLauncherSession` (WebCrypto, Node)
class LauncherSealKeyTest {
    private val key = LauncherSealKey.fromEncoded(
        privateKeyPkcs8 = LauncherAuth.decode("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgZvnzEw8fEuSB7BYWbPuL7LkqtfTSpljgU3jXvNCwm_yhRANCAAQGIubap1bVMFLINCofns6YCv95j_y6xKMOmXGGwxrVSL_5lqv4W6yuf_kB5ecMqiWJVZbsiZNIf61UNnS0dbnA")!!,
        publicKeySpki = LauncherAuth.decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEBiLm2qdW1TBSyDQqH57OmAr_eY_8usSjDplxhsMa1Ui_-Zar-Fusrn_5AeXnDKoliVWW7ImTSH-tVDZ0tHW5wA")!!,
        nonce = "OigZoKzhJ44KEjwFRQA75Q",
    )
    private val sealed = "AgBbMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE12hDLvafFMnjwJdVGoPgX6ulA9ozmupTqJkKOGjOfB8Pckn3f8_Cobv-gPAdSvOhchl6VlsYVZMkMaVuStyVMzLPJrpBIWte9K85a5Zcrw66DaMrpMjkO8nj3sFbWsBr4D0Xf-bCjVjvAG5wjiAXpPTS5OmSPZbkZ6oqXFvFmrDRW40D3i2W5sU9W04aTCu3oHShrt2IuZtaK21hsCx4ttkgflhurjLxUCyEu-SKHM6qkwM"

    @Test
    fun opensASessionSealedByModrinth() {
        assertEquals("mra_xksCQkaQqTzS0WUR8KXX2tGVSZPsDtlv01O5MOms5nuuh1D4sjkPHV6gopH0", key.open(LauncherHandoff(sealed, key.nonce)))
    }

    @Test
    fun rejectsAnotherNonce() {
        assertNull(key.open(LauncherHandoff(sealed, "somethingElse")))
    }

    @Test
    fun rejectsATamperedSeal() {
        val bytes = LauncherAuth.decode(sealed)!!
        bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()
        assertNull(key.open(LauncherHandoff(LauncherAuth.encode(bytes), key.nonce)))
    }

    @Test
    fun rejectsADifferentKey() {
        assertNull(LauncherSealKey().open(LauncherHandoff(sealed, key.nonce)))
    }

    @Test
    fun freshKeysBuildAProtocolTwoUrl() {
        val fresh = LauncherSealKey()
        assertEquals(91, fresh.publicKeySpki.size)
        assertEquals(22, fresh.nonce.length)
        assertEquals(LauncherAuth.signInUrl(fresh.publicKeySpki, fresh.nonce), fresh.signInUrl)
    }
}
