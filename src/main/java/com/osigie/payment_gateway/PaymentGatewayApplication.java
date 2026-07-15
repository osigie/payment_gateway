package com.osigie.payment_gateway;

import com.osigie.payment_gateway.config.BankProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableResilientMethods
@EnableScheduling
@EnableConfigurationProperties(BankProperties.class)
public class PaymentGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentGatewayApplication.class, args);
	}

}
