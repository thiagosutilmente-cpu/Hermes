import sys

with open('index.html', 'r') as f:
    content = f.read()

js_code = """
    window.mergeQuestCount = 0;
    window.incrementMergeQuest = function() {
        if (window.quest2Completed) return;
        window.mergeQuestCount++;
        const maxMerges = 3;
        let percent = Math.min(100, (window.mergeQuestCount / maxMerges) * 100);
        const bar = document.getElementById('quest2Progress');
        const text = document.getElementById('quest2Text');
        if (bar && text) {
            bar.style.width = `${percent}%`;
            text.innerText = `${window.mergeQuestCount} / ${maxMerges} Mesclas`;
            if (percent >= 100 && !window.quest2Completed) {
                window.quest2Completed = true;
                window.addDriverXP(500);
                if (window.showToast) window.showToast("🌟 Missão Concluída: 3 Mesclas! +500 XP", "success");
                if (window.speakText) window.speakText("Estratégia avançada concluída. 500 pontos de experiência creditados.");
            }
        }
    };
"""

content = content.replace('    window.updateQuestsProgress = function(km) {', js_code + '\n    window.updateQuestsProgress = function(km) {')

with open('index.html', 'w') as f:
    f.write(content)
print("Merge quest logic injected!")
