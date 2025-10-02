-- liquibase formatted sql
-- =====================================================================
-- db.changelog-001.sql  |  PostgreSQL  |  Liquibase formatted SQL
-- Proiect: SaaS Employee Management (multi-tenant, JWT + Google OAuth2)
-- =====================================================================

-- =====================================================================
-- 000: Pre-setup (extensii utile)
-- =====================================================================
-- changeset ems:000-pre-extensions context:prod,dev,local
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- rollback DROP EXTENSION IF EXISTS pgcrypto;

-- =====================================================================
-- 001: Tabelul company (tenant container)
-- =====================================================================
-- changeset ems:001-company
CREATE TABLE company (
  id            UUID PRIMARY KEY,
  name          TEXT        NOT NULL,
  slug          TEXT        NOT NULL UNIQUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE company IS 'Companiile/tenants';
COMMENT ON COLUMN company.slug IS 'Identificator tenant (subdomeniu)';
-- rollback DROP TABLE IF EXISTS company;

-- =====================================================================
-- 002: Tabel app_user (utilizatori pe tenant)
-- =====================================================================
-- changeset ems:002-app_user
CREATE TABLE app_user (
  id            UUID PRIMARY KEY,
  tenant_id     TEXT        NOT NULL,
  email         TEXT        NOT NULL,
  password      TEXT,
  first_name    TEXT        NOT NULL,
  last_name     TEXT        NOT NULL,
  role          TEXT        NOT NULL, -- OWNER, ADMIN, EMPLOYEE
  status        TEXT        NOT NULL, -- ACTIVE, INVITED, DISABLED
  provider      TEXT        NOT NULL, -- LOCAL, GOOGLE
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- unicitate email în cadrul tenantului
ALTER TABLE app_user
  ADD CONSTRAINT uq_app_user_tenant_email UNIQUE (tenant_id, email);

-- validări tip ENUM via CHECK
ALTER TABLE app_user
  ADD CONSTRAINT chk_app_user_role
  CHECK (role IN ('OWNER','ADMIN','EMPLOYEE'));

ALTER TABLE app_user
  ADD CONSTRAINT chk_app_user_status
  CHECK (status IN ('ACTIVE','INVITED','DISABLED'));

ALTER TABLE app_user
  ADD CONSTRAINT chk_app_user_provider
  CHECK (provider IN ('LOCAL','GOOGLE'));

-- indexe uzuale
CREATE INDEX idx_app_user_tenant ON app_user(tenant_id);
CREATE INDEX idx_app_user_created_at ON app_user(created_at);
-- rollback
--  DROP INDEX IF EXISTS idx_app_user_created_at;
--  DROP INDEX IF EXISTS idx_app_user_tenant;
--  ALTER TABLE app_user DROP CONSTRAINT IF EXISTS chk_app_user_provider;
--  ALTER TABLE app_user DROP CONSTRAINT IF EXISTS chk_app_user_status;
--  ALTER TABLE app_user DROP CONSTRAINT IF EXISTS chk_app_user_role;
--  ALTER TABLE app_user DROP CONSTRAINT IF EXISTS uq_app_user_tenant_email;
--  DROP TABLE IF EXISTS app_user;

-- =====================================================================
-- 003: Invitations (flow de invitații)
-- =====================================================================
-- changeset ems:003-invitation
CREATE TABLE invitation (
  id            UUID PRIMARY KEY,
  tenant_id     TEXT        NOT NULL,
  email         TEXT        NOT NULL,
  role          TEXT        NOT NULL, -- OWNER, ADMIN, EMPLOYEE (de regulă ADMIN/EMPLOYEE)
  token         TEXT        NOT NULL UNIQUE,
  expires_at    TIMESTAMPTZ NOT NULL,
  accepted_at   TIMESTAMPTZ,
  created_by    UUID,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_invitation_created_by
    FOREIGN KEY (created_by) REFERENCES app_user(id)
);

ALTER TABLE invitation
  ADD CONSTRAINT chk_invitation_role
  CHECK (role IN ('OWNER','ADMIN','EMPLOYEE'));

CREATE INDEX idx_invitation_tenant_email ON invitation(tenant_id, email);
CREATE INDEX idx_invitation_expires_at   ON invitation(expires_at);
-- rollback
--  DROP INDEX IF EXISTS idx_invitation_expires_at;
--  DROP INDEX IF EXISTS idx_invitation_tenant_email;
--  ALTER TABLE invitation DROP CONSTRAINT IF EXISTS chk_invitation_role;
--  ALTER TABLE invitation DROP CONSTRAINT IF EXISTS fk_invitation_created_by;
--  DROP TABLE IF EXISTS invitation;

-- =====================================================================
-- 004: Leave requests (concedii / învoiri)
-- =====================================================================
-- changeset ems:004-leave_request
CREATE TABLE leave_request (
  id            UUID PRIMARY KEY,
  tenant_id     TEXT        NOT NULL,
  user_id       UUID        NOT NULL,
  type          TEXT        NOT NULL, -- ANNUAL, SICK, UNPAID, OTHER
  start_date    DATE        NOT NULL,
  end_date      DATE        NOT NULL,
  days          NUMERIC(5,2) NOT NULL,
  status        TEXT        NOT NULL, -- PENDING, APPROVED, REJECTED, CANCELLED
  approver_id   UUID,
  reason        TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_leave_user      FOREIGN KEY (user_id)    REFERENCES app_user(id),
  CONSTRAINT fk_leave_approver  FOREIGN KEY (approver_id) REFERENCES app_user(id)
);

ALTER TABLE leave_request
  ADD CONSTRAINT chk_leave_type
  CHECK (type IN ('ANNUAL','SICK','UNPAID','OTHER'));

ALTER TABLE leave_request
  ADD CONSTRAINT chk_leave_status
  CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'));

-- trigger simplu pentru updated_at
CREATE OR REPLACE FUNCTION trg_touch_leave_request() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END$$ LANGUAGE plpgsql;

CREATE TRIGGER touch_leave_request
BEFORE UPDATE ON leave_request
FOR EACH ROW EXECUTE FUNCTION trg_touch_leave_request();

CREATE INDEX idx_leave_tenant_dates ON leave_request(tenant_id, start_date, end_date);
CREATE INDEX idx_leave_user ON leave_request(user_id);
-- rollback
--  DROP INDEX IF EXISTS idx_leave_user;
--  DROP INDEX IF EXISTS idx_leave_tenant_dates;
--  DROP TRIGGER IF EXISTS touch_leave_request ON leave_request;
--  DROP FUNCTION IF EXISTS trg_touch_leave_request;
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS chk_leave_status;
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS chk_leave_type;
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_approver;
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_user;
--  DROP TABLE IF EXISTS leave_request;

-- =====================================================================
-- 005: Attendance (prezență/availability daily)
-- =====================================================================
-- changeset ems:005-attendance
CREATE TABLE attendance (
  id            UUID PRIMARY KEY,
  tenant_id     TEXT        NOT NULL,
  user_id       UUID        NOT NULL,
  date          DATE        NOT NULL,
  status        TEXT        NOT NULL, -- PRESENT, REMOTE, OFF, LEAVE, SICK
  notes         TEXT,
  CONSTRAINT fk_att_user FOREIGN KEY (user_id) REFERENCES app_user(id),
  CONSTRAINT uq_att_unique_day UNIQUE (tenant_id, user_id, date)
);

ALTER TABLE attendance
  ADD CONSTRAINT chk_attendance_status
  CHECK (status IN ('PRESENT','REMOTE','OFF','LEAVE','SICK'));

CREATE INDEX idx_att_tenant_date  ON attendance(tenant_id, date);
CREATE INDEX idx_att_user_date    ON attendance(user_id, date);
-- rollback
--  DROP INDEX IF EXISTS idx_att_user_date;
--  DROP INDEX IF EXISTS idx_att_tenant_date;
--  ALTER TABLE attendance DROP CONSTRAINT IF EXISTS chk_attendance_status;
--  ALTER TABLE attendance DROP CONSTRAINT IF EXISTS uq_att_unique_day;
--  ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_att_user;
--  DROP TABLE IF EXISTS attendance;

-- =====================================================================
-- 006: Audit log (acțiuni importante)
-- =====================================================================
-- changeset ems:006-audit_log
CREATE TABLE audit_log (
  id            UUID PRIMARY KEY,
  tenant_id     TEXT        NOT NULL,
  actor_user_id UUID,
  action        TEXT        NOT NULL,
  entity        TEXT        NOT NULL,
  entity_id     TEXT        NOT NULL,
  payload       JSONB,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_user(id)
);

CREATE INDEX idx_audit_tenant_time ON audit_log(tenant_id, created_at);
CREATE INDEX idx_audit_entity ON audit_log(entity, entity_id);
-- rollback
--  DROP INDEX IF EXISTS idx_audit_entity;
--  DROP INDEX IF EXISTS idx_audit_tenant_time;
--  ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS fk_audit_actor;
--  DROP TABLE IF EXISTS audit_log;

-- =====================================================================
-- 007: Subscription/Billing (per company)
-- =====================================================================
-- changeset ems:007-subscription
CREATE TABLE subscription (
  id                   UUID PRIMARY KEY,
  company_id           UUID        NOT NULL,
  plan                 TEXT        NOT NULL, -- FREE, PRO, ENTERPRISE
  status               TEXT        NOT NULL, -- ACTIVE, TRIAL, PAST_DUE, EXPIRED
  started_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  trial_until          TIMESTAMPTZ,
  expires_at           TIMESTAMPTZ,
  last_payment_at      TIMESTAMPTZ,
  next_payment_due     TIMESTAMPTZ,
  external_customer_id TEXT,
  external_sub_id      TEXT,
  CONSTRAINT fk_sub_company FOREIGN KEY (company_id) REFERENCES company(id)
);

ALTER TABLE subscription
  ADD CONSTRAINT chk_subscription_plan
  CHECK (plan IN ('FREE','PRO','ENTERPRISE'));

ALTER TABLE subscription
  ADD CONSTRAINT chk_subscription_status
  CHECK (status IN ('ACTIVE','TRIAL','PAST_DUE','EXPIRED'));

CREATE UNIQUE INDEX uq_subscription_company ON subscription(company_id);
CREATE INDEX idx_subscription_next_due ON subscription(next_payment_due);
-- rollback
--  DROP INDEX IF EXISTS idx_subscription_next_due;
--  DROP INDEX IF EXISTS uq_subscription_company;
--  ALTER TABLE subscription DROP CONSTRAINT IF EXISTS chk_subscription_status;
--  ALTER TABLE subscription DROP CONSTRAINT IF EXISTS chk_subscription_plan;
--  ALTER TABLE subscription DROP CONSTRAINT IF EXISTS fk_sub_company;
--  DROP TABLE IF EXISTS subscription;

-- =====================================================================
-- 008: Conveniențe (valori implicite și generare UUID din DB) – opțional
-- =====================================================================
-- changeset ems:008-defaults-and-uuids context:dev,local
ALTER TABLE company       ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE app_user      ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE invitation    ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE leave_request ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE attendance    ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE audit_log     ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE subscription  ALTER COLUMN id SET DEFAULT gen_random_uuid();
-- rollback
--  ALTER TABLE subscription  ALTER COLUMN id DROP DEFAULT;
--  ALTER TABLE audit_log     ALTER COLUMN id DROP DEFAULT;
--  ALTER TABLE attendance    ALTER COLUMN id DROP DEFAULT;
--  ALTER TABLE leave_request ALTER COLUMN id DROP DEFAULT;
--  ALTER TABLE invitation    ALTER COLUMN id DROP DEFAULT;
--  ALTER TABLE app_user      ALTER COLUMN id DROP DEFAULT;
--  ALTER TABLE company       ALTER COLUMN id DROP DEFAULT;

-- =====================================================================
-- 009: Indici suplimentari pe căutări frecvente
-- =====================================================================
-- changeset ems:009-extra-indexes
CREATE INDEX idx_user_name ON app_user(tenant_id, last_name, first_name);
CREATE INDEX idx_leave_status ON leave_request(tenant_id, status);
-- (NU mai creăm idx_invitation_token — token e deja UNIQUE)
-- rollback
--  DROP INDEX IF EXISTS idx_leave_status;
--  DROP INDEX IF EXISTS idx_user_name;

-- =====================================================================
-- 010: Email verification fields (fără context, pentru toate mediile)
-- =====================================================================
-- changeset ems:010-email-verification
ALTER TABLE app_user
  ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ NULL,
  ADD COLUMN IF NOT EXISTS email_verif_token_hash VARCHAR(64) NULL,
  ADD COLUMN IF NOT EXISTS email_verif_expires_at TIMESTAMPTZ NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_verif_token_hash
  ON app_user(email_verif_token_hash)
  WHERE email_verif_token_hash IS NOT NULL;
-- rollback
--  DROP INDEX IF EXISTS ux_app_user_verif_token_hash;
--  ALTER TABLE app_user DROP COLUMN IF EXISTS email_verif_expires_at;
--  ALTER TABLE app_user DROP COLUMN IF EXISTS email_verif_token_hash;
--  ALTER TABLE app_user DROP COLUMN IF EXISTS email_verified_at;
--  ALTER TABLE app_user DROP COLUMN IF EXISTS email_verified;

-- =====================================================================
-- 011: Integritate tenant_id -> company.slug (FK pe slug)
-- =====================================================================
-- changeset ems:011-fk-tenant-to-company
ALTER TABLE app_user
  ADD CONSTRAINT fk_app_user_company_slug
  FOREIGN KEY (tenant_id) REFERENCES company(slug)
  ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE invitation
  ADD CONSTRAINT fk_invitation_company_slug
  FOREIGN KEY (tenant_id) REFERENCES company(slug)
  ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE leave_request
  ADD CONSTRAINT fk_leave_company_slug
  FOREIGN KEY (tenant_id) REFERENCES company(slug)
  ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE attendance
  ADD CONSTRAINT fk_attendance_company_slug
  FOREIGN KEY (tenant_id) REFERENCES company(slug)
  ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE audit_log
  ADD CONSTRAINT fk_audit_company_slug
  FOREIGN KEY (tenant_id) REFERENCES company(slug)
  ON UPDATE CASCADE ON DELETE RESTRICT;
-- rollback
--  ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS fk_audit_company_slug;
--  ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_attendance_company_slug;
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_company_slug;
--  ALTER TABLE invitation DROP CONSTRAINT IF EXISTS fk_invitation_company_slug;
--  ALTER TABLE app_user DROP CONSTRAINT IF EXISTS fk_app_user_company_slug;

-- =====================================================================
-- 012: Constrângeri suplimentare concedii
-- =====================================================================
-- changeset ems:012-leave-constraints
ALTER TABLE leave_request
  ADD CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
  ADD CONSTRAINT chk_leave_days_positive CHECK (days > 0);
-- rollback
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS chk_leave_days_positive;
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS chk_leave_dates;

-- =====================================================================
-- 013: (Opțional) email case-insensitive cu CITEXT
-- =====================================================================
-- changeset ems:013-citext context:opt
CREATE EXTENSION IF NOT EXISTS citext;
-- rollback DROP EXTENSION IF EXISTS citext;

-- (opțional) conversia coloanei la CITEXT -- scoate contextul dacă vrei să ruleze
-- changeset ems:013b-email-citext context:opt
-- ALTER TABLE app_user ALTER COLUMN email TYPE CITEXT;
-- rollback
--  ALTER TABLE app_user ALTER COLUMN email TYPE TEXT;

-- =====================================================================
-- 014: ON DELETE behavior pentru FK spre app_user
-- =====================================================================
-- changeset ems:014-fk-delete-behavior
ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_user;
ALTER TABLE leave_request ADD CONSTRAINT fk_leave_user
  FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT;

ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_approver;
ALTER TABLE leave_request ADD CONSTRAINT fk_leave_approver
  FOREIGN KEY (approver_id) REFERENCES app_user(id) ON DELETE SET NULL;

ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_att_user;
ALTER TABLE attendance ADD CONSTRAINT fk_att_user
  FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT;
-- rollback
--  -- revenire la varianta fără ON DELETE declarat
--  ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_att_user;
--  ALTER TABLE attendance ADD CONSTRAINT fk_att_user FOREIGN KEY (user_id) REFERENCES app_user(id);
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_approver;
--  ALTER TABLE leave_request ADD CONSTRAINT fk_leave_approver FOREIGN KEY (approver_id) REFERENCES app_user(id);
--  ALTER TABLE leave_request DROP CONSTRAINT IF EXISTS fk_leave_user;
--  ALTER TABLE leave_request ADD CONSTRAINT fk_leave_user FOREIGN KEY (user_id) REFERENCES app_user(id);

-- =====================================================================
-- Sfârșit changelog
-- =====================================================================
