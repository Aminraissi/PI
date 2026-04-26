"""
fraud/analyzer.py
Orchestrateur principal du pipeline de détection de fraude
"""

import sys

# 🔥 Patcher print pour utiliser stderr au lieu de stdout
import builtins
_original_print = builtins.print

def _stderr_print(*args, **kwargs):
    kwargs.setdefault('file', sys.stderr)
    _original_print(*args, **kwargs)

builtins.print = _stderr_print

from metadata_extractor import extract_metadata
from ocr_extractor import extract_text
from doc_type_detector import detect_doc_type
from groq_analyzer import analyze_document_with_groq


def analyze_dossier(
        doc_map: dict,
        agriculteur_id: int,
        demande_id: int,
) -> dict:
    print(f"\n[FRAUD] Démarrage analyse — {len(doc_map)} documents")
    print(f"[FRAUD] Demande #{demande_id} — Agriculteur #{agriculteur_id}")

    individual_results = []

    for filename, tmp_path in doc_map.items():
        print(f"\n[FRAUD] Analyse de '{filename}'...")

        metadata = extract_metadata(tmp_path)
        print(f"  [META] créateur={metadata.get('creator')}, "
              f"risque métadonnées={metadata.get('metadata_risk')}, "
              f"signaux={len(metadata.get('signals', []))}")

        ocr_text = extract_text(tmp_path)
        print(f"  [OCR] {len(ocr_text)} chars extraits")

        doc_type_key, doc_type_label = detect_doc_type(filename, ocr_text)
        print(f"  [TYPE] → {doc_type_key} ({doc_type_label})")

        print(f"  [GROQ] Appel API en cours...")
        groq_result = analyze_document_with_groq(
            doc_type=doc_type_label,
            filename=filename,
            ocr_text=ocr_text,
            metadata=metadata,
        )

        if isinstance(groq_result, str):
            import json
            try:
                groq_result = json.loads(groq_result)
            except:
                groq_result = {
                    "risk_level": "ERROR",
                    "risk_score": 0,
                    "ai_generated_probability": 0,
                    "suspicious_fields": [],
                    "fraud_signals": [{"type": "ERROR", "detail": "Erreur parsing Groq", "severity": "HIGH"}],
                    "document_narrative": "Erreur d'analyse Groq"
                }

        doc_report = _build_document_report(
            filename=filename,
            doc_type_key=doc_type_key,
            doc_type_label=doc_type_label,
            metadata=metadata,
            ocr_chars=len(ocr_text),
            groq_result=groq_result,
        )

        individual_results.append(doc_report)
        print(f"  [RESULT] Risque={doc_report['risk_level']} "
              f"(score={doc_report['risk_score']}, IA={doc_report['ai_generated_probability']}%)")

    # ── Synthèse globale CALCULÉE LOCALEMENT (sans appel API) ─────────────────
    print(f"\n[FRAUD] Synthèse globale ({len(individual_results)} docs)...")
    synthesis = _calculate_global_risk(individual_results)

    global_risk = synthesis.get("global_risk", "ERROR")
    global_score = synthesis.get("global_score", 0)

    print(f"\n[FRAUD] ✓ Analyse terminée — "
          f"Risque global={global_risk} (score={global_score})")
    print(f"[FRAUD] Recommandation: {synthesis.get('recommendation', '?')}")

    return {
        "global_risk": global_risk,
        "global_score": global_score,
        "fraud_confirmed": synthesis.get("fraud_confirmed", False),
        "recommendation": synthesis.get("recommendation", "MANUAL_REVIEW"),
        "recommendation_justification": synthesis.get("recommendation_justification", ""),
        "dossier_narrative": synthesis.get("dossier_narrative", ""),
        "all_suspicious_fields": synthesis.get("all_suspicious_fields", []),
        "cross_document_inconsistencies": synthesis.get("cross_document_inconsistencies", []),
        "critical_documents": synthesis.get("critical_documents", []),
        "documents": individual_results,
        "stats": _compute_stats(individual_results),
    }


