package com.petmate.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "code", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code", "group_code"})
})
public class CodeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id;

    @Column(name = "GROUP_CODE", nullable = false, length = 50)
    private String groupCode;

    @Column(name = "CODE", nullable = false, length = 2)
    private String code;

    @Column(name = "CODE_NAME_ENG", length = 100)
    private String codeNameEng;

    @Column(name = "CODE_NAME_KOR", length = 100)
    private String codeNameKor;

    @Column(name = "SORT", nullable = false)
    private int sort;
}
