"""Dump all six QA databases and restore into new disposable QA databases."""
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import secrets
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
started = datetime.now(timezone.utc)
output = ROOT / 'evidence/encargo3/backup' / started.strftime('%Y%m%dT%H%M%S%fZ')
output.mkdir(parents=True)
base = ['docker', 'compose', '--env-file', str(ROOT / 'evidence/encargo3/qa.env'),
        '-f', str(ROOT / 'docker-compose.qa.yml'), 'exec', '-T', 'postgres']


def command(*args, data=None):
    return subprocess.run(base + list(args), input=data, stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, check=True, timeout=120).stdout


def sql(database, query):
    return command('psql', '-U', 'qa', '-d', database, '-At', '-v', 'ON_ERROR_STOP=1', '-c', query).decode().strip()


def fingerprint(database):
    tables = sql(database, "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename").splitlines()
    result = {}
    for table in tables:
        if not re.fullmatch(r'[a-z_][a-z_0-9]*', table):
            raise ValueError('Nombre de tabla no admitido por la verificación QA')
        query = f'''SELECT count(*) || '|' || COALESCE(md5(string_agg(to_jsonb(t)::text, E'\\n' ORDER BY to_jsonb(t)::text)), md5('')) FROM public."{table}" t'''
        result[table] = sql(database, query)
    return result


results = []
for database in ('bd_auth', 'bd_clientes', 'bd_inventario', 'bd_ventas', 'bd_finanzas', 'bd_alertas'):
    restore_database = 'qa_restore_' + database + '_' + secrets.token_hex(4)
    clock = time.perf_counter()
    row = {'database': database, 'status': 'ERROR'}
    try:
        dump = command('pg_dump', '-U', 'qa', '-Fc', '--no-owner', database)
        (output / (database + '.dump')).write_bytes(dump)
        source_fingerprint = fingerprint(database)
        command('createdb', '-U', 'qa', '-T', 'template0', restore_database)
        command('pg_restore', '-U', 'qa', '--no-owner', '-d', restore_database, data=dump)
        restored_fingerprint = fingerprint(restore_database)
        matches = source_fingerprint == restored_fingerprint
        row.update(status='PASSED' if matches else 'FAILED', dumpBytes=len(dump),
            sourceFingerprint=source_fingerprint, restoredFingerprint=restored_fingerprint)
        if matches:
            # Only the database created above, never the source database, is removed.
            command('dropdb', '-U', 'qa', restore_database)
        else:
            row['retainedRestoreDatabase'] = restore_database
    except (subprocess.SubprocessError, ValueError) as error:
        row['errorType'] = type(error).__name__
        row['retainedRestoreDatabaseIfCreated'] = restore_database
    row['durationSeconds'] = round(time.perf_counter() - clock, 2)
    results.append(row)
    print(database, row['status'], str(row['durationSeconds']) + 's', flush=True)

report = {'startedUtc': started.isoformat(), 'finishedUtc': datetime.now(timezone.utc).isoformat(),
    'databases': results,
    'scope': 'Six logical databases in the isolated QA PostgreSQL instance. Row counts and row-data hashes compared after real restoration; production operations not assessed.',
    'limitations': 'Run in a quiescent QA dataset. It does not verify encrypted off-site retention, role recovery, sequence continuity, point-in-time recovery or a production RPO/RTO.'}
(output / 'results.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print('Evidencia:', output.relative_to(ROOT))
raise SystemExit(0 if all(row['status'] == 'PASSED' for row in results) else 1)
