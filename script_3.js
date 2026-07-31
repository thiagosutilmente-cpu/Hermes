
    setTimeout(() => {
       if(window.openTtsSettingsModal) {
          window.openTtsSettingsModal();
          const voiceEngineSelect = document.getElementById("settingsJarvisVoiceEngine");
          if (voiceEngineSelect) voiceEngineSelect.value = "NEURAL";
          const elevenLabsVoicePreset = document.getElementById("modalElevenLabsVoicePreset");
          if (elevenLabsVoicePreset) {
              elevenLabsVoicePreset.value = "pNInz6obpg7AN6ZbeS31";
              if (window.handleVoicePresetChangeFromModal) {
                  window.handleVoicePresetChangeFromModal("pNInz6obpg7AN6ZbeS31");
              }
          }
          const toggleEleven = document.getElementById("modalElevenLabsToggle");
          if (toggleEleven) {
              toggleEleven.checked = true;
              if (window.toggleElevenLabsFromModal) window.toggleElevenLabsFromModal(true);
          }
          if (window.showToast) {
             window.showToast("Integração ElevenLabs ativa. Preset Adam selecionado.", "success");
          }
       }
    }, 1500);

  