package cl.artcrate.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Punto de entrada del servicio de pedidos (puerto 8085). */
@SpringBootApplication
@EnableFeignClients // activa las interfaces @FeignClient del paquete "client"
public class PedidosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosServiceApplication.class, args);
    }
}
