import os

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

server_code = read_file('server.py')
index_html = read_file('index.html')
firebase_js = read_file('firebase.js') if os.path.exists('firebase.js') else ''
manifest = read_file('manifest.json') if os.path.exists('manifest.json') else ''
sw_js = read_file('sw.js') if os.path.exists('sw.js') else ''

new_app_py = server_code.replace(
    "return send_from_directory('.', 'index.html')",
    "return index_html_content"
).replace(
    "return send_from_directory('.', 'firebase.js', mimetype='application/javascript')",
    "return firebase_js_content, 200, {'Content-Type': 'application/javascript'}"
)

# We will inject the strings at the top after imports.
import_end = new_app_py.find('app = Flask(__name__)')

html_escaped = index_html.replace('"""', '\\"\\"\\"')
fb_escaped = firebase_js.replace('"""', '\\"\\"\\"')

inject = f'''
index_html_content = """{html_escaped}"""
firebase_js_content = """{fb_escaped}"""
'''

final_code = new_app_py[:import_end] + inject + new_app_py[import_end:]

with open('app.py', 'w', encoding='utf-8') as f:
    f.write(final_code)
print("app.py created successfully!")
