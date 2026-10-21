package com.example.fooddelivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FoodDeliveryBackendApplicationTests {

	@Test
	@DisplayName("Should successfully load the Spring application context")
	void contextLoads() {
	}

}
