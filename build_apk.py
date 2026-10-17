import os
import subprocess
import sys
import zipfile
import shutil

PROJECT_DIR = r"d:\Projects\reverseeeee\SmartWasherApp"
APP_DIR = os.path.join(PROJECT_DIR, "app")
SRC_DIR = os.path.join(APP_DIR, "src", "main")
RES_DIR = os.path.join(SRC_DIR, "res")
JAVA_DIR = os.path.join(SRC_DIR, "java")
MANIFEST = os.path.join(SRC_DIR, "AndroidManifest.xml")
OUT_DIR = os.path.join(PROJECT_DIR, "build")

ANDROID_JAR = r"C:\Users\Admin\AppData\Local\Android\Sdk\platforms\android-34\android.jar"
BUILD_TOOLS = r"C:\Users\Admin\AppData\Local\Android\Sdk\build-tools\36.0.0"
AAPT2 = os.path.join(BUILD_TOOLS, "aapt2.exe")
D8 = os.path.join(BUILD_TOOLS, "d8.bat")
ZIPALIGN = os.path.join(BUILD_TOOLS, "zipalign.exe")
APKSIGNER = os.path.join(BUILD_TOOLS, "apksigner.bat")

JAVA_HOME = r"C:\Program Files\Android\Android Studio\jbr"
JAVA_HOME_BIN = os.path.join(JAVA_HOME, "bin")
JAVAC = os.path.join(JAVA_HOME_BIN, "javac.exe")
KEYTOOL = os.path.join(JAVA_HOME_BIN, "keytool.exe")

os.environ["JAVA_HOME"] = JAVA_HOME
os.environ["PATH"] = JAVA_HOME_BIN + os.pathsep + os.environ.get("PATH", "")

