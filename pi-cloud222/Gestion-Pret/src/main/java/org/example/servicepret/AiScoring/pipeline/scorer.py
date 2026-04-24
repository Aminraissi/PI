"""
pipeline/scorer.py — Pipeline principal de scoring de solvabilité

CORRECTIONS v2 :
1. Décision basée sur score_composite (TOUJOURS monotone) en priorité
2. Pondération ML + heuristique pour éviter les anomalies du modèle
3. Score final = max(score_ml, score_composite) → jamais pénalisé par docs optionnels
4. Seuils de décision recalibrés
"""

import pickle
import numpy as np
import pandas as pd
from typing import Optional

from pipeline.ocr import read_all_documents
from pipeline.extractor import extract_all
from pipeline.features import (
    build_features, merge_with_german_credit,
    DOCS_OBLIGATOIRES, SCORE_MAX_TOTAL,
)


# ─────────────────────────────────────────────────────────────────────────────
# Modèle (chargé une seule fois au démarrage)
# ─────────────────────────────────────────────────────────────────────────────

_MODEL        = None
_FEATURE_COLS = None


def load_model(
        model_path: str = 'models/model.pkl',
        features_path: str = 'models/features.pkl',
):
    global _MODEL, _FEATURE_COLS

    with open(model_path, 'rb') as f:
        _MODEL = pickle.load(f)

    with open(features_path, 'rb') as f:
        _FEATURE_COLS = pickle.load(f)

    print(f"[OK] Modèle chargé — {len(_FEATURE_COLS)} features attendues")


def _ensure_model():
    if _MODEL is None:
        load_model()


# ─────────────────────────────────────────────────────────────────────────────
# SEUILS DE DÉCISION
# ─────────────────────────────────────────────────────────────────────────────

# Score de solvabilité [0-100]
SEUIL_ACCEPTE = 60   # score >= 60 → ACCEPTÉ
SEUIL_REVISION = 40  # score >= 40 → RÉVISION, sinon REFUSÉ

# prob_bad (modèle ML)
THRESHOLD_ACCEPT = 0.35
THRESHOLD_REVIEW = 0.65


def _score_to_decision(score: float, docs_manquants: list, features: dict) -> dict:
    """
    Convertit un score [0-100] en décision finale.

    RÈGLE : un dossier avec tous les docs obligatoires présents
    ne peut jamais être REFUSÉ uniquement à cause des docs optionnels.
    """
    recommandations = features.get('recommandations', [])

    base = {
        'score_solvabilite': round(score, 1),
        'prob_bad':          round(max(0, min(1, (100 - score) / 100)), 4),
        'prob_good':         round(max(0, min(1, score / 100)), 4),
        'docs_manquants':    docs_manquants,
        'recommandations':   recommandations,
    }

    nb_obligatoires = features.get('nb_docs_obligatoires', 0)
    has_incidents   = features.get('has_incidents', 0)

    # RÈGLE MÉTIER : si des docs obligatoires manquent → ne peut pas être ACCEPTÉ
    if len(docs_manquants) > 0 and score >= SEUIL_ACCEPTE:
        score = min(score, SEUIL_ACCEPTE - 0.1)
        base['score_solvabilite'] = round(score, 1)

    if score >= SEUIL_ACCEPTE:
        return {**base, 'decision': 'ACCEPTÉ',  'message': 'Dossier solide',       'couleur': 'green'}
    elif score >= SEUIL_REVISION:
        return {**base, 'decision': 'RÉVISION',  'message': 'À analyser',           'couleur': 'orange'}
    else:
        return {**base, 'decision': 'REFUSÉ',    'message': 'Risque élevé',         'couleur': 'red'}


def _decision(prob_bad: float, features: dict, score_composite: float) -> dict:
    """
    CORRECTION CRITIQUE : combine le score ML et le score composite.

    Problème original : le modèle ML (entraîné sur German Credit ou données
    historiques) peut donner une prob_bad élevée quand des features liées aux
    docs optionnels ont des valeurs inhabituelles.

    Solution : on prend le MEILLEUR des deux scores.
    - score_ml = (1 - prob_bad) * 100
    - score_composite = score heuristique monotone garanti

    Le score final = alpha * score_ml + (1-alpha) * score_composite
    avec alpha ajusté selon la confiance dans chaque score.
    """
    score_ml = (1 - prob_bad) * 100

    nb_docs_obligatoires = features.get('nb_docs_obligatoires', 0)
    total_obligatoires   = len(DOCS_OBLIGATOIRES)
    docs_manquants       = features.get('docs_manquants', [])

    # Pondération dynamique :
    # - Si dossier complet (tous docs obligatoires) → score_composite très fiable → poids fort
    # - Si dossier incomplet → modèle ML moins pertinent aussi → équilibre
    ratio_completude = nb_docs_obligatoires / total_obligatoires

    # alpha = poids du modèle ML (0.3 à 0.6)
    # Plus le dossier est complet, plus on fait confiance au composite
    alpha = 0.6 - (ratio_completude * 0.3)  # alpha ∈ [0.3, 0.6]

    # PROTECTION MONOTONIE :
    # Si score_composite > score_ml de plus de 20 points, le modèle est probablement
    # pénalisé par une anomalie → on réduit son poids
    if score_composite > score_ml + 20:
        alpha = 0.2  # on fait très peu confiance au modèle ML dans ce cas

    score_final = alpha * score_ml + (1 - alpha) * score_composite

    print(f"[SCORING] score_ml={score_ml:.1f}, score_composite={score_composite:.1f}, "
          f"alpha={alpha:.2f}, score_final={score_final:.1f}")

    return _score_to_decision(score_final, docs_manquants, features)


