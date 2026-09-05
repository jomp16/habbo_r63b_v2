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
package ovh.rwx;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

@Plugin(name = "UnixSocket", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class TuiUnixSocketAppender extends AbstractAppender {
    private final String path;
    private BufferedWriter writer;

    private TuiUnixSocketAppender(String name, Layout<? extends Serializable> layout, String path) {
        super(name, null, layout, true, null);
        this.path = path;
    }

    @PluginFactory
    public static TuiUnixSocketAppender create(
            @PluginAttribute("name") String name,
            @PluginAttribute("path") String path,
            @PluginAttribute(value = "pattern", defaultString = "%d{dd/MM/yyyy HH:mm:ss:SSS}") String pattern) {
        return new TuiUnixSocketAppender(name, PatternLayout.newBuilder().setPattern(pattern).build(), path);
    }

    @Override
    public void start() {
        try {
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            channel.connect(UnixDomainSocketAddress.of(Path.of(path)));
            writer = new BufferedWriter(Channels.newWriter(channel, java.nio.charset.StandardCharsets.UTF_8.newEncoder(), -1));
            writer.write("HELLO\tLOG\n");
            writer.flush();
        } catch (Exception ignored) {
            writer = null;
        }
        super.start();
    }

    @Override
    public void append(LogEvent event) {
        if (writer == null) return;
        String message = event.getMessage().getFormattedMessage().replace("\r", "").replace("\n", "\\n");
        String logger = event.getLoggerName() == null ? "unknown" : event.getLoggerName().replace("\t", " ");
        String timestamp = getLayout().toSerializable(event).toString().replace("\r", "").replace("\n", "");
        try {
            writer.write("LOG\t" + event.getLevel() + "\t" + timestamp + "\t" + logger + "\t" + message + "\n");
            writer.flush();
        } catch (Exception ignored) {
            // The launcher may have closed the socket while the JVM is shutting down.
        }
    }

    @Override
    public void stop() {
        try {
            if (writer != null) writer.close();
        } catch (Exception ignored) {
            // Nothing to do while closing the appender.
        }
        super.stop();
    }
}
