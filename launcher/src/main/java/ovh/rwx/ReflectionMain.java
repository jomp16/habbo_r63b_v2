/*
 * Copyright (C) 2015-2018 jomp16 <root@rwx.ovh>
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

package ovh.rwx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import ovh.rwx.habbo.HabboServer;
import ovh.rwx.habbo.config.HabboConfig;

import java.io.File;

@SuppressWarnings("unchecked")
class ReflectionMain {
    public static void main(String[] args) throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new KotlinModule());
        //noinspection unused
        final HabboConfig habboConfig = objectMapper.readValue(new File("config.json"), HabboConfig.class);

        // REFLECTION: LOAD CLASS HabboServer DIRECTLY
        final Class<HabboServer> habboServerClass = (Class<HabboServer>) Class.forName("ovh.rwx.habbo.HabboServer");
        final HabboServer habboServer = (HabboServer) habboServerClass.getField("INSTANCE").get(null);

        habboServer.start();
    }
}
