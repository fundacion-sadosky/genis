#!/usr/bin/env python3
import http.server
import socketserver
import json
from pathlib import Path

PORT = 9000

class LoginHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/' or self.path == '/login':
            self.send_response(200)
            self.send_header('Content-type', 'text/html; charset=utf-8')
            self.end_headers()
            
            # Servir la página de login desde el archivo
            login_page = Path('/home/cdiaz/Descargas/genis/app/views/login.scala.html').read_text()
            # Remover directivas Twirl
            login_page = login_page.replace('@()', '')
            self.wfile.write(login_page.encode('utf-8'))
        else:
            super().do_GET()
    
    def do_POST(self):
        if self.path == '/api/v2/auth/login':
            content_length = int(self.headers['Content-Length'])
            body = self.rfile.read(content_length)
            
            try:
                data = json.loads(body)
                # Simulación de login exitoso
                if data.get('username') and data.get('password'):
                    self.send_response(200)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    response = {
                        'success': True,
                        'token': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test',
                        'user': data.get('username'),
                        'expiresAt': 1735939200,
                        'message': 'Autenticación exitosa'
                    }
                    self.wfile.write(json.dumps(response).encode('utf-8'))
                else:
                    self.send_response(400)
                    self.send_header('Content-type', 'application/json')
                    self.end_headers()
                    self.wfile.write(json.dumps({'error': 'Faltan campos'}).encode('utf-8'))
            except:
                self.send_response(400)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({'error': 'Error en la solicitud'}).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    with socketserver.TCPServer(('', PORT), LoginHandler) as httpd:
        print(f'Servidor en http://localhost:{PORT}')
        print('Presiona Ctrl+C para parar')
        httpd.serve_forever()
