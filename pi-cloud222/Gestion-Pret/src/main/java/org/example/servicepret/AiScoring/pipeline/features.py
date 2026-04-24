"""
pipeline/features.py — Construction du vecteur de features pour le modèle ML

CORRECTIONS MAJEURES v2 :
1. MONOTONIE DU SCORE : plus de docs = score >= score sans ces docs (TOUJOURS)
2. ratio_montant_revenu : pénalité retirée quand revenu inconnu (valeur neutre 1.0)
3. score_total_docs plafonné à 100 pour normalisation correcte
4. Poids des docs optionnels calibrés pour ne jamais faire baisser le score
5. revenu_estime=0 ne pénalise plus le ratio (→ 1.0 neutre)
6. Suppression des features antagonistes avec les docs optionnels
"""

import numpy as np
import pandas as pd
from typing import Optional


# ─────────────────────────────────────────────────────────────────────────────
# Mapping types catégoriels → entiers
# ─────────────────────────────────────────────────────────────────────────────

CULTURE_MAP = {
    'cereales': 1, 'olivier': 2, 'maraichage': 3,
    'arboriculture': 4, 'fourrage': 5, 'vigne': 6,
    'dattier': 7, 'agrumes': 8, None: 0,
}

ELEVAGE_MAP = {
    'ovin': 1, 'bovin': 2, 'caprin': 3,
    'avicole': 4, 'apicole': 5, 'camelin': 6,
    None: 0,
}

PROPRIETE_MAP = {
    'titre': 2, 'location': 1, 'autre': 0, None: 0,
}

ASSURANCE_MAP = {
    'multirisque': 3, 'recolte': 2,
    'materiel': 1, 'elevage': 1,
    None: 0,
}

DOMICILE_MAP = {
    'steg': 2, 'sonede': 2, 'telecom': 1,
    'certificat': 1, 'autre': 1, None: 0,
}

FINANCIER_MAP = {
    'releve': 2, 'fiscal': 1, None: 0,
}

# Documents obligatoires vs optionnels
DOCS_OBLIGATOIRES = ['cin', 'domicile', 'releves', 'projet', 'titre_foncier', 'carte_agri']
DOCS_OPTIONNELS   = ['non_endettement', 'assurance', 'garantie']

# ─────────────────────────────────────────────────────────────────────────────
# CORRECTION 1 : Poids recalibrés pour garantir la MONOTONIE
#
# Règle absolue : score(A ∪ {doc}) >= score(A) pour tout dossier A et tout doc
#
# Les poids représentent la CONTRIBUTION POSITIVE maximale de chaque doc.
# Un doc présent ne peut jamais faire baisser le score.
# ─────────────────────────────────────────────────────────────────────────────

POIDS_OBLIGATOIRES = {
    'cin':          12,   # identité = fondamental
    'domicile':      8,   # justificatif domicile
    'releves':      20,   # solvabilité financière = plus important
    'projet':       12,   # viabilité projet
    'titre_foncier':14,   # garantie foncière
    'carte_agri':   10,   # légitimité agriculteur
}
# Total max obligatoires = 76 points

POIDS_OPTIONNELS = {
    'non_endettement': 8,   # réduit le risque
    'assurance':      12,   # protège l'investissement
    'garantie':        4,   # renforcement modéré
}
# Total max optionnels = 24 points
# Total max global = 100 points → normalisé proprement

SCORE_MAX_OBLIGATOIRE = sum(POIDS_OBLIGATOIRES.values())  # 76
SCORE_MAX_TOTAL       = SCORE_MAX_OBLIGATOIRE + sum(POIDS_OPTIONNELS.values())  # 100


# ─────────────────────────────────────────────────────────────────────────────
# Construction features
# ─────────────────────────────────────────────────────────────────────────────

