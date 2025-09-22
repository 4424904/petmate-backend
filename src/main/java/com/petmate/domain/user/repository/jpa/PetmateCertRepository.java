// domain/user/repository/jpa/PetmateCertRepository.java
package com.petmate.domain.user.repository.jpa;
import com.petmate.domain.user.entity.PetmateCertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PetmateCertRepository extends JpaRepository<PetmateCertEntity, Long> {
    List<PetmateCertEntity> findByUserId(Integer userId);
}
