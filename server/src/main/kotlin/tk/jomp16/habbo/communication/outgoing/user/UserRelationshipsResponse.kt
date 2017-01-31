package tk.jomp16.habbo.communication.outgoing.user

import tk.jomp16.habbo.communication.HabboResponse
import tk.jomp16.habbo.communication.Response
import tk.jomp16.habbo.communication.outgoing.Outgoing
import tk.jomp16.habbo.game.user.information.UserInformation

@Suppress("unused", "UNUSED_PARAMETER")
class UserRelationshipsResponse {
    @Response(Outgoing.USER_RELATIONSHIPS)
    fun handle(habboResponse: HabboResponse, userId: Int, relationships: Collection<UserInformation>, loves: Int, likes: Int, hates: Int) {
        // todo: relationships

        habboResponse.apply {
            writeInt(userId)
            writeInt(relationships.size)

            relationships.forEach {
                writeInt(1) // todo: relationship type
                writeInt(loves) // todo: relationship type
                writeInt(it.id)
                writeUTF(it.username)
                writeUTF(it.figure)
            }
        }
    }
}