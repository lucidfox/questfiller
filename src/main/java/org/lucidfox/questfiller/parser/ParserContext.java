package org.lucidfox.questfiller.parser;

import static org.lucidfox.questfiller.parser.ParseUtils.getRegexGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public final class ParserContext {
	private final Map<Integer, String> questCategories = new HashMap<>();
	
	public static ParserContext load() throws IOException {
		// Worker thread
		final String url = "http://wow.zamimg.com/js/locale_enus.js";
		
		try (final Reader reader = new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8)) {
			return new ParserContext(reader);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private ParserContext(final Reader localeJsReader) throws IOException {	
		// Obtain localization for quest categories
		final String fullScript = new BufferedReader(localeJsReader).lines().collect(Collectors.joining("\n"));
		final String questScript = getRegexGroup(fullScript, "var mn_quests=[^;]+;", 0).get();
		final ScriptEngine js = new ScriptEngineManager().getEngineByName("nashorn");
		
		try {
			// Eval the piece of JS that interests us, then convert the resulting data structure into our map
			js.eval(questScript);
			js.put("questCategories", questCategories);
			
			try (final Reader reader = new InputStreamReader(
					getClass().getResourceAsStream("LocaleCategories.js"), StandardCharsets.UTF_8)) {
				js.eval(reader);
			}
		} catch (final ScriptException e) {
			throw new RuntimeException(e);
		} 
	}
	
	public String getQuestCategory(final int categoryId) {
		return questCategories.get(categoryId);
	}
}