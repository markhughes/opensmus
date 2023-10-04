package net.sf.opensmus.io;

import org.jboss.netty.channel.DefaultChannelPipeline;
import net.sf.opensmus.MUSUser;

// Subclass with the only purpose of storing the user object connected with the channel
public class SMUSPipeline extends DefaultChannelPipeline {

    public MUSUser user;

    public SMUSPipeline() {
        super();
    }
}