def _calculate_global_risk(individual_results: list) -> dict:
    """Calcule le risque global à partir des résultats individuels (sans API)"""
    if not individual_results:
        return {
            "global_risk": "LOW",
            "global_score": 0,
            "fraud_confirmed": False,
            "recommendation": "APPROVE",
            "recommendation_justification": "Aucun document analysé",
            "dossier_narrative": "Aucun document à analyser.",
            "all_suspicious_fields": [],
            "cross_document_inconsistencies": [],
            "critical_documents": []
        }

    # Calculer le score moyen
    scores = [r.get('risk_score', 0) for r in individual_results]
    avg_score = sum(scores) // len(scores)

    # Compter les documents par niveau de risque
    high_risk_count = sum(1 for r in individual_results if r.get('risk_level') == 'HIGH')
    medium_risk_count = sum(1 for r in individual_results if r.get('risk_level') in ('MEDIUM', 'LOW_MEDIUM'))

    # Déterminer le risque global
    if high_risk_count > 0 or avg_score >= 70:
        global_risk = "HIGH"
        recommendation = "REJECT"
        fraud_confirmed = True
    elif medium_risk_count >= 2 or avg_score >= 40:
        global_risk = "MEDIUM"
        recommendation = "MANUAL_REVIEW"
        fraud_confirmed = False
    else:
        global_risk = "LOW"
        recommendation = "APPROVE"
        fraud_confirmed = False

    # Liste des documents critiques (ceux à haut risque)
    critical_documents = [r.get('filename') for r in individual_results if r.get('risk_level') == 'HIGH']

    # Rassembler tous les champs suspects
    all_suspicious_fields = []
    for r in individual_results:
        for field in r.get('suspicious_fields', []):
            all_suspicious_fields.append({
                "document": r.get('filename'),
                "field_name": field if isinstance(field, str) else field.get('field_name', str(field)),
                "suspicious_value": field.get('suspicious_value', '') if isinstance(field, dict) else '',
                "reason": field.get('reason', '') if isinstance(field, dict) else '',
                "severity": field.get('severity', 'MEDIUM') if isinstance(field, dict) else 'MEDIUM'
            })

    # Détecter les incohérences inter-documents (simplifié)
    cross_document_inconsistencies = []
    # Vérifier si le même champ apparaît avec des valeurs différentes
    if len(individual_results) > 1:
        cross_document_inconsistencies.append("Plusieurs documents analysés - vérifier la cohérence des informations personnelles")

    return {
        "global_risk": global_risk,
        "global_score": avg_score,
        "fraud_confirmed": fraud_confirmed,
        "recommendation": recommendation,
        "recommendation_justification": f"Analyse de {len(individual_results)} documents. Score moyen: {avg_score}. Documents à haut risque: {high_risk_count}",
        "dossier_narrative": f"Le dossier contient {len(individual_results)} documents. Score de fraude moyen: {avg_score}/100. Risque global: {global_risk}. {high_risk_count} document(s) présentent un risque élevé.",
        "all_suspicious_fields": all_suspicious_fields[:50],  # Limiter à 50
        "cross_document_inconsistencies": cross_document_inconsistencies,
        "critical_documents": critical_documents
    }


def _build_document_report(
        filename: str,
        doc_type_key: str,
        doc_type_label: str,
        metadata: dict,
        ocr_chars: int,
        groq_result: dict,
) -> dict:
    meta_risk = metadata.get("metadata_risk", "LOW")
    groq_risk = groq_result.get("risk_level", "LOW")

    risk_order = {"LOW": 0, "LOW_MEDIUM": 1, "MEDIUM": 2, "HIGH": 3, "ERROR": 2}
    final_risk = groq_risk if risk_order.get(groq_risk, 0) >= risk_order.get(meta_risk, 0) else meta_risk

    all_signals = list(metadata.get("signals", []))
    all_signals += groq_result.get("fraud_signals", [])

    suspicious_fields = groq_result.get("suspicious_fields", [])

    meta_score_map = {"LOW": 10, "LOW_MEDIUM": 25, "MEDIUM": 45, "HIGH": 75, "ERROR": 50}
    meta_score = meta_score_map.get(meta_risk, 10)
    groq_score = groq_result.get("risk_score", 0)
    final_score = max(groq_score, meta_score)

    return {
        "filename": filename,
        "doc_type": doc_type_key,
        "doc_type_label": doc_type_label,
        "risk_level": final_risk,
        "risk_score": final_score,
        "ai_generated_probability": groq_result.get("ai_generated_probability", 0),
        "confidence": groq_result.get("confidence", 0),
        "metadata": {
            "creator": metadata.get("creator"),
            "producer": metadata.get("producer"),
            "creation_date": metadata.get("creation_date"),
            "modification_date": metadata.get("modification_date"),
            "num_pages": metadata.get("num_pages"),
            "file_size_kb": metadata.get("file_size_kb"),
            "metadata_risk": meta_risk,
        },
        "fraud_signals": all_signals,
        "suspicious_fields": suspicious_fields,
        "coherence_issues": groq_result.get("coherence_issues", []),
        "positive_indicators": groq_result.get("positive_indicators", []),
        "document_narrative": groq_result.get("document_narrative", ""),
        "recommended_action": groq_result.get("recommended_action", "MANUAL_REVIEW"),
        "ocr_chars": ocr_chars,
    }


def _compute_stats(individual_results: list) -> dict:
    total = len(individual_results)
    high = sum(1 for d in individual_results if d["risk_level"] == "HIGH")
    medium = sum(1 for d in individual_results if d["risk_level"] in ("MEDIUM", "LOW_MEDIUM"))
    low = sum(1 for d in individual_results if d["risk_level"] == "LOW")
    avg_score = round(sum(d["risk_score"] for d in individual_results) / total, 1) if total > 0 else 0
    total_signals = sum(len(d.get("fraud_signals", [])) for d in individual_results)
    total_fields = sum(len(d.get("suspicious_fields", [])) for d in individual_results)

    return {
        "total_documents": total,
        "high_risk_documents": high,
        "medium_risk_documents": medium,
        "low_risk_documents": low,
        "average_risk_score": avg_score,
        "total_fraud_signals": total_signals,
        "total_suspicious_fields": total_fields,
    }