def run_cmd(cmd, cwd=None):
    print("Running:", " ".join(cmd) if isinstance(cmd, list) else cmd)
    env = os.environ.copy()
    env["JAVA_HOME"] = JAVA_HOME
    res = subprocess.run(cmd, cwd=cwd, shell=True, capture_output=True, text=True, env=env)
    if res.stdout:
        print("STDOUT:", res.stdout)
    if res.stderr:
        print("STDERR:", res.stderr)
    if res.returncode != 0:
        raise RuntimeError(f"Command failed with code {res.returncode}")

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    compiled_res_dir = os.path.join(OUT_DIR, "compiled_res")
    gen_dir = os.path.join(OUT_DIR, "gen")
    classes_dir = os.path.join(OUT_DIR, "classes")
    os.makedirs(compiled_res_dir, exist_ok=True)
    os.makedirs(gen_dir, exist_ok=True)
    os.makedirs(classes_dir, exist_ok=True)

    # 1. Compile resources with aapt2
    print("--- 1. Compiling resources with aapt2 ---")
    compiled_flats = []
    for root, dirs, files in os.walk(RES_DIR):
        for f in files:
            full_path = os.path.join(root, f)
            cmd = f'"{AAPT2}" compile "{full_path}" -o "{compiled_res_dir}"'
            run_cmd(cmd)

    # 2. Link resources with aapt2
    print("--- 2. Linking resources with aapt2 ---")
    proto_apk = os.path.join(OUT_DIR, "proto.apk")
    flat_files = [os.path.join(compiled_res_dir, f) for f in os.listdir(compiled_res_dir) if f.endswith(".flat")]
    flat_args = " ".join([f'"{f}"' for f in flat_files])
    assets_dir = os.path.join(SRC_DIR, "assets")
    assets_flag = f'-A "{assets_dir}"' if os.path.exists(assets_dir) else ""
    link_cmd = f'"{AAPT2}" link -I "{ANDROID_JAR}" --manifest "{MANIFEST}" --java "{gen_dir}" --min-sdk-version 24 --target-sdk-version 34 --version-code 1 --version-name "1.0.0" {assets_flag} -o "{proto_apk}" --auto-add-overlay {flat_args}'
    run_cmd(link_cmd)

    # 3. Find all java source files
    print("--- 3. Compiling Java with javac ---")
    java_files = []
    for root, dirs, files in os.walk(JAVA_DIR):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))
    for root, dirs, files in os.walk(gen_dir):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))

    sources_file = os.path.join(OUT_DIR, "sources.txt")
    with open(sources_file, "w", encoding="utf-8") as sf:
        for jf in java_files:
            sf.write(f'{jf.replace(os.sep, "/")}\n')

    javac_cmd = f'"{JAVAC}" -cp "{ANDROID_JAR}" -d "{classes_dir}" --release 17 @"{sources_file}"'
    run_cmd(javac_cmd)

    # 4. Convert classes to DEX with d8
    print("--- 4. Building DEX with d8 ---")
    class_files = []
    for root, dirs, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(f'"{os.path.join(root, f)}"')
    
    dex_dir = os.path.join(OUT_DIR, "dex")
    os.makedirs(dex_dir, exist_ok=True)
    d8_jar = os.path.join(BUILD_TOOLS, "lib", "d8.jar")
    d8_args = " ".join(class_files)
    d8_cmd = f'"{os.path.join(JAVA_HOME_BIN, "java.exe")}" -cp "{d8_jar}" com.android.tools.r8.D8 --lib "{ANDROID_JAR}" --output "{dex_dir}" --min-api 24 {d8_args}'
    run_cmd(d8_cmd)

    # 5. Clean Package of DEX and Resources into unaligned APK
    print("--- 5. Packaging DEX into APK ---")
    unaligned_apk = os.path.join(OUT_DIR, "unaligned.apk")
    if os.path.exists(unaligned_apk):
        os.remove(unaligned_apk)

    dex_file = os.path.join(dex_dir, "classes.dex")
    with zipfile.ZipFile(proto_apk, 'r') as zin:
        with zipfile.ZipFile(unaligned_apk, 'w') as zout:
            for item in zin.infolist():
                data = zin.read(item.filename)
                # resources.arsc must be stored uncompressed (ZIP_STORED) for Android runtime memory mapping
                if item.filename == "resources.arsc" or item.filename.endswith(".png") or item.filename.endswith(".bin"):
                    zout.writestr(item.filename, data, compress_type=zipfile.ZIP_STORED)
                else:
                    zout.writestr(item.filename, data, compress_type=zipfile.ZIP_DEFLATED)
            # Add classes.dex
            with open(dex_file, "rb") as df:
                zout.writestr("classes.dex", df.read(), compress_type=zipfile.ZIP_DEFLATED)

    # 6. Zipalign
    print("--- 6. Running zipalign ---")
    aligned_apk = os.path.join(OUT_DIR, "aligned.apk")
    if os.path.exists(aligned_apk):
        os.remove(aligned_apk)
    zipalign_cmd = f'"{ZIPALIGN}" -v -p 4 "{unaligned_apk}" "{aligned_apk}"'
    run_cmd(zipalign_cmd)

    # 7. Generate debug keystore and sign
    print("--- 7. Signing APK ---")
    keystore = os.path.join(OUT_DIR, "debug.keystore")
    if not os.path.exists(keystore):
        genkey_cmd = f'"{KEYTOOL}" -genkeypair -v -keystore "{keystore}" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"'
        run_cmd(genkey_cmd)

    final_apk = os.path.join(PROJECT_DIR, "OpenWasher-release.apk")
    if os.path.exists(final_apk):
        os.remove(final_apk)
    apksigner_jar = os.path.join(BUILD_TOOLS, "lib", "apksigner.jar")
    sign_cmd = f'"{os.path.join(JAVA_HOME_BIN, "java.exe")}" -jar "{apksigner_jar}" sign --ks "{keystore}" --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "{final_apk}" "{aligned_apk}"'
    run_cmd(sign_cmd)

    print("\n==========================================")
    print(f"SUCCESS! APK built and signed successfully:")
    print(final_apk)
    print("Size:", os.path.getsize(final_apk), "bytes")
    print("==========================================")

if __name__ == "__main__":
    main()
