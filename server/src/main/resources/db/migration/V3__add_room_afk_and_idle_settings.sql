ALTER TABLE `rooms`
  ADD COLUMN `allow_navigator_dynamic_cats` tinyint(1) NOT NULL DEFAULT 1 AFTER `allow_walk_through`,
  ADD COLUMN `leave_on_door_tile_enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `allow_navigator_dynamic_cats`,
  ADD COLUMN `idle_sleep_enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `leave_on_door_tile_enabled`,
  ADD COLUMN `idle_sleep_timeout_seconds` int(11) NOT NULL DEFAULT 1200 AFTER `idle_sleep_enabled`,
  ADD COLUMN `idle_autokick_enabled` tinyint(1) NOT NULL DEFAULT 0 AFTER `idle_sleep_timeout_seconds`,
  ADD COLUMN `idle_autokick_timeout_seconds` int(11) NOT NULL DEFAULT 1800 AFTER `idle_autokick_enabled`,
  ADD COLUMN `mute_all_pets` tinyint(1) NOT NULL DEFAULT 0 AFTER `idle_autokick_timeout_seconds`;

