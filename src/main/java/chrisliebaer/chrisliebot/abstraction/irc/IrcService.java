package chrisliebaer.chrisliebot.abstraction.irc;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceAttached;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class IrcService implements ChrislieService {
	
	@Getter private Client client;
	
	private Set<String> admins;
	private Set<String> ignores;
	
	@Setter private Consumer<ChrislieMessage> sink;
	
	public IrcService(@NonNull Client client, Set<String> admins, Set<String> ignores) {
		this.client = client;
		this.admins = admins;
		this.ignores = ignores;
		
		client.getEventManager().registerEventListener(this);
	}
	
	@Handler
	public void onChannelMessage(ChannelMessageEvent ev) {
		if (blockedUser(ev.getActor()))
			return;
		
		var sink = this.sink;
		if (sink != null)
			sink.accept(IrcMessage.of(this, ev));
		
	}
	
	@Handler
	public void onPrivateMessage(PrivateMessageEvent ev) {
		if (blockedUser(ev.getActor()))
			return;
		
		var sink = this.sink;
		if (sink != null)
			sink.accept(IrcMessage.of(this, ev));
	}
	
	@Override
	public void reconnect() {
		client.reconnect();
	}
	
	@Override
	public void exit() throws Exception {
		client.getEventManager().unregisterEventListener(this);
		client.shutdown();
	}
	
	@Override
	public Optional<IrcChannel> channel(String identifier) {
		return client.getChannel(identifier)
				.map(channel -> new IrcChannel(this, channel));
	}
	
	// TODO: figure out a way that doesn't required looping over every single user
	@Override
	public Optional<IrcUser> user(String identifier) {
		for (Channel channel : client.getChannels()) {
			for (User user : channel.getUsers()) {
				if (user.getAccount().orElse(user.getNick()).equals(identifier))
					return Optional.of(new IrcUser(this, user));
			}
		}
		return Optional.empty();
	}
	
	public boolean isAdmin(User user) {
		return user.getAccount().map(admins::contains).orElse(false);
	}
	
	private boolean blockedUser(User user) {
		return client.isUser(user) || ignores.contains(user.getNick());
	}
	
	public static void run(ChrislieService service, Consumer<IrcService> fn) {
		if (service instanceof IrcService)
			fn.accept((IrcService) service);
	}
	
	public static void run(ServiceAttached serviceAttached, Consumer<IrcService> fn) {
		run(serviceAttached.service(), fn);
	}
}
