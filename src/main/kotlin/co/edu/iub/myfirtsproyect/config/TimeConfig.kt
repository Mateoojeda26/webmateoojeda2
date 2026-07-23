package co.edu.iub.myfirtsproyect.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class TimeConfig {
    @Bean
    fun applicationClock(): Clock = Clock.system(ZoneId.of("America/Bogota"))
}
