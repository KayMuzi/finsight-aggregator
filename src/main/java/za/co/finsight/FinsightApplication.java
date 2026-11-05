package za.co.finsight;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import za.co.finsight.config.FinsightProperties;

@SpringBootApplication
@EnableConfigurationProperties(FinsightProperties.class)
public class FinsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinsightApplication.class, args);
    }

    /** Injected rather than read from the system clock directly, so time is controllable in tests. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
