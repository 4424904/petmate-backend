package com.petmate.domain.pet.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "OWNER_USER_ID", nullable = false)
    private Long ownerUserId;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    @Column(name = "SPECIES", nullable = false, length = 1)
    private String species;

    @Column(name = "BREED_ID")
    private Long breedId;

    @Column(name = "GENDER", nullable = false, length = 1)
    private String gender;

    @Column(name = "AGE_YEAR", precision = 4, scale = 1)
    private BigDecimal ageYear;

    @Column(name = "WEIGHT_KG", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "NEUTERED")
    private Integer neutered;

    @Column(name = "TEMPER", length = 50)
    private String temper;

    @Column(name = "NOTE", columnDefinition = "TEXT")
    private String note;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // 연관 관계 매핑 (선택사항)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BREED_ID", insertable = false, updatable = false)
    private PetBreedEntity breed;

    @Builder
    public PetEntity(Long ownerUserId, String name, String imageUrl, String species,
                     Long breedId, String gender, BigDecimal ageYear, BigDecimal weightKg,
                     Integer neutered, String temper, String note) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.species = species;
        this.breedId = breedId;
        this.gender = gender;
        this.ageYear = ageYear;
        this.weightKg = weightKg;
        this.neutered = neutered;
        this.temper = temper;
        this.note = note;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 업데이트 메서드
    public void updatePet(String name, String imageUrl, String species, Long breedId,
                          String gender, BigDecimal ageYear, BigDecimal weightKg,
                          Integer neutered, String temper, String note) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.species = species;
        this.breedId = breedId;
        this.gender = gender;
        this.ageYear = ageYear;
        this.weightKg = weightKg;
        this.neutered = neutered;
        this.temper = temper;
        this.note = note;
        this.updatedAt = LocalDateTime.now();
    }

    // 이미지 업데이트
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}