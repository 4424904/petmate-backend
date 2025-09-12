package com.petmate.domain.address.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADDRESS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "OWNER_ID", nullable = false)
    private Integer ownerId;

    @Column(name = "LABEL", length = 1, nullable = false)
    private String label;

    @Column(name = "ALIAS", length = 50)
    private String alias;

    @Column(name = "ROAD_ADDR", length = 255, nullable = false)
    private String roadAddr;

    @Column(name = "DETAIL_ADDR", length = 255)
    private String detailAddr;

    @Column(name = "POSTCODE", length = 10)
    private String postCode;

    @Column(name = "LATITUDE", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "IS_DEFAULT", nullable = false)
    @Builder.Default
    private Integer isDefault = 0;

    @Column(name = "CREATED_AT", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
