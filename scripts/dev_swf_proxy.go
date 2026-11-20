// Lightweight local reverse proxy for Habbo dev.
// Splits /api/v1/* and /api/camera/* to the local emulator (plugin_webapp)
// while proxying static SWF/gamedata/c_images from upstream and rewriting
// external_variables / external_override_variables to point back here.
package main

import (
	"bytes"
	"flag"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"
	"time"
)

func main() {
	listenAddr := flag.String("listen", "127.0.0.1:8081", "address to listen on")
	emulatorRaw := flag.String("emulator", "http://127.0.0.1:30003", "local emulator web_port URL")
	upstreamRaw := flag.String("upstream", "https://swf.habbo.rwx.ovh", "upstream SWF/gamedata URL")
	wsAddr := flag.String("ws", "127.0.0.1:35001", "local emulator ws_port host:port (target of /game)")
	flag.Parse()

	emuURL, err := url.Parse(*emulatorRaw)
	if err != nil {
		log.Fatalf("invalid -emulator: %v", err)
	}
	upURL, err := url.Parse(*upstreamRaw)
	if err != nil {
		log.Fatalf("invalid -upstream: %v", err)
	}

	localBase := "http://" + *listenAddr
	upstreamOrigin := upURL.Scheme + "://" + upURL.Host

	emuProxy := httputil.NewSingleHostReverseProxy(emuURL)
	upProxy := httputil.NewSingleHostReverseProxy(upURL)

	// WebSocket do client AIR (?relay=ws://<este proxy>): /game?host=..&port=.. -> WS do emulador local em "/".
	wsProxy := &httputil.ReverseProxy{Director: func(req *http.Request) {
		req.URL.Scheme = "http"
		req.URL.Host = *wsAddr
		req.URL.Path = "/"
		req.URL.RawQuery = ""
		req.Host = *wsAddr
	}}

	origUpDirector := upProxy.Director
	upProxy.Director = func(req *http.Request) {
		origUpDirector(req)
		req.Host = upURL.Host
	}
	upProxy.ModifyResponse = func(resp *http.Response) error {
		if resp.Header.Get("Access-Control-Allow-Origin") == "" {
			resp.Header.Set("Access-Control-Allow-Origin", "*")
		}
		return nil
	}

	httpClient := &http.Client{Timeout: 15 * time.Second}

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		p := r.URL.Path

		// 1. Local emulator API (banner supersecret, habbo-imaging, status, admin, camera)
		if strings.HasPrefix(p, "/api/v1/") || p == "/api/v1" || strings.HasPrefix(p, "/api/camera/") {
			emuProxy.ServeHTTP(w, r)
			return
		}

		// 1b. WebSocket relay for the AIR client
		if p == "/game" {
			wsProxy.ServeHTTP(w, r)
			return
		}

		// 2. Permissive crossdomain.xml for Flash / Ruffle
		if p == "/crossdomain.xml" {
			w.Header().Set("Content-Type", "application/xml; charset=utf-8")
			w.Header().Set("Access-Control-Allow-Origin", "*")
			_, _ = w.Write([]byte(`<?xml version="1.0"?>` + "\n" +
				`<!DOCTYPE cross-domain-policy SYSTEM "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd">` + "\n" +
				`<cross-domain-policy><allow-access-from domain="*" secure="false"/></cross-domain-policy>` + "\n"))
			return
		}

		// 3. Rewrite external_variables / external_override_variables to keep banner & api calls local
		if strings.HasPrefix(p, "/gamedata/external_variables") || strings.HasPrefix(p, "/gamedata/override/external_override_variables") {
			target := *upURL
			target.Path = r.URL.Path
			target.RawQuery = r.URL.RawQuery

			req, err := http.NewRequestWithContext(r.Context(), http.MethodGet, target.String(), nil)
			if err != nil {
				http.Error(w, err.Error(), http.StatusBadGateway)
				return
			}
			req.Header.Set("Accept-Encoding", "identity")

			resp, err := httpClient.Do(req)
			if err != nil {
				http.Error(w, err.Error(), http.StatusBadGateway)
				return
			}
			defer resp.Body.Close()

			body, err := io.ReadAll(resp.Body)
			if err != nil {
				http.Error(w, err.Error(), http.StatusBadGateway)
				return
			}

			body = bytes.ReplaceAll(body, []byte(upstreamOrigin), []byte(localBase))
			if strings.HasPrefix(p, "/gamedata/override/external_override_variables") {
				extra := "\nhotelview.banner.url=" + localBase + "/api/v1/banner\n"
				body = append(body, []byte(extra)...)
			}

			w.Header().Set("Content-Type", "text/plain; charset=utf-8")
			w.Header().Set("Access-Control-Allow-Origin", "*")
			w.WriteHeader(resp.StatusCode)
			_, _ = w.Write(body)
			return
		}

		// 4. Everything else -> upstream SWF/gamedata/c_images (read-only)
		upProxy.ServeHTTP(w, r)
	})

	log.Printf("dev_swf_proxy listening on %s (api -> %s, static -> %s)", localBase, *emulatorRaw, *upstreamRaw)
	if err := http.ListenAndServe(*listenAddr, handler); err != nil {
		log.Fatal(err)
	}
}
