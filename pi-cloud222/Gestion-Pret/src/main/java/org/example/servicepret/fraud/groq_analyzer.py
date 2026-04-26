# fraud/groq_analyzer.py
import sys
import os
import json
import re
import requests
from typing import Optional

# 🔥 Patcher print pour utiliser stderr
import builtins
_original_print = builtins.print

def _stderr_print(*args, **kwargs):
    kwargs.setdefault('file', sys.stderr)
    _original_print(*args, **kwargs)

builtins.print = _stderr_print

GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

def analyze_document_with_groq(doc_type: str, filename: str, ocr_text: str, metadata: dict) -> dict:
    """Analyze document for fraud detection - Returns analysis in ENGLISH"""

    api_key = os.environ.get("GROQ_API_KEY")

    if not api_key:
        return {
            "risk_level": "ERROR",
            "risk_score": 0,
            "ai_generated_probability": 0,
            "suspicious_fields": [],
            "fraud_signals": [{"type": "ERROR", "detail": "GROQ_API_KEY not configured", "severity": "HIGH"}],
            "coherence_issues": [],
            "positive_indicators": [],
            "document_narrative": "Error: Groq API key missing",
            "recommended_action": "MANUAL_REVIEW",
            "confidence": 0
        }

    prompt = f"""You are a fraud detection expert specializing in Tunisian agricultural loan applications.

Analyze this document for potential fraud and return ONLY valid JSON (no markdown, no extra text):

=== DOCUMENT ===
Filename: {filename}
Type: {doc_type}
OCR Content: {ocr_text[:3000] if ocr_text else 'No text extracted'}

=== ANALYSIS REQUIREMENTS ===
Return EXACTLY this JSON structure (all text in ENGLISH):

{{
  "risk_level": "HIGH/MEDIUM/LOW",
  "risk_score": 0-100,
  "ai_generated_probability": 0-100,
  "suspicious_fields": [
    {{
      "field_name": "example field",
      "suspicious_value": "example value",
      "reason": "why it's suspicious",
      "severity": "HIGH/MEDIUM/LOW"
    }}
  ],
  "fraud_signals": [
    {{
      "type": "SIGNAL_TYPE",
      "detail": "description of the signal",
      "severity": "HIGH/MEDIUM/LOW"
    }}
  ],
  "coherence_issues": [
    "description of any internal inconsistency"
  ],
  "positive_indicators": [
    "element that confirms authenticity"
  ],
  "document_narrative": "Brief analysis in English for the bank advisor (2-3 sentences)",
  "recommended_action": "APPROVE/MANUAL_REVIEW/REJECT",
  "confidence": 0-100
}}"""

    try:
        response = requests.post(
            GROQ_API_URL,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.1,
                "max_tokens": 2000
            },
            timeout=60
        )

        if response.status_code != 200:
            return {
                "risk_level": "ERROR",
                "risk_score": 0,
                "ai_generated_probability": 0,
                "suspicious_fields": [],
                "fraud_signals": [{"type": "ERROR", "detail": f"API Error: {response.status_code}", "severity": "HIGH"}],
                "coherence_issues": [],
                "positive_indicators": [],
                "document_narrative": f"Groq API error: {response.status_code}",
                "recommended_action": "MANUAL_REVIEW",
                "confidence": 0
            }

        result = response.json()
        content = result["choices"][0]["message"]["content"]

        content = content.strip()
        if content.startswith("```json"):
            content = content[7:]
        if content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]

        parsed = json.loads(content)

        return {
            "risk_level": parsed.get("risk_level", "LOW"),
            "risk_score": parsed.get("risk_score", 0),
            "ai_generated_probability": parsed.get("ai_generated_probability", 0),
            "suspicious_fields": parsed.get("suspicious_fields", []),
            "fraud_signals": parsed.get("fraud_signals", []),
            "coherence_issues": parsed.get("coherence_issues", []),
            "positive_indicators": parsed.get("positive_indicators", []),
            "document_narrative": parsed.get("document_narrative", "Analysis completed."),
            "recommended_action": parsed.get("recommended_action", "MANUAL_REVIEW"),
            "confidence": parsed.get("confidence", 50)
        }

    except Exception as e:
        return {
            "risk_level": "ERROR",
            "risk_score": 0,
            "ai_generated_probability": 0,
            "suspicious_fields": [],
            "fraud_signals": [{"type": "ERROR", "detail": str(e), "severity": "HIGH"}],
            "coherence_issues": [],
            "positive_indicators": [],
            "document_narrative": f"Error: {str(e)}",
            "recommended_action": "MANUAL_REVIEW",
            "confidence": 0
        }


