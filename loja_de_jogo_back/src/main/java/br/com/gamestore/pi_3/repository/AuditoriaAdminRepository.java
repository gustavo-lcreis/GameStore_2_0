package br.com.gamestore.pi_3.repository;

import br.com.gamestore.pi_3.model.AuditoriaAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaAdminRepository extends JpaRepository<AuditoriaAdmin, Long> {
}
