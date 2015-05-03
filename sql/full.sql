# --- !Ups

CREATE TABLE user (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(25) NOT NULL UNIQUE,
    name VARCHAR(25) NOT NULL,
    photo INT DEFAULT NULL,
    photo_name VARCHAR(40) DEFAULT NULL,
    PRIMARY KEY (user_id),
    FULLTEXT(name),
    FULLTEXT(username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE user_info (
    user_id INT UNSIGNED NOT NULL,
    password VARCHAR(105) NOT NULL,
    email VARCHAR(255) UNIQUE,
    time BIGINT NOT NULL,
    reputation INT NOT NULL,
    bio VARCHAR(200),
    PRIMARY KEY(user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE admin (
    admin_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    PRIMARY KEY (admin_id),
    CONSTRAINT `user__admin_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE social_user (
    social_user_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    social_id VARCHAR(100),
    PRIMARY KEY (social_user_id),
    CONSTRAINT `user__social_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE board (
    board_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    time BIGINT NOT NULL,
    description VARCHAR(500),
    creator_id INT,
    followers INT,
    photo INT,
    privacy TINYINT,
    CONSTRAINT `user__board_fk` FOREIGN KEY (`creator_id`) REFERENCES user(`user_id`),
    PRIMARY KEY (board_id),
    FULLTEXT(name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE post (
    post_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(100),
    data VARCHAR(10000),
    board_id INT NOT NULL,
    repost_id INT,
    votes INT NOT NULL,
    comment_count INT NOT NULL,
    post_type TINYINT NOT NULL,
    media VARCHAR(200) DEFAULT '',
    time BIGINT NOT NULL,
    INDEX `post__user_id_idx` (`user_id` DESC),
    INDEX `post__board_id_idx` (`board_id` DESC),
    INDEX `post__time_idx` (`time` DESC),
    CONSTRAINT `board__post_fk`FOREIGN KEY (`board_id`) REFERENCES board(`board_id`),
    CONSTRAINT `user__post_fk`FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),    
    PRIMARY KEY (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE comment (
    comment_id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    data VARCHAR(10000) NOT NULL,
    time BIGINT NOT NULL,
    path VARCHAR(766) CHARACTER SET latin1,
    votes INT,
    CONSTRAINT `user__comment_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `post__comment_fk` FOREIGN KEY (`post_id`) REFERENCES post(`post_id`),
    INDEX `comment__path_idx` (`path` DESC),
    PRIMARY KEY (comment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE post_vote (
    post_vote_id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    value TINYINT NOT NULL,
    PRIMARY KEY (post_vote_id),
    CONSTRAINT `post__post_vote_fk` FOREIGN KEY (`post_id`) REFERENCES post(`post_id`),
    CONSTRAINT `user__post_vote_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    INDEX `post_vote__post_id_user_id_idx` (`post_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE comment_vote (
    comment_vote_id INT NOT NULL AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    value TINYINT NOT NULL,
    PRIMARY KEY (comment_vote_id),
    CONSTRAINT `comment__comment_vote_fk` FOREIGN KEY (`comment_id`) REFERENCES comment(`comment_id`),
    CONSTRAINT `user__comment_vote_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
    INDEX `comment_vote__comment_id_user_id_idx` (`comment_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE user_to_board (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    board_id INT NOT NULL,
    time BIGINT NOT NULL,
    CONSTRAINT `user__user_to_board_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `board__user_to_board_fk` FOREIGN KEY (`board_id`) REFERENCES board(`board_id`),
    INDEX `user_to_board__user_id_board_id_idx` (`user_id`, `board_id`)
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE media (
    media_id INT NOT NULL AUTO_INCREMENT,
    media_type TINYINT NOT NULL,
    media_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (media_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;
