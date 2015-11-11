package smartthings.ratpack.cassandra;

import com.datastax.driver.core.NettyOptions;
import io.netty.channel.EventLoopGroup;
import ratpack.exec.Execution;

import java.util.concurrent.ThreadFactory;

public class RatpackCassandraNettyOptions extends NettyOptions {

	@Override
	public EventLoopGroup eventLoopGroup(ThreadFactory threadFactory) {
		return Execution.current().getController().getEventLoopGroup();
	}

	@Override
	public void onClusterClose(EventLoopGroup eventLoopGroup) {
		//Noop let Ratpack deal with the loop shutdown.
	}
}
