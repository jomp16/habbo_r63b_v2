SELECT `id` as `teleport_id`, `room_id`
FROM `items`
WHERE `id` in (:teleport_ids)
