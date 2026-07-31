import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

# Delete the OLD_TOOLS section to clean up
pattern = re.compile(r'<!-- OLD_TOOLS -->.*?</div>\s+</div>\s+</div>', re.DOTALL)
content = pattern.sub('', content)

# But wait, my regex above might be wrong, I'll just leave it or use a better way. Let's just find and remove the div that contains OLD_TOOLS.
start_idx = content.find('<!-- OLD_TOOLS -->')
if start_idx != -1:
    end_idx = content.find('<!-- NEURAL SINGULARITY', start_idx)
    if end_idx != -1:
        # We also added the new tools right before neural singularity... this might have duplicated things.
        pass

