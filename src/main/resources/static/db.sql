-- DB_JWT
CREATE DATABASE DB_PETMATE;
USE DB_PETMATE;

CREATE TABLE MEMBER(
	NO				INT AUTO_INCREMENT,
    ID				VARCHAR(20) UNIQUE,
    PW				VARCHAR(100) NOT NULL,
    MAIL			VARCHAR(50) NOT NULL,
    REG_DATE		DATETIME DEFAULT NOW(),
    MOD_DATE		DATETIME DEFAULT NOW(),
    PRIMARY KEY(NO)
);
SELECT * FROM MEMBER;
DELETE FROM MEMBER;

CREATE TABLE REFRESH_TOKEN(
	NO					INT AUTO_INCREMENT,
    OWNER_NO			INT NOT NULL,
    REFRESH_TOKEN		TEXT NOT NULL,
    REG_DATE			DATETIME DEFAULT NOW(),
    MOD_DATE			DATETIME DEFAULT NOW(),
    PRIMARY KEY(NO)
);

SELECT * FROM REFRESH_TOKEN;
DELETE FROM REFRESH_TOKEN;

-- 결제 테이블 생성
CREATE TABLE payment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    provider VARCHAR(20) NOT NULL,  -- 길이 수정: 1 -> 20
    provider_tx_id VARCHAR(100),
    amount INT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    status VARCHAR(1) NOT NULL DEFAULT '0',
    paid_at DATETIME,
    cancelled_at DATETIME,
    raw_json JSON
);

-- 공통 코드 테이블 생성
CREATE TABLE common_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code_type VARCHAR(50) NOT NULL,
    code_value VARCHAR(10) NOT NULL,
    code_name VARCHAR(50) NOT NULL,
    code_desc VARCHAR(100),
    sort_order INT,
    use_yn VARCHAR(1) DEFAULT 'Y'
);

-- 결제 상태 코드 데이터 삽입
INSERT INTO common_codes (code_type, code_value, code_name, code_desc, sort_order, use_yn) VALUES
('PAYMENT_STATUS', '0', 'PENDING', '결제대기', 1, 'Y'),
('PAYMENT_STATUS', '1', 'PAID', '결제완료', 2, 'Y'),
('PAYMENT_STATUS', '2', 'CANCELLED', '결제취소', 3, 'Y'),
('PAYMENT_STATUS', '3', 'FAILED', '결제실패', 4, 'Y');

-- 결제사 코드 데이터 삽입
INSERT INTO common_codes (code_type, code_value, code_name, code_desc, sort_order, use_yn) VALUES
('PAYMENT_PROVIDER', 'TOSS', 'TOSS', '토스페이', 1, 'Y'),
('PAYMENT_PROVIDER', 'KAKAO', 'KAKAO', '카카오페이', 2, 'Y'),
('PAYMENT_PROVIDER', 'NAVER', 'NAVER', '네이버페이', 3, 'Y'),
('PAYMENT_PROVIDER', 'CARD', 'CARD', '신용카드', 4, 'Y'),
('PAYMENT_PROVIDER', 'BANK', 'BANK', '계좌이체', 5, 'Y');

-- 테이블 확인
SELECT * FROM payment;
SELECT * FROM common_codes;
