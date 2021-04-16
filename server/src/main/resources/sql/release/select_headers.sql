SELECT 'incoming' AS `type`, `release_name`, `name`, `header`, `override_method`
FROM `releases_incoming_headers`
UNION ALL
SELECT 'outgoing' AS `type`, `release_name`, `name`, `header`, `override_method`
FROM `releases_outgoing_headers`
UNION ALL
SELECT 'incoming' AS `type`, 'R63A' AS `release_name`, `name`, `header`, `override_method`
FROM `releases_incoming_headers_r63a`
UNION ALL
SELECT 'outgoing' AS `type`, 'R63A' AS `release_name`, `name`, `header`, `override_method`
FROM `releases_outgoing_headers_r63a`