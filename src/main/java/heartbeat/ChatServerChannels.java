package heartbeat;

import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServerChannels {
    private static final Set<Channel> channels = Collections.synchronizedSet(new HashSet<>());

    public static void addChannel(Channel channel) {
        channels.add(channel);
    }

    public static void removeChannel(Channel channel) {
        channels.remove(channel);
    }

    public static Set<Channel> getChannels() {
        return channels;
    }
}
