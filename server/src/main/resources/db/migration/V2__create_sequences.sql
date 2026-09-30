CREATE TABLE IF NOT EXISTS `sequences` (
    `sequence_name` VARCHAR(64) NOT NULL,
    `next_hi` INT NOT NULL,
    PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;

INSERT INTO `sequences` (`sequence_name`, `next_hi`)
SELECT 'items', COALESCE(FLOOR(MAX(id) / 1000) + 1, 1) FROM `items`
ON DUPLICATE KEY UPDATE `sequence_name` = `sequence_name`;
