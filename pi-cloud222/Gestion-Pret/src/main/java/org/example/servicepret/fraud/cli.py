#!/usr/bin/env python3
import sys
import json
import os
from pathlib import Path

# Patcher print pour utiliser stderr UNIQUEMENT pour ce script
# mais les prints dans les modules importés ne sont pas patchés

# Ajouter le répertoire courant au path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Capturer stdout pour ne garder que le JSON final
class JsonCapture:
    def __init__(self):
        self.json_output = None
        self.original_stdout = sys.stdout

    def write(self, text):
        # Seul le JSON final doit passer
        if text.strip().startswith('{') and text.strip().endswith('}'):
            self.json_output = text
        else:
            # Tout le reste va sur stderr
            sys.stderr.write(text)

    def flush(self):
        pass

# Remplacer stdout temporairement
capture = JsonCapture()
sys.stdout = capture

# Importer après avoir patché stdout
from analyzer import analyze_dossier

def main():
    if len(sys.argv) != 2:
        # Restaurer stdout pour l'erreur
        sys.stdout = capture.original_stdout
        print(json.dumps({"error": "Missing input file"}))
        sys.exit(1)

    input_file = sys.argv[1]

    try:
        with open(input_file, 'r') as f:
            input_data = json.load(f)

        files = input_data.get("files", {})
        agriculteur_id = input_data.get("agriculteur_id")
        demande_id = input_data.get("demande_id")

        result = analyze_dossier(files, agriculteur_id, demande_id)

        # Restaurer stdout et imprimer le JSON
        sys.stdout = capture.original_stdout
        print(json.dumps(result, ensure_ascii=False))

    except Exception as e:
        sys.stdout = capture.original_stdout
        error_result = {
            "global_risk": "ERROR",
            "error": str(e)
        }
        print(json.dumps(error_result))
        sys.exit(1)

if __name__ == "__main__":
    main()