def synthesize_dossier_with_groq(
        individual_results: list,
        agriculteur_id: int,
        demande_id: int,
) -> dict:
    """Global dossier synthesis - Returns analysis in ENGLISH"""

    api_key = os.environ.get("GROQ_API_KEY")

    if not api_key:
        return _calculate_global_risk_locally(individual_results)

    summary = []
    for r in individual_results[:10]:
        summary.append(f"""
Document: {r.get('filename')}
Risk: {r.get('risk_level')} (score={r.get('risk_score')})
""")

    prompt = f"""You are a fraud detection expert specializing in Tunisian agricultural loan applications.

=== DOSSIER INFO ===
Farmer ID: #{agriculteur_id}
Application ID: #{demande_id}
Total documents: {len(individual_results)}

=== INDIVIDUAL RESULTS ===
{''.join(summary)}

=== TASK ===
Analyze the overall coherence of this dossier and return ONLY valid JSON (no markdown, all text in ENGLISH):

{{
  "global_risk": "HIGH/MEDIUM/LOW",
  "global_score": 0-100,
  "fraud_confirmed": false,
  "critical_documents": ["filename1.pdf", "filename2.pdf"],
  "all_suspicious_fields": [
    {{
      "document": "filename.pdf",
      "field_name": "field name",
      "suspicious_value": "value found",
      "reason": "why it's suspicious",
      "severity": "HIGH/MEDIUM/LOW"
    }}
  ],
  "cross_document_inconsistencies": [
    "inconsistency detected between documents"
  ],
  "dossier_narrative": "Complete report in English for the bank advisor (3-5 sentences)",
  "recommendation": "APPROVE/MANUAL_REVIEW/REJECT",
  "recommendation_justification": "Clear justification (1-2 sentences)"
}}"""

    try:
        response = requests.post(
            GROQ_API_URL,
            headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.1,
                "max_tokens": 3000
            },
            timeout=90
        )

        if response.status_code != 200:
            return _calculate_global_risk_locally(individual_results)

        result = response.json()
        content = result["choices"][0]["message"]["content"]

        content = content.strip()
        if content.startswith("```json"):
            content = content[7:]
        if content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]

        return json.loads(content)

    except Exception as e:
        return _calculate_global_risk_locally(individual_results)


def _calculate_global_risk_locally(individual_results: list) -> dict:
    """Fallback: Calculate global risk locally - ALL TEXT IN ENGLISH"""
    if not individual_results:
        return {
            "global_risk": "LOW",
            "global_score": 0,
            "fraud_confirmed": False,
            "critical_documents": [],
            "all_suspicious_fields": [],
            "cross_document_inconsistencies": [],
            "dossier_narrative": "No documents were analyzed for this application.",
            "recommendation": "APPROVE",
            "recommendation_justification": "No documents provided for analysis."
        }

    scores = [r.get('risk_score', 0) for r in individual_results]
    avg_score = sum(scores) // len(scores)

    high_risk_count = sum(1 for r in individual_results if r.get('risk_level') == 'HIGH')
    medium_risk_count = sum(1 for r in individual_results if r.get('risk_level') in ('MEDIUM', 'LOW_MEDIUM'))

    if high_risk_count > 0 or avg_score >= 70:
        global_risk = "HIGH"
        recommendation = "REJECT"
        fraud_confirmed = True
    elif avg_score >= 40 or medium_risk_count >= 2:
        global_risk = "MEDIUM"
        recommendation = "MANUAL_REVIEW"
        fraud_confirmed = False
    else:
        global_risk = "LOW"
        recommendation = "APPROVE"
        fraud_confirmed = False

    critical_documents = [r.get('filename') for r in individual_results if r.get('risk_level') == 'HIGH']

    all_suspicious_fields = []
    for r in individual_results:
        for field in r.get('suspicious_fields', []):
            if isinstance(field, dict):
                all_suspicious_fields.append({
                    "document": r.get('filename'),
                    "field_name": field.get('field_name', ''),
                    "suspicious_value": field.get('suspicious_value', ''),
                    "reason": field.get('reason', ''),
                    "severity": field.get('severity', 'MEDIUM')
                })

    cross_doc_inconsistencies = []
    if len(individual_results) > 1:
        cross_doc_inconsistencies.append("Multiple documents found - verify consistency of personal information (name, CIN, address) across all documents")

    # 🔥 FORCER L'ANGLAIS - Ces lignes sont les plus importantes !
    return {
        "global_risk": global_risk,
        "global_score": avg_score,
        "fraud_confirmed": fraud_confirmed,
        "critical_documents": critical_documents,
        "all_suspicious_fields": all_suspicious_fields[:50],
        "cross_document_inconsistencies": cross_doc_inconsistencies,
        "dossier_narrative": f"This dossier contains {len(individual_results)} documents. Average fraud score: {avg_score}/100. Global risk level: {global_risk}. {high_risk_count} document(s) show high risk, {medium_risk_count} document(s) show medium risk.",
        "recommendation": recommendation,
        "recommendation_justification": f"Analysis of {len(individual_results)} documents. Average score: {avg_score}/100. High-risk documents: {high_risk_count}."
    }