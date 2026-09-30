/*M!999999\- enable the sandbox mode */ 
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `achievements` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `achievement_group_id` int(11) NOT NULL,
  `level` int(11) NOT NULL,
  `reward_activity_points` int(11) NOT NULL,
  `reward_achievement_points` int(11) NOT NULL,
  `progress_requirement` int(11) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `achievement_group_id_category_level` (`achievement_group_id`,`level`),
  KEY `achievement_group_id` (`achievement_group_id`),
  CONSTRAINT `achievements_ibfk_2` FOREIGN KEY (`achievement_group_id`) REFERENCES `achievements_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6137 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `achievements_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `category` enum('crafting','identity','explore','music','social','games','room_builder','pets','tutorial','crackables','trading','collectibles','') NOT NULL,
  `badge_append_level` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=100229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `camera_pictures` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_id` int(11) NOT NULL COMMENT 'The User that created the picture',
  `file_name` varchar(100) NOT NULL COMMENT 'The name of the picture saved on the server',
  `reports` int(11) NOT NULL DEFAULT 0 COMMENT 'How many times this picture was reported',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `file_name` (`file_name`),
  CONSTRAINT `camera_pictures_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Camera Pictures';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_club_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `club_type` enum('habbo_club','builders_club') NOT NULL DEFAULT 'habbo_club',
  `months` int(11) NOT NULL,
  `credits` int(11) NOT NULL,
  `points` int(11) NOT NULL,
  `points_type` int(11) NOT NULL,
  `items_limit` int(11) NOT NULL DEFAULT 0,
  `giftable` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_id` (`item_id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_deals` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_name` varchar(100) NOT NULL,
  `amount` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `item_name` (`item_name`),
  CONSTRAINT `catalog_deals_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `page_id` int(11) NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `order_num` int(11) NOT NULL DEFAULT 1,
  `deal_id` int(11) DEFAULT NULL,
  `catalog_name` varchar(100) NOT NULL,
  `badge` varchar(100) NOT NULL DEFAULT '',
  `cost_credits` int(11) NOT NULL DEFAULT 0,
  `cost_pixels` int(11) NOT NULL DEFAULT 0,
  `cost_vip` int(11) NOT NULL DEFAULT 0,
  `amount` int(11) NOT NULL DEFAULT 1,
  `club_only` tinyint(1) NOT NULL DEFAULT 0,
  `limited_sells` int(11) NOT NULL DEFAULT 0,
  `limited_stack` int(11) NOT NULL DEFAULT 0,
  `offer_active` tinyint(1) NOT NULL DEFAULT 1,
  `extra_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT json_object() CHECK (json_valid(`extra_data`)),
  PRIMARY KEY (`id`),
  KEY `page_id` (`page_id`),
  KEY `item_name` (`item_name`),
  KEY `deal_id` (`deal_id`),
  KEY `order_num` (`order_num`),
  CONSTRAINT `catalog_items_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `catalog_items_ibfk_2` FOREIGN KEY (`page_id`) REFERENCES `catalog_pages` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `catalog_items_ibfk_3` FOREIGN KEY (`deal_id`) REFERENCES `catalog_deals` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13669 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_marketplace_developments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_name` varchar(100) NOT NULL,
  `time` datetime NOT NULL DEFAULT current_timestamp(),
  `credits_request` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `item_name` (`item_name`),
  CONSTRAINT `catalog_marketplace_developments_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_marketplace_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `state` int(11) NOT NULL DEFAULT 1,
  `credits_request` int(11) NOT NULL,
  `credits_request_total` int(11) NOT NULL,
  `time` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `item_name` (`item_name`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `catalog_marketplace_offers_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `catalog_marketplace_offers_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_pages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_id` int(11) NOT NULL DEFAULT -1,
  `name` varchar(100) NOT NULL,
  `code_name` varchar(100) NOT NULL,
  `icon_image` int(11) NOT NULL DEFAULT 1,
  `visible` tinyint(1) NOT NULL DEFAULT 1,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `min_rank` int(11) NOT NULL DEFAULT 1,
  `club_only` tinyint(1) NOT NULL DEFAULT 0,
  `order_num` int(11) NOT NULL DEFAULT 1,
  `page_layout` varchar(100) NOT NULL DEFAULT 'default_3x3',
  `page_headline` varchar(200) NOT NULL DEFAULT '',
  `page_teaser` varchar(100) NOT NULL DEFAULT '',
  `page_special` text NOT NULL DEFAULT '',
  `page_text1` text NOT NULL DEFAULT '',
  `page_text2` text NOT NULL DEFAULT '',
  `page_text_details` text NOT NULL DEFAULT '',
  `page_text_teaser` text NOT NULL DEFAULT '',
  `page_link_description` text NOT NULL DEFAULT '',
  `page_link_pagename` text NOT NULL DEFAULT '',
  `custom_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '{}' CHECK (json_valid(`custom_data`)),
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`),
  KEY `order_num` (`order_num`),
  CONSTRAINT `catalog_pages_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `catalog_pages` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=404 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_recycler` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_name` varchar(100) NOT NULL,
  `level` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_name` (`item_name`),
  CONSTRAINT `catalog_recycler_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_targeted_offer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `enabled` tinyint(1) NOT NULL DEFAULT 0,
  `identifier` varchar(100) NOT NULL,
  `credits` int(11) NOT NULL,
  `points` int(11) NOT NULL,
  `points_type` int(11) NOT NULL,
  `amount` int(11) NOT NULL,
  `offer_expire` datetime NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` varchar(100) NOT NULL,
  `large_image` varchar(100) NOT NULL,
  `icon_image` varchar(100) NOT NULL,
  `images` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalog_vouchers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(100) NOT NULL,
  `credits` int(11) NOT NULL,
  `pixels` int(11) NOT NULL,
  `vip_points` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chest_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `chest_item_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `lock_state` int(11) NOT NULL DEFAULT 0,
  `transaction_id` bigint(20) NOT NULL DEFAULT 0,
  `added_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chest_items_item` (`item_id`),
  KEY `idx_chest_items_chest` (`chest_item_id`),
  CONSTRAINT `fk_chest_items_chest` FOREIGN KEY (`chest_item_id`) REFERENCES `chests` (`item_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chest_items_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chest_logs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `chest_item_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `username` varchar(32) NOT NULL,
  `withdraw_furni_count` int(11) NOT NULL DEFAULT 0,
  `deposit_furni_count` int(11) NOT NULL DEFAULT 0,
  `withdraw_coins_count` int(11) NOT NULL DEFAULT 0,
  `deposit_coins_count` int(11) NOT NULL DEFAULT 0,
  `items_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`items_data`)),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_chest_logs_chest` (`chest_item_id`),
  KEY `fk_chest_logs_user` (`user_id`),
  CONSTRAINT `fk_chest_logs_chest` FOREIGN KEY (`chest_item_id`) REFERENCES `chests` (`item_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chest_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `chests` (
  `item_id` int(11) NOT NULL COMMENT 'The Chest RoomItem ID (items.id)',
  `user_id` int(11) NOT NULL COMMENT 'The owner of the chest',
  `name` varchar(64) NOT NULL DEFAULT '',
  `description` varchar(128) NOT NULL DEFAULT '',
  `capacity` int(11) NOT NULL DEFAULT 1000,
  `coins` int(11) NOT NULL DEFAULT 0,
  `is_wired` tinyint(1) NOT NULL DEFAULT 0,
  `locked` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Baús ficam trancados por padrão ao serem colocados',
  `auto_lock` tinyint(1) NOT NULL DEFAULT 0,
  `state_mode` int(11) NOT NULL DEFAULT 0 COMMENT '0 = abre ao observar, 1 = sempre aberto, 2 = sempre fechado, 3 = Wired',
  `preview_mode` int(11) NOT NULL DEFAULT 0 COMMENT 'Pré-visualização dos itens quando aberto (0-7)',
  `preview_amount` int(11) NOT NULL DEFAULT 1,
  `anyone_can_open` tinyint(1) NOT NULL DEFAULT 0,
  `anyone_can_donate` tinyint(1) NOT NULL DEFAULT 0,
  `notification_mode` int(11) NOT NULL DEFAULT 0,
  `notify_full` tinyint(1) NOT NULL DEFAULT 0,
  `notify_donation` tinyint(1) NOT NULL DEFAULT 0,
  `notify_withdraw` tinyint(1) NOT NULL DEFAULT 0,
  `notify_empty` tinyint(1) NOT NULL DEFAULT 0,
  `notify_transaction` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`item_id`),
  KEY `idx_chests_user_id` (`user_id`),
  CONSTRAINT `fk_chests_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chests_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `furnishings` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `item_name` varchar(100) NOT NULL COMMENT 'The Item name',
  `type` enum('s','i','e','h','v','b','r') NOT NULL DEFAULT 's' COMMENT 'The type of the Item (s = floor, i = wallpaper, e = effect, h = HC, v = VIP, b = badge, r = bot)',
  `stack_height` varchar(100) NOT NULL DEFAULT '1' COMMENT 'The stack height of the Item',
  `can_stack` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Can this Item be stacked?',
  `allow_recycle` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Can this Item be recycled?',
  `allow_trade` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Can this Item be traded?',
  `allow_marketplace_sell` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Can this Item be sold on Marketplace?',
  `allow_gift` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Can this Item be gifted?',
  `allow_inventory_stack` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Can this Item be stacked in Inventory?',
  `interaction_type` varchar(100) NOT NULL DEFAULT 'default' COMMENT 'The Interaction type of the Item.',
  `interaction_modes_count` int(11) NOT NULL DEFAULT 1 COMMENT 'How many times this Item can be interacted (change state)',
  `vending_ids` varchar(100) NOT NULL DEFAULT '0' COMMENT 'Which IDs of the drinks that this item (vending machine) can give. Separated by a comma ( , )',
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_name` (`item_name`),
  KEY `interaction_type` (`interaction_type`)
) ENGINE=InnoDB AUTO_INCREMENT=18707 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Furnishings informations';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(100) NOT NULL,
  `badge` varchar(100) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `state` enum('0','1','2') NOT NULL DEFAULT '0',
  `symbol_color` int(11) NOT NULL,
  `background_color` int(11) NOT NULL,
  `only_admin_can_decorate` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_id` (`room_id`),
  KEY `owner_id` (`owner_id`),
  KEY `primary_background_color` (`background_color`),
  KEY `secondary_background_color` (`symbol_color`),
  KEY `name` (`name`),
  KEY `description` (`description`),
  CONSTRAINT `groups_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `groups_ibfk_2` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `groups_ibfk_3` FOREIGN KEY (`background_color`) REFERENCES `groups_badges_background_color` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `groups_ibfk_5` FOREIGN KEY (`symbol_color`) REFERENCES `groups_badges_symbol_color` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_badges_background_color` (
  `id` int(11) NOT NULL,
  `color` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `color` (`color`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_badges_base` (
  `id` int(11) NOT NULL,
  `value1` varchar(100) NOT NULL,
  `value2` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `value1` (`value1`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_badges_base_color` (
  `id` int(11) NOT NULL,
  `color` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `color` (`color`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_badges_symbol` (
  `id` int(11) NOT NULL,
  `value1` varchar(100) NOT NULL,
  `value2` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `value1` (`value1`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_badges_symbol_color` (
  `id` int(11) NOT NULL,
  `color` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `color` (`color`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_members` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `rank` enum('0','1','2') NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `groups_members_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `groups_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `groups_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `groups_requests_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `groups_requests_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `habbicon_collections` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `price_credits` int(11) NOT NULL DEFAULT 0,
  `price_activity_points` int(11) NOT NULL DEFAULT 0,
  `activity_point_type` int(11) NOT NULL DEFAULT 0,
  `reward_habbicon_id` int(11) DEFAULT NULL,
  `reward_state` int(11) NOT NULL DEFAULT 2,
  PRIMARY KEY (`id`),
  KEY `idx_habbicon_collections_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `habbicons` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `collection_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `purchasable` tinyint(1) NOT NULL DEFAULT 1,
  `price_credits` int(11) NOT NULL DEFAULT 0,
  `price_activity_points` int(11) NOT NULL DEFAULT 0,
  `activity_point_type` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_habbicons_collection_id` (`collection_id`),
  KEY `idx_habbicons_enabled` (`enabled`),
  CONSTRAINT `fk_habbicons_collection` FOREIGN KEY (`collection_id`) REFERENCES `habbicon_collections` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `item_name` varchar(100) NOT NULL,
  `extra_data` text NOT NULL,
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` double NOT NULL DEFAULT 0,
  `rot` int(11) NOT NULL DEFAULT 0,
  `wall_pos` varchar(100) NOT NULL DEFAULT '0',
  `is_builders_club` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `room_id` (`room_id`),
  KEY `user_id` (`user_id`),
  KEY `item_name` (`item_name`),
  CONSTRAINT `items_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `items_ibfk_2` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `items_ibfk_3` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1447 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_dimmer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_id` int(11) NOT NULL COMMENT 'The Item ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Is the dimmer enabled?',
  `current_preset` int(11) NOT NULL COMMENT 'Which preset the dimmer is in',
  `preset_one` text NOT NULL COMMENT 'The first preset',
  `preset_two` text NOT NULL COMMENT 'The second preset',
  `preset_three` text NOT NULL COMMENT 'The third preset',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_id` (`item_id`),
  CONSTRAINT `items_dimmer_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Dimmer/Moodlight information';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_gift` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `item_id` int(11) NOT NULL COMMENT 'The Item ID',
  `item_name` varchar(100) NOT NULL COMMENT 'The Item Name of the Gift',
  `amount` int(11) NOT NULL COMMENT 'How many Items this Gift contains',
  `extradata` text NOT NULL COMMENT 'The extradata of the Gift Item',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  KEY `item_id` (`item_id`),
  KEY `item_name` (`item_name`),
  CONSTRAINT `items_gift_ibfk_3` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `items_gift_ibfk_4` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Gift information';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_limited` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `item_id` int(11) NOT NULL COMMENT 'The Item ID',
  `limited_num` int(11) NOT NULL COMMENT 'The number of this limited Item',
  `limited_total` int(11) NOT NULL COMMENT 'The total of limited Items',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_id` (`item_id`),
  CONSTRAINT `items_limited_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Limited informations';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_teleport` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `teleport_one_id` int(11) NOT NULL COMMENT 'The first Teleport ID',
  `teleport_two_id` int(11) NOT NULL COMMENT 'The second Teleport ID',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `teleport_one_id` (`teleport_one_id`),
  UNIQUE KEY `teleport_two_id` (`teleport_two_id`),
  CONSTRAINT `items_teleport_ibfk_1` FOREIGN KEY (`teleport_one_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `items_teleport_ibfk_2` FOREIGN KEY (`teleport_two_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Teleport informations';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `items_wired` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `item_id` int(11) NOT NULL COMMENT 'The Item ID',
  `delay` int(11) NOT NULL DEFAULT 0 COMMENT 'Delay',
  `items` varchar(255) NOT NULL DEFAULT '' COMMENT 'Primary Items (stuffIds)',
  `stuff_ids2` varchar(255) NOT NULL DEFAULT '' COMMENT 'Secondary Items',
  `message` varchar(255) NOT NULL DEFAULT '' COMMENT 'Text box string',
  `options` varchar(255) NOT NULL DEFAULT '' COMMENT 'Options',
  `extradata` text NOT NULL DEFAULT '' COMMENT 'Extra data',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  `is_filter` tinyint(1) NOT NULL DEFAULT 0,
  `is_inverse` tinyint(1) NOT NULL DEFAULT 0,
  `furni_sources` varchar(100) NOT NULL DEFAULT '' COMMENT 'Input sources for Furnis',
  `user_sources` varchar(100) NOT NULL DEFAULT '' COMMENT 'Input sources for Users',
  `variable_ids` varchar(255) NOT NULL DEFAULT '' COMMENT 'Variable IDs associados ao Wired',
  PRIMARY KEY (`id`),
  UNIQUE KEY `item_id` (`item_id`),
  CONSTRAINT `items_wired_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=119 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Wireds informations';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `landing_promos` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `header` varchar(100) NOT NULL,
  `body` varchar(100) NOT NULL,
  `button` varchar(100) NOT NULL,
  `show_button` tinyint(1) NOT NULL DEFAULT 0,
  `button_link` varchar(100) NOT NULL,
  `image` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `landing_reward` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_name` varchar(100) NOT NULL,
  `total_amount` int(11) NOT NULL,
  `random_rewards` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `name` (`item_name`),
  CONSTRAINT `landing_reward_ibfk_1` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `messenger_friendships` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_one_id` int(11) NOT NULL COMMENT 'The User ID',
  `user_two_id` int(11) NOT NULL COMMENT 'The Friend''s User ID',
  `relationship` int(11) NOT NULL DEFAULT 0 COMMENT 'The relationship that the Friend has with the User',
  `group_id` int(11) DEFAULT NULL COMMENT 'The group ID of the Messenger',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_one_id_user_two_id` (`user_one_id`,`user_two_id`),
  UNIQUE KEY `user_two_id_user_one_id` (`user_two_id`,`user_one_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `messenger_friendships_ibfk_1` FOREIGN KEY (`user_one_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `messenger_friendships_ibfk_2` FOREIGN KEY (`user_two_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `messenger_friendships_ibfk_4` FOREIGN KEY (`group_id`) REFERENCES `messenger_groups` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Friendships';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `messenger_groups` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_id` int(11) NOT NULL COMMENT 'The User ID',
  `name` int(11) NOT NULL COMMENT 'The group name',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `messenger_groups_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Messenger Groups';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `messenger_offline_messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `to_id` int(11) NOT NULL,
  `from_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `timestamp` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `to_id` (`to_id`),
  KEY `from_id` (`from_id`),
  CONSTRAINT `messenger_offline_messages_ibfk_2` FOREIGN KEY (`to_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `messenger_offline_messages_ibfk_3` FOREIGN KEY (`from_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `messenger_requests` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `from_id` int(11) NOT NULL,
  `to_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `from_id_to_id` (`from_id`,`to_id`),
  UNIQUE KEY `to_id_from_id` (`to_id`,`from_id`),
  CONSTRAINT `messenger_requests_ibfk_1` FOREIGN KEY (`from_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `messenger_requests_ibfk_2` FOREIGN KEY (`to_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `moderation_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `moderation_chatlogs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `timestamp` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `room_id` (`room_id`),
  FULLTEXT KEY `message` (`message`),
  CONSTRAINT `moderation_chatlogs_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `moderation_chatlogs_ibfk_4` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `moderation_topics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `topic_id` int(11) NOT NULL,
  `consequence` varchar(100) NOT NULL DEFAULT 'mods',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `category_id` (`category_id`),
  KEY `consequence` (`consequence`),
  KEY `topic_id` (`topic_id`),
  CONSTRAINT `moderation_topics_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `moderation_categories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `navigator_event_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `caption` varchar(100) NOT NULL,
  `visible` tinyint(1) NOT NULL DEFAULT 1,
  `min_rank` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `min_rank` (`min_rank`),
  KEY `caption` (`caption`),
  CONSTRAINT `navigator_event_categories_ibfk_1` FOREIGN KEY (`min_rank`) REFERENCES `ranks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `navigator_room_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `caption` varchar(100) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `min_rank` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `min_rank` (`min_rank`),
  KEY `caption` (`caption`),
  CONSTRAINT `navigator_room_categories_ibfk_1` FOREIGN KEY (`min_rank`) REFERENCES `ranks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions_ranks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rank` int(11) NOT NULL,
  `cmd_update_catalogue` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_update_items` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_update_landing` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_givebadge` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_removebadge` tinyint(1) NOT NULL DEFAULT 0,
  `acc_catalog_voucher_full` tinyint(1) NOT NULL DEFAULT 0,
  `acc_server_console` tinyint(1) NOT NULL DEFAULT 0,
  `acc_floor_editor` tinyint(1) NOT NULL DEFAULT 0,
  `acc_any_room_owner` tinyint(1) NOT NULL DEFAULT 0,
  `acc_any_group_admin` tinyint(1) NOT NULL DEFAULT 0,
  `acc_enter_full_room` tinyint(1) NOT NULL DEFAULT 0,
  `acc_mod_tools` tinyint(1) NOT NULL DEFAULT 0,
  `acc_can_use_camera` tinyint(1) NOT NULL DEFAULT 0,
  `acc_can_follow_anybody` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_giveitem` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rank` (`rank`),
  CONSTRAINT `permissions_ranks_ibfk_1` FOREIGN KEY (`rank`) REFERENCES `ranks` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `cmd_update_catalogue` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_update_items` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_update_landing` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_givebadge` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_removebadge` tinyint(1) NOT NULL DEFAULT 0,
  `acc_catalog_voucher_full` tinyint(1) NOT NULL DEFAULT 0,
  `acc_server_console` tinyint(1) NOT NULL DEFAULT 0,
  `acc_floor_editor` tinyint(1) NOT NULL DEFAULT 0,
  `acc_any_room_owner` tinyint(1) NOT NULL DEFAULT 0,
  `acc_any_group_admin` tinyint(1) NOT NULL DEFAULT 0,
  `acc_enter_full_room` tinyint(1) NOT NULL DEFAULT 0,
  `acc_mod_tools` tinyint(1) NOT NULL DEFAULT 0,
  `acc_can_use_camera` tinyint(1) NOT NULL DEFAULT 0,
  `acc_can_follow_anybody` tinyint(1) NOT NULL DEFAULT 0,
  `cmd_giveitem` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `permissions_users_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_breeds` (
  `pet_type` int(11) NOT NULL,
  `breed_id` int(11) NOT NULL,
  `palette_id` int(11) NOT NULL,
  `sellable` tinyint(1) NOT NULL DEFAULT 1,
  `rare` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`pet_type`,`breed_id`,`palette_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `plugins_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `plugin_name` varchar(100) NOT NULL,
  `key_name` varchar(100) NOT NULL,
  `value` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `key_name_plugin_name` (`key_name`,`plugin_name`),
  KEY `plugin_name` (`plugin_name`),
  KEY `value` (`value`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `ranks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `badge` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `releases` (
  `release_name` varchar(100) NOT NULL,
  PRIMARY KEY (`release_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `releases_incoming_headers` (
  `release_name` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `header` int(11) unsigned DEFAULT NULL,
  `override_method` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`release_name`,`name`),
  UNIQUE KEY `release_name_header` (`release_name`,`header`),
  CONSTRAINT `releases_incoming_headers_ibfk_1` FOREIGN KEY (`release_name`) REFERENCES `releases` (`release_name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `releases_incoming_headers_r63a` (
  `name` varchar(100) NOT NULL,
  `header` int(11) unsigned NOT NULL,
  `override_method` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`header`),
  UNIQUE KEY `releases_incoming_headers_r63a_name_uindex` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `releases_outgoing_headers` (
  `release_name` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `header` int(11) unsigned DEFAULT NULL,
  `override_method` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`release_name`,`name`),
  UNIQUE KEY `release_header` (`release_name`,`header`),
  CONSTRAINT `releases_outgoing_headers_ibfk_1` FOREIGN KEY (`release_name`) REFERENCES `releases` (`release_name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `releases_outgoing_headers_r63a` (
  `name` varchar(100) NOT NULL,
  `header` int(11) unsigned NOT NULL,
  `override_method` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`header`),
  UNIQUE KEY `releases_outgoing_headers_r63a_name_uindex` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_type` enum('public','private') NOT NULL DEFAULT 'private',
  `name` varchar(100) NOT NULL DEFAULT 'Room name',
  `owner_id` int(11) NOT NULL,
  `description` varchar(100) NOT NULL,
  `category` int(11) NOT NULL DEFAULT 0,
  `state` enum('open','locked','password','hidden') NOT NULL DEFAULT 'open',
  `trade_state` enum('0','1','2','3') NOT NULL DEFAULT '0',
  `users_max` int(11) NOT NULL DEFAULT 25,
  `model_name` varchar(100) NOT NULL DEFAULT 'model_a',
  `score` int(11) NOT NULL DEFAULT 0,
  `tags` varchar(100) NOT NULL DEFAULT '',
  `password` varchar(100) NOT NULL DEFAULT '',
  `wallpaper` varchar(100) NOT NULL DEFAULT '0.0',
  `floor` varchar(100) NOT NULL DEFAULT '0.0',
  `landscape` varchar(100) NOT NULL DEFAULT '0.0',
  `hide_wall` tinyint(1) NOT NULL DEFAULT 0,
  `wall_thick` int(11) NOT NULL DEFAULT 0,
  `wall_height` int(11) NOT NULL DEFAULT -1,
  `floor_thick` int(11) NOT NULL DEFAULT 0,
  `mute_settings` int(11) NOT NULL DEFAULT 0,
  `ban_settings` int(11) NOT NULL DEFAULT 0,
  `kick_settings` int(11) NOT NULL DEFAULT 0,
  `chat_type` int(11) NOT NULL DEFAULT 0,
  `chat_balloon` int(11) NOT NULL DEFAULT 0,
  `chat_speed` int(11) NOT NULL DEFAULT 0,
  `chat_max_distance` int(11) NOT NULL DEFAULT 14,
  `chat_flood_protection` int(11) NOT NULL DEFAULT 0,
  `allow_pets` tinyint(1) NOT NULL DEFAULT 1,
  `allow_pets_eat` tinyint(1) NOT NULL DEFAULT 0,
  `allow_walk_through` tinyint(1) NOT NULL DEFAULT 0,
  `group_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_id` (`group_id`),
  KEY `owner_id` (`owner_id`),
  CONSTRAINT `rooms_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `rooms_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms_models` (
  `id` varchar(100) NOT NULL,
  `door_x` int(11) NOT NULL,
  `door_y` int(11) NOT NULL,
  `door_z` double NOT NULL,
  `door_dir` int(11) NOT NULL DEFAULT 2,
  `heightmap` text NOT NULL,
  `club_only` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms_models_customs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `door_x` int(11) NOT NULL,
  `door_y` int(11) NOT NULL,
  `door_z` double NOT NULL,
  `door_dir` int(11) NOT NULL DEFAULT 2,
  `heightmap` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_id` (`room_id`),
  CONSTRAINT `rooms_models_customs_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms_rights` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_id_user_id` (`room_id`,`user_id`),
  UNIQUE KEY `user_id_room_id` (`user_id`,`room_id`),
  CONSTRAINT `rooms_rights_ibfk_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `rooms_rights_ibfk_4` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms_word_filter` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `room_id` int(11) NOT NULL,
  `word` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_id_word` (`room_id`,`word`),
  UNIQUE KEY `word_room_id` (`word`,`room_id`),
  CONSTRAINT `rooms_word_filter_ibfk_1` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `sequences` (
  `sequence_name` varchar(64) NOT NULL,
  `next_hi` int(11) NOT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `snowwar_arena_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `arena_id` int(11) NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `x` int(11) NOT NULL,
  `y` int(11) NOT NULL,
  `z` int(11) NOT NULL DEFAULT 0,
  `rot` int(11) NOT NULL DEFAULT 0,
  `extra_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '{}' CHECK (json_valid(`extra_params`)),
  PRIMARY KEY (`id`),
  KEY `idx_arena_id` (`arena_id`),
  CONSTRAINT `fk_snowwar_arena_items_arena` FOREIGN KEY (`arena_id`) REFERENCES `snowwar_arenas` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1533 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `snowwar_arenas` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `width` int(11) NOT NULL DEFAULT 50,
  `height` int(11) NOT NULL DEFAULT 50,
  `heightmap` mediumtext NOT NULL,
  `blue_spawns` varchar(255) NOT NULL DEFAULT '22,9;25,12;26,8;31,14;23,13',
  `red_spawns` varchar(255) NOT NULL DEFAULT '30,43;33,42;38,41;26,42;33,46',
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `account_created` datetime NOT NULL DEFAULT current_timestamp(),
  `realname` varchar(100) NOT NULL DEFAULT '',
  `auth_ticket` varchar(100) DEFAULT NULL,
  `rank` int(11) NOT NULL DEFAULT 1,
  `credits` int(11) NOT NULL DEFAULT 50000,
  `pixels` int(11) NOT NULL DEFAULT 50000,
  `vip_points` int(11) NOT NULL DEFAULT 0,
  `special_points` int(11) NOT NULL DEFAULT 0,
  `figure` varchar(100) NOT NULL DEFAULT 'hr-115-42.hd-190-1.ch-215-62.lg-285-91.sh-290-62',
  `gender` enum('M','F') NOT NULL DEFAULT 'M',
  `motto` varchar(100) NOT NULL DEFAULT '',
  `online` tinyint(1) NOT NULL DEFAULT 0,
  `ip_last` varchar(100) NOT NULL DEFAULT '',
  `ip_reg` varchar(100) NOT NULL DEFAULT '',
  `home_room` int(11) NOT NULL DEFAULT 0,
  `vip` tinyint(1) NOT NULL DEFAULT 0,
  `talent_status` enum('normal','citizenship','helper') NOT NULL DEFAULT 'normal',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `mail` (`email`),
  UNIQUE KEY `auth_ticket` (`auth_ticket`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_achievements` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `achievement_group_id` int(11) NOT NULL,
  `level` int(11) NOT NULL,
  `progress` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_achievement_group_id` (`user_id`,`achievement_group_id`),
  KEY `achievement_group_id` (`achievement_group_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `users_achievements_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `users_achievements_ibfk_4` FOREIGN KEY (`achievement_group_id`) REFERENCES `achievements_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_badges` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_id` int(11) NOT NULL COMMENT 'The User ID',
  `code` varchar(100) NOT NULL COMMENT 'The badge code',
  `slot` int(11) NOT NULL DEFAULT 0 COMMENT 'The slot the badge is in',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_code` (`user_id`,`code`),
  CONSTRAINT `users_badges_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=250 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the User Badges';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_clothing` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `item_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_item_name` (`user_id`,`item_name`),
  UNIQUE KEY `item_name_user_id` (`item_name`,`user_id`),
  CONSTRAINT `users_clothing_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `users_clothing_ibfk_2` FOREIGN KEY (`item_name`) REFERENCES `furnishings` (`item_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_currencies` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `type` int(11) NOT NULL DEFAULT 1,
  `points` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `users_currencies_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1163 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_favorites` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_room_id` (`user_id`,`room_id`),
  UNIQUE KEY `room_id_user_id` (`room_id`,`user_id`),
  CONSTRAINT `users_favorites_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `users_favorites_ibfk_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_habbicon_recent` (
  `user_id` int(11) NOT NULL,
  `position` int(11) NOT NULL,
  `habbicon_id` int(11) NOT NULL,
  PRIMARY KEY (`user_id`,`position`),
  UNIQUE KEY `uk_users_habbicon_recent_item` (`user_id`,`habbicon_id`),
  KEY `fk_users_habbicon_recent_habbicon` (`habbicon_id`),
  CONSTRAINT `fk_users_habbicon_recent_habbicon` FOREIGN KEY (`habbicon_id`) REFERENCES `habbicons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_users_habbicon_recent_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_habbicons` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `habbicon_id` int(11) NOT NULL,
  `state` int(11) NOT NULL DEFAULT 2,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_habbicons_user_habbicon` (`user_id`,`habbicon_id`),
  KEY `idx_users_habbicons_user_id` (`user_id`),
  KEY `idx_users_habbicons_habbicon_id` (`habbicon_id`),
  CONSTRAINT `fk_users_habbicons_habbicon` FOREIGN KEY (`habbicon_id`) REFERENCES `habbicons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_users_habbicons_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_ips` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_id` int(11) NOT NULL COMMENT 'The user ID',
  `ip` varchar(100) NOT NULL COMMENT 'The IP address',
  `internal` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'If the IP is an internal IP',
  `country_code` varchar(100) NOT NULL COMMENT 'The country code',
  `country` varchar(100) NOT NULL COMMENT 'The country',
  `region_code` varchar(100) NOT NULL COMMENT 'The region code',
  `region` varchar(100) NOT NULL COMMENT 'The region',
  `timezone` varchar(100) NOT NULL COMMENT 'The timezone',
  `latitude` double NOT NULL COMMENT 'The latitude',
  `longitude` double NOT NULL COMMENT 'The longitude',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_ip` (`user_id`,`ip`),
  KEY `timezone` (`timezone`),
  KEY `country_code_country` (`country_code`,`country`),
  KEY `region_code_region` (`region_code`,`region`),
  CONSTRAINT `users_ips_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the IPs of the User';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_logins` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `date` datetime NOT NULL DEFAULT current_timestamp(),
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `date` (`date`),
  CONSTRAINT `users_logins_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_pets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `room_id` int(11) DEFAULT NULL,
  `name` varchar(25) NOT NULL DEFAULT 'Pet',
  `type` int(11) NOT NULL,
  `race` int(11) NOT NULL,
  `color` varchar(6) NOT NULL,
  `experience` int(11) NOT NULL DEFAULT 0,
  `energy` int(11) NOT NULL DEFAULT 100,
  `happiness` int(11) NOT NULL DEFAULT 100,
  `hunger` int(11) NOT NULL DEFAULT 0,
  `thirst` int(11) NOT NULL DEFAULT 0,
  `respect` int(11) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `x` int(11) NOT NULL DEFAULT 0,
  `y` int(11) NOT NULL DEFAULT 0,
  `z` double NOT NULL DEFAULT 0,
  `rot` int(11) NOT NULL DEFAULT 0,
  `extra_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT json_object() CHECK (json_valid(`extra_data`)),
  PRIMARY KEY (`id`),
  KEY `idx_users_pets_room_id` (`room_id`),
  KEY `idx_users_pets_user_id` (`user_id`),
  CONSTRAINT `fk_users_pets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_preferences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `volume` varchar(100) NOT NULL DEFAULT '100,100,100',
  `prefer_old_chat` tinyint(1) NOT NULL DEFAULT 0,
  `ignore_room_invite` tinyint(1) NOT NULL DEFAULT 0,
  `disable_camera_follow` tinyint(1) NOT NULL DEFAULT 0,
  `navigator_x` int(11) NOT NULL DEFAULT 0,
  `navigator_y` int(11) NOT NULL DEFAULT 0,
  `navigator_width` int(11) NOT NULL DEFAULT 580,
  `navigator_height` int(11) NOT NULL DEFAULT 600,
  `hide_in_room` tinyint(1) NOT NULL DEFAULT 0,
  `hide_online` tinyint(1) NOT NULL DEFAULT 0,
  `block_new_friends` tinyint(1) NOT NULL DEFAULT 0,
  `chat_color` int(11) NOT NULL DEFAULT 0,
  `friend_bar_open` tinyint(1) NOT NULL DEFAULT 1,
  `friend_stream_enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `users_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_snowwar_stats` (
  `user_id` int(11) NOT NULL,
  `total_score` int(11) NOT NULL DEFAULT 0,
  `weekly_score` int(11) NOT NULL DEFAULT 0,
  `games_played` int(11) NOT NULL DEFAULT 0,
  `weekly_games_played` int(11) NOT NULL DEFAULT 0,
  `skill_level` int(11) NOT NULL DEFAULT 1,
  `last_week_number` int(11) NOT NULL DEFAULT 0,
  `last_year` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id`),
  KEY `idx_total_score` (`total_score`),
  KEY `idx_weekly_score` (`weekly_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_stats` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `last_online` datetime NOT NULL DEFAULT current_timestamp(),
  `online_seconds` int(11) unsigned NOT NULL DEFAULT 0,
  `room_visits` int(10) unsigned NOT NULL DEFAULT 0,
  `respect` int(11) unsigned NOT NULL DEFAULT 0,
  `gifts_given` int(11) unsigned NOT NULL DEFAULT 0,
  `gifts_received` int(11) unsigned NOT NULL DEFAULT 0,
  `daily_respect_points` int(11) unsigned NOT NULL DEFAULT 3,
  `daily_pet_respect_points` int(11) unsigned NOT NULL DEFAULT 3,
  `daily_competition_votes` int(11) unsigned NOT NULL DEFAULT 3,
  `achievement_score` int(11) unsigned NOT NULL DEFAULT 0,
  `quest_id` int(11) unsigned NOT NULL DEFAULT 0,
  `quest_progress` int(11) unsigned NOT NULL DEFAULT 0,
  `favorite_group` int(11) DEFAULT NULL,
  `tickets_answered` int(11) unsigned NOT NULL DEFAULT 0,
  `marketplace_tickets` int(11) unsigned NOT NULL DEFAULT 0,
  `credits_last_update` datetime NOT NULL DEFAULT current_timestamp(),
  `respect_last_update` datetime NOT NULL DEFAULT current_timestamp(),
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `favorite_group` (`favorite_group`),
  CONSTRAINT `users_stats_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `users_stats_ibfk_2` FOREIGN KEY (`favorite_group`) REFERENCES `groups` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_subscriptions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `club_type` enum('habbo_club','builders_club') NOT NULL DEFAULT 'habbo_club',
  `activated` datetime DEFAULT current_timestamp(),
  `expire` datetime DEFAULT current_timestamp(),
  `items_limit` int(11) NOT NULL DEFAULT 100,
  `items_used` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_club_type` (`user_id`,`club_type`),
  CONSTRAINT `users_subscriptions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_tags` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `tag` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_tag` (`user_id`,`tag`),
  CONSTRAINT `users_tags_ibfk_4` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_unique_ids` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_id` int(11) NOT NULL COMMENT 'The User that holds this unique ID',
  `unique_id` varchar(100) NOT NULL COMMENT 'The unique ID',
  `os_information` varchar(100) NOT NULL COMMENT 'The extra information given by the client',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'Created at',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_unique_id` (`user_id`,`unique_id`),
  CONSTRAINT `users_unique_ids_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Unique IDs of the User';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_wardrobe` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'A unique identifier ',
  `user_id` int(11) NOT NULL COMMENT 'The User ID',
  `slot_id` int(11) NOT NULL COMMENT 'The slot the clothes is in the Wardrobe',
  `figure` varchar(100) NOT NULL COMMENT 'The figure of the clothes',
  `gender` enum('M','F') NOT NULL COMMENT 'The gender of the clothes',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_slot_id` (`user_id`,`slot_id`),
  CONSTRAINT `users_wardrobe_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=COMPRESSED COMMENT='Table to hold the Clothes in the Wardrobe of the User';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `wired_variables` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `variable_id` varchar(255) NOT NULL,
  `variable_name` varchar(255) NOT NULL,
  `variable_type` int(11) NOT NULL,
  `availability_type` int(11) NOT NULL,
  `owner_id` int(11) NOT NULL,
  `owner_type` enum('ROOM','USER','FURNI','GLOBAL') NOT NULL,
  `value` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_var` (`variable_id`,`owner_type`,`owner_id`),
  KEY `idx_owner` (`owner_type`,`owner_id`),
  KEY `idx_variable` (`variable_id`,`owner_type`,`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
