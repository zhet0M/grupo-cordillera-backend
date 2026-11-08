"""Generate Maven SBOMs and scan them locally with a pinned official Trivy image."""
import argparse
import collections
import datetime as dt
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
MODULES = ('authentication', 'clientes', 'finanzas', 'ventas', 'inventario', 'alertas', 'kpis', 'reportes', 'api-gateway')
IMAGE = 'aquasec/trivy@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb'
PLUGIN = 'org.cyclonedx:cyclonedx-maven-plugin:2.9.3:makeBom'


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--maven-command')
    parser.add_argument('--local-repository')
    parser.add_argument('--reuse-db', action='store_true', help='Reuse an existing vulnerability DB for a before/after comparison')
    args = parser.parse_args()
    run = ROOT / 'evidence/encargo3/security/dependencies' / dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    run.mkdir(parents=True)
    results = []
    db_ready = args.reuse_db
    docker = ['docker', 'run', '--rm', '--mount', 'type=bind,src=' + str(run) + ',dst=/work',
        '--mount', 'type=volume,src=cordillera-encargo3-trivy-cache,dst=/root/.cache/trivy', IMAGE]
    for module in MODULES:
        target = run / module
        target.mkdir()
        directory = ROOT / module
        command = args.maven_command or str(directory / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))
        parameters = [command, '-B', '-ntp', PLUGIN, '-DoutputFormat=json', '-DschemaVersion=1.6',
            '-DoutputDirectory=' + str(target), '-DincludeTestScope=false', '-DincludeProvidedScope=false',
            '-Dcyclonedx.skipAttach=true']
        if args.local_repository:
            parameters.append('-Dmaven.repo.local=' + args.local_repository)
        print('Inventariando dependencias:', module, flush=True)
        with (target / 'sbom.log').open('w', encoding='utf-8') as log:
            sbom = subprocess.run(parameters, cwd=directory, stdout=log, stderr=subprocess.STDOUT, timeout=600)
        row = {'module': module, 'status': 'ERROR', 'sbomExitCode': sbom.returncode}
        if sbom.returncode == 0 and (target / 'bom.json').exists():
            inventory = json.loads((target / 'bom.json').read_text(encoding='utf-8'))
            row['components'] = len(inventory.get('components', []))
            scan_parameters = docker + ['sbom', '--scanners', 'vuln', '--timeout', '10m',
                '--format', 'json', '--output', '/work/' + module + '/vulnerabilities.json']
            if db_ready:
                scan_parameters += ['--skip-db-update']
            scan_parameters += ['/work/' + module + '/bom.json']
            print('Buscando vulnerabilidades:', module, flush=True)
            with (target / 'trivy.log').open('w', encoding='utf-8') as log:
                scan = subprocess.run(scan_parameters, stdout=log, stderr=subprocess.STDOUT, timeout=660)
            row['scannerExitCode'] = scan.returncode
            if scan.returncode == 0 and (target / 'vulnerabilities.json').exists():
                db_ready = True
                report = json.loads((target / 'vulnerabilities.json').read_text(encoding='utf-8'))
                findings = [finding for group in report.get('Results', []) for finding in group.get('Vulnerabilities', [])]
                row.update(status='ANALYZED', findings=len(findings),
                    severities=dict(collections.Counter(finding.get('Severity', 'UNKNOWN') for finding in findings)))
        results.append(row)
        (run / 'summary.json').write_text(json.dumps({'image': IMAGE, 'sbomPlugin': PLUGIN,
            'reuseExistingDatabase': args.reuse_db,
            'scope': 'Resolved compile/runtime dependency graph; excludes test/provided scopes. Third-party SBOM identification and vulnerability applicability require review.',
            'modules': results}, indent=2), encoding='utf-8')
        print(module, row['status'], row.get('severities', {}), flush=True)
    with (run / 'scanner-version.txt').open('w', encoding='utf-8') as log:
        subprocess.run(docker + ['--version'], stdout=log, stderr=subprocess.STDOUT, timeout=60)
    print('Evidencia:', run.relative_to(ROOT))
    raise SystemExit(0 if all(row['status'] == 'ANALYZED' for row in results) else 1)
