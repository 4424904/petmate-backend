// domain/user/entity/PetmateCertEntity.java
package com.petmate.domain.user.entity;

import com.petmate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="petmate_cert")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PetmateCertEntity extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="USER_ID", nullable=false)
    private Integer userId;

    @Column(name="UUID_NAME", length=100, nullable=false)
    private String uuidName;  // 🔹 예: abcd-1234.png

    @Column(name="FILE_PATH", nullable=false, length=500)
    private String filePath;  // 🔹 실제 저장 경로

    @Column(name="ORIGINAL_NAME", length=200)
    private String originalName;
}
