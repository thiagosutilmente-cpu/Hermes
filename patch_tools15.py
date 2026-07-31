import sys

with open('index.html', 'r') as f:
    content = f.read()

# I also need to move the actual dashboards (AETHER, HAARP, INCEPTION, STEALTH, SINGULARITY) inside the modal so they are accessible and not just cluttering the UI.
# Let's extract them all.

idx_aether = content.find('<!-- AETHER PROTOCOL (REALITY OVERRIDE & EMP) -->')
idx_sing = content.find('<!-- QUANTUM OBFUSCATION ENGINE (UNDETECTABLE MODE) -->')

# Let's find where they end. The last one is QUANTUM OBFUSCATION ENGINE. We need to find the end of it.
# It ends with a </div>. So let's find the next "<!--" after QUANTUM OBFUSCATION ENGINE ends.
if idx_aether != -1 and idx_sing != -1:
    idx_next_comment = content.find('<!-- ATIVIDADE DA CONCORRÊNCIA E ALERTA DE CHUVA -->', idx_sing)
    if idx_next_comment != -1:
        modules_content = content[idx_aether:idx_next_comment]
        content = content[:idx_aether] + content[idx_next_comment:]
        
        # Now let's inject it into the modal
        idx_modal = content.find('{tools_content}')
        # Oh wait, we already hardcoded {tools_content} which only has the buttons.
        # Let's find the modal we just added.
        
        idx_modal_content = content.find('<!-- CAIXA DE FERRAMENTAS DO SISTEMA (SYSTEM TOOLS) -->')
        # This is inside the modal now.
        
        # Let's append the modules right after the tools dashboard.
        if idx_modal_content != -1:
            idx_end_tools = content.find('</div>', content.find('Aether Protocol (EMP)', idx_modal_content)) + 20 # Rough estimate
            # It's better to just append it to the modal body.
            idx_modal_body_end = content.find('</div>\n            </div>\n        </div>', idx_modal_content)
            
            if idx_modal_body_end != -1:
                content = content[:idx_modal_body_end] + '\n' + modules_content + '\n' + content[idx_modal_body_end:]
                with open('index.html', 'w') as f:
                    f.write(content)
                print("Modules moved to modal!")
            else:
                print("Could not find modal body end")
        else:
            print("Could not find tools inside modal")
    else:
        print("Could not find next comment")
