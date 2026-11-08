"""Local read-only load measurement; 100 clients are not 100 requests/second."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
from pathlib import Path
import threading

from test_system import Api, ROOT, load_qa_env

env = load_qa_env()
api = Api('http://127.0.0.1:18080')
status, login, _ = api.request('POST', '/auth/login', {
    'email': env['BOOTSTRAP_ADMIN_EMAIL'], 'password': env['BOOTSTRAP_ADMIN_PASSWORD']})
if status != 200:
    raise SystemExit('No se pudo autenticar el usuario QA.')
token = login['token']
started = datetime.now(timezone.utc)
for _ in range(10):
    if api.request('GET', '/clientes', token=token)[0] != 200:
        raise SystemExit('El calentamiento no obtuvo respuestas HTTP 200.')

normal = []
for _ in range(50):
    status = api.request('GET', '/clientes', token=token)[0]
    normal.append({'status': status, 'durationMs': api.calls[-1]['durationMs']})

clients = 100
requests_per_client = 5
barrier = threading.Barrier(clients)
lock = threading.Lock()
active = 0
peak = 0


def worker(_):
    global active, peak
    client = Api('http://127.0.0.1:18080')
    rows = []
    barrier.wait(timeout=60)
    for _ in range(requests_per_client):
        with lock:
            active += 1
            peak = max(peak, active)
        try:
            status = client.request('GET', '/clientes', token=token)[0]
            rows.append({'status': status, 'durationMs': client.calls[-1]['durationMs']})
        except OSError:
            rows.append({'status': 'NETWORK_ERROR', 'durationMs': None})
        finally:
            with lock:
                active -= 1
    return rows


with ThreadPoolExecutor(max_workers=clients) as pool:
    load = [row for group in pool.map(worker, range(clients)) for row in group]


def statistics(rows):
    times = sorted(row['durationMs'] for row in rows if row['durationMs'] is not None)
    return {'samples': len(rows), 'errors': sum(row['status'] != 200 for row in rows),
        'p50Ms': times[len(times) // 2] if times else None,
        'p95Ms': times[min(len(times) - 1, int(len(times) * .95))] if times else None,
        'maxMs': max(times) if times else None}


normal_stats = statistics(normal)
load_stats = statistics(load)
normal_pass = normal_stats['errors'] == 0 and normal_stats['maxMs'] < 300
load_pass = load_stats['errors'] == 0 and len(load) == clients * requests_per_client and peak == clients
report = {'startedUtc': started.isoformat(), 'finishedUtc': datetime.now(timezone.utc).isoformat(),
    'endpoint': 'GET /clientes through QA Gateway', 'warmupRequests': 10,
    'normal': normal_stats, 'normalUnder300ms': normal_pass,
    'load': load_stats, 'clients': clients, 'requestsPerClient': requests_per_client,
    'peakClientRequestsInFlight': peak, 'loadWithoutErrors': load_pass,
    'normalSamples': normal, 'loadSamples': load,
    'limitations': 'Short local read-only run with small fictitious dataset and one load generator. Does not certify all CRUD operations, sustained capacity or production performance.'}
output = ROOT / 'evidence/encargo3/load' / started.strftime('%Y%m%dT%H%M%S%fZ')
output.mkdir(parents=True)
(output / 'results.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps({'normal': normal_stats, 'load': load_stats, 'peakClientRequestsInFlight': peak,
    'normalUnder300ms': normal_pass, 'loadWithoutErrors': load_pass}, indent=2))
print('Evidencia:', output.relative_to(ROOT))
raise SystemExit(0 if normal_pass and load_pass else 1)
