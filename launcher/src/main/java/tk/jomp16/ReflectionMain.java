/*
 * Copyright (C) 2015-2017 jomp16
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

package tk.jomp16;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import tk.jomp16.habbo.config.HabboConfig;
import tk.jomp16.habbo.kotlin.HabboServerUtilsKt;

import java.io.File;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
class ReflectionMain {
    public static void main(String[] args) throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new KotlinModule());
        final HabboConfig habboConfig = objectMapper.readValue(new File("config.json"), HabboConfig.class);

        // REFLECTION: LOAD CLASS HabboServer DIRECTLY
        /*final Class<HabboServer> habboServerClass = (Class<HabboServer>) Class.forName("tk.jomp16.habbo.HabboServer");

        final HabboServer habboServer = (HabboServer) habboServerClass.getField("INSTANCE").get(null);

        habboServer.setHabboConfig(habboConfig);

        habboServer.init();*/

        // REFLECTION: LOAD USING HabboServerUtilsKt
        final Class<HabboServerUtilsKt> habboServerUtilsKtClass = (Class<HabboServerUtilsKt>) Class.forName("tk.jomp16.habbo.kotlin.HabboServerUtilsKt");
        final Method method = habboServerUtilsKtClass.getDeclaredMethod("habboServer", HabboConfig.class);

        method.invoke(null, habboConfig);
    }
}
