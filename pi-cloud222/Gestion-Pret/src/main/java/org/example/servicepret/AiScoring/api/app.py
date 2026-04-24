
import sys
import os
import re
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.concurrency import run_in_threadpool
from typing import List

from pipeline.scorer import score_dossier, load_model

app = FastAPI(title="Service de Scoring Agricole")


# ─────────────────────────────────────────────────────────────────────────────
# Chargement du modèle au démarrage
# ────────────────────────────────────────────────────────────────────────────

@app.on_event("startup")
def startup_event():
    load_model('models/model.pkl', 'models/features.pkl')


# ─────────────────────────────────────────────────────────────────────────────
# Configuration des types de documents
# ─────────────────────────────────────────────────────────────────────────────

DOC_ORDER = [
    'cin', 'domicile', 'releves', 'projet',
    'titre_foncier', 'carte_agri',
    'non_endettement', 'assurance', 'garantie',
]

# Niveau 2 : keywords dans le nom de fichier
DOC_KEYWORDS = {
    'cin':             ['cin', 'identit', 'cni', 'national', 'carte_id'],
    'domicile':        ['domicile', 'steg', 'sonede', 'residence', 'adresse', 'facture', 'electricite', 'eau'],
    'releves':         ['releve', 'bancaire', 'compte', 'fiscal', 'bank', 'bna', 'stb', 'biat', 'amen', 'attijari'],
    'projet':          ['projet', 'description', 'agricol', 'culture', 'activite'],
    'titre_foncier':   ['titre', 'foncier', 'cadastr', 'propriete', 'immatriculation'],
    'carte_agri':      ['carte', 'agriculteur', 'agricul', 'crda', 'mdrt'],
    'non_endettement': ['endettement', 'non_endet', 'attestation'],
    'assurance':       ['assurance', 'police', 'star', 'assur'],
    'garantie':        ['garantie', 'caution', 'solidaire'],
}

# Niveau 1 : signatures dans le CONTENU du PDF (priorité maximale)
# Peu importe le nom du fichier — on lit ce qu'il contient
DOC_CONTENT_SIGNATURES = {
    'cin': [
        "carte d'identité nationale", "carte d'identite nationale",
        "identité nationale", "identite nationale",
        "republique tunisienne", "république tunisienne",
        "date de naissance", "né le", "nee le",
        "date d'expiration", "expire le",
        "n° cin", "numero cin",
    ],
    'domicile': [
        "société tunisienne de l'electricité", "steg",
        "société nationale d'exploitation", "sonede",
        "facture de consommation", "consommation electrique",
        "certificat de résidence", "certificat de domicile",
        "résidence principale", "justificatif de domicile",
        "topnet", "ooredoo", "tunisie telecom",
    ],
    'releves': [
        "relevé de compte", "releve de compte",
        "extrait de compte",
        "compte courant", "compte d'épargne",
        "solde débiteur", "solde créditeur",
        "mouvement du compte", "opérations du compte",
        "date valeur", "libellé", "montant débit", "montant crédit",
        "bna", "stb", "biat", "amen bank", "attijari bank",
        "zitouna", "banque nationale agricole",
        "rib", "code swift", "bic",
        "déclaration fiscale", "declaration fiscale",
        "revenu imposable", "bénéfice imposable",
        "numéro de compte", "titulaire du compte",
    ],
    'projet': [
        "description du projet", "projet agricole",
        "plan d'exploitation",
        "type de culture", "type d'élevage",
        "surface agricole utile", "superficie cultivée",
        "hectares", " ha ",
        "oléiculture", "maraîchage", "arboriculture",
        "cheptel", "têtes",
        "gouvernorat", "délégation",
        "objectifs du projet",
    ],
    'titre_foncier': [
        "titre foncier",
        "conservation foncière", "conservation fonciere",
        "immatriculation foncière",
        "numéro de parcelle", "lot n°",
        "contrat de location agricole",
        "bail rural", "bail agricole", "fermage",
        "acte de propriété",
    ],
    'carte_agri': [
        "carte d'agriculteur", "carte d agriculteur",
        "exploitant agricole",
        "ministère de l'agriculture",
        "crda",
        "office de l'élevage et du pâturage",
        "groupement interprofessionnel",
    ],
    'non_endettement': [
        "attestation de non endettement",
        "attestation de non-endettement",
        "non endettement", "non-endettement",
        "aucune dette", "aucun endettement",
        "ne doit aucune somme",
        "ne présente aucune dette",
        "trésorerie générale", "tresorerie generale",
        "recette des finances",
        "direction générale des impôts",
        "certifie que", "certifions que",
        "à jour de ses obligations fiscales",
    ],
    'assurance': [
        "police d'assurance", "contrat d'assurance",
        "prime d'assurance",
        "star assurances", "comar", "gat assurance", "maghrebia",
        "assurance récolte", "assurance agricole",
        "assurance multirisque",
        "montant assuré", "couverture d'assurance",
        "bénéficiaire de l'assurance",
    ],
    'garantie': [
        "lettre de garantie solidaire",
        "garantie solidaire",
        "caution solidaire",
        "cautionnement",
        "le garant soussigné",
        "s'engage solidairement",
        "co-débiteur solidaire",
    ],
}


