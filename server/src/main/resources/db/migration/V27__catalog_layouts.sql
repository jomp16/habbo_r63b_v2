-- =========================================================================
-- V27: layouts de pagina do catalogo (Flash, 2009-06 a 2014-01), por faixa de build do client
--  O client monta a pagina a partir do layout `ctlg_<layout_code>` e do pacote CATALOG_PAGE:
--   * IMAGENS: o LocalizationCatalogWidget associa a imagem N do pacote, POR POSICAO, a tabela padrao
--     (ctlg_header_img, ctlg_teaserimg_1, ctlg_special_img, ctlg_teaserimg_2, ctlg_teaserimg_3) e faz
--     findChildByName(nome).tags sem checar null: imagem nao vazia numa posicao que o layout nao tem derruba o
--     update receiver (TypeError ao abrir a pagina; ex.: o HC / club_buy so tem ctlg_header_img).
--   * TEXTOS: a posicao N vira o nome da tabela de textos do layout (custom, senao o padrao de 5 nomes). Quantos
--     o servidor preenche (`text_count`) e o tamanho dessa tabela limitado a 4 (os campos da pagina: text1,
--     text2, details, teaser); sem tabela propria sao 3. Ver CatalogLayouts.texts.
--   * EXISTENCIA: layout que a release nao tem = pagina em branco; o servidor cai em catalog_layout_fallbacks e,
--     por fim, em default_3x3.
--
--  Evidencia (AS3 + Habbo.swf, 1107 releases): as 570 releases de 2009-06-16 a 2013-06-23 usam esse codigo sem
--  checagem de null e a mesma tabela padrao de nomes; a partir de 2014-01-30 ele ganhou a checagem (`if (x && x.tags`),
--  e os layouts passam a vir de fora do SWF. Por isso a tabela cobre so [2009-06-16 13:27, 2014-01-30 16:19): fora
--  disso o servidor manda a pagina como esta. Os layouts foram extraidos dos XML (DefineBinaryData
--  `<layout name="ctlg_*">`) e as tabelas de nomes de textos do AS3 de cada release. Regras do levantamento:
--   - SWF com dois XML do mesmo nome: vale o maior (o outro e uma sub-janela, ex.: ctlg_trophies so com o textInputWidget);
--   - release sem o layout entre duas com o mesmo estado: ruido da ordem dos branches (RELEASE48/49), contado como presente;
--   - camera1/camera2/purse/plasto/plasto_larger/spaces somem do SWF em 2013-03 (carregados de fora): ficam com o
--     ultimo estado conhecido ate 2014-01-30 (pressuposto; se algum mudou, corrigir a linha);
--   - `frontpage*` fica de fora: tem nomes e textos proprios (ctlg_txt1..7, frontpage_promotion), tratado no codigo.
--  available_from/available_until: janela de data de build (mesma convencao de activity_point_types).
-- =========================================================================

