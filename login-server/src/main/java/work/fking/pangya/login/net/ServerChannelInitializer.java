package work.fking.pangya.login.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import work.fking.pangya.login.LoginServer;
import work.fking.pangya.networking.protocol.SimplePacketEncoder;

public class ServerChannelInitializer extends ChannelInitializer<Channel> {

    private final HelloHandler helloHandler;

    private ServerChannelInitializer(LoginServer loginServer) {
        this.helloHandler = HelloHandler.create(loginServer);
    }

    public static ServerChannelInitializer create(LoginServer loginServer) {
        return new ServerChannelInitializer(loginServer);
    }

    @Override
    protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("encoder", new SimplePacketEncoder());
        pipeline.addLast(helloHandler);
    }
}
