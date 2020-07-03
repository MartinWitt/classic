package chrisliebaer.chrisliebot.abstraction;

import lombok.NonNull;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import java.util.function.BiFunction;
import java.util.function.Function;

// most methods overriden to adjust return type
public class PlainOutputSubstituionImpl extends PlainOutputImpl implements PlainOutput.PlainOutputSubstituion {
	
	private StrSubstitutor substitutor;
	
	public PlainOutputSubstituionImpl(@NonNull Function<String, String> escaper,
									  @NonNull BiFunction<Object, String, String> formatResolver,
									  StrLookup lookup) {
		super(escaper, formatResolver);
		substitutor = new StrSubstitutor(lookup);
	}
	
	@Override
	public PlainOutputSubstituion appendSub(String s, Object... format) {
		append(() -> substitutor.replace(s), format);
		return this;
	}
	
	@Override
	public PlainOutputSubstituion appendEscapeSub(String s, Object... format) {
		appendEscape(() -> substitutor.replace(s), format);
		return this;
	}
	
	@Override
	public PlainOutputSubstituion append(String s, Object... format) {
		super.append(s, format);
		return this;
	}
	
	@Override
	public PlainOutputSubstituion appendEscape(String s, Object... format) {
		super.appendEscape(s, format);
		return this;
	}
	
	@Override
	public PlainOutputSubstituion newLine() {
		super.newLine();
		return this;
	}
	
	@Override
	public PlainOutputSubstituion clear() {
		super.clear();
		return this;
	}
}
