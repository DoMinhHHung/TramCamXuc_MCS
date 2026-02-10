package iuh.fit.se.serviceidentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ServiceIdentityApplication {

    /**
     * Application entry point that launches the Spring Boot application context.
     *
     * @param args command-line arguments passed to the application; forwarded to SpringApplication and may influence startup (for example, `--spring.*` properties)
     */
    public static void main(String[] args) {
        SpringApplication.run(ServiceIdentityApplication.class, args);
    }

}