def build_features(
        extracted: dict,
        applicant_info: Optional[dict] = None,
        doc_paths: Optional[dict] = None,
) -> dict:
    """
    Construit le vecteur de features à partir des données extraites.

    GARANTIE DE MONOTONIE :
        Pour tout sous-ensemble A de documents et tout document D supplémentaire :
        score_solvabilite(A ∪ {D}) >= score_solvabilite(A)

    Paramètres :
        extracted     : résultat de extract_all()
        applicant_info: {'montant_demande': float, 'duree_mois': int}
        doc_paths     : dict original des chemins (source de vérité pour présence)
    """
    f = {}
    info = applicant_info or {}

    all_doc_keys = DOCS_OBLIGATOIRES + DOCS_OPTIONNELS

    # ─────────────────────────────
    # 1. PRÉSENCE DES DOCUMENTS
    # Priorité : fichier physique présent > OCR dit présent
    # ─────────────────────────────
    for k in all_doc_keys:
        file_present    = 1 if (doc_paths and doc_paths.get(k)) else 0
        extract_present = int(extracted.get(k, {}).get('present', 0) or 0)
        f[f'has_{k}'] = int(bool(file_present or extract_present))

    # ─────────────────────────────
    # 2. SCORES DOCUMENTS (CORRIGÉS)
    #
    # CORRECTION : score_optionnel et score_total_docs sont TOUJOURS >= 0
    # et CROISSANTS avec le nombre de documents.
    # ─────────────────────────────
    f['score_obligatoire'] = sum(
        POIDS_OBLIGATOIRES[k] * f[f'has_{k}'] for k in DOCS_OBLIGATOIRES
    )
    f['score_optionnel'] = sum(
        POIDS_OPTIONNELS[k] * f[f'has_{k}'] for k in DOCS_OPTIONNELS
    )
    f['score_total_docs']  = f['score_obligatoire'] + f['score_optionnel']

    # Ratios normalisés [0, 1] — TOUJOURS croissants
    f['ratio_obligatoire'] = round(f['score_obligatoire'] / SCORE_MAX_OBLIGATOIRE, 4)
    f['ratio_total']       = round(f['score_total_docs'] / SCORE_MAX_TOTAL, 4)

    # Comptages bruts
    f['nb_docs_total']        = sum(f[f'has_{k}'] for k in all_doc_keys)
    f['nb_docs_obligatoires'] = sum(f[f'has_{k}'] for k in DOCS_OBLIGATOIRES)
    f['nb_docs_optionnels']   = sum(f[f'has_{k}'] for k in DOCS_OPTIONNELS)

    # ─────────────────────────────
    # 3. CIN
    # ─────────────────────────────
    cin = extracted.get('cin', {})
    f['age']          = int(cin.get('age') or 0)
    f['cin_valide']   = int(cin.get('cin_valide') or 0)
    f['cin_a_numero'] = 1 if cin.get('cin_number') else 0

    # CORRECTION : si CIN absent, ces features sont neutres (0), pas pénalisantes
    # Le modèle verra has_cin=0 → pénalité déjà via score_obligatoire
    if not f['has_cin']:
        f['cin_valide']   = 0
        f['cin_a_numero'] = 0

    # ─────────────────────────────
    # 4. RELEVÉS BANCAIRES (CORRIGÉS)
    # ─────────────────────────────
    releves = extracted.get('releves', {})
    f['type_financier_code'] = FINANCIER_MAP.get(releves.get('type_financier'), 0)
    f['nb_releves']          = int(releves.get('nb_releves') or 0)
    f['solde_moyen']         = float(releves.get('solde_moyen') or 0)
    f['revenu_estime']       = float(releves.get('revenu_estime') or 0)
    f['nb_incidents']        = int(releves.get('incidents') or 0)
    f['has_incidents']       = int(f['nb_incidents'] > 0)
    f['revenu_norm']         = min(f['revenu_estime'] / 50000.0, 1.0) if f['revenu_estime'] > 0 else 0.0

    # CORRECTION CRITIQUE : ratio_montant_revenu
    # Ancienne version : ratio=10.0 si revenu inconnu → PÉNALISE les dossiers
    # sans relevés même s'ils ont d'autres bons docs.
    # Nouvelle version : valeur neutre 1.0 si revenu inconnu (ratio indéterminé)
    montant = float(info.get('montant_demande') or 0)
    if f['revenu_estime'] > 0:
        f['ratio_montant_revenu'] = round(montant / f['revenu_estime'], 4)
    else:
        f['ratio_montant_revenu'] = 1.0  # NEUTRE — ni bon ni mauvais

    # ─────────────────────────────
    # 5. PROJET AGRICOLE
    # ─────────────────────────────
    projet = extracted.get('projet', {})
    f['culture_code']      = CULTURE_MAP.get(projet.get('type_culture'), 0)
    f['elevage_code']      = ELEVAGE_MAP.get(projet.get('type_elevage'), 0)
    f['surface_ha']        = float(projet.get('surface_ha') or 0)
    f['projet_completude'] = int(projet.get('completude') or 0)
    f['has_culture']       = int(bool(projet.get('type_culture')))
    f['has_elevage']       = int(bool(projet.get('type_elevage')))
    f['profil_mixte']      = int(f['has_culture'] == 1 and f['has_elevage'] == 1)
    f['surface_norm']      = min(f['surface_ha'] / 100.0, 1.0)

    # Cohérence surface titre foncier / projet
    titre = extracted.get('titre_foncier', {})
    f['surface_titre_ha'] = float(titre.get('surface_ha') or 0)
    if f['surface_ha'] > 0 and f['surface_titre_ha'] > 0:
        ratio = min(f['surface_ha'], f['surface_titre_ha']) / max(f['surface_ha'], f['surface_titre_ha'])
        f['surface_coherente'] = 1 if ratio >= 0.5 else 0
    else:
        # CORRECTION : absence de surface n'est pas une incohérence
        f['surface_coherente'] = 0  # indéterminé → neutre

    # ─────────────────────────────
    # 6. TITRE FONCIER
    # ─────────────────────────────
    f['propriete_code'] = PROPRIETE_MAP.get(titre.get('type_propriete'), 0)
    f['titre_a_numero'] = 1 if titre.get('numero_titre') else 0

    # ─────────────────────────────
    # 7. CARTE AGRICULTEUR
    # ─────────────────────────────
    carte = extracted.get('carte_agri', {})
    f['carte_valide']   = int(carte.get('valide') or 0)
    f['carte_a_numero'] = 1 if carte.get('numero_carte') else 0

    # ─────────────────────────────
    # 8. DOCUMENTS OPTIONNELS (CORRIGÉS)
    #
    # CORRECTION CRITIQUE : ces features doivent TOUJOURS être >= 0 et
    # TOUJOURS améliorer ou maintenir le score quand le doc est présent.
    # Elles ne doivent JAMAIS être utilisées pour pénaliser.
    # ─────────────────────────────
    non_end = extracted.get('non_endettement', {})
    f['non_endett_valide'] = int(non_end.get('valide') or 0)
    # Bonus supplémentaire si le doc est présent ET valide
    f['non_endett_bonus'] = int(f['has_non_endettement'] and f['non_endett_valide'])

    assur = extracted.get('assurance', {})
    f['assurance_code']   = ASSURANCE_MAP.get(assur.get('type_assurance'), 0) if f['has_assurance'] else 0
    f['assurance_valide'] = int(assur.get('valide') or 0) if f['has_assurance'] else 0
    # Score assurance positif uniquement
    f['assurance_score']  = f['assurance_code'] * f['has_assurance']

    gar = extracted.get('garantie', {})
    f['garantie_valide']  = int(gar.get('valide') or 0) if f['has_garantie'] else 0
    f['garant_identifie'] = int(gar.get('garant_identifie') or 0) if f['has_garantie'] else 0

    # ─────────────────────────────
    # 9. DOMICILE
    # ─────────────────────────────
    domicile = extracted.get('domicile', {})
    f['type_logement_code'] = DOMICILE_MAP.get(domicile.get('type_domicile'), 0)
    # CORRECTION : si domicile absent → code 0 (neutre), pas pénalisant ici
    # (la pénalité vient de has_domicile=0 dans score_obligatoire)
    if not f['has_domicile']:
        f['type_logement_code'] = 0

    # ─────────────────────────────
    # 10. DEMANDE DE PRÊT
    # ─────────────────────────────
    f['montant_demande'] = montant
    f['duree_mois']      = float(info.get('duree_mois') or 0)
    f['montant_norm']    = min(montant / 200000.0, 1.0)
    f['duree_norm']      = min(f['duree_mois'] / 120.0, 1.0)

    # ─────────────────────────────
    # 11. SCORE DE QUALITÉ DOCUMENTAIRE (NOUVEAU)
    #
    # Feature composite qui mesure la QUALITÉ des docs présents,
    # pas seulement leur présence. Toujours croissante.
    # ─────────────────────────────
    qualite_score = 0

    # CIN valide avec numéro
    if f['has_cin']:
        qualite_score += f['cin_valide'] * 2 + f['cin_a_numero']

    # Relevés avec données financières réelles
    if f['has_releves']:
        qualite_score += min(f['revenu_norm'] * 3, 3)
        qualite_score += f['type_financier_code']
        qualite_score -= f['has_incidents'] * 2  # seule pénalité: incidents bancaires réels

    # Projet complet
    if f['has_projet']:
        qualite_score += f['projet_completude']

    # Titre foncier avec numéro
    if f['has_titre_foncier']:
        qualite_score += f['propriete_code'] + f['titre_a_numero']

    # Carte valide
    if f['has_carte_agri']:
        qualite_score += f['carte_valide'] + f['carte_a_numero']

    # Optionnels (contribution positive uniquement)
    qualite_score += f['non_endett_bonus'] * 2
    qualite_score += f['assurance_score']
    qualite_score += f['garantie_valide'] + f['garant_identifie']

    f['qualite_docs_score'] = max(0, round(qualite_score, 2))

    # ─────────────────────────────
    # 12. SCORE DE SOLVABILITÉ COMPOSITE (NOUVEAU)
    #
    # Score final calculé directement (en parallèle du modèle ML).
    # Sert de feature d'entrée ET de score de fallback.
    # PROPRIÉTÉ CLÉ : strictement croissant avec l'ajout de documents.
    # ─────────────────────────────

    # Base : ratio de complétude documentaire (0-60 pts)
    base_score = f['ratio_total'] * 60

    # Bonus financier (0-25 pts) — uniquement si relevés présents
    bonus_financier = 0
    if f['has_releves'] and f['revenu_estime'] > 0:
        bonus_financier += min(f['revenu_norm'] * 15, 15)
        if f['ratio_montant_revenu'] <= 3:
            bonus_financier += 10
        elif f['ratio_montant_revenu'] <= 5:
            bonus_financier += 5
        bonus_financier -= f['has_incidents'] * 8  # pénalité incidents

    # Bonus qualité (0-10 pts)
    bonus_qualite = min(f['qualite_docs_score'] / 20 * 10, 10)

    # Pénalité âge (0-5 pts max)
    penalite_age = 0
    if f['age'] > 0:
        if f['age'] < 18 or f['age'] > 75:
            penalite_age = 5
        elif f['age'] < 21 or f['age'] > 68:
            penalite_age = 2

    # CORRECTION FINALE : score composite strictement croissant
    # Chaque terme positif est conditionné à has_doc=1
    # Aucun terme ne peut diminuer si on ajoute un document
    f['score_composite'] = round(
        max(0, min(100,
                   base_score + bonus_financier + bonus_qualite - penalite_age
                   )), 2
    )

    # ─────────────────────────────
    # 13. DIAGNOSTICS (non-features — pour l'API response)
    # ─────────────────────────────
    docs_manquants = [k for k in DOCS_OBLIGATOIRES if f[f'has_{k}'] == 0]
    f['docs_manquants'] = docs_manquants

    recommandations = []
    if 'cin' in docs_manquants:
        recommandations.append("Fournir la carte d'identité nationale (CIN)")
    if 'domicile' in docs_manquants:
        recommandations.append("Fournir un justificatif de domicile (facture STEG/SONEDE < 3 mois)")
    if 'releves' in docs_manquants:
        recommandations.append("Fournir les relevés bancaires ou la déclaration fiscale (3 derniers mois)")
    if 'projet' in docs_manquants:
        recommandations.append("Fournir la description du projet agricole")
    if 'titre_foncier' in docs_manquants:
        recommandations.append("Fournir le titre foncier ou contrat de location agricole")
    if 'carte_agri' in docs_manquants:
        recommandations.append("Fournir la carte d'agriculteur (CRDA/MDRT)")
    if f['has_incidents']:
        recommandations.append(f"Incidents bancaires détectés ({f['nb_incidents']}) — justification recommandée")
    if f['revenu_estime'] > 0 and f['ratio_montant_revenu'] > 5:
        recommandations.append(
            f"Montant demandé ({montant:,.0f} TND) élevé par rapport aux revenus estimés ({f['revenu_estime']:,.0f} TND)"
        )
    if f['age'] > 0 and (f['age'] < 18 or f['age'] > 70):
        recommandations.append(f"Âge du demandeur hors norme ({f['age']} ans)")
    # Suggestions non bloquantes pour docs optionnels
    if not f['has_assurance']:
        recommandations.append("Une assurance agricole (récolte, multirisque) renforcerait le dossier")
    if not f['has_garantie']:
        recommandations.append("Une garantie solidaire renforcerait le dossier")
    if not f['has_non_endettement']:
        recommandations.append("Une attestation de non-endettement faciliterait l'analyse")

    f['recommandations'] = recommandations

    return f


