"""Analyze all nine modules against the dedicated local SonarQube laboratory."""
import argparse
import base64
import datetime as dt
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
HOST = 'http://127.0.0.1:9001'
MODULES = ('authentication', 'clientes', 'finanzas', 'ventas', 'inventario', 'alertas', 'kpis', 'reportes', 'api-gateway')
SCANNER = 'org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar'
BASE = ROOT / 'evidence/encargo3/security/sonar'
BASE.mkdir(parents=True, exist_ok=True)
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def api(path, password=None, form=None):
    headers = {}
    if password:
        headers['Authorization'] = 'Basic ' + base64.b64encode(('admin:' + password).encode()).decode()
    data = urllib.parse.urlencode(form).encode() if form is not None else None
    request = urllib.request.Request(HOST + path, data=data, headers=headers)
    with OPENER.open(request, timeout=30) as response:
        raw = response.read()
    return json.loads(raw) if raw else {}


def wait_ready():
    deadline = time.monotonic() + 300
    while time.monotonic() < deadline:
        try:
            if api('/api/system/status').get('status') == 'UP':
                return
        except (OSError, ValueError):
            pass
        time.sleep(3)
    raise RuntimeError('SonarQube local no estuvo listo en 300 segundos.')


def credentials():
    path = BASE / 'credentials.json'
    if path.exists():
        return json.loads(path.read_text(encoding='utf-8'))
    # These initial credentials belong only to the newly created dedicated instance.
    password = secrets.token_urlsafe(32)
    api('/api/users/change_password', 'admin', {'login': 'admin', 'previousPassword': 'admin', 'password': password})
    # Persist the changed password before the next API operation so initialization can recover.
    config = {'adminPassword': password}
    path.write_text(json.dumps(config), encoding='utf-8')
    return config


def collect(config, project):
    password = config['adminPassword']
    result = {}
    measures = 'bugs,vulnerabilities,security_hotspots,code_smells,coverage,duplicated_lines_density,ncloc,security_rating,reliability_rating,sqale_rating'
    result['measures'] = api('/api/measures/component?' + urllib.parse.urlencode({'component': project, 'metricKeys': measures}), password)
    result['qualityGate'] = api('/api/qualitygates/project_status?' + urllib.parse.urlencode({'projectKey': project}), password)
    for label, endpoint, params, key in [
        ('issues', '/api/issues/search', {'componentKeys': project, 'resolved': 'false', 'ps': 500}, 'issues'),
        ('hotspots', '/api/hotspots/search', {'projectKey': project, 'ps': 500}, 'hotspots')]:
        items = []
        page = 1
        while True:
            payload = api(endpoint + '?' + urllib.parse.urlencode(dict(params, p=page)), password)
            items.extend(payload.get(key, []))
            paging = payload.get('paging', {})
            total = paging.get('total', payload.get('total', 0))
            if page * 500 >= total:
                break
            page += 1
        result[label] = items
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--maven-command')
    parser.add_argument('--local-repository')
    parser.add_argument('--project-version', default='encargo3-2026-11-06')
    args = parser.parse_args()
    wait_ready()
    config = credentials()
    if not config.get('token'):
        config['token'] = api('/api/user_tokens/generate', config['adminPassword'], {
            'name': 'encargo3-local-' + secrets.token_hex(5), 'type': 'GLOBAL_ANALYSIS_TOKEN'})['token']
        (BASE / 'credentials.json').write_text(json.dumps(config), encoding='utf-8')
    run = BASE / dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    run.mkdir()
    results = []
    for module in MODULES:
        project = 'encargo3-' + module
        directory = ROOT / module
        command = args.maven_command or str(directory / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))
        parameters = [command, '-B', '-ntp', SCANNER, '-Dsonar.host.url=' + HOST,
            '-Dsonar.projectKey=' + project, '-Dsonar.projectName=Encargo 3 - ' + module,
            '-Dsonar.projectVersion=' + args.project_version, '-Dsonar.sourceEncoding=UTF-8',
            '-Dsonar.java.source=17', '-Dsonar.scm.disabled=true']
        if args.local_repository:
            parameters.append('-Dmaven.repo.local=' + args.local_repository)
        environment = dict(os.environ, SONAR_TOKEN=config['token'])
        started = time.monotonic()
        print('Analizando', module, flush=True)
        with (run / (module + '.log')).open('w', encoding='utf-8') as log:
            scan = subprocess.run(parameters, cwd=directory, env=environment, stdout=log,
                stderr=subprocess.STDOUT, timeout=600)
        row = {'module': module, 'projectKey': project, 'projectVersion': args.project_version,
            'scannerExitCode': scan.returncode, 'status': 'ERROR'}
        task_file = directory / 'target/sonar/report-task.txt'
        if not task_file.exists():
            task_file = directory / '.scannerwork/report-task.txt'
        if scan.returncode == 0 and task_file.exists():
            task_properties = dict(line.split('=', 1) for line in task_file.read_text(encoding='utf-8').splitlines() if '=' in line)
            task_id = task_properties['ceTaskId']
            deadline = time.monotonic() + 300
            task = {}
            while time.monotonic() < deadline:
                task = api('/api/ce/task?id=' + urllib.parse.quote(task_id), config['adminPassword'])['task']
                if task['status'] in ('SUCCESS', 'FAILED', 'CANCELED'):
                    break
                time.sleep(2)
            row['computeTaskStatus'] = task.get('status')
            if task.get('status') == 'SUCCESS':
                details = collect(config, project)
                (run / (module + '.json')).write_text(json.dumps(details, ensure_ascii=False, indent=2), encoding='utf-8')
                row.update(status='ANALYZED', qualityGate=details['qualityGate']['projectStatus']['status'],
                    issues=len(details['issues']), hotspots=len(details['hotspots']))
        row['durationSeconds'] = round(time.monotonic() - started, 2)
        results.append(row)
        (run / 'summary.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
        print(module, row['status'], 'gate=' + row.get('qualityGate', 'N/A'), flush=True)
    print('Evidencia:', run.relative_to(ROOT))
    raise SystemExit(0 if all(row['status'] == 'ANALYZED' for row in results) else 1)
