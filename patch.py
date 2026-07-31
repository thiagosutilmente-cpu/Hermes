import sys

with open('index.html', 'r') as f:
    lines = f.readlines()

start = -1
end = -1
for i, line in enumerate(lines):
    if '// Drag and drop events' in line:
        start = i
    if 'listContainer.appendChild(itemDiv);' in line and start != -1 and end == -1:
        end = i

if start != -1 and end != -1:
    new_content = [
        "                    listContainer.appendChild(itemDiv);\n",
        "                });\n",
        "\n",
        "                if (typeof Sortable !== 'undefined') {\n",
        "                    new Sortable(listContainer, {\n",
        "                        animation: 150,\n",
        "                        ghostClass: 'sortable-ghost',\n",
        "                        onEnd: function (evt) {\n",
        "                            if (evt.oldIndex !== evt.newIndex) {\n",
        "                                window.reorderTimelineSteps(evt.oldIndex, evt.newIndex);\n",
        "                                setTimeout(() => {\n",
        "                                    const updatedItems = document.querySelectorAll('.timeline-draggable-item');\n",
        "                                    if (updatedItems[evt.newIndex]) {\n",
        "                                        updatedItems[evt.newIndex].style.boxShadow = '0 0 25px rgba(0, 245, 212, 0.8)';\n",
        "                                        updatedItems[evt.newIndex].style.borderColor = 'rgba(0, 245, 212, 1)';\n",
        "                                        updatedItems[evt.newIndex].style.transform = 'scale(1.03)';\n",
        "                                        if (window.playCustomSonarTone) {\n",
        "                                            window.playCustomSonarTone();\n",
        "                                        }\n",
        "                                        setTimeout(() => {\n",
        "                                            updatedItems[evt.newIndex].style.boxShadow = '';\n",
        "                                            updatedItems[evt.newIndex].style.borderColor = '';\n",
        "                                            updatedItems[evt.newIndex].style.transform = '';\n",
        "                                        }, 800);\n",
        "                                    }\n",
        "                                }, 50);\n",
        "                            }\n",
        "                        }\n",
        "                    });\n",
        "                }\n",
        "            };\n"
    ]
    
    # We need to replace up to the closing `};` of renderMasterSequenceTimeline.
    # So let's find `};` after listContainer.appendChild(itemDiv);
    
    end_func = end + 1
    while end_func < len(lines) and '};' not in lines[end_func]:
        end_func += 1
        
    lines = lines[:start] + new_content + lines[end_func + 1:]
    
    with open('index.html', 'w') as f:
        f.writelines(lines)
    print("Patched successfully")
else:
    print("Could not find start or end")