CREATE TABLE IF NOT EXISTS `catalog_layouts`
(
    `layout_code`     varchar(64) NOT NULL,
    `available_from`  datetime    NOT NULL,
    `available_until` datetime    NOT NULL,
    `image_elements`  set ('ctlg_header_img','ctlg_teaserimg_1','ctlg_special_img','ctlg_teaserimg_2','ctlg_teaserimg_3') NOT NULL DEFAULT '',
    `text_count`      tinyint(4)  NOT NULL DEFAULT 3,
    PRIMARY KEY (`layout_code`, `available_from`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_520_ci;

-- layout pedido -> layout que o substitui quando a release nao o tem (o antigo mapa fixo do resolveLegacyPresentation)
CREATE TABLE IF NOT EXISTS `catalog_layout_fallbacks`
(
    `layout_code`     varchar(64) NOT NULL,
    `fallback_layout` varchar(64) NOT NULL,
    PRIMARY KEY (`layout_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_520_ci;

INSERT INTO `catalog_layout_fallbacks` (`layout_code`, `fallback_layout`)
VALUES ('pets', 'pets2'),
       ('pets3', 'pets2'),
       ('spaces_new', 'spaces'),
       ('vip_buy', 'club_buy')
ON DUPLICATE KEY UPDATE `fallback_layout` = VALUES(`fallback_layout`);

INSERT INTO `catalog_layouts` (`layout_code`, `available_from`, `available_until`, `image_elements`, `text_count`)
VALUES
       ('badge_display', '2013-03-22 11:06:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('bots', '2009-10-15 01:42:00', '2011-06-01 08:52:00', 'ctlg_header_img', 3),
       ('bots', '2012-05-02 23:03:00', '2012-05-03 23:02:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('bots', '2012-05-03 23:02:00', '2012-05-07 09:35:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('bots', '2012-05-07 09:35:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 4),
       ('camera1', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img,ctlg_teaserimg_2', 2),
       ('camera2', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('cars', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('club1', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 4),
       ('club2', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_teaserimg_2', 4),
       ('club_buy', '2010-04-07 15:50:00', '2014-01-30 16:19:00', 'ctlg_header_img', 3),
       ('collectibles', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img', 2),
       ('default_3x3', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('default_3x3_extrainfo', '2010-11-12 00:41:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('default_3x3_larger', '2012-02-08 23:02:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('guild_custom_furni', '2011-10-03 11:02:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('guild_frontpage', '2011-09-22 23:17:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('info_credits', '2009-06-16 13:27:00', '2009-08-26 14:14:00', 'ctlg_header_img', 4),
       ('info_credits', '2009-08-26 14:14:00', '2014-01-30 16:19:00', '', 4),
       ('info_duckets', '2013-02-21 12:27:00', '2014-01-30 16:19:00', 'ctlg_header_img', 1),
       ('info_loyalty', '2013-04-26 11:01:00', '2014-01-30 16:19:00', 'ctlg_header_img', 1),
       ('info_pixels', '2009-06-16 13:27:00', '2009-08-26 14:14:00', 'ctlg_header_img', 4),
       ('info_pixels', '2009-08-26 14:14:00', '2014-01-30 16:19:00', '', 4),
       ('info_rentables', '2013-02-21 12:27:00', '2014-01-30 16:19:00', 'ctlg_header_img', 4),
       ('infopage1', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('infopage2', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_teaserimg_2', 3),
       ('latest_added', '2011-05-23 23:07:00', '2011-06-06 09:24:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('layout1', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('layout2', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('layout3', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('loading', '2009-06-16 13:27:00', '2014-01-30 16:19:00', '', 3),
       ('loyalty_vip_buy', '2013-06-23 01:29:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('mad_money', '2011-03-15 09:23:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('marketplace', '2009-12-11 12:24:00', '2014-01-30 16:19:00', 'ctlg_header_img', 3),
       ('monkey', '2011-07-08 23:22:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('norares', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('petcustomization', '2011-10-03 11:02:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('pets', '2009-06-16 13:27:00', '2009-07-27 13:18:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('pets', '2011-02-09 23:06:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 4),
       ('pets2', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 4),
       ('pets3', '2012-05-28 15:41:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 4),
       ('pixeldeals', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('pixeleffects', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('pixelrent', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('pixelsdeals', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('plasto', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('plasto_larger', '2012-04-30 13:59:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('presents', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_teaserimg_2', 2),
       ('productpage1', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('productpage2', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img,ctlg_teaserimg_2', 3),
       ('productpage2b', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_teaserimg_2', 3),
       ('productpage3', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_teaserimg_2,ctlg_teaserimg_3', 3),
       ('productpage3b', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_teaserimg_2,ctlg_teaserimg_3', 3),
       ('purse', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 2),
       ('recycler', '2009-06-16 13:27:00', '2009-09-21 12:24:00', 'ctlg_header_img', 3),
       ('recycler', '2009-09-21 12:24:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('recycler_info', '2012-05-02 23:03:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('recycler_prizes', '2009-09-21 12:24:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('roomads', '2012-10-15 11:03:00', '2014-01-30 16:19:00', 'ctlg_header_img', 3),
       ('sold_ltd_items', '2012-04-11 11:28:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('sold_ltd_items_larger', '2012-04-12 10:35:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('soundmachine', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('spaces', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img', 3),
       ('spaces_new', '2011-04-05 13:10:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('top_sellers', '2011-05-23 23:07:00', '2011-06-06 09:24:00', 'ctlg_header_img,ctlg_teaserimg_1,ctlg_special_img', 3),
       ('trophies', '2009-06-16 13:27:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3),
       ('vip_buy', '2011-11-25 11:40:00', '2014-01-30 16:19:00', 'ctlg_header_img,ctlg_teaserimg_1', 3)
ON DUPLICATE KEY UPDATE `available_until` = VALUES(`available_until`),
                        `image_elements` = VALUES(`image_elements`),
                        `text_count` = VALUES(`text_count`);
