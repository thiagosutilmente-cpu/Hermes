import sys
with open('index.html', 'r') as f:
    content = f.read()

idx = content.find('currentOrdersList')
if idx != -1:
    print(content[idx-100:idx+200])
