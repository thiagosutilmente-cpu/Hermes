import re

file_path = 'app/src/main/java/com/example/voice/JarvisPersonaEngine.kt'

with open(file_path, 'r') as f:
    content = f.read()

pattern = r'val prompt = """\s*\$SYSTEM_PROMPT\s*Contexto da situação atual: \$screenContext'
repl = r'''val tone = com.example.coordinator.RadarCoordinator.settings.value.jarvisVoiceTone
            val prompt = """
                $SYSTEM_PROMPT
                
                ATENÇÃO À PERSONA (HUMOR):
                Você deve adotar estritamente o humor/persona: $tone.
                Se for "AMIGÁVEL": Seja mais caloroso, empático e companheiro.
                Se for "FORMAL": Seja extremamente profissional, objetivo e semelhante a um mordomo inglês clássico (ex: "Sim, senhor").
                Se for "DIRETO": Seja militar, focado em lucro, conciso e sem enrolações.

                Contexto da situação atual: $screenContext'''

new_content = re.sub(pattern, repl, content)

with open(file_path, 'w') as f:
    f.write(new_content)

