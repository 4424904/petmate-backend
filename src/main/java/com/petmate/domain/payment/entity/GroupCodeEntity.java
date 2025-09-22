package com.petmate.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "danal_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "code_type", length = 50, nullable = false)
    private String codeType;  // PAYMENT_STATUS, PAYMENT_PROVIDER

    @Column(name = "code_value", length = 10, nullable = false)
    private String codeValue; // 0, 1, 2, 3...

    @Column(name = "code_name", length = 50, nullable = false)
    private String codeName;  // TOSS, KAKAO, NONE, PAID...

    @Column(name = "code_desc", length = 100)
    private String codeDesc;  // 토스페이, 카카오페이, 결제전, 결제완료...

    @Column(name = "sort_order")
    private int sortOrder;    // 1, 2, 3, 4...

    @Column(name = "use_yn", length = 1)
    private String useYn = "Y";

}
