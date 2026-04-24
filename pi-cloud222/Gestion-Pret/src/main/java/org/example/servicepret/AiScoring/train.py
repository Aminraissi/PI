"""
train.py — Entraînement du modèle XGBoost
Génère un dataset synthétique agricole tunisien, le fusionne avec
le German Credit Dataset, entraîne XGBoost et sauvegarde model.pkl
"""

import numpy as np
import pandas as pd
import pickle
import os
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.preprocessing import LabelEncoder
import xgboost as xgb

# ─────────────────────────────────────────────────────────────────────────────
# 1. Génération du dataset synthétique agricole tunisien
# ─────────────────────────────────────────────────────────────────────────────

def generate_synthetic_dataset(n_samples: int = 2000, random_state: int = 42) -> pd.DataFrame:
    """
    Génère n_samples dossiers agricoles synthétiques.
    Les features reflètent exactement la sortie de features.build_features().
    La cible 'risk' (0=Good, 1=Bad) est déterminée par règles métier.
    """
    rng = np.random.default_rng(random_state)

    n = n_samples

    # ── Présence des documents ────────────────────────────────────────────────
    # Agriculteurs sérieux : plus de chances d'avoir tous les docs
    profil_serieux = rng.random(n) > 0.4  # 60% de profils sérieux

    def doc_present(p_serieux, p_non_serieux):
        return (profil_serieux * rng.binomial(1, p_serieux, n) +
                (~profil_serieux) * rng.binomial(1, p_non_serieux, n)).astype(int)

    has_cin           = doc_present(0.99, 0.85)
    has_domicile      = doc_present(0.95, 0.70)
    has_releves       = doc_present(0.90, 0.55)
    has_projet        = doc_present(0.88, 0.60)
    has_titre_foncier = doc_present(0.85, 0.50)
    has_carte_agri    = doc_present(0.80, 0.45)
    has_non_endett    = doc_present(0.55, 0.20)  # optionnel
    has_assurance     = doc_present(0.60, 0.15)  # optionnel
    has_garantie      = doc_present(0.40, 0.10)  # optionnel

    # ── Scores documentaires ─────────────────────────────────────────────────
    score_obligatoire = (has_cin * 10 + has_domicile * 5 + has_releves * 15 +
                         has_projet * 10 + has_titre_foncier * 10 + has_carte_agri * 10)
    score_optionnel   = has_non_endett * 10 + has_assurance * 12 + has_garantie * 8
    score_total_docs  = score_obligatoire + score_optionnel

    # ── Features personnelles ─────────────────────────────────────────────────
    age = rng.integers(20, 70, n)
    montant_demande = rng.integers(5000, 150000, n)
    duree_mois = rng.choice([12, 24, 36, 48, 60, 84, 120], n)

    # ── Features agricoles ────────────────────────────────────────────────────
    surface_ha    = rng.exponential(8, n).clip(0.5, 100)
    culture_code  = rng.integers(0, 9, n)
    elevage_code  = rng.integers(0, 6, n)
    propriete_code = rng.choice([0, 1, 2], n, p=[0.1, 0.3, 0.6])
    projet_completude = rng.integers(0, 5, n)

    # ── Features financières ─────────────────────────────────────────────────
    revenu_estime = rng.integers(3000, 80000, n)
    solde_moyen   = revenu_estime * rng.uniform(0.2, 2.0, n)
    nb_incidents  = rng.choice([0, 0, 0, 1, 2, 3], n)  # majorité sans incident
    nb_releves    = rng.choice([0, 1, 2, 3], n, p=[0.1, 0.2, 0.3, 0.4])

    # ── Features dérivées ─────────────────────────────────────────────────────
    ratio_montant_revenu = np.minimum(montant_demande / (revenu_estime + 1), 10.0)
    revenu_norm   = np.minimum(revenu_estime / 50000.0, 1.0)
    montant_norm  = np.minimum(montant_demande / 200000.0, 1.0)
    duree_norm    = np.minimum(duree_mois / 120.0, 1.0)
    surface_norm  = np.minimum(surface_ha / 100.0, 1.0)
    ratio_obligatoire = score_obligatoire / 60.0

    nb_docs_total        = (has_cin + has_domicile + has_releves + has_projet +
                            has_titre_foncier + has_carte_agri +
                            has_non_endett + has_assurance + has_garantie)
    nb_docs_obligatoires = (has_cin + has_domicile + has_releves +
                            has_projet + has_titre_foncier + has_carte_agri)
    nb_docs_optionnels   = has_non_endett + has_assurance + has_garantie

    # ── Cible : risk (0=Good, 1=Bad) ─────────────────────────────────────────
    # Logique métier pour générer des labels réalistes
    prob_bad = np.zeros(n)

    # Documents incomplets → risque ++
    prob_bad += (1 - ratio_obligatoire) * 0.4

    # Incidents bancaires → risque fort
    prob_bad += nb_incidents * 0.15

    # Ratio montant/revenu élevé → risque
    prob_bad += np.clip(ratio_montant_revenu / 10.0, 0, 0.3)

    # Documents optionnels → réduction du risque
    prob_bad -= (has_assurance * 0.08 + has_non_endett * 0.06 + has_garantie * 0.05)

    # Propriété titre → confiance
    prob_bad -= (propriete_code == 2) * 0.05

    # Age
    prob_bad += (age < 25) * 0.1
    prob_bad -= (age > 50) * 0.05

    # Revenus élevés
    prob_bad -= revenu_norm * 0.1

    prob_bad = np.clip(prob_bad + rng.normal(0, 0.05, n), 0.05, 0.95)
    risk = rng.binomial(1, prob_bad, n)  # 1 = Bad

    # ── Assemblage DataFrame ──────────────────────────────────────────────────
    df = pd.DataFrame({
        'has_cin': has_cin, 'has_domicile': has_domicile,
        'has_releves': has_releves, 'has_projet': has_projet,
        'has_titre_foncier': has_titre_foncier, 'has_carte_agri': has_carte_agri,
        'has_non_endettement': has_non_endett, 'has_assurance': has_assurance,
        'has_garantie': has_garantie,
        'score_obligatoire': score_obligatoire,
        'score_optionnel': score_optionnel,
        'score_total_docs': score_total_docs,
        'ratio_obligatoire': ratio_obligatoire,
        'cin_valide': has_cin, 'cin_a_numero': has_cin,
        'age': age,
        'type_financier_code': (nb_releves > 0).astype(int),
        'nb_releves': nb_releves,
        'solde_moyen': solde_moyen,
        'revenu_estime': revenu_estime,
        'nb_incidents': nb_incidents,
        'has_incidents': (nb_incidents > 0).astype(int),
        'revenu_norm': revenu_norm,
        'culture_code': culture_code,
        'elevage_code': elevage_code,
        'surface_ha': surface_ha,
        'projet_completude': projet_completude,
        'has_culture': (culture_code > 0).astype(int),
        'has_elevage': (elevage_code > 0).astype(int),
        'surface_norm': surface_norm,
        'propriete_code': propriete_code,
        'titre_a_numero': has_titre_foncier,
        'surface_titre_ha': surface_ha * rng.uniform(0.8, 1.2, n),
        'carte_valide': has_carte_agri,
        'carte_a_numero': has_carte_agri,
        'non_endett_valide': has_non_endett,
        'assurance_code': has_assurance * rng.integers(1, 4, n),
        'assurance_valide': has_assurance,
        'garantie_valide': has_garantie,
        'garant_identifie': has_garantie * rng.binomial(1, 0.7, n),
        'montant_demande': montant_demande,
        'duree_mois': duree_mois,
        'montant_norm': montant_norm,
        'duree_norm': duree_norm,
        'ratio_montant_revenu': ratio_montant_revenu,
        'nb_docs_total': nb_docs_total,
        'nb_docs_obligatoires': nb_docs_obligatoires,
        'nb_docs_optionnels': nb_docs_optionnels,
        'profil_mixte': ((culture_code > 0) & (elevage_code > 0)).astype(int),
        'surface_coherente': rng.binomial(1, 0.7, n),
        'type_logement_code': rng.choice([0, 1, 2], n, p=[0.1, 0.3, 0.6]),
        'risk': risk
    })

    return df


