package cl.artcrate.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway (puerto 8080): la única puerta de entrada para el front.
 * Las rutas y el CORS se definen en application.yml; el único código propio es el
 * manejador de errores (ManejadorErroresGateway).
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
