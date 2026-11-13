# habbo_r63b_v2
> A Habbo R63B server made with Kotlin and Java. In development.

[![Build Status](https://drone.rwx.ovh/api/badges/Habbo/habbo_r63b_v2/status.svg)](https://drone.rwx.ovh/Habbo/habbo_r63b_v2)

## Supported releases

See [SUPPORTED_RELEASES.md](SUPPORTED_RELEASES.md) file.

## Download latest artifact
[Get the latest artifact from GitLab CI server](https://git.rwx.ovh/Habbo/habbo_r63b_v2/-/jobs/artifacts/master/download?job=build)

## How to build

`git clone https://git.rwx.ovh/Habbo/habbo_r63b_v2.git`

`git submodule init`

`git submodule update`

`./gradlew clean assembleDist`

## Local development stack

To run the full stack locally without affecting production (MariaDB in Docker, lightweight SWF/API split proxy, emulator, and CMS):

1. Ensure `config.yaml` points to the local DB (`127.0.0.1:3306`, `habbixed_r63b_v2`) and does **not** define `dynamic_dns` (`dev_stack.sh` aborts if `dynamic_dns` is present).
2. Populate the local database (first time):
   ```bash
   ./scripts/dev_db.sh sync --baseline
   ```
3. Start the local stack (`dev_swf_proxy` on `127.0.0.1:8081`, emulator, and CMS):
   ```bash
   ./scripts/dev_stack.sh start
   ```
   Useful commands: `setup`, `start`, `stop`, `status`, `logs [proxy|emulator|cms]`.
   - `scripts/dev_swf_proxy.go` splits `/api/v1/*` (including `/api/v1/banner` for `supersecret` clients) and `/api/camera/*` to the local emulator (`web_port`), rewrites `external_variables` / `external_override_variables` to keep `hotelview.banner.url` local, and proxies static SWF/gamedata read-only from upstream.
   - `CMS_DIR` overrides the CMS path (defaults to `/home/jomp16/Downloads/Habbo/cms`), and generated configs/logs/PIDs live in `build/dev-stack/`.

## Contributing

Pull requests and stars are always welcome. For bugs and feature requests, [please create an issue](https://git.rwx.ovh/Habbo/habbo_r63b_v2/issues)

## Author

**jomp16**

+ https://github.com/jomp16
+ https://git.rwx.ovh/jomp16

## License

Copyright (C) 2015-2017 [jomp16](https://git.rwx.ovh).

Licensed under the GPLv3 and LGPLv3 license. See [LICENSE](LICENSE)
