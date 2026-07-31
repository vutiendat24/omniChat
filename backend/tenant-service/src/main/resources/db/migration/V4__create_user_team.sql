CREATE TABLE user_team (
    user_id VARCHAR(255) NOT NULL,
    team_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, team_id),
    CONSTRAINT fk_ut_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);
