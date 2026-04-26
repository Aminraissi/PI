"""
fraud/doc_type_detector.py
Détection du type de document (CIN, relevés, domicile, etc.)
basée sur le nom de fichier et le contenu OCR.
"""

import re

DOC_TYPES = {
    "cin":             "Carte d'Identité Nationale",
    "domicile":        "Justificatif de domicile",
    "releves":         "Relevé bancaire / Déclaration fiscale",
    "projet":          "Description du projet agricole",
    "titre_foncier":   "Titre foncier / Bail agricole",
    "carte_agri":      "Carte d'agriculteur",
    "non_endettement": "Attestation de non-endettement",
    "assurance":       "Police d'assurance",
    "garantie":        "Lettre de garantie solidaire",
}

DOC_KEYWORDS = {
    "cin":             ["cin", "identit", "cni", "national", "carte_id"],
    "domicile":        ["domicile", "steg", "sonede", "residence", "adresse", "facture", "electricite"],
    "releves":         ["releve", "bancaire", "compte", "fiscal", "bank", "bna", "stb", "biat"],
    "projet":          ["projet", "description", "agricol", "culture", "activite"],
    "titre_foncier":   ["titre", "foncier", "cadastr", "propriete", "immatriculation"],
    "carte_agri":      ["carte", "agriculteur", "agricul", "crda", "mdrt"],
    "non_endettement": ["endettement", "non_endet", "attestation"],
    "assurance":       ["assurance", "police", "star", "assur"],
    "garantie":        ["garantie", "caution", "solidaire"],
}

DOC_CONTENT_SIGNATURES = {
    "cin":             ["carte d'identité nationale", "identité nationale", "date de naissance", "cin"],
    "domicile":        ["steg", "sonede", "facture de consommation", "résidence"],
    "releves":         ["relevé de compte", "extrait de compte", "solde", "bna", "biat", "rib"],
    "projet":          ["projet agricole", "type de culture", "surface agricole", "hectares"],
    "titre_foncier":   ["titre foncier", "conservation foncière", "superficie"],
    "carte_agri":      ["carte d'agriculteur", "exploitant agricole", "crda", "ministère"],
    "non_endettement": ["non endettement", "aucune dette", "trésorerie"],
    "assurance":       ["police d'assurance", "prime d'assurance", "star assurances"],
    "garantie":        ["garantie solidaire", "caution solidaire", "s'engage solidairement"],
}


def detect_doc_type(filename: str, ocr_text: str = "") -> tuple:
    """
    Retourne (doc_type_key, doc_type_label) ou ("inconnu", "Document inconnu")
    """
    # Scoring par contenu
    text_lower = (ocr_text or "").lower()
    content_scores = {}
    if len(text_lower.strip()) >= 20:
        for doc_type, signatures in DOC_CONTENT_SIGNATURES.items():
            count = sum(1 for sig in signatures if sig in text_lower)
            if count > 0:
                content_scores[doc_type] = count

    # Scoring par nom de fichier
    name = filename.lower().replace("-", "_").replace(" ", "_")
    name = re.sub(r"^\d+_", "", name)
    name_type = None
    for doc_type, keywords in DOC_KEYWORDS.items():
        if any(kw in name for kw in keywords):
            name_type = doc_type
            break

    # Combinaison
    combined = dict(content_scores)
    if name_type:
        combined[name_type] = combined.get(name_type, 0) + 5

    if not combined:
        return "inconnu", "Document inconnu"

    best = max(combined, key=lambda t: combined[t])
    if combined[best] < 2:
        return "inconnu", "Document inconnu"

    return best, DOC_TYPES.get(best, "Document inconnu")
