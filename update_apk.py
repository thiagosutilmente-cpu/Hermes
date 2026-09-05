import zipfile, os

apk_path = '.build-outputs/app-debug.apk'
print(f"Checking {apk_path}, exists: {os.path.exists(apk_path)}")
