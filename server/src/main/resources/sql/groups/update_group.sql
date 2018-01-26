UPDATE `groups`
SET
  `name`                    = :name,
  `description`             = :description,
  `badge`                   = :badge,
  `state`                   = :state,
  `symbol_color`            = :symbol_color,
  `background_color`        = :background_color,
  `only_admin_can_decorate` = :only_admin_can_decorate
WHERE `id` = :group_id