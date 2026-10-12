-- =========================================================================
-- V8: Campaign Calendar Feature
-- Feature campaign calendar (advent calendar, nascida no protocolo em
-- PRODUCTION-201512012203-525044429):
--   1) Rename dos enums CAMPAIGN_OPEN1/CAMPAIGN_OPEN2 -> OPEN_CAMPAIGN_CALENDAR_DOOR /
--      OPEN_CAMPAIGN_CALENDAR_DOOR_AS_STAFF via UPDATE.
--   2) Headers do enum Outgoing.CAMPAIGN_CALENDAR_DOOR_OPENED para todas as releases.
--   3) DDL das tabelas da feature (campanhas, recompensas por porta, portas abertas).
-- =========================================================================

-- 1) Rename dos headers de incoming (mutation in-place, nunca DELETE+INSERT)
UPDATE `releases_incoming_headers` SET `name` = 'OPEN_CAMPAIGN_CALENDAR_DOOR' WHERE `name` = 'CAMPAIGN_OPEN1';
UPDATE `releases_incoming_headers` SET `name` = 'OPEN_CAMPAIGN_CALENDAR_DOOR_AS_STAFF' WHERE `name` = 'CAMPAIGN_OPEN2';

-- 2) Headers outgoing do CampaignCalendarDoorOpenedMessageEvent
-- NULLs: releases sem a feature (pré-nascimento 2015-12-01) e não cobertas pelos concretos
INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`)
SELECT r.`release_name`, 'CAMPAIGN_CALENDAR_DOOR_OPENED', NULL, NULL
FROM `releases` r
WHERE NOT EXISTS (
    SELECT 1 FROM `releases_outgoing_headers` h
    WHERE h.`release_name` = r.`release_name` AND h.`name` = 'CAMPAIGN_CALENDAR_DOOR_OPENED'
);

-- Concretos validados
INSERT INTO `releases_outgoing_headers` (`release_name`, `name`, `header`, `override_method`) VALUES
    ('RELEASE63-201305221112-286302280', 'CAMPAIGN_CALENDAR_DOOR_OPENED', NULL, 'DISABLED'),
    ('PRODUCTION-201611291003-338511768', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 2551, NULL),
    ('PRODUCTION-202104091531-593308026', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 545, NULL),
    ('PRODUCTION-202111191349-636893688', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1733, NULL),
    ('PRODUCTION-202311200305-397722618', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 926, NULL),
    ('WIN63-202512101753-364126151', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 3174, NULL),
    ('WIN63-202601062255-671620806', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 3765, NULL),
    ('WIN63-202601121721-391685409', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 789, NULL),
    ('WIN63-202601221005-86019430', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 2022, NULL),
    ('WIN63-202602091725-383015954', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1921, NULL),
    ('WIN63-202602131518-521997437', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 3969, NULL),
    ('WIN63-202602181049-129106954', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1496, NULL),
    ('WIN63-202604081729-823620372', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 875, NULL),
    ('WIN63-202608061514-667123007', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 54, NULL),
    ('WIN63-202609021410-356326612', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 2471, NULL),
    ('WIN63-202609021434-290314188', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1749, NULL),
    ('WIN63-202609091217-117204808', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 3339, NULL),
    ('WIN63-202609161723-93809945', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 744, NULL),
    ('MAC63-202608061514-527840553', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 3960, NULL),
    ('MAC63-202609021410-305332595', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1677, NULL),
    ('MAC63-202609021434-949602573', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1797, NULL),
    ('MAC63-202609091217-14311778', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 3452, NULL),
    ('MAC63-202609161723-620992255', 'CAMPAIGN_CALENDAR_DOOR_OPENED', 1537, NULL)
ON DUPLICATE KEY UPDATE `header` = VALUES(`header`), `override_method` = VALUES(`override_method`);

-- 3) DDL da feature
CREATE TABLE IF NOT EXISTS `campaign_calendars` (
  `name` varchar(64) NOT NULL,
  `start_date` date NOT NULL,
  `days` int(11) NOT NULL DEFAULT 24,
  `campaign_image` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;

CREATE TABLE IF NOT EXISTS `campaign_calendar_rewards` (
  `campaign_name` varchar(64) NOT NULL,
  `door_index` int(11) NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `custom_image` varchar(255) DEFAULT NULL,
  `furniture_class_name` varchar(128) NOT NULL,
  PRIMARY KEY (`campaign_name`, `door_index`),
  CONSTRAINT `campaign_calendar_rewards_ibfk_1` FOREIGN KEY (`campaign_name`) REFERENCES `campaign_calendars` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;

CREATE TABLE IF NOT EXISTS `user_campaign_calendar_opens` (
  `user_id` int(11) NOT NULL,
  `campaign_name` varchar(64) NOT NULL,
  `door_index` int(11) NOT NULL,
  `opened_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `campaign_name`, `door_index`),
  CONSTRAINT `user_campaign_calendar_opens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_campaign_calendar_opens_ibfk_2` FOREIGN KEY (`campaign_name`) REFERENCES `campaign_calendars` (`name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
