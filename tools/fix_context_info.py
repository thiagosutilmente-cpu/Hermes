import re

file_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'val contextInfo = "\$offerText\$quickReplies"\s*if'
repl = r'val contextInfo = "$offerText - $quickReplies"\n\n                    if'

content = re.sub(pattern, repl, content)

with open(file_path, 'w') as f:
    f.write(content)

