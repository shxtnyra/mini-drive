CREATE UNIQUE INDEX uq_shares_active_recipient
    ON shares(node_id, recipient_id)
    WHERE deleted_at IS NULL AND recipient_id IS NOT NULL;

CREATE UNIQUE INDEX uq_shares_active_group
    ON shares(node_id, group_id)
    WHERE deleted_at IS NULL AND group_id IS NOT NULL;

CREATE UNIQUE INDEX uq_shares_active_link
    ON shares(node_id)
    WHERE deleted_at IS NULL AND share_token IS NOT NULL;
