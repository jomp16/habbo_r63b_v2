#!/usr/bin/env python3
"""
generate_headers.py — gera a migration de headers multi-release para uma feature nova
do emulador habbo_r63b_v2, a partir dos caches Sulek do habboair-patcher e suporte R63A.

REGRAS codificadas:
  - releases_incoming_headers  <- cache .messages.outgoing (Composers, client->server)
  - releases_outgoing_headers  <- cache .messages.incoming (Events,   server->client)
  - releases_incoming_headers_r63a / releases_outgoing_headers_r63a <- pré-shuffle R63A
  - Release só recebe IDs concretos se TODOS os pacotes da feature estiverem nomeados
    no cache dela; caso contrário recebe NULL (nunca inventar ID).
  - Prefixo de plataforma no cache (windows-/osx-) é removido para casar com o banco.
  - Idempotente e sem warnings 1062:
    - releases com ON DUPLICATE KEY UPDATE (evita avisos no Flyway).
    - NULLs com INSERT IGNORE + WHERE NOT EXISTS.
    - Concretos agrupados em um único INSERT com ON DUPLICATE KEY UPDATE.
    - R63A com ON DUPLICATE KEY UPDATE.
  - Não gera SELECTs comentados de diagnóstico (saída limpa).

Uso:
  python3 generate_headers.py \\
      --feature room_banned_users \\
      --incoming ROOM_BAN_USER=BanUserWithDurationMessageComposer \\
      --incoming ROOM_UNBAN_USER=UnbanUserFromRoomMessageComposer \\
      --incoming-r63a ROOM_BAN_USER=320 \\
      --outgoing ROOM_BANNED_USERS=BannedUsersFromRoomEvent \\
      --outgoing ROOM_USER_UNBANNED=UserUnbannedFromRoomEvent \\
      --patcher-dir ~/IdeaProjects/Habbo/habboair-patcher \\
      --releases-from-config \\
      -o server/src/main/resources/db/migration/V4__create_rooms_bans.sql
"""

import argparse
import glob
import json
import os
import sys
from pathlib import Path


def normalize_release(cache_name: str) -> str:
    # flash-windows-WIN63-... -> WIN63-... ; flash-osx-MAC63-... -> MAC63-...
    stem = cache_name
    if stem.endswith(".json"):
        stem = stem[:-5]
    if stem.startswith("flash-"):
        stem = stem[len("flash-"):]
    for prefix in ("windows-", "osx-"):
        if stem.startswith(prefix):
            return stem[len(prefix):]
    return stem


def load_caches(patcher_dir: str):
    caches = {}
    cache_dir = Path(patcher_dir) / "cache"
    for path in sorted(glob.glob(str(cache_dir / "flash-*.json"))):
        name = normalize_release(os.path.basename(path))
        with open(path) as f:
            caches[name] = json.load(f)
    return caches


def name_index(messages: list) -> dict:
    return {m["name"]: m["id"] for m in messages if "name" in m and "id" in m}


