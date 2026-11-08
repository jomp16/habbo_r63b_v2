#!/usr/bin/env python3
"""Analisa um degrau de carga gravado por scale_run.sh.

Uso: python3 scripts/stress/analyze.py <dir-do-degrau> [--jfr]

Lê, do diretório do degrau, info.txt/client.log/rec.jfr e, da raiz do repositório, stress-test.log (métrica do tick por
sala, escrita pelo emulador com `stress_test: true` + logger ovh.rwx.habbo.stress em info) e gc.log*.
Mostra: resumo do cliente, falhas, tick (rampa e regime), pausas de GC, RSS e (com --jfr) CPU/alocação do emulador.
"""
import collections
import glob
import os
import re
import subprocess
import sys
import time
from pathlib import Path

REPO = str(Path(__file__).resolve().parents[2]) + '/'
JFR = os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-25-openjdk') + '/bin/jfr'
D = sys.argv[1].rstrip('/') + '/'

info = dict(kv.split('=') for kv in open(D + 'info.txt').read().split() if '=' in kv)
st = int(info['start_epoch'])
en = int(info.get('end_epoch', st + 120))
hms = lambda x: time.strftime('%H:%M:%S', time.localtime(x))
num = lambda s: float(s.replace(',', '.'))

print("=== %s | %s bots/sala (x%s salas) | RAM livre antes: %s MB | min durante: %s MB" % (
    os.path.basename(D.rstrip('/')), info.get('per_room'), info.get('rooms', '10'), info.get('avail_before_mb'),
    open(D + 'min_avail_mb').read().strip() if os.path.exists(D + 'min_avail_mb') else '?'))
for flag in ('ABORTED', 'ERROR'):
    if os.path.exists(D + flag):
        print("  !!", flag, open(D + flag).read().strip())

# --- cliente
txt = open(D + 'client.log').read()
m = re.search(r'=== FINAL SUMMARY ===(.*?)Reminder', txt, re.S)
if m:
    s = m.group(1)
    g = lambda k: (re.search(k + r'\s*=\s*(\S+ ?\w*)', s) or [None, '?'])[1]
    print("  cliente: acoes=%s eventos=%s | RTT p50=%s p95=%s max=%s" % (
        g('actions sent'), g('inbound events'), g('latency p50'), g('latency p95'), g('latency max')))
    f = re.search(r'failures \((\d+)\)(.*)', s, re.S)
    print("  falhas:", (f.group(1) + ' ' + ' / '.join(l.strip() for l in f.group(2).splitlines() if 'x ' in l)) if f else "0")

# --- tick (stress-test.log)
pat = re.compile(r'\S+ (\S+) .*room=(\d+) ticks=\d+ proc: avg=([\d,.]+)ms p95=(\d+)ms max=(\d+)ms \| major-interval: avg=([\d,.]+)ms p95=(\d+)ms max=(\d+)ms drift=[-\d,.]+ms \| sched-lag: avg=([\d,.]+)ms p95=(\d+)ms max=(\d+)ms')
rows = []
for l in open(REPO + 'stress-test.log'):
    mm = pat.match(l)
    if mm:
        g_ = mm.groups()
        rows.append((g_[0][:8], int(g_[1]), num(g_[2]), int(g_[3]), int(g_[4]), num(g_[5]), int(g_[6]), int(g_[7]), num(g_[8]), int(g_[9]), int(g_[10])))


