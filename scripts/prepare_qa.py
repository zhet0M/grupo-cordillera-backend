"""Create random credentials for the isolated local QA environment, without printing them."""
from pathlib import Path
import secrets

root = Path(__file__).resolve().parents[1]
destination = root / 'evidence' / 'encargo3' / 'qa.env'
if destination.exists():
    raise SystemExit('El archivo QA ya existe; se conserva para no cambiar claves de una base existente.')
destination.parent.mkdir(parents=True, exist_ok=True)
values = {
    'QA_DB_PASSWORD': secrets.token_hex(24),
    'JWT_SECRET': secrets.token_hex(48),
    'BOOTSTRAP_ADMIN_ENABLED': 'true',
    'BOOTSTRAP_ADMIN_EMAIL': 'qa.bootstrap@grupocordillera.com',
    'BOOTSTRAP_ADMIN_USERNAME': 'qa-bootstrap',
    'BOOTSTRAP_ADMIN_PASSWORD': secrets.token_hex(24),
}
destination.write_text(''.join(f'{key}={value}\n' for key, value in values.items()), encoding='utf-8')
print('Credenciales QA creadas en evidence/encargo3/qa.env; no compartir ni subir a Git.')
