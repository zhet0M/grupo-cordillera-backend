"""HTTP system tests against the isolated QA Gateway; standard library only."""
import argparse
import datetime as dt
import json
import secrets
import subprocess
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load_qa_env():
    path = ROOT / 'evidence/encargo3/qa.env'
    return dict(line.split('=', 1) for line in path.read_text(encoding='utf-8').splitlines() if line and not line.startswith('#'))


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Api:
    def __init__(self, base_url):
        parsed = urllib.parse.urlsplit(base_url)
        if parsed.scheme != 'http' or parsed.hostname not in ('127.0.0.1', 'localhost') or parsed.port != 18080:
            raise ValueError('Las pruebas solo admiten el Gateway QA local del puerto 18080.')
        self.base_url = base_url.rstrip('/')
        self.calls = []

    def request(self, method, path, body=None, token=None, extra_headers=None):
        headers = {'Content-Type': 'application/json'}
        if token is not None:
            headers['Authorization'] = 'Bearer ' + token
        if extra_headers:
            headers.update(extra_headers)
        request = urllib.request.Request(self.base_url + path,
            data=json.dumps(body).encode() if body is not None else None, headers=headers, method=method)
        # Do not forward tokens through environment proxies or redirects.
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
        started = time.perf_counter()
        try:
            response = opener.open(request, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read()
            status = response.code
            content_type = response.headers.get('Content-Type', '')
            response_headers = {name: response.headers.get(name) for name in (
                'X-Content-Type-Options', 'Cross-Origin-Resource-Policy', 'Cache-Control',
                'Access-Control-Allow-Origin')}
        elapsed = (time.perf_counter() - started) * 1000
        self.calls.append({'method': method, 'path': path, 'status': status,
            'durationMs': round(elapsed, 2), 'responseHeaders': response_headers})
        data = json.loads(raw) if 'application/json' in content_type and raw else raw
        return status, data, content_type


class BackendSystemTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.api = Api(ARGS.base_url)
        cls.env = load_qa_env()
        cls.run_id = secrets.token_hex(5)
        cls.users = {}
        deadline = time.monotonic() + 240
        while time.monotonic() < deadline:
            try:
                status, data, _ = cls.api.request('POST', '/auth/login', {
                    'email': cls.env['BOOTSTRAP_ADMIN_EMAIL'], 'password': cls.env['BOOTSTRAP_ADMIN_PASSWORD']})
                if status == 200:
                    cls.admin = data['token']
                    if all(cls.api.request('GET', '/' + module, token=cls.admin)[0] == 200
                           for module in ('clientes', 'inventario', 'ventas', 'finanzas')):
                        return
            except (OSError, ValueError, KeyError):
                pass
            time.sleep(2)
        raise RuntimeError('El entorno QA no estuvo listo en 240 segundos; revisar logs de los servicios.')

    def usuario(self, role):
        if role in self.users:
            return self.users[role]
        email = f'qa.{self.run_id}.{role.lower()}@grupocordillera.com'
        password = secrets.token_hex(20)
        self.assertEqual(200, self.api.request('POST', '/auth/registro', {
            'username': f'qa-{self.run_id}-{role.lower()}', 'email': email, 'password': password})[0])
        status, users, _ = self.api.request('GET', '/auth/admin/pendientes', token=self.admin)
        self.assertEqual(200, status)
        user = next(user for user in users if user['email'] == email)
        self.assertEqual(200, self.api.request('PUT', f"/auth/admin/usuarios/{user['id']}/aprobar",
            {'rol': role}, self.admin)[0])
        status, data, _ = self.api.request('POST', '/auth/login', {'email': email, 'password': password})
        self.assertEqual(200, status)
        self.assertEqual(role, data['rol'])
        self.users[role] = data['token']
        return data['token']

    def test_01_cuenta_pendiente_no_inicia_sesion(self):
        """SYS-01 RF-1.2 RF-2.1 RB-2: pending accounts must return a controlled 4xx."""
        email = f'pendiente.{self.run_id}@grupocordillera.com'
        password = secrets.token_hex(20)
        status = self.api.request('POST', '/auth/registro', {
            'username': 'pendiente-' + self.run_id, 'email': email, 'password': password})[0]
        self.assertEqual(200, status)
        status, _, _ = self.api.request('POST', '/auth/login', {'email': email, 'password': password})
        self.assertTrue(400 <= status < 500, f'Cuenta pendiente: se esperaba 4xx y se obtuvo {status}')

    def test_02_aprobacion_y_permisos_por_rol(self):
        """SYS-02 RF-2.3 RF-3.3 RB-4 RB-5: roles can read, and cannot write outside their domain."""
        for role in ('ADMIN_VENTAS', 'ADMIN_INVENTARIO', 'ADMIN_FINANZAS', 'ADMIN_CLIENTES', 'EJECUTIVO', 'ANALISTA'):
            token = self.usuario(role)
            with self.subTest(role=role):
                self.assertEqual(200, self.api.request('GET', '/clientes', token=token)[0])
                target = '/inventario' if role == 'ADMIN_CLIENTES' else '/clientes'
                self.assertEqual(403, self.api.request('POST', target, {}, token)[0])
                self.assertEqual(403, self.api.request('GET', '/auth/admin/usuarios', token=token)[0])

    def test_03_venta_actualiza_inventario_cliente_y_finanzas(self):
        """SYS-03 RF-4.3 RF-5.1 RF-5.2 RF-5.3: persistent effects across four domain services."""
        status, customer, _ = self.api.request('POST', '/clientes', {
            'nombre': 'Cliente', 'apellido': 'Prueba QA', 'email': f'cliente.{self.run_id}@example.invalid',
            'telefono': '000000000', 'direccion': 'Direccion ficticia QA'}, self.usuario('ADMIN_CLIENTES'))
        self.assertEqual(200, status)
        status, product, _ = self.api.request('POST', '/inventario', {
            'nombre': 'Producto QA ' + self.run_id, 'categoria': 'TECNOLOGIA', 'precio': 10000,
            'costo': 6000, 'stock': 10, 'stockMinimo': 2, 'sucursal': 'SUC-CENTRAL',
            'estado': 'DISPONIBLE', 'mesesGarantia': 12}, self.usuario('ADMIN_INVENTARIO'))
        self.assertEqual(200, status)
        status, sale, _ = self.api.request('POST', '/ventas', {
            'skuProducto': product['sku'], 'clienteId': customer['id'], 'cantidad': 2,
            'sucursal': 'SUC-CENTRAL', 'canal': 'ECOMMERCE'}, self.usuario('ADMIN_VENTAS'))
        self.assertEqual(200, status)
        self.assertEqual(20000, sale['montoTotal'])
        self.assertEqual('SINCRONIZADO', sale['estadoFinanzas'])
        status, stored_product, _ = self.api.request('GET', '/inventario/sku/' + product['sku'], token=self.admin)
        self.assertEqual(200, status)
        self.assertEqual(8, stored_product['stock'])
        status, stored_customer, _ = self.api.request('GET', f"/clientes/{customer['id']}", token=self.admin)
        self.assertEqual(200, status)
        self.assertEqual(1, stored_customer['cantidadCompras'])
        self.assertEqual(20000, stored_customer['montoAcumulado'])
        status, movements, _ = self.api.request('GET', '/finanzas', token=self.admin)
        self.assertEqual(200, status)
        sale_movements = [m for m in movements if m.get('ventaId') == sale['id']]
        self.assertEqual(1, len(sale_movements))
        self.assertEqual(20000, sale_movements[0]['ingresos'])
        self.assertEqual(12000, sale_movements[0]['costo'])
        self.assertEqual(8000, sale_movements[0]['margen'])

    def test_04_autenticacion_y_rutas_publicas(self):
        """SYS-04 NFR-SEG-4 NFR-SEG-9: unauthenticated requests and public-route lookalikes."""
        for path in ('/clientes', '/auth/admin/usuarios', '/auth/login-extra', '/swagger-ui-malicioso'):
            with self.subTest(path=path):
                self.assertEqual(401, self.api.request('GET', path)[0])
        self.assertEqual(401, self.api.request('GET', '/clientes', token='jwt-alterado')[0])
        self.assertEqual(403, self.api.request('GET', '/nuevo-modulo', token=self.admin)[0])

    def test_05_datos_invalidos_y_recurso_inexistente(self):
        """SYS-05 NFR-SEG-5 RF-4.1: invalid customer input and missing resources."""
        self.assertEqual(400, self.api.request('POST', '/clientes', {}, self.usuario('ADMIN_CLIENTES'))[0])
        self.assertEqual(404, self.api.request('GET', '/clientes/9223372036854775807', token=self.admin)[0])

    def test_06_kpis_alertas_y_reportes_pdf(self):
        """SYS-06 RF-6.2: executive endpoints and PDF signatures through the Gateway."""
        token = self.usuario('ANALISTA')
        for path in ('/kpis', '/alertas/resumen'):
            with self.subTest(path=path):
                self.assertEqual(200, self.api.request('GET', path, token=token)[0])
        for domain in ('ventas', 'inventario', 'finanzas'):
            with self.subTest(report=domain):
                status, pdf, content_type = self.api.request('GET', f'/reportes/{domain}/pdf', token=token)
                self.assertEqual(200, status)
                self.assertIn('application/pdf', content_type)
                self.assertTrue(pdf.startswith(b'%PDF-'))
                self.assertGreater(len(pdf), 100)

    def producto_de_prueba(self):
        status, product, _ = self.api.request('POST', '/inventario', {
            'nombre': 'Producto QA ' + secrets.token_hex(5), 'categoria': 'TECNOLOGIA',
            'precio': 10000, 'costo': 6000, 'stock': 10, 'stockMinimo': 2,
            'sucursal': 'SUC-CENTRAL', 'estado': 'DISPONIBLE'}, self.usuario('ADMIN_INVENTARIO'))
        self.assertEqual(200, status)
        return product

    def test_07_venta_pendiente_y_reproceso_financiero(self):
        """SYS-07 RF-5.3 RF-5.4: stop only QA finances, preserve sale and retry without duplicates."""
        product = self.producto_de_prueba()
        token = self.usuario('ADMIN_VENTAS')
        compose = ['docker', 'compose', '--env-file', str(ROOT / 'evidence/encargo3/qa.env'),
                   '-f', str(ROOT / 'docker-compose.qa.yml')]
        try:
            subprocess.run(compose + ['stop', 'finanzas'], check=True, capture_output=True, timeout=60)
            status, sale, _ = self.api.request('POST', '/ventas', {
                'skuProducto': product['sku'], 'cantidad': 1, 'sucursal': 'SUC-CENTRAL', 'canal': 'POS'}, token)
            self.assertEqual(200, status)
            self.assertEqual('PENDIENTE', sale['estadoFinanzas'])
            self.assertEqual(1, sale['intentosFinanzas'])
            status, pending, _ = self.api.request('GET', '/ventas/finanzas/pendientes', token=token)
            self.assertEqual(200, status)
            self.assertTrue(any(row['id'] == sale['id'] for row in pending))
        finally:
            subprocess.run(compose + ['start', 'finanzas'], check=True, capture_output=True, timeout=60)
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            try:
                if self.api.request('GET', '/finanzas', token=self.admin)[0] == 200:
                    break
            except OSError:
                pass
            time.sleep(2)
        else:
            self.fail('Finanzas QA no volvió a estar disponible en 120 segundos')
        self.assertEqual(200, self.api.request('POST', '/ventas/finanzas/reprocesar', {}, token)[0])
        status, sales, _ = self.api.request('GET', '/ventas', token=token)
        self.assertEqual(200, status)
        self.assertEqual('SINCRONIZADO', next(row for row in sales if row['id'] == sale['id'])['estadoFinanzas'])
        status, movements, _ = self.api.request('GET', '/finanzas', token=self.admin)
        self.assertEqual(200, status)
        self.assertEqual(1, sum(row.get('ventaId') == sale['id'] for row in movements))
        status, stored, _ = self.api.request('GET', '/inventario/sku/' + product['sku'], token=self.admin)
        self.assertEqual(200, status)
        self.assertEqual(9, stored['stock'])

    def test_08_cantidades_invalidas_no_modifican_stock(self):
        """SYS-08 NFR-SEG-5 RF-5.2: zero/negative quantities must fail before changing data."""
        product = self.producto_de_prueba()
        self.assertEqual(400, self.api.request('PUT', '/inventario/descontar/' + product['sku'],
            {'cantidad': -1}, self.usuario('ADMIN_INVENTARIO'))[0])
        for quantity in (0, -1):
            self.assertEqual(400, self.api.request('POST', '/ventas', {
                'skuProducto': product['sku'], 'cantidad': quantity, 'sucursal': 'SUC-CENTRAL',
                'canal': 'POS'}, self.usuario('ADMIN_VENTAS'))[0])
        status, stored, _ = self.api.request('GET', '/inventario/sku/' + product['sku'], token=self.admin)
        self.assertEqual(200, status)
        self.assertEqual(10, stored['stock'])


    def test_09_cabeceras_y_cors(self):
        """SYS-09 NFR-SEG-5 NFR-SEG-6: security headers on success/denial and allowed CORS preflight."""
        token = self.usuario('ANALISTA')
        for method, auth, expected in [('GET', token, 200), ('GET', None, 401), ('POST', token, 403)]:
            with self.subTest(method=method, expected=expected):
                self.assertEqual(expected, self.api.request(method, '/clientes', token=auth)[0])
                headers = self.api.calls[-1]['responseHeaders']
                self.assertEqual('nosniff', headers['X-Content-Type-Options'])
                self.assertEqual('same-origin', headers['Cross-Origin-Resource-Policy'])
                self.assertEqual('no-store', headers['Cache-Control'])
        self.assertEqual(200, self.api.request('OPTIONS', '/clientes', extra_headers={
            'Origin': 'http://localhost:4200', 'Access-Control-Request-Method': 'GET'})[0])
        self.assertEqual('http://localhost:4200', self.api.calls[-1]['responseHeaders']['Access-Control-Allow-Origin'])

    def test_10_diagnostico_de_alertas_no_publicado(self):
        """SYS-10 NFR-SEG-6: retired debug endpoint cannot be invoked through the Gateway."""
        self.assertEqual(404, self.api.request('GET', '/alertas/test-inventario', token=self.usuario('ANALISTA'))[0])


class RecordedResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.records = {}

    def startTest(self, test):
        super().startTest(test)
        self.records[test.id()] = {'case': test.id().split('.')[-1], 'description': test.shortDescription(), 'status': 'RUNNING'}

    def addSuccess(self, test):
        super().addSuccess(test)
        self.records[test.id()]['status'] = 'PASSED'

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self.records[test.id()]['status'] = 'FAILED'

    def addError(self, test, err):
        super().addError(test, err)
        self.records.setdefault(test.id(), {'case': test.id(), 'description': 'Setup or execution error'})['status'] = 'ERROR'

    def addSubTest(self, test, subtest, err):
        super().addSubTest(test, subtest, err)
        if err is not None:
            self.records[test.id()]['status'] = 'FAILED'


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://127.0.0.1:18080')
    parser.add_argument('--case', help='Nombre exacto de un método de prueba para una reverificación dirigida')
    ARGS = parser.parse_args()
    started = dt.datetime.now(dt.timezone.utc)
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(BackendSystemTests)
    if ARGS.case:
        suite = unittest.TestSuite(test for test in suite if test._testMethodName == ARGS.case)
        if suite.countTestCases() == 0:
            raise SystemExit('Caso desconocido; no se ejecutaron pruebas.')
    result = unittest.TextTestRunner(verbosity=2, resultclass=RecordedResult).run(suite)
    output = ROOT / 'evidence/encargo3/system' / started.strftime('%Y%m%dT%H%M%S%fZ')
    output.mkdir(parents=True)
    report = {'startedUtc': started.isoformat(), 'finishedUtc': dt.datetime.now(dt.timezone.utc).isoformat(),
        'successful': result.wasSuccessful(), 'testMethodsRun': result.testsRun, 'cases': list(result.records.values()),
        'calls': getattr(getattr(BackendSystemTests, 'api', None), 'calls', []),
        'scope': 'Real HTTP Gateway, nine Java services and PostgreSQL; no frontend. Credentials and response bodies excluded.'}
    (output / 'results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('Evidencia:', output.relative_to(ROOT))
    raise SystemExit(0 if result.wasSuccessful() else 1)