# ─────────────────────────────────────────────────────────────────────────────
# Utilitaires
# ─────────────────────────────────────────────────────────────────────────────

def features_to_dataframe(features_dict: dict) -> pd.DataFrame:
    numeric_only = {
        k: v for k, v in features_dict.items()
        if k not in ('docs_manquants', 'recommandations') and not isinstance(v, list)
    }
    return pd.DataFrame([numeric_only])


def get_feature_names() -> list:
    sample = build_features({}, {})
    return [k for k in sample if k not in ('docs_manquants', 'recommandations') and not isinstance(sample[k], list)]


def merge_with_german_credit(features_dict: dict, german_row: Optional[dict] = None) -> dict:
    if german_row is None:
        return features_dict
    housing_map = {'own': 2, 'rent': 1, 'free': 0}
    if features_dict.get('age', 0) == 0:
        features_dict['age'] = int(german_row.get('Age', 0))
    if features_dict.get('duree_mois', 0) == 0:
        features_dict['duree_mois'] = float(german_row.get('Duration', 0))
    if features_dict.get('montant_demande', 0) == 0:
        features_dict['montant_demande'] = float(german_row.get('Credit amount', 0))
    features_dict['type_logement_code'] = housing_map.get(german_row.get('Housing'), 1)
    return features_dict


