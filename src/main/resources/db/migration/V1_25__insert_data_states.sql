INSERT INTO state (id, initial, enabled, labels, created_by, created_at) VALUES ('PENDING', true, false, '{ "en": "Pending", "sv": "Avvaktande" }', 'system', CURRENT_TIMESTAMP(6));
INSERT INTO state (id, initial, enabled, labels, created_by, created_at) VALUES ('ACTIVE', false, true, '{ "en": "Active", "sv": "Aktiv" }', 'system', CURRENT_TIMESTAMP(6));
INSERT INTO state (id, initial, enabled, labels, created_by, created_at) VALUES ('INACTIVE', false, false, '{ "en": "Inactive", "sv": "Inaktiv" }', 'system', CURRENT_TIMESTAMP(6));
INSERT INTO state (id, initial, enabled, labels, created_by, created_at) VALUES ('REJECTED', false, false, '{ "en": "Rejected", "sv": "Avvisad" }', 'system', CURRENT_TIMESTAMP(6));
