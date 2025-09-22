package com.petmate.domain.pet.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "breed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetBreedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** 종 코드 (D=강아지, C=고양이, R=토끼, S=설치류, H=말, B=새, P=파충류, F=가축, O=기타) */
    @Column(name = "SPECIES", nullable = false, length = 1)
    private String species;

    /** 품종명 (DB: NAME) */
    @Column(name = "NAME", nullable = false, length = 120)
    private String name;

    /** 반려동물 연관 관계 */
    @OneToMany(mappedBy = "breed", fetch = FetchType.LAZY)
    private List<PetEntity> pets;

    @Builder
    public PetBreedEntity(String species, String name) {
        this.species = species;
        this.name = name;
    }

    /** 품종명 변경 */
    public void updateBreed(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }
}
