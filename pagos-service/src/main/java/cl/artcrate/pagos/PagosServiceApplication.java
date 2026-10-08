package cl.artcrate.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Punto de entrada del servicio de pagos (puerto 8086). */
@SpringBootApplication
@EnableFeignClients // activa las interfaces @FeignClient del paquete "client"
public class PagosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagosServiceApplication.class, args);
    }
}
