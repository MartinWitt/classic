package chrisliebaer.chrisliebot.command.urlpreview;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.LimiterConfig;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.kitteh.irc.client.library.util.Format;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class GenericUrlPreview implements Callback {
	
	private static final int MAX_IRC_MESSAGE_LENGTH = 700;
	private static final int MAX_CONTENT_LENGTH = 5 * 1024 * 1024;
	private static final long PREVIEW_TIMEOUT = 10000; // cancel connection after 10 seconds even if we are still receiving data
	
	private OkHttpClient client = SharedResources.INSTANCE().httpClient();
	private Timer timer = SharedResources.INSTANCE().timer();
	
	private URL url;
	private ChrislieMessage m;
	private Set<UrlPreviewCommand.HistoryEntry> titleHistory;
	
	@SneakyThrows
	public GenericUrlPreview(@NonNull URL url, @NonNull ChrislieMessage m, Set<UrlPreviewCommand.HistoryEntry> titleHistory) {
		this.url = url;
		this.m = m;
		this.titleHistory = titleHistory;
	}
	
	public void start() {
		var req = new Request.Builder().get()
				.url(url)
				.header("User-Agent", "Twitterbot/1.0") // otherwise we get blocked too often :(
				.build();
		var call = client.newCall(req);
		call.enqueue(this);
		
		// queue timer for cancelation
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				call.cancel();
				if (!call.isExecuted())
					log.debug(C.LOG_PUBLIC, "canceled preview of {} since it took to long", url);
			}
		}, PREVIEW_TIMEOUT);
		
	}
	
	@Override
	public void onFailure(Call call, IOException e) {
		if (!e.getMessage().isEmpty())
			log.debug(C.LOG_PUBLIC, "failed to connect to {}: {}", url, e.getMessage());
	}
	
	@Override
	public void onResponse(Call call, Response response) throws IOException {
		
		// check for mime type
		String contentType = response.header("Content-Type");
		if (contentType == null) {
			log.debug(C.LOG_PUBLIC, "no content type provided: {}", url);
			return;
		}
		
		// we only care about html pages
		String mime = contentType.split(";")[0].trim();
		if (!"text/html".equalsIgnoreCase(mime)) {
			log.debug(C.LOG_PUBLIC, "can't parse content type {} for {}", mime, url);
		}
		
		// documentation doesn't mention it, but we have to close the body
		try (response; ResponseBody cutBody = response.peekBody(MAX_CONTENT_LENGTH)) {
			Document doc = Jsoup.parse(cutBody.string());
			
			// try to get title first
			String summary = doc.title();
			
			// but prefer open graph
			Elements metaOgTitle = doc.select("meta[property=og:title]");
			if (metaOgTitle != null) {
				var ogTitle = metaOgTitle.attr("content");
				summary = ogTitle.isEmpty() ? summary : ogTitle;
			}
			
			// and try to also append open graph description
			Elements metaOgDescription = doc.select("meta[property=og:description]");
			if (metaOgDescription != null) {
				var ogDescription = metaOgDescription.attr("content");
				summary += ogDescription.isEmpty() ? "" : (" - " + ogDescription);
			}
			
			summary = summary
					.replaceAll("[\n\r\u0000]", "") // remove illegal irc characters
					.trim();
			
			// limit output to 500 characters at max
			if (summary.length() > MAX_IRC_MESSAGE_LENGTH)
				summary = summary.substring(0, MAX_IRC_MESSAGE_LENGTH).trim() + "[...]";
			
			// check if summary was posted before within timeout window
			UrlPreviewCommand.HistoryEntry historyLookup = new UrlPreviewCommand.HistoryEntry(summary, m.channel().identifier());
			if (titleHistory.contains(historyLookup)) {
				// output has been posted, don't repeat
				log.debug(C.LOG_PUBLIC, "not posting summary of {} in {} since it's identical with a recently posted summary",
						url.toExternalForm(), m.channel().displayName());
				return;
			}
			
			// add output to history
			titleHistory.add(historyLookup);
			
			if (!summary.isEmpty()) {
				var reply = m.reply();
				m.channel().output(LimiterConfig.create().maxLines(1)).plain(C.format("Linkvorschau: ", Format.BOLD) + summary).send();
			}
		}
	}
}