# ─────────────────────────────────────────────────────────────────────────────
# Fonctions de détection
# ─────────────────────────────────────────────────────────────────────────────

def detect_by_filename(filename: str, already_assigned: set) -> str | None:
    """Cherche des keywords dans le nom de fichier (strip timestamp Java)."""
    name = filename.lower().replace('-', '_').replace(' ', '_')
    name = re.sub(r'^\d+_', '', name)
    for doc_type, keywords in DOC_KEYWORDS.items():
        if doc_type not in already_assigned and any(kw in name for kw in keywords):
            return doc_type
    return None


def detect_doc_type(tmp_path: str, filename: str, already_assigned: set) -> tuple[str | None, str]:
    """
    Détection combinée contenu + nom de fichier.

    Stratégie :
      - Calcule un score contenu pour chaque type (nb signatures trouvées)
      - Si le nom du fichier confirme un type → bonus +5 sur ce type
      - Le type avec le score total le plus élevé gagne
      - Score minimum de 2 requis pour éviter les faux positifs
      - Si aucun type ne dépasse le seuil → fallback positionnel
    """
    # ── Score par contenu ────────────────────────────────────────────────────
    content_scores = {}
    try:
        import pdfplumber
        with pdfplumber.open(tmp_path) as pdf:
            pages_text = []
            for page in pdf.pages[:2]:
                t = page.extract_text()
                if t:
                    pages_text.append(t)
            text = "\n".join(pages_text).lower()

        if len(text.strip()) >= 20:
            for doc_type, signatures in DOC_CONTENT_SIGNATURES.items():
                if doc_type in already_assigned:
                    continue
                count = sum(1 for sig in signatures if sig in text)
                if count > 0:
                    content_scores[doc_type] = count
    except Exception as e:
        print(f"[DETECT] Erreur lecture contenu: {e}")

    # ── Score par nom de fichier ──────────────────────────────────────────────
    name_type = detect_by_filename(filename, already_assigned)

    # ── Combinaison : nom confirme → bonus +5 ────────────────────────────────
    combined_scores = dict(content_scores)

    if name_type:
        # Bonus fort si le nom confirme un type déjà détecté par contenu
        combined_scores[name_type] = combined_scores.get(name_type, 0) + 5

    print(f"[DETECT] '{filename}' → contenu={content_scores}, nom={name_type}, combiné={combined_scores}")

    if not combined_scores:
        return None, "non_detecte"

    best = max(combined_scores, key=lambda t: combined_scores[t])
    best_score = combined_scores[best]

    # Score minimum 2 : évite qu'un seul mot ambigu décide du type
    if best_score < 2:
        print(f"[DETECT] Score trop faible ({best_score}) → non détecté")
        return None, "non_detecte"

    method = "contenu+nom" if name_type and name_type == best else \
        ("nom" if best == name_type else "contenu")

    print(f"[DETECT] → {best} ({best_score} pts, méthode: {method})")
    return best, method


