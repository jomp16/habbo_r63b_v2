/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

package ovh.rwx.habbo.game.campaign

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ovh.rwx.habbo.HabboServer
import ovh.rwx.habbo.database.campaign.CampaignCalendarDao
import ovh.rwx.habbo.database.item.ItemDao
import ovh.rwx.habbo.database.item.ItemPurchaseData
import ovh.rwx.habbo.game.user.HabboSession
import java.util.concurrent.ConcurrentHashMap

class CampaignCalendarManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private val campaigns = ConcurrentHashMap<String, CampaignCalendar>()
    private val rewards = ConcurrentHashMap<String, Map<Int, CampaignCalendarReward>>()

    fun load() {
        log.info("Loading campaign calendars...")

        campaigns.clear()
        rewards.clear()

        CampaignCalendarDao.getCampaignCalendars().forEach {
            campaigns[it.name] = it
            rewards[it.name] = CampaignCalendarDao.getRewards(it.name).associateBy { reward -> reward.doorIndex }
        }

        log.info("Loaded {} campaign calendars!", campaigns.size)
    }

    fun activeCampaign(): CampaignCalendar? =
        campaigns.values.firstOrNull { it.isActive() }

    /**
     * Payload do CampaignCalendarDataMessageEvent para o login. NULL = sem campanha
     * ativa (não enviamos o pacote — releases pré-2015-12-01 têm header NULL e o
     * envio seria descartado de qualquer forma).
     */
    fun calendarDataFor(habboSession: HabboSession): CampaignCalendarData? {
        val campaign = activeCampaign() ?: return null
        val currentDay = campaign.currentDay()
        val opened = CampaignCalendarDao.getOpenedDoors(habboSession.userInformation.id, campaign.name)
        val missed = (0 until currentDay).filter { it !in opened }

        return CampaignCalendarData(
            campaignName = campaign.name,
            campaignImage = campaign.campaignImage,
            currentDay = currentDay,
            daysOfCampaign = campaign.days,
            openedBoxes = opened,
            lateBoxes = missed
        )
    }

    /**
     * Abre uma porta do calendário (OpenCampaignCalendarDoorComposer /
     * OpenCampaignCalendarDoorAsStaffComposer). Regras do cliente
     * (CalendarItem.resolveDayState): só o dia atual (estado 2) é abrí­vel; dias
     * passados não abertos são "missed" (estado 3) e futuros são travados (estado 4).
     *
     * `doorOpened=false` faz o cliente ignorar silenciosamente a resposta.
     */
    fun openDoor(habboSession: HabboSession, campaignName: String, doorIndex: Int, asStaff: Boolean): CampaignCalendarDoorOpenedData {
        val campaign = campaigns[campaignName]

        if (campaign == null || !campaign.isActive() || doorIndex !in 0 until campaign.days) {
            return CampaignCalendarDoorOpenedData.notOpened()
        }

        if (!asStaff) {
            // Só o dia corrente; missed/future são bloqueados server-side (o cliente
            // também bloqueia na UI, mas o protocolo é a fronteira de confiança).
            if (doorIndex != campaign.currentDay()) return CampaignCalendarDoorOpenedData.notOpened()
        }

        val userId = habboSession.userInformation.id

        if (CampaignCalendarDao.isOpen(userId, campaignName, doorIndex)) {
            return CampaignCalendarDoorOpenedData.notOpened()
        }

        val reward = rewards[campaignName]?.get(doorIndex)
            ?: return CampaignCalendarDoorOpenedData.notOpened()

        deliverReward(habboSession, reward)

        CampaignCalendarDao.insertOpen(userId, campaignName, doorIndex)

        return CampaignCalendarDoorOpenedData(
            doorOpened = true,
            productName = reward.productName,
            customImage = reward.customImage,
            furnitureClassName = reward.furnitureClassName
        )
    }

    private fun deliverReward(habboSession: HabboSession, reward: CampaignCalendarReward) {
        val furnishing = HabboServer.habboGame.itemManager.furnishings[reward.furnitureClassName] ?: run {
            log.warn("Campaign calendar reward '{}' references unknown furniture class '{}'", reward.productName, reward.furnitureClassName)
            return
        }

        val extraData = HabboServer.habboGame.itemManager.correctExtradataCatalog(habboSession, "", furnishing) ?: ""

        val userItems = ItemDao.addItems(
            habboSession.userInformation.id,
            listOf(ItemPurchaseData(furnishing, extraData, limited = false, buildersClub = false))
        )

        habboSession.habboInventory.addItems(userItems)
    }
}

/**
 * Payload do CampaignCalendarDoorOpenedMessageEvent (parser: doorOpened, productName,
 * customImage, furnitureClassName). doorOpened=true dispara showProductNotification
 * no cliente.
 */
data class CampaignCalendarDoorOpenedData(
    val doorOpened: Boolean,
    val productName: String,
    val customImage: String,
    val furnitureClassName: String,
) {
    companion object {
        fun notOpened() = CampaignCalendarDoorOpenedData(false, "", "", "")
    }
}
