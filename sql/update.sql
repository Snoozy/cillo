ALTER TABLE comment ADD status TINYINT NOT NULL DEFAULT 0;

CREATE TABLE referral (
    referral_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    CONSTRAINT `user__referral_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
)

alter table post add index `post__time_board_idx` (`board_id`, `time`);
