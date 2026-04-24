import os
import shutil

src_root = "pi-cloud"
dst_root = "pi-cloud222"

src_files = []
for root, dirs, files in os.walk(src_root):
    for f in files:
        if f == "application.properties" and "src/main/resources" in root:
            src_files.append(os.path.join(root, f))

dst_files = []
for root, dirs, files in os.walk(dst_root):
    for f in files:
        if f == "application.properties" and "src/main/resources" in root:
            dst_files.append(os.path.join(root, f))

for src in src_files:
    # Try to find corresponding dst
    # Match by looking for folder name containing the service name
    service_name = src.split('/')[1]
    
    # farmer-support had a path change from farmer-support/src to farmer-support/farmer-support/src
    if service_name == "farmer-support":
        for dst in dst_files:
            if "farmer-support" in dst:
                print(f"Copying {src} -> {dst}")
                shutil.copy2(src, dst)
        continue
    
    # Generic matcher
    for dst in dst_files:
        if service_name in dst and src.count("Payment-Service") == dst.count("Payment-Service"):
            print(f"Copying {src} -> {dst}")
            shutil.copy2(src, dst)

