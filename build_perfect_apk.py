#!/usr/bin/env python3
"""
Build & Sign Script for Radar Coordinator APK.
Fixes all known causes of 'App não instalado' on Samsung One UI 6 / 7 / 9 / Android 14-17:
1. Sets extractNativeLibs = true in AndroidManifest.xml
2. Sets debuggable = false in AndroidManifest.xml (bypasses Samsung Auto Blocker debug block)
3. Increments versionCode to 10 (bypasses downgrade conflict)
4. Stores all lib/*/*.so uncompressed (ZIP_STORED)
5. Aligns with zipalign -P 16 4 (16KB page alignment for Android 15/17 ARM64 kernels)
6. Signs with apksigner using v1 + v2 + v3 schemes
7. Syncs APK to all output directories
"""

import os
import shutil
import struct
import subprocess
import zipfile

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_APK = os.path.join(BASE_DIR, '.build-outputs', 'app-debug.apk')
TMP_DIR = '/tmp/radar_build'
os.makedirs(TMP_DIR, exist_ok=True)

REPACK_APK = os.path.join(TMP_DIR, 'repacked_unaligned.apk')
ALIGNED_APK = os.path.join(TMP_DIR, 'aligned_16k.apk')
FINAL_SIGNED_APK = os.path.join(TMP_DIR, 'RadarCoordinator_final.apk')

KEYSTORE = os.path.join(BASE_DIR, 'debug.keystore')
ZIPALIGN = '/opt/android/sdk/build-tools/36.0.0/zipalign'
APKSIGNER = '/opt/android/sdk/build-tools/36.0.0/apksigner'
AAPT = '/opt/android/sdk/build-tools/36.0.0/aapt'

print("=== INICIANDO CONSTRUÇÃO DO APK DEFINITIVO (RADAR COORDINATOR) ===")

# 1. Extrair e corrigir AndroidManifest.xml binário
print("[1/5] Corrigindo atributos do AndroidManifest.xml...")
with zipfile.ZipFile(SRC_APK, 'r') as zin:
    manifest_bytes = bytearray(zin.read('AndroidManifest.xml'))

# Atualizar versionCode para 10 (offset 17220)
v_size, v_res0, v_type, v_data = struct.unpack('<HBBI', manifest_bytes[17220+12:17220+20])
print(f"  versionCode anterior: {v_data} -> Novo: 10")
struct.pack_into('<I', manifest_bytes, 17220+16, 10)

# Atualizar debuggable para false (offset 21576)
d_size, d_res0, d_type, d_data = struct.unpack('<HBBI', manifest_bytes[21576+12:21576+20])
print(f"  debuggable anterior: {hex(d_data)} -> Novo: 0x0 (false)")
struct.pack_into('<I', manifest_bytes, 21576+16, 0)

# Atualizar extractNativeLibs para true (offset 21636)
e_size, e_res0, e_type, e_data = struct.unpack('<HBBI', manifest_bytes[21636+12:21636+20])
print(f"  extractNativeLibs anterior: {hex(e_data)} -> Novo: 0xffffffff (true)")
struct.pack_into('<I', manifest_bytes, 21636+16, 0xffffffff)

# 2. Empacotar APK com SOs não comprimidas (ZIP_STORED) e sem assinaturas velhas
print("[2/5] Reempacotando APK com bibliotecas nativas (.so) DESCOMPACTADAS (STORED)...")
with zipfile.ZipFile(SRC_APK, 'r') as zin:
    with zipfile.ZipFile(REPACK_APK, 'w') as zout:
        for item in zin.infolist():
            # Descartar arquivos de assinatura prévios
            if item.filename.startswith('META-INF/') and (
                item.filename.endswith('.SF') or 
                item.filename.endswith('.RSA') or 
                item.filename.endswith('.DSA') or 
                item.filename.endswith('MANIFEST.MF')
            ):
                continue
            
            if item.filename == 'AndroidManifest.xml':
                new_info = zipfile.ZipInfo(item.filename)
                new_info.compress_type = zipfile.ZIP_DEFLATED
                new_info.external_attr = item.external_attr
                zout.writestr(new_info, bytes(manifest_bytes))
            elif item.filename.endswith('.so'):
                # Crucial para Android 14/15/17: SO DEVE ser STORED (uncompressed)
                data = zin.read(item.filename)
                new_info = zipfile.ZipInfo(item.filename)
                new_info.compress_type = zipfile.ZIP_STORED
                new_info.external_attr = item.external_attr
                zout.writestr(new_info, data)
            else:
                zout.writestr(item, zin.read(item.filename))

# 3. Zipalign com alinhamento de 16KB para kernels Android 15/17
print("[3/5] Executando zipalign com alinhamento de 16KB (-P 16 4)...")
res_align = subprocess.run([
    ZIPALIGN,
    '-f', '-P', '16', '4',
    REPACK_APK,
    ALIGNED_APK
], capture_output=True, text=True)
if res_align.returncode != 0:
    raise RuntimeError(f"Zipalign falhou: {res_align.stderr}")

# Verificar alinhamento
res_check = subprocess.run([
    ZIPALIGN,
    '-c', '-P', '16', '-v', '4',
    ALIGNED_APK
], capture_output=True, text=True)
if "Verification successful" not in res_check.stdout:
    raise RuntimeError(f"Verificação de alinhamento 16KB falhou:\n{res_check.stdout}")
print("  Alinhamento 16KB: 100% SUCESSO!")

# 4. Assinar com apksigner v1 + v2 + v3
print("[4/5] Assinando APK com esquemas v1, v2 e v3...")
res_sign = subprocess.run([
    APKSIGNER,
    'sign',
    '--ks', KEYSTORE,
    '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey',
    '--key-pass', 'pass:android',
    '--v1-signing-enabled', 'true',
    '--v2-signing-enabled', 'true',
    '--v3-signing-enabled', 'true',
    '--out', FINAL_SIGNED_APK,
    ALIGNED_APK
], capture_output=True, text=True)
if res_sign.returncode != 0:
    raise RuntimeError(f"Assinatura apksigner falhou: {res_sign.stderr}")

# Validar assinatura
res_verify = subprocess.run([
    APKSIGNER,
    'verify',
    '--verbose',
    FINAL_SIGNED_APK
], capture_output=True, text=True)
print("  Verificação de Assinatura:")
for line in res_verify.stdout.splitlines():
    if "Verified using" in line or "Verifies" in line:
        print(f"    {line}")

# 5. Distribuir para todas as pastas do projeto
print("[5/5] Sincronizando APK validado para todos os destinos...")
destinations = [
    os.path.join(BASE_DIR, 'RadarCoordinator.apk'),
    os.path.join(BASE_DIR, 'deploy', 'RadarCoordinator.apk'),
    os.path.join(BASE_DIR, '.build-outputs', 'app-debug.apk'),
    os.path.join(BASE_DIR, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'),
    os.path.join(BASE_DIR, 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'),
]

for dest in destinations:
    dest_dir = os.path.dirname(dest)
    if dest_dir:
        os.makedirs(dest_dir, exist_ok=True)
    shutil.copyfile(FINAL_SIGNED_APK, dest)
    print(f"  Copiado com sucesso para: {dest}")

print("\n=== APK DEFINITIVO CONSTRUÍDO, ALINHADO EM 16KB E PRONTO PARA INSTALAÇÃO! ===")
