package za.co.finsight.categorisation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import za.co.finsight.domain.Category;
import za.co.finsight.domain.NormalizedTransaction;

/**
 * Assigns a category to each normalised transaction using the ordered rule set.
 * Keyword matching uses word boundaries so that "SPAR" does not match "SPARKLING".
 */
@Component
public class CategorisationEngine {

    private static final String RULES_LOCATION = "rules/categorisation-rules.json";

    private record CompiledRule(CategoryRule rule, List<Pattern> keywordPatterns) {}

    private final List<CompiledRule> rules;

    public CategorisationEngine(ObjectMapper mapper) throws IOException {
        List<CategoryRule> loaded;
        try (InputStream in = new ClassPathResource(RULES_LOCATION).getInputStream()) {
            loaded = mapper.readValue(in, new TypeReference<List<CategoryRule>>() {});
        }
        validate(loaded);
        this.rules = loaded.stream()
                .sorted(Comparator.comparingInt(CategoryRule::priority))
                .map(r -> new CompiledRule(r, r.keywords().stream().map(CategorisationEngine::wordPattern).toList()))
                .toList();
    }

    public Category categorise(NormalizedTransaction tx) {
        String haystack = (tx.description() + " " + tx.merchantName()).toUpperCase(Locale.ROOT);
        for (CompiledRule compiled : rules) {
            if (matches(compiled, tx, haystack)) {
                return compiled.rule().category();
            }
        }
        return Category.OTHER;
    }

    private static boolean matches(CompiledRule compiled, NormalizedTransaction tx, String haystack) {
        CategoryRule rule = compiled.rule();
        if (rule.direction() != null && rule.direction() != tx.direction()) {
            return false;
        }
        if (!rule.mcc().isEmpty() && (tx.mcc() == null || !rule.mcc().contains(tx.mcc()))) {
            return false;
        }
        if (!compiled.keywordPatterns().isEmpty()
                && compiled.keywordPatterns().stream().noneMatch(p -> p.matcher(haystack).find())) {
            return false;
        }
        BigDecimal abs = tx.amount().abs();
        if (rule.minAmount() != null && abs.compareTo(rule.minAmount()) < 0) {
            return false;
        }
        return rule.maxAmount() == null || abs.compareTo(rule.maxAmount()) <= 0;
    }

    private static Pattern wordPattern(String keyword) {
        String normalised = keyword.toUpperCase(Locale.ROOT);
        return Pattern.compile("(?<![A-Z0-9])" + Pattern.quote(normalised) + "(?![A-Z0-9])");
    }

    private static void validate(List<CategoryRule> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            throw new IllegalStateException("categorisation rule set is empty");
        }
        Set<String> ids = new HashSet<>();
        for (CategoryRule rule : loaded) {
            if (!ids.add(rule.id())) {
                throw new IllegalStateException("duplicate categorisation rule id: " + rule.id());
            }
            if (!rule.hasCriteria()) {
                throw new IllegalStateException("categorisation rule has no criteria: " + rule.id());
            }
        }
    }
}
