import subprocess, zipfile, shutil, os

src_apk = '.build-outputs/app-debug.apk'
unsigned_apk = '/tmp/app-unsigned.apk'
aligned_apk = '/tmp/app-aligned.apk'
final_apk = '/tmp/app-debug-signed.apk'

print("1. Repacking clean zip...")
with zipfile.ZipFile(src_apk, 'r') as zin:
    with zipfile.ZipFile(unsigned_apk, 'w') as zout:
        for item in zin.infolist():
            if item.filename.startswith('META-INF/') and (item.filename.endswith('.SF') or item.filename.endswith('.RSA') or item.filename.endswith('.DSA') or item.filename.endswith('MANIFEST.MF')):
                continue
            zout.writestr(item, zin.read(item.filename))

print("2. Jarsigner (v1 signature)...")
res = subprocess.run([
    '/usr/lib/jvm/temurin-21-jdk-amd64/bin/jarsigner',
    '-keystore', 'debug.keystore',
    '-storepass', 'android',
    '-keypass', 'android',
    '-sigalg', 'SHA256withRSA',
    '-digestalg', 'SHA-256',
    unsigned_apk,
    'androiddebugkey'
], capture_output=True, text=True)
print('jarsigner res:', res.returncode, res.stderr)

print("3. Zipalign...")
res2 = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/zipalign',
    '-f', '-p', '4',
    unsigned_apk,
    aligned_apk
], capture_output=True, text=True)
print('zipalign res:', res2.returncode, res2.stderr)

print("4. Apksigner (v2 + v3 + v1 preserved)...")
res3 = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/apksigner',
    'sign',
    '--ks', 'debug.keystore',
    '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey',
    '--key-pass', 'pass:android',
    '--v1-signing-enabled', 'true',
    '--v2-signing-enabled', 'true',
    '--v3-signing-enabled', 'true',
    '--out', final_apk,
    aligned_apk
], capture_output=True, text=True)
print('apksigner res:', res3.returncode, res3.stderr)

print("5. Verify...")
res4 = subprocess.run([
    '/opt/android/sdk/build-tools/36.0.0/apksigner',
    'verify',
    '--verbose',
    final_apk
], capture_output=True, text=True)
print('Verify:\n', res4.stdout)

if "Verified using v1 scheme (JAR signing): true" in res4.stdout and "Verified using v2 scheme (APK Signature Scheme v2): true" in res4.stdout:
    print("SUCCESS: Replacing APKs with dual-signed build!")
    shutil.copyfile(final_apk, '.build-outputs/app-debug.apk')
    shutil.copyfile(final_apk, 'app/build/outputs/apk/debug/app-debug.apk')