# ─────────────────────────────────────────────────────────────────────────────
# 2. Fusion avec German Credit Dataset
# ─────────────────────────────────────────────────────────────────────────────

def load_and_merge_german(german_path: str, synthetic_df: pd.DataFrame) -> pd.DataFrame:
    """
    Charge le German Credit Dataset, mappe les colonnes compatibles
    et fusionne avec le dataset synthétique.
    """
    try:
        german = pd.read_csv(german_path)
    except FileNotFoundError:
        print(f"[WARN] German Credit non trouvé: {german_path}. On utilise uniquement les données synthétiques.")
        return synthetic_df

    # Colonnes compatibles
    german_mapped = pd.DataFrame()
    german_mapped['age']             = german.get('Age', pd.Series(dtype=float))
    german_mapped['duree_mois']      = german.get('Duration', pd.Series(dtype=float))
    german_mapped['montant_demande'] = german.get('Credit amount', pd.Series(dtype=float))

    housing_map = {'own': 2, 'rent': 1, 'free': 0}
    german_mapped['type_logement_code'] = german.get('Housing', pd.Series(dtype=str)).map(housing_map).fillna(1)

    # Remplir les colonnes manquantes avec des valeurs neutres
    for col in synthetic_df.columns:
        if col not in german_mapped.columns:
            if col == 'risk':
                # German Credit n'a pas de colonne risk explicite
                # On génère un label approximatif basé sur le score
                german_mapped['risk'] = (german_mapped['age'] < 30).astype(int)
            else:
                german_mapped[col] = synthetic_df[col].median() if synthetic_df[col].dtype != object else 0

    german_mapped = german_mapped.dropna(subset=['age', 'duree_mois', 'montant_demande'])

    print(f"[INFO] German Credit: {len(german_mapped)} lignes mappées")
    merged = pd.concat([synthetic_df, german_mapped[synthetic_df.columns]], ignore_index=True)
    print(f"[INFO] Dataset final: {len(merged)} lignes")
    return merged


