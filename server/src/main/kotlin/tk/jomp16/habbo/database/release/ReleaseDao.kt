package tk.jomp16.habbo.database.release

import tk.jomp16.habbo.HabboServer

object ReleaseDao {
    fun getReleases(): List<String> = HabboServer.database {
        select("SELECT release_name FROM releases") {
            it.string("release_name")
        }
    }

    fun getIncomingHeaders(): List<ReleaseHeaderInfo> = HabboServer.database {
        select("SELECT * FROM releases_incoming_headers") {
            ReleaseHeaderInfo(it.string("release_name"), it.string("name"), it.int("header"), it.stringOrNull("override_method"))
        }
    }

    fun getOutgoingHeaders(): List<ReleaseHeaderInfo> = HabboServer.database {
        select("SELECT * FROM releases_outgoing_headers") {
            ReleaseHeaderInfo(it.string("release_name"), it.string("name"), it.int("header"), it.stringOrNull("override_method"))
        }
    }
}

data class ReleaseHeaderInfo(
        val release: String,
        val name: String,
        val header: Int,
        val overrideMethod: String?
)
