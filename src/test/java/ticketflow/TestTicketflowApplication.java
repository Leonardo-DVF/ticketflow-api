package ticketflow;

import org.springframework.boot.SpringApplication;

public class TestTicketflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(TicketflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
