import sys
import re

with open('index.html', 'r') as f:
    content = f.read()

# Let's verify that the buttons to scroll to the sections now just scroll inside the modal, or we should just let them scroll.
# Actually, since they are in a modal which has overflow-y: auto, scrollIntoView will work perfectly inside the modal.

# Add a close button to the modal logic if needed.
js_code = """
    window.toggleToolsModal = function() {
        const modal = document.getElementById('toolsModal');
        modal.style.display = modal.style.display === 'none' ? 'flex' : 'none';
        if (window.triggerHapticFeedback) window.triggerHapticFeedback('light');
    };
"""

content = content.replace("    window.triggerZeroDay = function() {", js_code + "\n    window.triggerZeroDay = function() {")

with open('index.html', 'w') as f:
    f.write(content)

print("Modal toggle JS added")
