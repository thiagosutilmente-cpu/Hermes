import sys

with open('index.html', 'r') as f:
    content = f.read()

idx_aether = content.find('<!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->')
# Find the end of obfuscation dashboard
idx_obf = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')

if idx_aether != -1 and idx_obf != -1:
    idx_end = content.find('</div>\n\n      <!-- ATIVIDADE DA', idx_obf)
    if idx_end == -1:
        # Just find the next section "<!--" after obfuscation
        idx_end = content.find('<!--', idx_obf + 100)
    
    if idx_end != -1:
        modules_content = content[idx_aether:idx_end]
        content = content[:idx_aether] + content[idx_end:]
        
        idx_modal_content = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
        if idx_modal_content != -1:
            idx_modal_body_end = content.find('</div>\n            </div>\n        </div>', idx_modal_content)
            if idx_modal_body_end != -1:
                content = content[:idx_modal_body_end] + '\n' + modules_content + '\n' + content[idx_modal_body_end:]
                with open('index.html', 'w') as f:
                    f.write(content)
                print("Modules moved to modal successfully!")
            else:
                print("Could not find modal body end")
        else:
            print("Could not find tools inside modal")
    else:
        print("Could not find end of modules")
