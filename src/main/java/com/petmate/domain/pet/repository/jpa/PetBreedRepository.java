// src/main/java/com/petmate/domain/pet/repository/jpa/PetBreedRepository.java
package com.petmate.domain.pet.repository.jpa;

import com.petmate.domain.pet.entity.PetBreedEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetBreedRepository extends JpaRepository<PetBreedEntity, Long> {

    List<PetBreedEntity> findBySpeciesOrderByNameAsc(String species);

    Optional<PetBreedEntity> findBySpeciesAndName(String species, String name);

    boolean existsBySpeciesAndName(String species, String name);
}
