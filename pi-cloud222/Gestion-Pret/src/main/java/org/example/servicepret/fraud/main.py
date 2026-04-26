"""
fraud-detection-service/main.py
Service de détection de fraude documentaire via Claude AI
"""

import os
import sys
import tempfile
from pathlib import Path
from typing import List

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.concurrency import run_in_threadpool

# Modifier ici : import direct sans "fraud."
from analyzer import analyze_dossier

app = FastAPI(title="Service de Détection de Fraude Documentaire")


@app.on_event("startup")
def startup_event():
    key = os.environ.get("GROQ_API_KEY", "")  # Changer ANTHROPIC par GROQ
    if not key:
        print("[WARN] GROQ_API_KEY non définie — les appels Groq échoueront")
    else:
        print("[OK] GROQ_API_KEY détectée")


@app.post("/analyze-fraud")
async def analyze_fraud(
        files: List[UploadFile] = File(...),
        agriculteur_id: int = Form(...),
        demande_id: int = Form(...),
):
    tmp_paths = []
    doc_map = {}

    try:
        for upload in files:
            content = await upload.read()
            if not content:
                continue
            suffix = Path(upload.filename).suffix.lower() or ".pdf"
            tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
            tmp.write(content)
            tmp.close()
            tmp_paths.append(tmp.name)
            doc_map[upload.filename] = tmp.name

        result = await run_in_threadpool(
            analyze_dossier,
            doc_map,
            agriculteur_id,
            demande_id,
        )
        return result

    except Exception as e:
        import traceback
        traceback.print_exc()
        return {
            "global_risk": "ERROR",
            "global_score": 0,
            "documents": [],
            "suspicious_fields": [],
            "narrative": f"Erreur lors de l'analyse: {str(e)}",
            "recommendation": "ERREUR_ANALYSE",
        }
    finally:
        for p in tmp_paths:
            try:
                if os.path.exists(p):
                    os.remove(p)
            except Exception:
                pass


@app.get("/health")
def health():
    return {"status": "ok", "service": "fraud-detection"}