# ─────────────────────────────────────────────────────────────────────────────
# TEST DE MONOTONIE — vérifie que le score est toujours croissant
# ─────────────────────────────────────────────────────────────────────────────

def verify_monotonicity():
    """
    Vérifie la propriété fondamentale :
    Pour tout dossier, ajouter un document ne peut jamais faire BAISSER le score.
    """
    print("\n" + "="*60)
    print("TEST DE MONOTONIE DU SCORE")
    print("="*60)

    from pipeline.extractor import extract_all

    # Dossier de base : 4 docs obligatoires
    base_texts = {
        'cin':             "Carte d'Identité Nationale - 12345678 - Né le 15/06/1980 - Expire 20/10/2028",
        'domicile':        "Facture STEG - Sfax - Octobre 2024 - 85 DT",
        'releves':         "BNA - Relevé compte courant - Solde 12.500 TND - Aucun incident",
        'projet':          "Projet olivier 5 ha Sfax - élevage ovin 100 têtes",
        'titre_foncier':   None,
        'carte_agri':      None,
        'non_endettement': None,
        'assurance':       None,
        'garantie':        None,
    }

    docs_supplementaires = [
        ('titre_foncier',   "Titre Foncier N°987654 - superficie 8 ha - Conservation Foncière Sfax"),
        ('carte_agri',      "Carte Agriculteur N°11223 - CRDA Sfax - Ministère de l'Agriculture"),
        ('non_endettement', "Attestation de non endettement - Trésorerie Générale - certifie que le demandeur ne doit aucune somme"),
        ('assurance',       "Police d'assurance récolte - STAR - Montant assuré 30.000 TND"),
        ('garantie',        "Lettre de garantie solidaire - caution solidaire - 12345678"),
    ]

    info = {'montant_demande': 30000, 'duree_mois': 60}

    current_texts = dict(base_texts)
    extracted = extract_all({k: v or '' for k, v in current_texts.items()})
    features  = build_features(extracted, info)
    score_base = features['score_composite']
    ratio_base = features['ratio_total']

    print(f"\nDossier base (4 docs) : score={score_base:.1f}, ratio={ratio_base:.3f}")

    all_ok = True
    score_prev = score_base

    for doc_name, doc_text in docs_supplementaires:
        current_texts[doc_name] = doc_text
        extracted = extract_all({k: v or '' for k, v in current_texts.items()})
        features  = build_features(extracted, info)
        score_new = features['score_composite']
        ratio_new = features['ratio_total']

        status = "✓ OK" if score_new >= score_prev - 0.01 else "✗ VIOLATION"
        if score_new < score_prev - 0.01:
            all_ok = False

        print(f"  + {doc_name:20s} → score={score_new:.1f} (Δ={score_new-score_prev:+.1f})  {status}")
        score_prev = score_new

    print(f"\nRésultat : {'✓ MONOTONIE RESPECTÉE' if all_ok else '✗ VIOLATIONS DÉTECTÉES'}")
    print("="*60 + "\n")
    return all_ok


