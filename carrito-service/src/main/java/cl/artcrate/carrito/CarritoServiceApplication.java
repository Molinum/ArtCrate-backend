package cl.artcrate.carrito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Punto de entrada del servicio de carrito (puerto 8084). */
@SpringBootApplication
@EnableFeignClients // activa las interfaces @FeignClient del paquete "client"
public class CarritoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarritoServiceApplication.class, args);
    }
}
