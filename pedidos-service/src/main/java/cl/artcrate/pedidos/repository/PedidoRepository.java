package cl.artcrate.pedidos.repository;

import cl.artcrate.pedidos.model.Pedido;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Los más recientes primero
    List<Pedido> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    /**
     * Busca el pedido y lo BLOQUEA hasta que termine la transacción.
     * Evita que dos cambios de estado a la vez (por ejemplo, dos "cancelar") repongan el stock dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarParaActualizar(@Param("id") Long id);
}
