package cl.artcrate.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** Servidor Eureka (puerto 8761). Panel: http://localhost:8761 */
@SpringBootApplication
@EnableEurekaServer // convierte esta aplicación en el registro de servicios
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
