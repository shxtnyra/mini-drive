ALTER TABLE users
    ADD COLUMN reserved_bytes BIGINT NOT NULL DEFAULT 0
        CHECK (reserved_bytes >= 0);

CREATE TABLE upload_sessions (
    session_uuid UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_folder_id BIGINT REFERENCES nodes(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    mime_type VARCHAR(255),
    storage_id BIGINT NOT NULL REFERENCES storages(id),
    storage_key TEXT NOT NULL,
    upload_id VARCHAR(255),
    chunk_size BIGINT NOT NULL CHECK (chunk_size > 0),
    total_parts INTEGER NOT NULL CHECK (total_parts > 0),
    reserved_bytes BIGINT NOT NULL CHECK (reserved_bytes >= 0),
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('INITIALIZING', 'UPLOADING', 'FINALIZING', 'COMPLETED', 'ABORTED')
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_upload_sessions_owner_active
    ON upload_sessions(owner_id)
    WHERE status IN ('INITIALIZING', 'UPLOADING', 'FINALIZING');

CREATE INDEX idx_upload_sessions_expiration
    ON upload_sessions(expires_at)
    WHERE status IN ('INITIALIZING', 'UPLOADING', 'FINALIZING');
