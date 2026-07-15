ALTER TABLE ambiguities ADD COLUMN related_requirement_ids JSONB DEFAULT '[]' NOT NULL;
