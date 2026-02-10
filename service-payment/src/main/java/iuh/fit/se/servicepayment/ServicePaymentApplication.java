package iuh.fit.se.servicepayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServicePaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicePaymentApplication.class, args);
    }

}
