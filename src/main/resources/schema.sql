CREATE TABLE IF NOT EXISTS contas (
    id BIGINT PRIMARY KEY,
    titular VARCHAR(160) NOT NULL,
    saldo DECIMAL(19, 2) NOT NULL CHECK (saldo >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transacoes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(32) NOT NULL,
    valor DECIMAL(19, 2) NOT NULL CHECK (valor > 0),
    horario TIMESTAMP NOT NULL,
    conta_id BIGINT NOT NULL,
    conta_contraparte_id BIGINT,
    INDEX idx_transacoes_conta_horario (conta_id, horario),
    CONSTRAINT fk_transacoes_conta
        FOREIGN KEY (conta_id) REFERENCES contas (id),
    CONSTRAINT fk_transacoes_contraparte
        FOREIGN KEY (conta_contraparte_id) REFERENCES contas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
