SELECT 'incoming' AS `type`, `release_name`, `name`, `header`, `override_method`
FROM `releases_incoming_headers`
UNION ALL
SELECT 'outgoing' AS `type`, `release_name`, `name`, `header`, `override_method`
FROM `releases_outgoing_headers`