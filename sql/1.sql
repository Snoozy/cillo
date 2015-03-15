# --- !Ups

DROP TABLE IF EXISTS user;
CREATE TABLE user (
    user_id INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(15) NOT NULL UNIQUE,
    name VARCHAR(20) NOT NULL,
    password VARCHAR(105) NOT NULL,
    email VARCHAR(255),
    time BIGINT NOT NULL,
    reputation INT,
    photo INT DEFAULT NULL,
    bio VARCHAR(200),
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS board;
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
    PRIMARY KEY (board_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS post;
CREATE TABLE post (
    post_id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(50),
    data VARCHAR(10000) NOT NULL,
    board_id INT NOT NULL,
    repost TINYINT NOT NULL,
    votes INT NOT NULL,
    comment_count INT NOT NULL,
    post_type TINYINT NOT NULL,
    media VARCHAR(200) DEFAULT '',
    time BIGINT NOT NULL,
    INDEX `post__user_id_idx` (`user_id` DESC),
    CONSTRAINT `board__post_fk`FOREIGN KEY (`board_id`) REFERENCES board(`board_id`),
    PRIMARY KEY (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS comment;
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

DROP TABLE IF EXISTS post_vote;
CREATE TABLE post_vote (
    post_vote_id INT NOT NULL AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    value TINYINT NOT NULL,
    PRIMARY KEY (post_vote_id),
    CONSTRAINT `post__post_vote_fk` FOREIGN KEY (`post_id`) REFERENCES post(`post_id`),
    CONSTRAINT `user__post_vote_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS comment_vote;
CREATE TABLE comment_vote (
    comment_vote_id INT NOT NULL AUTO_INCREMENT,
    comment_id INT NOT NULL,
    user_id INT NOT NULL,
    time BIGINT NOT NULL,
    value TINYINT NOT NULL,
    PRIMARY KEY (comment_vote_id),
    CONSTRAINT `comment__comment_vote_fk` FOREIGN KEY (`comment_id`) REFERENCES comment(`comment_id`),
    CONSTRAINT `user__comment_vote_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS user_to_board;
CREATE TABLE user_to_board (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    board_id INT NOT NULL,
    time BIGINT NOT NULL,
    CONSTRAINT `user__user_to_board_fk` FOREIGN KEY (`user_id`) REFERENCES user(`user_id`),
    CONSTRAINT `board__user_to_board_fk` FOREIGN KEY (`board_id`) REFERENCES board(`board_id`),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS media;
CREATE TABLE media (
    media_id INT NOT NULL AUTO_INCREMENT,
    media_type TINYINT NOT NULL,
    media_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (media_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;