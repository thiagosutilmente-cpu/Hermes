with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("htmlContent = htmlContent.replace(/\\n/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>');", "htmlContent = htmlContent.replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>');")
content = content.replace("htmlContent = htmlContent.replace(/\\n/", "htmlContent = htmlContent.replace(/\\n/g, '<br>');")

with open('index.html', 'w') as f:
    f.write(content)
