package ovh.rwx.habbo.communication.incoming.misc

import ovh.rwx.habbo.BuildConfig
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.communication.HabboRequest
import ovh.rwx.habbo.communication.Handler
import ovh.rwx.habbo.communication.incoming.Incoming
import ovh.rwx.habbo.game.misc.NotificationType
import ovh.rwx.habbo.game.user.HabboSession

@Suppress("unused", "UNUSED_PARAMETER")
class MiscGetMotdHandler {
    @Handler(Incoming.MISC_GET_MOTD)
    fun handle(habboSession: HabboSession, habboRequest: HabboRequest) {
        if (HabboServer.habboConfig.motdEnabled) {
            val motd = HabboServer.habboConfig.motdContents
                .replace("\$server_name", BuildConfig.NAME)
                .replace("\$server_version", BuildConfig.VERSION)
                .replace("\$username", habboSession.userInformation.username)

            habboSession.sendNotification(NotificationType.MOTD_ALERT, motd)
        }
    }
}