package cl.artcrate.inventario.repository;

import cl.artcrate.inventario.model.Stock;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Busca varios stocks y los BLOQUEA hasta que termine la transacción.
     * Así, si dos compras llegan al mismo tiempo, la segunda espera a la primera
     * y no pueden descontar el mismo stock a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productoId in :ids")
    List<Stock> buscarYBloquear(@Param("ids") Collection<Long> ids);
}
