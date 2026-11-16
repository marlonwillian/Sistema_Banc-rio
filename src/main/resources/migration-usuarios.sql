CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(80) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    conta_id BIGINT NULL,
    UNIQUE KEY uk_usuarios_login (login),
    KEY idx_usuarios_perfil (perfil),
    CONSTRAINT fk_usuarios_conta
        FOREIGN KEY (conta_id) REFERENCES contas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
