import re

with open('index.html', 'r') as f:
    content = f.read()

bad_importmap = """    {
      "imports": {
        "firebase/app": "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js",
        "firebase/auth": "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js",
        "firebase/firestore": "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js"
      }
    }
    setTimeout(() => {"""

good_importmap = """    {
      "imports": {
        "firebase/app": "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js",
        "firebase/auth": "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js",
        "firebase/firestore": "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js"
      }
    }
  </script>
  <script>
    setTimeout(() => {"""

content = content.replace(bad_importmap, good_importmap)

with open('index.html', 'w') as f:
    f.write(content)
