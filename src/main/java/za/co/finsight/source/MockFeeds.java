package za.co.finsight.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

public final class MockFeeds {

    private MockFeeds() {}

    public static <T> List<T> load(ObjectMapper mapper, String classpathLocation, TypeReference<List<T>> type)
            throws IOException {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return mapper.readValue(in, type);
        }
    }
}