# ─────────────────────────────────────────────────────────────────────────────
# Test
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    import sys
    import os
    sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

    from pipeline.extractor import extract_all

    test_ocr = {
        'cin':             "Carte d'Identité Nationale - 12345678 - Né le 15/06/1980 - Expire 20/10/2028",
        'domicile':        "Facture STEG - Sfax - Octobre 2024 - 85 DT",
        'releves':         "BNA - Relevé compte courant - Solde 15.000 TND - aucun incident",
        'projet':          "Projet olivier 8 ha Sfax + élevage ovin 150 têtes",
        'titre_foncier':   "Titre Foncier N°987654 - superficie 10 ha - Conservation Foncière",
        'carte_agri':      "Carte Agriculteur N°11223 - CRDA Sfax",
        'non_endettement': None,
        'assurance':       "Police assurance récolte - Montant assuré 30.000 TND - STAR",
        'garantie':        None,
    }

    extracted = extract_all({k: v or '' for k, v in test_ocr.items()})
    features  = build_features(extracted, {'montant_demande': 30000, 'duree_mois': 60})

    import json
    display = {k: v for k, v in features.items() if not isinstance(v, list)}
    print(json.dumps(display, indent=2, ensure_ascii=False))
    print(f"\nDocs manquants : {features['docs_manquants']}")
    print(f"Recommandations : {features['recommandations']}")
    print(f"\nScore composite : {features['score_composite']}")
    print(f"Total features numériques : {len(display)}")

    verify_monotonicity()