def tick(lo, hi, label):
    a, b = hms(st + lo), hms(st + hi)
    w = [r for r in rows if a <= r[0] <= b]
    if not w:
        print("  tick %-12s sem janelas" % label)
        return
    iv = sorted(r[5] for r in w)
    wi = sorted(r[7] for r in w)
    print("  tick %-12s jan=%3d | intervalo avg med %.1f max %.1f | pior intervalo med %d p95 %d max %d | >600ms %d/%d | proc avg max %.1f p95max %d max %d | sched-lag p95max %d max %d" % (
        label, len(w), iv[len(iv) // 2], iv[-1], wi[len(wi) // 2], wi[int(len(wi) * .95)], wi[-1],
        sum(1 for r in w if r[7] > 600), len(w), max(r[2] for r in w), max(r[3] for r in w), max(r[4] for r in w),
        max(r[9] for r in w), max(r[10] for r in w)))


tick(0, 25, 'rampa 0-25s')
tick(30, en - st, 'regime 30s+')

# --- GC no intervalo (gc.log*)
gcs = collections.defaultdict(list)
for gf in sorted(glob.glob(REPO + 'gc.log*')):
    for l in open(gf):
        mm = re.match(r'\[(\d{4}-\d\d-\d\dT(\d\d:\d\d:\d\d))[^\]]*\]\[[^\]]*\] GC\(\d+\) Pause (\w+).*?([\d,.]+)ms\s*$', l)
        if mm and hms(st) <= mm.group(2) <= hms(en):
            gcs[mm.group(3)].append(num(mm.group(4)))
print("  GC no intervalo:", "; ".join("%s n=%d avg=%.1fms max=%.1fms" % (k, len(v), sum(v) / len(v), max(v)) for k, v in gcs.items()) or "nenhum")
if os.path.exists(D + 'emu_rss_kb'):
    print("  RSS emulador: %.0f MB" % (int(open(D + 'emu_rss_kb').read().strip()) / 1024))

# --- JFR
if '--jfr' in sys.argv and os.path.exists(D + 'rec.jfr'):
    def events(ev):
        out = subprocess.run([JFR, 'print', '--events', ev, '--stack-depth', '60', D + 'rec.jfr'], capture_output=True, text=True).stdout
        cur = None
        for l in out.splitlines():
            if l.startswith('jdk.'):
                if cur:
                    yield cur
                cur = {'f': [], 't': None, 'w': 0.0, 'c': None}
            elif cur is not None:
                m1 = re.search(r'sampledThread = "([^"]+)"', l)
                if m1:
                    cur['t'] = re.sub(r'\d+', 'N', m1.group(1))
                m1 = re.search(r'objectClass = (\S+)', l)
                if m1:
                    cur['c'] = m1.group(1)
                m1 = re.search(r'weight = ([\d.,]+) (\w+)', l)
                if m1:
                    cur['w'] = num(m1.group(1)) * {'bytes': 1, 'kB': 1024, 'MB': 1 << 20, 'GB': 1 << 30}.get(m1.group(2), 1)
                m1 = re.match(r'\s+([\w.$<>/]+)\.([\w$<>-]+)\(.*\)\s+line:', l)
                if m1:
                    cur['f'].append(m1.group(1) + '.' + m1.group(2))
        if cur:
            yield cur

    short = lambda k: '.'.join(k.split('.')[-2:])
    ex = list(events('jdk.ExecutionSample'))
    n = max(1, len(ex))
    th = collections.Counter(e['t'] for e in ex)
    selft = collections.Counter(e['f'][0] for e in ex if e['f'])
    inc = collections.Counter()
    for e in ex:
        for k in set(x for x in e['f'] if x.startswith('ovh.rwx')):
            inc[k] += 1
    print("  JFR CPU: %d amostras | threads %s" % (len(ex), dict(th.most_common(3))))
    print("   self:", "; ".join("%.1f%% %s" % (100 * v / n, short(k)) for k, v in selft.most_common(7)))
    print("   incl:", "; ".join("%.1f%% %s" % (100 * v / n, short(k)) for k, v in inc.most_common(14) if 'invokeSuspend' not in k and 'access$' not in k))
    al = list(events('jdk.ObjectAllocationSample'))
    tot = max(1.0, sum(e['w'] for e in al))
    site = collections.Counter()
    cls = collections.Counter()
    for e in al:
        site[next((short(x) for x in e['f'] if x.startswith('ovh.rwx')), 'None')] += e['w']
        cls[e['c']] += e['w']
    print("  JFR alloc: %.1f GB amostrados (~%.0f MB/s)" % (tot / (1 << 30), tot / (1 << 20) / 40))
    print("   classe:", "; ".join("%.0f%% %s" % (100 * v / tot, k.split('.')[-1]) for k, v in cls.most_common(5)))
    print("   origem:", "; ".join("%.0f%% %s" % (100 * v / tot, k) for k, v in site.most_common(6)))
