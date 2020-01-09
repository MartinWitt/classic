package chrisliebaer.chrisliebot.command.qwant;

import lombok.AllArgsConstructor;
import lombok.Data;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import javax.validation.constraints.NotNull;
import java.util.List;

// https://github.com/NLDev/qwant-api/blob/master/DOCUMENTATION.md
// or just visit website and check network monitor
// some fields are skipped since they provide no use for this bot
public interface QwantService {
	
	public static final String BASE_URL = "https://api.qwant.com/";
	
	public static final String DEFAULT_LOCALE = "de_DE";
	
	public static final String TYPE_WEB = "web";
	public static final String TYPE_NEWS = "news";
	public static final String TYPE_IMAGES = "images";
	
	@AllArgsConstructor
	enum SafeSearch {
		OFF(0),
		MEDIUM(1),
		HIGH(2);
		private int code;
	}
	
	@AllArgsConstructor
	enum Type {
		WEB("web"),
		NEWS("news"),
		IMAGE("images");
		private String code;
	}
	
	@Deprecated // dont call directly
	@GET("api/search/{type}?uiv=4&locale=" + DEFAULT_LOCALE)
	@SuppressWarnings({"MissingDeprecatedAnnotation"})
	public Call<QwantResponse> search(
			@Path("type") String type,
			@Query("t") String t,
			@Query("q") String query,
			@Query("safesearch") int safesearch,
			@Query("count") int count
	);
	
	public default Call<QwantResponse> search(String query, SafeSearch safesearch, int count, @NotNull Type type) {
		return search(type.code, type.code, query, safesearch.code, count);
	}
	
	@Data
	public static class QwantResponse {
		
		private String status;
		private int error; // only set if status is "error"
		private QwantData data;
		
		public List<QwantItem> items() {
			return data.result.items;
		}
		
		private static class QwantData {
			
			public QwantResult result;
		}
		
		private static class QwantResult {
			
			public List<QwantItem> items;
		}
		
		@Data
		public static class QwantItem {
			
			private String title;
			private String type; // should match query type
			private String media;
			private String desc;
			private String url;
		}
	}
}
