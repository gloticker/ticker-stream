package live.gloticker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TickerStreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(TickerStreamApplication.class, args);
	}

}
