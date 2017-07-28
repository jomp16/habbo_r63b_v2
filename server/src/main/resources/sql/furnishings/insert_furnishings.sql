INSERT INTO `furnishings` (
  `item_name`,
  `type`,
  `stack_height`,
  `can_stack`,
  `allow_recycle`,
  `allow_trade`,
  `allow_marketplace_sell`,
  `allow_gift`,
  `allow_inventory_stack`,
  `interaction_type`,
  `interaction_modes_count`,
  `vending_ids`
)
VALUES (
  :item_name,
  :type,
  :stack_height,
  :can_stack,
  :allow_recycle,
  :allow_trade,
  :allow_marketplace_sell,
  :allow_gift,
  :allow_inventory_stack,
  :interaction_type,
  :interaction_modes_count,
  :vending_ids
)