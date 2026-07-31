import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# We will find where `AnimatedVisibility(visible = showConfigPanel)` starts and put our new modal right before it.
target = "            AnimatedVisibility(visible = showConfigPanel) {"

modal_code = """
            // Geofence Modal
            AnimatedVisibility(visible = showGeofenceModal) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlateBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).verticalScroll(rememberScrollState())
                        .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                                Text("Cerca Virtual Avançada", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            IconButton(onClick = { showGeofenceModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = AccentBlue)
                            }
                        }
                        
                        Text(
                            text = "Configure zonas de risco, áreas de bônus ou locais restritos. O Jarvis avisará por voz ao se aproximar.",
                            color = TextLight.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        
                        var localZones by remember(settings) { mutableStateOf(settings.geofenceZones) }
                        
                        // List of existing zones
                        if (localZones.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nenhuma zona configurada.", color = TextDim, fontSize = 14.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                localZones.forEachIndexed { index, zone ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (zone.isDangerZone) AccentRed.copy(alpha = 0.1f) else AccentGreen.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (zone.isDangerZone) AccentRed.copy(alpha = 0.3f) else AccentGreen.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(zone.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                Switch(
                                                    checked = zone.active,
                                                    onCheckedChange = { checked ->
                                                        val updated = localZones.toMutableList()
                                                        updated[index] = zone.copy(active = checked)
                                                        localZones = updated
                                                    }
                                                )
                                            }
                                            Text("Raio: ${zone.radiusMeters.toInt()}m - Lat: ${String.format("%.4f", zone.latitude)}, Lon: ${String.format("%.4f", zone.longitude)}", color = TextDim, fontSize = 12.sp)
                                            if (zone.customVoiceAlert.isNotBlank()) {
                                                Text("🗣️ Jarvis: \\\"${zone.customVoiceAlert}\\\"", color = AccentBlue, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                                            }
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                TextButton(onClick = {
                                                    val updated = localZones.toMutableList()
                                                    updated.removeAt(index)
                                                    localZones = updated
                                                }) {
                                                    Text("Remover", color = AccentRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        
                        // Add new zone form
                        var newName by remember { mutableStateOf("") }
                        var newLat by remember { mutableStateOf("") }
                        var newLon by remember { mutableStateOf("") }
                        var newRadius by remember { mutableStateOf("1000") }
                        var newIsDanger by remember { mutableStateOf(true) }
                        var newVoiceAlert by remember { mutableStateOf("") }
                        
                        Text("Adicionar Nova Zona", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nome da Zona (ex: Cracolândia, Centro)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newLat,
                                onValueChange = { newLat = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = newLon,
                                onValueChange = { newLon = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                        
                        OutlinedTextField(
                            value = newRadius,
                            onValueChange = { newRadius = it },
                            label = { Text("Raio (metros)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = newIsDanger, onCheckedChange = { newIsDanger = it })
                            Text("Zona de Risco (Bloquear ofertas dentro do raio)", color = Color.White, fontSize = 14.sp)
                        }
                        
                        OutlinedTextField(
                            value = newVoiceAlert,
                            onValueChange = { newVoiceAlert = it },
                            label = { Text("Alerta de Voz Personalizado (Jarvis)") },
                            placeholder = { Text("Ex: Atenção, entrando em área de risco.") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        
                        Button(
                            onClick = {
                                val lat = newLat.toDoubleOrNull()
                                val lon = newLon.toDoubleOrNull()
                                val rad = newRadius.toFloatOrNull()
                                if (lat != null && lon != null && rad != null && newName.isNotBlank()) {
                                    val zone = com.example.coordinator.GeofenceZone(
                                        name = newName,
                                        latitude = lat,
                                        longitude = lon,
                                        radiusMeters = rad,
                                        isDangerZone = newIsDanger,
                                        customVoiceAlert = newVoiceAlert
                                    )
                                    val updated = localZones.toMutableList()
                                    updated.add(zone)
                                    localZones = updated
                                    
                                    // Reset form
                                    newName = ""
                                    newLat = ""
                                    newLon = ""
                                    newRadius = "1000"
                                    newVoiceAlert = ""
                                } else {
                                    Toast.makeText(context, "Preencha os campos corretamente", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adicionar Zona", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val updatedSettings = settings.copy(geofenceZones = localZones)
                                RadarCoordinator.saveSettings(context, updatedSettings)
                                Toast.makeText(context, "Zonas salvas com sucesso!", Toast.LENGTH_SHORT).show()
                                showGeofenceModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Salvar Configurações", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }

"""

replacement = modal_code + "\n" + target

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Inserted modal")
else:
    print("Target not found")
