import re

file_path = 'app/src/main/java/com/example/service/RadarCoordinatorService.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'val contextInfo = "\$offerText\\n\$quickReplies"  km/h\."\s*\}\s*if \(command\.startsWith'
repl = r'val contextInfo = "$offerText\\n$quickReplies"\n\n                    if (command.startsWith'

content = re.sub(pattern, repl, content)

# I can see there is an unmatched } there

pattern2 = r'val contextInfo = "\$offerText\n\$quickReplies"  km/h\."\s*\}'
repl2 = r'val contextInfo = "$offerText\n$quickReplies"'

content = re.sub(pattern2, repl2, content)

with open(file_path, 'w') as f:
    f.write(content)

