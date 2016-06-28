/*
 * Copyright (C) 2016 jomp16
 *
 * This file is part of habbo_r63b.
 *
 * habbo_r63b is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b. If not, see <http://www.gnu.org/licenses/>.
 */

package tk.jomp16.habbo.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tk.jomp16.habbo.database.catalog.CatalogDao
import java.util.*

class CatalogManager {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    val catalogPages: MutableList<CatalogPage> = ArrayList()
    val catalogItems: MutableList<CatalogItem> = ArrayList()

    init {
        log.info("Loading catalog...")

        catalogPages += CatalogDao.getCatalogPages()
        catalogItems += CatalogDao.getCatalogItems()

        log.info("Loaded {} catalog pages!", catalogPages.size)
        log.info("Loaded {} catalog items!", catalogItems.size)
    }
}