# ─────────────────────────────────────────────────────────────────────────────
# Endpoint de scoring
# ─────────────────────────────────────────────────────────────────────────────

@app.post("/scorer")
async def scorer(
        files: List[UploadFile] = File(...),
        service_id: int = Form(...),
        montant_demande: float = Form(...),
        duree_mois: int = Form(...),
):
    tmp_paths = []
    doc_paths = {key: None for key in DOC_ORDER}
    assigned  = set()

    try:
        unmatched = []

        # ── Étape 1 : Sauvegarde + détection ────────────────────────────────
        for upload in files:
            content = await upload.read()

            if len(content) == 0:
                print(f"[UPLOAD] ⚠ Fichier vide ignoré : {upload.filename}")
                continue

            suffix = Path(upload.filename).suffix.lower() or '.pdf'
            tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
            tmp.write(content)
            tmp.close()
            tmp_paths.append(tmp.name)

            if not os.path.exists(tmp.name) or os.path.getsize(tmp.name) == 0:
                print(f"[UPLOAD] ⚠ Échec écriture : {upload.filename}")
                continue

            # Détection en cascade (contenu → nom)
            doc_type, method = detect_doc_type(tmp.name, upload.filename, assigned)

            if doc_type:
                doc_paths[doc_type] = tmp.name
                assigned.add(doc_type)
                print(f"[UPLOAD] ✓ '{upload.filename}' → {doc_type} ({method})")
            else:
                unmatched.append((upload.filename, tmp.name, len(content)))
                print(f"[UPLOAD] ? '{upload.filename}' → non détecté")

        # ── Étape 2 : Fallback positionnel (PDFs scannés + noms génériques) ─
        if unmatched:
            print(f"\n[UPLOAD] ⚠ {len(unmatched)} fichier(s) non identifiés → fallback positionnel")
            for filename, path, size in unmatched:
                for doc_type in DOC_ORDER:
                    if doc_paths[doc_type] is None:
                        doc_paths[doc_type] = path
                        print(f"[UPLOAD] → Fallback : '{filename}' → {doc_type}")
                        break

        # ── Résumé ───────────────────────────────────────────────────────────
        print("\n[UPLOAD] Récapitulatif final :")
        for k, v in doc_paths.items():
            status = f"✓ {Path(v).name}" if v else "✗ absent"
            print(f"  {k:20s}: {status}")

        # ── Étape 3 : Scoring ────────────────────────────────────────────────
        applicant_info = {
            'montant_demande': montant_demande,
            'duree_mois':      duree_mois,
        }

        result = await run_in_threadpool(
            score_dossier,
            doc_paths,
            applicant_info,
        )

        print(f"\n[SCORING] Décision : {result.get('decision')} (score={result.get('score_solvabilite')})")

        return {
            'decision':          result.get('decision', 'ERREUR'),
            'score_solvabilite': result.get('score_solvabilite', 0),
            'prob_bad':          result.get('prob_bad', 0),
            'prob_good':         result.get('prob_good', 0),
            'message':           result.get('message', ''),
            'docs_manquants':    result.get('docs_manquants', []),
            'recommandations':   result.get('recommandations', []),
        }

    except Exception as e:
        import traceback
        traceback.print_exc()
        return {
            'decision':          'ERREUR',
            'score_solvabilite': 0,
            'prob_bad':          1.0,
            'prob_good':         0.0,
            'message':           str(e),
            'docs_manquants':    [],
            'recommandations':   [],
        }

    finally:
        for path in tmp_paths:
            try:
                if os.path.exists(path):
                    os.remove(path)
            except Exception:
                pass


# ─────────────────────────────────────────────────────────────────────────────
# Endpoints utilitaires
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    from pipeline.scorer import _MODEL
    return {"status": "ok", "model_loaded": _MODEL is not None}

@app.get("/docs-types")
def docs_types():
    return DOC_KEYWORDS


if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host='0.0.0.0', port=8000, reload=False)