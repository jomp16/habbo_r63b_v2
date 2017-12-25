INSERT INTO `groups` (`name`, `description`, `badge`, `owner_id`, `room_id`, `state`, `symbol_color`, `background_color`, `only_admin_can_decorate`)
VALUES (
  :name,
  :description,
  :badge,
  :owner_id,
  :room_id,
  :state,
  :symbol_color,
  :background_color,
  :only_admin_can_decorate
)