def try_find_r63a_header(enum_name: str, class_name: str, direction: str, timeline_dir: str) -> int | None:
    """Tenta descobrir o header R63A (< 201110) a partir de correlações salvas do habbo-timeline."""
    correlations_dir = Path(timeline_dir) / "correlations"
    candidates = [
        correlations_dir / f"{enum_name}.json",
        correlations_dir / f"{class_name}.json",
    ]
    for path in candidates:
        if not path.is_file():
            continue
        try:
            data = json.loads(path.read_text())
            transitions = data.get("transitions", [])
            for t in transitions:
                date = t.get("date", 0)
                if date < 201110000000:
                    hid = t.get("header_id")
                    if hid is not None and hid > 0:
                        return int(hid)
        except Exception:
            continue
    return None


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--feature", required=True, help="nome da feature (só para o cabeçalho do SQL)")
    ap.add_argument("--incoming", action="append", default=[], metavar="ENUM=ComposerName[:R63A_ID]")
    ap.add_argument("--outgoing", action="append", default=[], metavar="ENUM=EventName[:R63A_ID]")
    ap.add_argument("--incoming-r63a", action="append", default=[], metavar="ENUM=HeaderID")
    ap.add_argument("--outgoing-r63a", action="append", default=[], metavar="ENUM=HeaderID")
    ap.add_argument("--timeline-dir", default="/mnt/DADOS/Downloads/Habbo/data", help="diretório de dados do habbo-timeline")
    ap.add_argument("--patcher-dir", default=os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "habboair-patcher"))
    ap.add_argument("--releases-from-config", action="store_true")
    ap.add_argument("--releases", nargs="+", default=[])
    ap.add_argument("--out", "-o", default="-")
    args = ap.parse_args()

    if not args.incoming and not args.outgoing:
        ap.error("informe ao menos um --incoming ou --outgoing")

    incoming_enums = {}
    outgoing_enums = {}
    incoming_r63a = {}
    outgoing_r63a = {}

    def parse_definitions(items, target_enums, target_r63a, direction: str):
        for item in items:
            if "=" not in item:
                ap.error(f"par inválido: {item} (use ENUM=ClassName ou ENUM=ClassName:R63A_ID)")
            enum, val = item.split("=", 1)
            enum = enum.strip()
            val = val.strip()
            r63a_id = None
            if ":" in val:
                val, r63a_str = val.rsplit(":", 1)
                try:
                    r63a_id = int(r63a_str.strip())
                except ValueError:
                    ap.error(f"header R63A inválido em {item}")
            target_enums[enum] = val.strip()
            if r63a_id is not None:
                target_r63a[enum] = r63a_id
            else:
                auto_id = try_find_r63a_header(enum, val.strip(), direction, args.timeline_dir)
                if auto_id is not None:
                    target_r63a[enum] = auto_id

    parse_definitions(args.incoming, incoming_enums, incoming_r63a, "outgoing")
    parse_definitions(args.outgoing, outgoing_enums, outgoing_r63a, "incoming")

    for pair in args.incoming_r63a:
        if "=" not in pair:
            ap.error(f"par inválido para --incoming-r63a: {pair}")
        enum, hid = pair.split("=", 1)
        incoming_r63a[enum.strip()] = int(hid.strip())

    for pair in args.outgoing_r63a:
        if "=" not in pair:
            ap.error(f"par inválido para --outgoing-r63a: {pair}")
        enum, hid = pair.split("=", 1)
        outgoing_r63a[enum.strip()] = int(hid.strip())

    patcher_dir = os.path.abspath(args.patcher_dir)
    caches = load_caches(patcher_dir)
    if not caches:
        print(f"ERRO: nenhum cache em {patcher_dir}/cache", file=sys.stderr)
        return 1

    # Lista de releases do sistema
    if args.releases:
        releases = list(args.releases)
    elif args.releases_from_config:
        config_path = Path(patcher_dir) / "releases_config.json"
        cfg = json.loads(config_path.read_text())
        releases = [cfg["oldRelease"]] + [r for r in cfg.get("newReleases", []) if r != cfg["oldRelease"]]
    else:
        releases = sorted(caches.keys())

    # Extrai headers por release (só se a feature estiver COMPLETA na release)
    concrete = {}  # release -> (found_incoming, found_outgoing)
    for release in releases:
        data = caches.get(release)
        if data is None:
            continue  # release sem cache -> fica com NULL

        composers = name_index(data["messages"]["outgoing"])  # -> incoming_headers
        events = name_index(data["messages"]["incoming"])     # -> outgoing_headers

        complete = all(n in composers for n in incoming_enums.values()) and \
                   all(n in events for n in outgoing_enums.values())
        if not complete:
            continue

        concrete[release] = (
            {enum: composers[name] for enum, name in incoming_enums.items()},
            {enum: events[name] for enum, name in outgoing_enums.items()},
        )

    # ---------- geração do SQL ----------
    out = sys.stdout if args.out == "-" else open(args.out, "w")
    w = out.write

    w(f"""-- Headers da feature '{args.feature}' - GERADO por generate_headers.py (skill habbo-feature-implement).
--
-- Fonte: caches Sulek de {patcher_dir}/cache
-- Mapeamento:
--   releases_incoming_headers  <- cache .messages.outgoing (composers, client->server)
--   releases_outgoing_headers  <- cache .messages.incoming (events, server->client)
-- Releases sem a feature completa no cache recebem NULL (nunca despachadas).
-- Idempotente (zero warnings 1062 no boot do Flyway).
-- Releases com concretos: {len(concrete)} de {len(releases)}.

""")

    # 1) releases (ON DUPLICATE KEY UPDATE evita warnings 1062 no MariaDB/Flyway)
    w("-- 1) Garante as releases conhecidas\nINSERT INTO `releases` (`release_name`) VALUES\n")
    w(",\n".join(f"    ('{r}')" for r in releases) + "\n")
    w("ON DUPLICATE KEY UPDATE `release_name` = VALUES(`release_name`);\n")

    # 2) NULLs (garante linhas criadas para todas as releases)
    def null_block(table: str, enums: dict):
        if not enums:
            return
        items = list(enums.keys())
        union_query = "\n    UNION ALL\n    ".join(
            f"SELECT '{item}' AS name" if i == 0 else f"SELECT '{item}'"
            for i, item in enumerate(items)
        )
        w(f"""
-- NULLs de {table}
INSERT IGNORE INTO releases_{table} (release_name, name, header, override_method)
SELECT r.release_name, v.name, NULL, NULL
FROM releases r
JOIN (
    {union_query}
) v
WHERE NOT EXISTS (
    SELECT 1 FROM releases_{table} h
    WHERE h.release_name = r.release_name AND h.name = v.name
);
""")

    if incoming_enums:
        null_block("incoming_headers", incoming_enums)
    if outgoing_enums:
        null_block("outgoing_headers", outgoing_enums)

    # 3) Concretos (agrupados em um único batch INSERT com ON DUPLICATE KEY UPDATE)
    for table, enums, key in (("incoming_headers", incoming_enums, 0), ("outgoing_headers", outgoing_enums, 1)):
        if not enums:
            continue
        rows = []
        for release in releases:
            if release not in concrete:
                continue
            found = concrete[release][key]
            for enum in enums:
                rows.append(f"    ('{release}', '{enum}', {found[enum]}, NULL)")
        if rows:
            w(f"\n-- Concretos de {table}\n")
            w(f"INSERT INTO `releases_{table}` (`release_name`, `name`, `header`, `override_method`) VALUES\n")
            w(",\n".join(rows) + "\n")
            w("ON DUPLICATE KEY UPDATE `header` = VALUES(`header`);\n")

    # 4) Headers pré-shuffle R63A (se definidos ou descobertos)
    if incoming_r63a:
        rows = [f"    ('{enum}', {hid}, NULL)" for enum, hid in sorted(incoming_r63a.items())]
        w("\n-- Headers pré-shuffle R63A (incoming)\n")
        w("INSERT INTO `releases_incoming_headers_r63a` (`name`, `header`, `override_method`) VALUES\n")
        w(",\n".join(rows) + "\n")
        w("ON DUPLICATE KEY UPDATE `header` = VALUES(`header`);\n")

    if outgoing_r63a:
        rows = [f"    ('{enum}', {hid}, NULL)" for enum, hid in sorted(outgoing_r63a.items())]
        w("\n-- Headers pré-shuffle R63A (outgoing)\n")
        w("INSERT INTO `releases_outgoing_headers_r63a` (`name`, `header`, `override_method`) VALUES\n")
        w(",\n".join(rows) + "\n")
        w("ON DUPLICATE KEY UPDATE `header` = VALUES(`header`);\n")

    if out is not sys.stdout:
        out.close()
        print(f"Gerado: {args.out}")
        print(f"  releases no sistema: {len(releases)} | com concretos: {len(concrete)}")
        print(f"  incoming enums: {len(incoming_enums)} (R63A: {len(incoming_r63a)})")
        print(f"  outgoing enums: {len(outgoing_enums)} (R63A: {len(outgoing_r63a)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
