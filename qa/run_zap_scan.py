"""Bounded authenticated ZAP API scan against nine read operations in local QA."""
import collections
import datetime as dt
import json
from pathlib import Path
import secrets
import subprocess

from test_system import Api, ROOT, load_qa_env

TAG = 'ghcr.io/zaproxy/zaproxy:stable'
PATHS = ('/clientes', '/inventario', '/ventas', '/finanzas', '/kpis', '/alertas/resumen',
         '/reportes/ventas/pdf', '/reportes/inventario/pdf', '/reportes/finanzas/pdf')


def require_status(response, expected):
    if response[0] != expected:
        raise RuntimeError('Precondición HTTP no satisfecha: se obtuvo ' + str(response[0]))
    return response[1]


if __name__ == '__main__':
    run = ROOT / 'evidence/encargo3/security/zap' / dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    private = run / 'private'
    private.mkdir(parents=True)
    image_digests = json.loads(subprocess.check_output([
        'docker', 'image', 'inspect', TAG, '--format', '{{json .RepoDigests}}'], text=True))
    image = next(digest for digest in image_digests if digest.startswith('ghcr.io/zaproxy/zaproxy@'))
    api = Api('http://127.0.0.1:18080')
    env = load_qa_env()
    admin = require_status(api.request('POST', '/auth/login', {
        'email': env['BOOTSTRAP_ADMIN_EMAIL'], 'password': env['BOOTSTRAP_ADMIN_PASSWORD']}), 200)['token']
    suffix = secrets.token_hex(5)
    email = 'zap.' + suffix + '@grupocordillera.com'
    password = secrets.token_hex(20)
    require_status(api.request('POST', '/auth/registro', {'username': 'zap-' + suffix,
        'email': email, 'password': password}), 200)
    users = require_status(api.request('GET', '/auth/admin/pendientes', token=admin), 200)
    user_id = next(user['id'] for user in users if user['email'] == email)
    require_status(api.request('PUT', f'/auth/admin/usuarios/{user_id}/aprobar', {'rol': 'ANALISTA'}, admin), 200)
    token = require_status(api.request('POST', '/auth/login', {'email': email, 'password': password}), 200)['token']
    paths = {}
    probes = []
    for path in PATHS:
        response = api.request('GET', path, token=token)
        require_status(response, 200)
        probes.append({'path': path, 'status': response[0], 'contentType': response[2]})
        paths[path] = {'get': {'operationId': 'get_' + path.strip('/').replace('/', '_'),
            'responses': {'200': {'description': 'Successful QA response'}}}}
    spec = {'openapi': '3.0.3', 'info': {'title': 'Grupo Cordillera QA read operations', 'version': '1.0',
        'description': 'Explicit limited scope based on controllers and HTTP probes, not a full Swagger export.'},
        'servers': [{'url': 'http://api-gateway:8080'}], 'paths': paths,
        'components': {'securitySchemes': {'bearerAuth': {'type': 'http', 'scheme': 'bearer', 'bearerFormat': 'JWT'}}},
        'security': [{'bearerAuth': []}]}
    (private / 'scope.json').write_text(json.dumps(spec, indent=2), encoding='utf-8')
    (private / 'auth.env').write_text('ZAP_AUTH_HEADER_VALUE=Bearer ' + token +
        '\nZAP_AUTH_HEADER=Authorization\nZAP_AUTH_HEADER_SITE=api-gateway\nJAVA_TOOL_OPTIONS=-Xmx768m\n', encoding='utf-8')
    command = ['docker', 'run', '--rm', '--network', 'cordillera-encargo3-qa_entry',
        '--env-file', str(private / 'auth.env'), '--mount', 'type=bind,src=' + str(private) + ',dst=/zap/wrk',
        image, 'zap-api-scan.py', '-t', '/zap/wrk/scope.json', '-f', 'openapi',
        '-r', 'report.html', '-J', 'report.json', '-w', 'report.md', '-T', '5',
        '-z', '-config ascan.maxScanDurationInMins=3 -config ascan.maxRuleDurationInMins=1 -config ascan.threadPerHost=2']
    print('ZAP: nueve rutas de lectura, usuario ANALISTA y objetivo exclusivo Gateway QA.', flush=True)
    with (private / 'scan.log').open('w', encoding='utf-8') as log:
        scan = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=900)
    for name in ('report.html', 'report.json', 'report.md', 'scan.log'):
        source = private / name
        if source.exists():
            text = source.read_text(encoding='utf-8', errors='replace')
            for value in (token, admin, password, env['BOOTSTRAP_ADMIN_PASSWORD'], env['JWT_SECRET']):
                text = text.replace(value, '[REDACTED-QA-CREDENTIAL]')
            (run / name).write_text(text, encoding='utf-8')
    report_path = run / 'report.json'
    alerts = []
    if report_path.exists():
        report = json.loads(report_path.read_text(encoding='utf-8'))
        alerts = [alert for site in report.get('site', []) for alert in site.get('alerts', [])]
    summary = {'image': image, 'exitCode': scan.returncode,
        'status': 'ANALYZED' if scan.returncode in (0, 1, 2) and report_path.exists() else 'ERROR',
        'probes': probes, 'role': 'ANALISTA', 'alertTypes': len(alerts),
        'riskCounts': dict(collections.Counter(alert.get('riskdesc', 'UNKNOWN') for alert in alerts)),
        'scope': 'Nine explicitly enumerated GET operations via the QA Gateway; active scan limited to three minutes.',
        'limitations': 'Does not cover write flows, all roles, all input vectors, business logic, frontend XSS or production TLS. API scan exit code 2 means warnings, not an execution failure.',
        'sharing': 'Use only the redacted reports at this directory level. The private subdirectory contains live QA credentials and raw reports.'}
    (run / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
    print('Resultado:', summary['status'], 'tipos de alerta:', len(alerts), 'codigo ZAP:', scan.returncode)
    print('Evidencia saneada:', run.relative_to(ROOT))
    raise SystemExit(0 if summary['status'] == 'ANALYZED' else 1)
