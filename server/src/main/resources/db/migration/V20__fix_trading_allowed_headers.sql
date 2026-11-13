-- =========================================================================
-- V20: Correção de headers invertidos de TradingOtherNotAllowedEvent
-- e TradingYouAreNotAllowedEvent nas releases multi-release (pós-shuffle/AIR)
-- =========================================================================

-- 1) Anula temporariamente os headers das duas mensagens para evitar violação
-- da constraint UNIQUE (release_name, header) durante a troca de valores
UPDATE `releases_outgoing_headers`
SET `header` = NULL
WHERE `name` IN ('TRADING_OTHER_NOT_ALLOWED', 'TRADING_YOU_ARE_NOT_ALLOWED');

-- 2) Aplica os headers corretos para cada release
-- MAC63-202608061514-527840553
UPDATE `releases_outgoing_headers`
SET `header`          = 384,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202608061514-527840553'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 5,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202608061514-527840553'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202609021410-305332595
UPDATE `releases_outgoing_headers`
SET `header`          = 2370,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609021410-305332595'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 194,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609021410-305332595'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202609021434-949602573
UPDATE `releases_outgoing_headers`
SET `header`          = 278,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609021434-949602573'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1157,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609021434-949602573'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202609091217-14311778
UPDATE `releases_outgoing_headers`
SET `header`          = 302,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609091217-14311778'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 820,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609091217-14311778'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202609161723-620992255
UPDATE `releases_outgoing_headers`
SET `header`          = 3888,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609161723-620992255'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 558,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202609161723-620992255'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202610121709-910676369
UPDATE `releases_outgoing_headers`
SET `header`          = 2912,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610121709-910676369'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1543,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610121709-910676369'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202610151745-513993601
UPDATE `releases_outgoing_headers`
SET `header`          = 2470,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610151745-513993601'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2771,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610151745-513993601'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202610191521-951500803
UPDATE `releases_outgoing_headers`
SET `header`          = 679,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610191521-951500803'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 699,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610191521-951500803'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202610201032-784031895
UPDATE `releases_outgoing_headers`
SET `header`          = 1543,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610201032-784031895'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3479,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610201032-784031895'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202610271046-110342586
UPDATE `releases_outgoing_headers`
SET `header`          = 2054,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610271046-110342586'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2739,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202610271046-110342586'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202611031222-355997562
UPDATE `releases_outgoing_headers`
SET `header`          = 3441,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202611031222-355997562'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3456,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202611031222-355997562'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- MAC63-202611041140-63291706
UPDATE `releases_outgoing_headers`
SET `header`          = 2883,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202611041140-63291706'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1051,
    `override_method` = NULL
WHERE `release_name` = 'MAC63-202611041140-63291706'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- PRODUCTION-201611291003-338511768
UPDATE `releases_outgoing_headers`
SET `header`          = 1254,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-201611291003-338511768'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3058,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-201611291003-338511768'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- PRODUCTION-202104091531-593308026
UPDATE `releases_outgoing_headers`
SET `header`          = 2314,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-202104091531-593308026'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1704,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-202104091531-593308026'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- PRODUCTION-202111191349-636893688
UPDATE `releases_outgoing_headers`
SET `header`          = 3602,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-202111191349-636893688'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3467,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-202111191349-636893688'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- PRODUCTION-202311200305-397722618
UPDATE `releases_outgoing_headers`
SET `header`          = 3539,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-202311200305-397722618'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 434,
    `override_method` = NULL
WHERE `release_name` = 'PRODUCTION-202311200305-397722618'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- RELEASE63-201207040943-767851649
UPDATE `releases_outgoing_headers`
SET `header`          = 1261,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 286,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201207040943-767851649'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- RELEASE63-201305221112-286302280
UPDATE `releases_outgoing_headers`
SET `header`          = 1437,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 909,
    `override_method` = NULL
WHERE `release_name` = 'RELEASE63-201305221112-286302280'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202512101753-364126151
UPDATE `releases_outgoing_headers`
SET `header`          = 1959,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202512101753-364126151'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 634,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202512101753-364126151'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202601062255-671620806
UPDATE `releases_outgoing_headers`
SET `header`          = 2399,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202601062255-671620806'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2218,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202601062255-671620806'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202601121721-391685409
UPDATE `releases_outgoing_headers`
SET `header`          = 949,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202601121721-391685409'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1116,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202601121721-391685409'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202601221005-86019430
UPDATE `releases_outgoing_headers`
SET `header`          = 3171,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202601221005-86019430'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2794,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202601221005-86019430'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202602091725-383015954
UPDATE `releases_outgoing_headers`
SET `header`          = 3840,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202602091725-383015954'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 16,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202602091725-383015954'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202602131518-521997437
UPDATE `releases_outgoing_headers`
SET `header`          = 241,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202602131518-521997437'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 724,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202602131518-521997437'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202602181049-129106954
UPDATE `releases_outgoing_headers`
SET `header`          = 1277,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202602181049-129106954'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3254,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202602181049-129106954'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202604081729-823620372
UPDATE `releases_outgoing_headers`
SET `header`          = 3676,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202604081729-823620372'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3538,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202604081729-823620372'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202608061514-667123007
UPDATE `releases_outgoing_headers`
SET `header`          = 1447,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202608061514-667123007'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1141,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202608061514-667123007'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202609021410-356326612
UPDATE `releases_outgoing_headers`
SET `header`          = 2729,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609021410-356326612'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1333,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609021410-356326612'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202609021434-290314188
UPDATE `releases_outgoing_headers`
SET `header`          = 52,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609021434-290314188'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2787,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609021434-290314188'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202609091217-117204808
UPDATE `releases_outgoing_headers`
SET `header`          = 303,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609091217-117204808'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2695,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609091217-117204808'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202609161723-93809945
UPDATE `releases_outgoing_headers`
SET `header`          = 481,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609161723-93809945'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 529,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202609161723-93809945'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202610121709-785561319
UPDATE `releases_outgoing_headers`
SET `header`          = 1059,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3357,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610121709-785561319'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202610151745-318700621
UPDATE `releases_outgoing_headers`
SET `header`          = 3973,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610151745-318700621'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 3334,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610151745-318700621'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202610191521-923172707
UPDATE `releases_outgoing_headers`
SET `header`          = 3630,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610191521-923172707'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 191,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610191521-923172707'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202610201032-107964540
UPDATE `releases_outgoing_headers`
SET `header`          = 1616,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610201032-107964540'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 1143,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610201032-107964540'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

/*
 * Copyright (C) 2015-2026 jomp16 <root@rwx.ovh>
 *
 * This file is part of habbo_r63b_v2.
 *
 * habbo_r63b_v2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * habbo_r63b_v2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with habbo_r63b_v2. If not, see <http://www.gnu.org/licenses/>.
 */

-- WIN63-202610271147-806594867
UPDATE `releases_outgoing_headers`
SET `header`          = 307,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610271147-806594867'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2256,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202610271147-806594867'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202611031222-591562852
UPDATE `releases_outgoing_headers`
SET `header`          = 2067,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202611031222-591562852'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2966,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202611031222-591562852'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';

-- WIN63-202611041140-327405090
UPDATE `releases_outgoing_headers`
SET `header`          = 1140,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202611041140-327405090'
  AND `name` = 'TRADING_OTHER_NOT_ALLOWED';
UPDATE `releases_outgoing_headers`
SET `header`          = 2623,
    `override_method` = NULL
WHERE `release_name` = 'WIN63-202611041140-327405090'
  AND `name` = 'TRADING_YOU_ARE_NOT_ALLOWED';
