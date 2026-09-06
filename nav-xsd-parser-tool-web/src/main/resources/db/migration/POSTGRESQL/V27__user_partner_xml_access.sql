CREATE TABLE user_partner_permission (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    CONSTRAINT uk_user_partner_permission UNIQUE (user_id, partner_id),
    CONSTRAINT fk_upp_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_upp_partner FOREIGN KEY (partner_id) REFERENCES partner(id)
);
CREATE INDEX idx_upp_user ON user_partner_permission(user_id);
CREATE INDEX idx_upp_partner ON user_partner_permission(partner_id);

CREATE TABLE user_tax_permission_rule (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rule_type VARCHAR(10) NOT NULL,
    tax_number VARCHAR(8),
    vat_code VARCHAR(1),
    county_code VARCHAR(2),
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    CONSTRAINT fk_utpr_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_utpr_user ON user_tax_permission_rule(user_id);
