import re

file_path = 'app/src/main/java/com/example/MainActivity.kt'

with open(file_path, 'r') as f:
    content = f.read()

# 1. Add state variables
state_vars = """                        var quickReply1Cmd by remember { mutableStateOf(settings.quickReply1Cmd) }
                        var quickReply1Text by remember { mutableStateOf(settings.quickReply1Text) }
                        var quickReply2Cmd by remember { mutableStateOf(settings.quickReply2Cmd) }
                        var quickReply2Text by remember { mutableStateOf(settings.quickReply2Text) }
                        var quickReply3Cmd by remember { mutableStateOf(settings.quickReply3Cmd) }
                        var quickReply3Text by remember { mutableStateOf(settings.quickReply3Text) }
"""
content = re.sub(r'(var voiceCmdVip by remember \{ mutableStateOf\(settings\.voiceCmdVip\) \}\n)', r'\1' + state_vars, content)

# 2. Add UI below voiceCmdVip
ui_code = """
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                            Text("Respostas Rápidas (Chat)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Diga o comando e o Jarvis digitará o texto no chat do iFood.", color = Color.Gray, fontSize = 10.sp)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quickReply1Cmd,
                                    onValueChange = {
                                        quickReply1Cmd = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply1Cmd = it))
                                    },
                                    label = { Text("Cmd 1", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                                OutlinedTextField(
                                    value = quickReply1Text,
                                    onValueChange = {
                                        quickReply1Text = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply1Text = it))
                                    },
                                    label = { Text("Texto 1", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quickReply2Cmd,
                                    onValueChange = {
                                        quickReply2Cmd = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply2Cmd = it))
                                    },
                                    label = { Text("Cmd 2", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                                OutlinedTextField(
                                    value = quickReply2Text,
                                    onValueChange = {
                                        quickReply2Text = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply2Text = it))
                                    },
                                    label = { Text("Texto 2", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = quickReply3Cmd,
                                    onValueChange = {
                                        quickReply3Cmd = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply3Cmd = it))
                                    },
                                    label = { Text("Cmd 3", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.3f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                                OutlinedTextField(
                                    value = quickReply3Text,
                                    onValueChange = {
                                        quickReply3Text = it
                                        RadarCoordinator.saveSettings(context, settings.copy(quickReply3Text = it))
                                    },
                                    label = { Text("Texto 3", color = Color.Gray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedBorderColor = AccentBlue)
                                )
                            }
"""
content = re.sub(r'(modifier = Modifier\.fillMaxWidth\(\)\.testTag\("voice_cmd_vip_field"\)\s*\n\s*\))', r'\1' + ui_code, content)

with open(file_path, 'w') as f:
    f.write(content)

print("Injected Quick Reply UI")
