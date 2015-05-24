ALTER TABLE comment ADD status TINYINT NOT NULL DEFAULT 0;

CREATE TABLE referral (
    referral_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    CONSTRAINT `user__referral_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
)

alter table post add index `post__time_board_idx` (`board_id`, `time`);

alter table post add index `post__votes_idx` (`votes`);

CREATE TABLE notification_listener (
    id INT NOT NULL AUTO_INCREMENT,
    entity_id INT NOT NULL,
    entity_type TINYINT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (id),
    INDEX `notifications_listener__user_id_idx` (`user_id`),
    INDEX `notifications_listener__entity_id_entity_type_idx` (`entity_id`, `entity_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE notification (
    notification_id INT NOT NULL AUTO_INCREMENT,
    entity_id INT NOT NULL,
    entity_type TINYINT NOT NULL.
    title_user INT NOT NULL,
    action_type TINYINT NOT NULL,    
    count INT DEFAULT 0,
    time BIGINT NOT NULL,
    PRIMARY KEY (`notification_id`),
    INDEX `notification__entity_id_entity_type_idx` (`entity_id`, `entity_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
