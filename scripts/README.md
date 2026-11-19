# Scripts

Utilitários de desenvolvimento local, banco de dados, deploy e stress test do `habbo_r63b_v2`.

## Stack local isolada (`dev_stack.sh` + `dev_swf_proxy.go`)

Sobe toda a stack localmente sem tocar em produção:

1. **MariaDB local** (`habbo-dev-mariadb` via `dev_db.sh`)
2. **Proxy SWF/API leve** ([dev_swf_proxy.go](dev_swf_proxy.go), em `127.0.0.1:8081`):
   - Encaminha `/api/v1/*` (incluindo `/api/v1/banner` da era `supersecret`, `habbo-imaging`, `status`, `admin`) e `/api/camera/*` para o emulador local (`web_port`, ex.: `127.0.0.1:30003`).
   - Serve `/crossdomain.xml` permissivo para Flash/Ruffle.
   - Reescreve `https://swf.habbo.rwx.ovh` → `http://127.0.0.1:8081` em `external_variables` e `external_override_variables`, injetando `hotelview.banner.url=http://127.0.0.1:8081/api/v1/banner`.
   - Faz proxy somente-leitura do restante (`/gordon/`, `/c_images/`, XMLs) para o upstream.
3. **Emulador local** (`launcher/build/distributions/launcher-0.1.0-SNAPSHOT.jar`).
4. **CMS local** (`CMS_DIR`, padrão `/home/jomp16/Downloads/Habbo/cms`), usando uma config isolada gerada em `build/dev-stack/cms.config.local.json`.

### Trava de segurança

O `dev_stack.sh` **aborta** se o `config.yaml` na raiz contiver `dynamic_dns`, evitando que o emulador local faça SSH e altere o `/etc/hosts` da `nginx.local.rwx.ovh` (`2804:4418:1102:1::103`).

### Comandos

```bash
./scripts/dev_stack.sh setup                 # Garante DB, compila proxy/emulador/CMS e gera config local
./scripts/dev_stack.sh start                 # Faz setup e sobe proxy, emulador e CMS em background
./scripts/dev_stack.sh status                # Status do MariaDB e dos processos
./scripts/dev_stack.sh logs [proxy|emulator|cms]
./scripts/dev_stack.sh stop                  # Para proxy, emulador e CMS (mantém o MariaDB)
```

Variáveis opcionais: `CMS_DIR`, `DEV_PROXY_ADDR` (padrão `127.0.0.1:8081`), `DEV_UPSTREAM_SWF` (padrão `https://swf.habbo.rwx.ovh`), `FORCE_BUILD=1`. Logs, PIDs e configs geradas ficam em `build/dev-stack/`.

---

## Banco de dados local (`dev_db.sh`)

Gerencia o container Docker `habbo-dev-mariadb` (`mariadb:13.0`) em `127.0.0.1:3306` (`habbixed_r63b_v2`, `root` / `habbo`):

```bash
./scripts/dev_db.sh sync             # Baixa dump completo de prod e restaura no local
./scripts/dev_db.sh sync --baseline  # Apenas estrutura + tabelas estáticas (catálogo/mobis/headers)
./scripts/dev_db.sh reset            # Recria a partir do último dump em build/dev-db/
./scripts/dev_db.sh up               # Inicia o container local
./scripts/dev_db.sh down             # Para e REMOVE o container: o banco local é apagado (não tem volume)
./scripts/dev_db.sh status           # Status e contagens
./scripts/dev_db.sh cli              # Abre o client mariadb no banco local
```

Para só parar o banco sem perder os dados: `docker stop habbo-dev-mariadb` (e `./scripts/dev_db.sh up` para voltar).

### Usuários de smoke test (`dev_smoke_users.sh`)

Um dump de produção não traz os usuários com que o smoke test dos clientes PIXI loga (`smoke-all.mjs` do
habbo-as3-transpiler, `SMOKE_USERS=4,5,6,7`). Depois de `sync`/`reset`:

```bash
./scripts/dev_smoke_users.sh                 # smoke1..smoke6 (ids 4..9), rank 1, quarto inicial 14
SMOKE_COUNT=8 SMOKE_HOME_ROOM=11 ./scripts/dev_smoke_users.sh
```

Idempotente; só toca no container local. O login do smoke é por token da CMS, a senha não é usada.

---

## Deploy em produção (`deploy.sh`)

Compila o projeto, sincroniza JARs/plugins para `/opt/habbo/`, gera `deploy/config.prod.yaml` (removendo `dynamic_dns` e forçando `debug: false`) e reinicia o serviço `habbo.service`:

```bash
DEPLOY_TARGET=root@2804:4418:1102:1::103 ./scripts/deploy.sh
```

Flags disponíveis: `--skip-build`, `--sync-camera`, `--restart-only`, `--status`, `--tail [N]`.

---

## Stress tests (`stress/`)

Scripts de carga e bots efêmeros em memória para testes de desempenho.
