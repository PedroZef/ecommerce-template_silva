package br.com.ecommerce.repository;

import br.com.ecommerce.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    List<Interaction> findByUsuarioOrderByTimestampDesc(String usuario);
    List<Interaction> findByUsuarioOrUsuarioIsNullOrderByTimestampDesc(String usuario);
    List<Interaction> findAllByOrderByTimestampDesc();
}