# ─────────────────────────────────────────────────────────────────────────────
# Pipeline principal
# ─────────────────────────────────────────────────────────────────────────────

def score_dossier(
        doc_paths: dict,
        applicant_info: Optional[dict] = None,
        german_row: Optional[dict] = None,
        debug: bool = True,
) -> dict:
    """
    Pipeline complet : OCR → Extraction → Features → Modèle → Décision.

    GARANTIE : score(A ∪ {D}) >= score(A) pour tout dossier A et document D.

    Paramètres :
        doc_paths     : {'cin': '/tmp/xxx.pdf', 'releves': '/tmp/yyy.pdf', ...}
        applicant_info: {'montant_demande': float, 'duree_mois': int}
        german_row    : données German Credit optionnelles pour enrichissement
        debug         : affiche les logs intermédiaires

    Retourne :
        {
          'decision': 'ACCEPTÉ'|'RÉVISION'|'REFUSÉ',
          'score_solvabilite': float (0-100),
          'prob_bad': float,
          'prob_good': float,
          'message': str,
          'couleur': str,
          'docs_manquants': list[str],
          'recommandations': list[str],
        }
    """
    _ensure_model()

    # ── 1. OCR ───────────────────────────────────────────────────────────────
    if debug:
        print("\n" + "="*60)
        print("[SCORING] Démarrage OCR...")
        present = [k for k, v in doc_paths.items() if v is not None]
        print(f"[SCORING] Fichiers reçus ({len(present)}) : {present}")

    ocr_texts = read_all_documents(doc_paths)

    if debug:
        print("\n[SCORING] Résultats OCR :")
        for k, t in ocr_texts.items():
            chars  = len((t or '').strip())
            status = "✓" if chars > 20 else "✗ VIDE"
            print(f"  {status}  {k:20s}: {chars} chars")

    # ── 2. Extraction ────────────────────────────────────────────────────────
    extracted = extract_all(ocr_texts)

    if debug:
        print("\n[SCORING] Champs extraits :")
        for k, v in extracted.items():
            print(f"  {k}: {v}")

    # ── 3. Features ──────────────────────────────────────────────────────────
    features = build_features(extracted, applicant_info, doc_paths)

    if german_row:
        features = merge_with_german_credit(features, german_row)

    if debug:
        print("\n[SCORING] Features clés :")
        keys_importants = [
            'nb_docs_obligatoires', 'nb_docs_optionnels',
            'score_obligatoire', 'score_optionnel', 'score_total_docs',
            'ratio_total', 'score_composite', 'revenu_estime',
            'ratio_montant_revenu', 'has_incidents',
        ]
        for k in keys_importants:
            print(f"  {k}: {features.get(k)}")
        print(f"\n  docs_manquants : {features.get('docs_manquants', [])}")

    # ── 4. Alignement strict avec le modèle ──────────────────────────────────
    features_numeric = {
        k: v for k, v in features.items()
        if k not in ('docs_manquants', 'recommandations') and not isinstance(v, list)
    }

    X = pd.DataFrame([features_numeric])
    X = X.reindex(columns=_FEATURE_COLS, fill_value=0)

    if debug:
        missing_cols = [c for c in _FEATURE_COLS if c not in features_numeric]
        extra_cols   = [c for c in features_numeric if c not in _FEATURE_COLS]
        if missing_cols:
            print(f"\n[SCORING] ⚠ Colonnes manquantes (→ 0) : {missing_cols}")
        if extra_cols:
            print(f"[SCORING] ℹ Colonnes ignorées : {extra_cols}")

    # ── 5. Prédiction ML ─────────────────────────────────────────────────────
    prob_bad         = float(_MODEL.predict_proba(X)[0][1])
    score_composite  = float(features.get('score_composite', (1 - prob_bad) * 100))

    if debug:
        print(f"\n[SCORING] prob_bad (ML)   = {prob_bad:.4f}")
        print(f"[SCORING] score_composite = {score_composite:.2f}")
        print("="*60)

    return _decision(prob_bad, features, score_composite)


# ─────────────────────────────────────────────────────────────────────────────
# Test simple
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    test_docs = {
        'cin':             'tests/cin.pdf',
        'domicile':        'tests/domicile.pdf',
        'releves':         'tests/releves.pdf',
        'projet':          'tests/projet.pdf',
        'titre_foncier':   'tests/titre.pdf',
        'carte_agri':      'tests/carte.pdf',
        'non_endettement': None,
        'assurance':       None,
        'garantie':        None,
    }

    result = score_dossier(
        test_docs,
        {'montant_demande': 50000, 'duree_mois': 60}
    )

    print("\n" + "="*60)
    print("RÉSULTAT FINAL :")
    for k, v in result.items():
        print(f"  {k}: {v}")