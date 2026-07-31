import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()
    
# Remove the inserted attachBaseContext from MainViewModel
content = re.sub(r'\s*override fun attachBaseContext\(base: android\.content\.Context\) \{.*?\n    \}', '', content, flags=re.DOTALL)

code_to_add = """
    override fun attachBaseContext(base: android.content.Context) {
        val context = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            base.createAttributionContext("Radar")
        } else {
            base
        }
        super.attachBaseContext(context)
    }
"""

# Now insert it into MainActivity
match = re.search(r'class MainActivity\s*:\s*ComponentActivity\(\)\s*\{', content)
if match:
    insert_pos = match.end()
    content = content[:insert_pos] + code_to_add + content[insert_pos:]
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Fixed MainActivity")
else:
    print("Could not find MainActivity")