# ─────────────────────────────────────────────────────────────────────────────
# 3. Entraînement XGBoost
# ─────────────────────────────────────────────────────────────────────────────

def train(
        german_path: str = 'german_credit_data.csv',
        output_model: str = 'models/model.pkl',
        output_features: str = 'models/features.pkl',
        n_synthetic: int = 2000,
        random_state: int = 42
):
    print("=== Génération du dataset synthétique ===")
    df = generate_synthetic_dataset(n_samples=n_synthetic, random_state=random_state)
    print(f"Distribution risk: {df['risk'].value_counts().to_dict()}")

    print("\n=== Fusion avec German Credit ===")
    df = load_and_merge_german(german_path, df)

    # Features et cible
    FEATURE_COLS = [c for c in df.columns if c != 'risk']
    X = df[FEATURE_COLS].fillna(0)
    y = df['risk']

    print(f"\n=== Features: {len(FEATURE_COLS)} ===")

    # Split train / test
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=random_state, stratify=y
    )

    # Ratio classes pour class_weight
    scale_pos_weight = (y_train == 0).sum() / (y_train == 1).sum()

    print("\n=== Entraînement XGBoost ===")
    model = xgb.XGBClassifier(
        n_estimators=300,
        max_depth=6,
        learning_rate=0.05,
        subsample=0.8,
        colsample_bytree=0.8,
        scale_pos_weight=scale_pos_weight,
        use_label_encoder=False,
        eval_metric='auc',
        random_state=random_state,
        early_stopping_rounds=20,
        verbosity=0,
    )

    model.fit(
        X_train, y_train,
        eval_set=[(X_test, y_test)],
        verbose=False
    )

    # Évaluation
    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]
    auc = roc_auc_score(y_test, y_prob)

    print(f"\nAUC-ROC: {auc:.4f}")
    print("\nRapport de classification:")
    print(classification_report(y_test, y_pred, target_names=['Good', 'Bad']))

    # Sauvegarde
    os.makedirs(os.path.dirname(output_model), exist_ok=True)
    with open(output_model, 'wb') as f:
        pickle.dump(model, f)
    with open(output_features, 'wb') as f:
        pickle.dump(FEATURE_COLS, f)

    print(f"\nModèle sauvegardé → {output_model}")
    print(f"Features sauvegardées → {output_features}")

    # Top features
    importance = pd.Series(
        model.feature_importances_, index=FEATURE_COLS
    ).sort_values(ascending=False)
    print("\nTop 10 features importantes:")
    print(importance.head(10).to_string())

    return model, FEATURE_COLS


if __name__ == '__main__':
    train(
        german_path='german_credit_data.csv',
        output_model='models/model.pkl',
        output_features='models/features.pkl',
        n_synthetic=2000,
    )