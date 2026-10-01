CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- ---------- Пользователи ----------
CREATE TABLE users (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    username            VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    email_verified_at   TIMESTAMPTZ,
    password_hash       VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    quota_bytes         BIGINT,
    used_bytes          BIGINT NOT NULL DEFAULT 0,
    is_admin            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
-- Обновление used_bytes при загрузке/удалении в сервисном слое

-- ---------- Группы ----------
CREATE TABLE groups (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(255) NOT NULL UNIQUE,
    members_count       INT NOT NULL,
    quota_bytes         BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_groups (
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

-- ---------- Хранилища ----------
CREATE TABLE storages (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    type        VARCHAR(20) NOT NULL CHECK (type IN ('S3','LOCAL')),
    config      JSONB NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_storages_default ON storages(is_default) WHERE is_default;

-- ---------- Nodes: единая сущность файл/папка ----------
CREATE TABLE nodes (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid                UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    owner_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    parent_id           BIGINT REFERENCES nodes(id) ON DELETE RESTRICT,
    node_type           VARCHAR(12) NOT NULL CHECK (node_type IN ('FILE', 'FOLDER')),
    name                VARCHAR(255) NOT NULL,
    mime_type           VARCHAR(255),
    size_bytes          BIGINT NOT NULL DEFAULT 0 CHECK(size_bytes >= 0),
    checksum_sha256     VARCHAR(64),
    storage_id          BIGINT REFERENCES storages(id),                     -- NULL для папок
    storage_key         TEXT,
    is_shared           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT chk_node_type CHECK (
        (node_type = 'FOLDER' AND storage_id IS NULL AND storage_key IS NULL AND mime_type IS NULL) OR
        (node_type = 'FILE' AND storage_id IS NOT NULL AND storage_key IS NOT NULL)
    )
);
CREATE INDEX idx_nodes_parent ON nodes(parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_nodes_owner  ON nodes(owner_id)  WHERE deleted_at IS NULL;
-- Уникальность имени среди живых узлов одной папки:
--CREATE UNIQUE INDEX uq_nodes_parent_name_live ON nodes(owner_id, COALESCE(parent_id, 0), name) WHERE deleted_at IS NULL;
CREATE INDEX idx_nodes_sha256 ON nodes(checksum_sha256) WHERE node_type = 'FILE';

-- ---------- Версии файлов ----------
CREATE TABLE file_versions (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    node_id             BIGINT NOT NULL REFERENCES nodes(id) ON DELETE RESTRICT,
    version_number      INTEGER NOT NULL CHECK(version_number >= 0),
    size_bytes          BIGINT NOT NULL CHECK (size_bytes >= 0),
    checksum_sha256     VARCHAR(64),
    storage_id          BIGINT NOT NULL REFERENCES storages(id),
    storage_key         TEXT NOT NULL,
    uploaded_by         BIGINT REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (node_id, version_number)
);

-- ---------- Ссылки ----------
CREATE TABLE shares (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    node_id         BIGINT NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id    BIGINT REFERENCES users(id) ON DELETE CASCADE,
    group_id        BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    permissions     VARCHAR(10) NOT NULL DEFAULT 'READ' CHECK (permissions IN ('READ','WRITE', 'ALL')),
    share_token     VARCHAR(64) UNIQUE,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_share_target CHECK (
        num_nonnulls(recipient_id, group_id, share_token) = 1
    )
);
CREATE INDEX idx_shares_recipient ON shares(recipient_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_shares_group     ON shares(group_id)     WHERE deleted_at IS NULL;
CREATE INDEX idx_shares_node      ON shares(node_id);

-- ---------- Теги / Избранное ----------
CREATE TABLE tags (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    color       VARCHAR(7),
    UNIQUE (owner_id, name)
);

CREATE TABLE node_tags (
    node_id     BIGINT NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (node_id, tag_id)
);

CREATE TABLE favorites (
    node_id     BIGINT NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (node_id, user_id)
);

-- ---------- Активность ----------
CREATE TABLE activities (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    action     VARCHAR(30) NOT NULL,
    node_id    BIGINT REFERENCES nodes(id) ON DELETE SET NULL,
    details    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_activities_user_time ON activities(user_id, created_at DESC);

-- ---------- Refresh tokens ----------
CREATE TABLE refresh_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL UNIQUE,
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    device_name     VARCHAR(255),
    last_used_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
