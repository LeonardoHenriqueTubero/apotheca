package br.dev.leonardo.apotheca.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

/**
 * Sorts by name with Portuguese rules ("água" before "Bolsa"). Lists are sorted here, not in SQL,
 * because database collations differ: the Alpine PostgreSQL image sorts "Z" before "a".
 */
final class NameOrder {

	private NameOrder() {
	}

	static <T> Comparator<T> by(Function<T, String> name) {
		return Comparator.comparing(name, Collator.getInstance(Locale.of("pt", "BR")));
	}

}
