"""
fraud/metadata_extractor.py
Extraction des métadonnées cachées des documents PDF
"""

import re
from datetime import datetime
from pathlib import Path
from typing import Optional


# ─────────────────────────────────────────────────────────────────────────────
# Softwares suspects (outils de manipulation de documents)
# ─────────────────────────────────────────────────────────────────────────────

SUSPICIOUS_CREATORS = [
    "canva", "photoshop", "gimp", "inkscape", "illustrator",
    "paint", "pixlr", "fotor", "snapseed", "lightroom",
    "picsart", "remove.bg", "image editor", "photo editor",
    "wondershare", "pdfelement", "smallpdf", "ilovepdf",
    "pdfcandy", "sejda", "pdfforge", "pdf24",
    "nitro", "foxit phantom", "pdfpro",
    "libre office draw", "scribus",
]

LEGITIMATE_CREATORS = [
    "microsoft word", "microsoft office", "adobe acrobat",
    "wkhtmltopdf", "reportlab", "fpdf", "libreoffice writer",
    "openoffice", "google docs", "docx2pdf",
    "oracle", "sap", "sage", "evolution",
    "scanner", "scansnap", "brother", "hp scan", "epson scan",
    "twain", "isis", "wia",
    "steg", "sonede", "bna", "sti", "cnss",
]


def extract_metadata(file_path: str) -> dict:
    """
    Extrait les métadonnées d'un fichier PDF.
    Détecte les signaux suspects dans le créateur, les dates, etc.
    """
    result = {
        "creator": None,
        "producer": None,
        "creation_date": None,
        "modification_date": None,
        "author": None,
        "title": None,
        "num_pages": None,
        "file_size_kb": None,
        "signals": [],
        "metadata_risk": "LOW",
    }

    path = Path(file_path)
    if not path.exists():
        return result

    result["file_size_kb"] = round(path.stat().st_size / 1024, 1)

    # ── Taille suspecte ──────────────────────────────────────────────────────
    if result["file_size_kb"] < 5:
        result["signals"].append({
            "type": "TAILLE_SUSPECTE",
            "detail": f"Fichier très petit ({result['file_size_kb']} KB) — document peut-être vide ou tronqué",
            "severity": "MEDIUM",
        })

    # ── Lecture métadonnées via pypdf ────────────────────────────────────────
    try:
        import pypdf
        reader = pypdf.PdfReader(file_path)
        result["num_pages"] = len(reader.pages)

        meta = reader.metadata
        if meta:
            result["creator"]           = _clean_meta(meta.get("/Creator"))
            result["producer"]          = _clean_meta(meta.get("/Producer"))
            result["author"]            = _clean_meta(meta.get("/Author"))
            result["title"]             = _clean_meta(meta.get("/Title"))
            result["creation_date"]     = _parse_pdf_date(meta.get("/CreationDate"))
            result["modification_date"] = _parse_pdf_date(meta.get("/ModDate"))

    except Exception as e:
        print(f"[META] pypdf erreur: {e}")
        # Fallback pikepdf
        try:
            import pikepdf
            pdf = pikepdf.open(file_path)
            meta = pdf.docinfo
            result["creator"]           = _clean_meta(str(meta.get("/Creator", "")))
            result["producer"]          = _clean_meta(str(meta.get("/Producer", "")))
            result["author"]            = _clean_meta(str(meta.get("/Author", "")))
            result["creation_date"]     = _parse_pdf_date(str(meta.get("/CreationDate", "")))
            result["modification_date"] = _parse_pdf_date(str(meta.get("/ModDate", "")))
            result["num_pages"]         = len(pdf.pages)
        except Exception as e2:
            print(f"[META] pikepdf erreur: {e2}")

    # ── Analyse des signaux ──────────────────────────────────────────────────
    creator_lower = (result["creator"] or "").lower()
    producer_lower = (result["producer"] or "").lower()
    combined = creator_lower + " " + producer_lower

    # Créateur suspect (outil graphique ou éditeur PDF)
    for suspect in SUSPICIOUS_CREATORS:
        if suspect in combined:
            result["signals"].append({
                "type": "CREATEUR_SUSPECT",
                "detail": f"Document créé avec '{result['creator'] or result['producer']}' (outil de manipulation graphique)",
                "severity": "HIGH",
            })
            break

    # Pas de métadonnées du tout (document nettoyé intentionnellement ?)
    if not result["creator"] and not result["producer"] and not result["author"]:
        result["signals"].append({
            "type": "METADONNEES_ABSENTES",
            "detail": "Aucune métadonnée de création — possible suppression intentionnelle",
            "severity": "MEDIUM",
        })

    # Date de création très récente (document créé juste avant le dépôt ?)
    if result["creation_date"]:
        try:
            creation = datetime.fromisoformat(result["creation_date"])
            days_old = (datetime.now() - creation).days
            if days_old < 3:
                result["signals"].append({
                    "type": "CREATION_TRES_RECENTE",
                    "detail": f"Document créé il y a seulement {days_old} jour(s) — possiblement falsifié pour ce dossier",
                    "severity": "HIGH",
                })
            elif days_old < 7:
                result["signals"].append({
                    "type": "CREATION_RECENTE",
                    "detail": f"Document créé il y a {days_old} jours — à vérifier",
                    "severity": "MEDIUM",
                })
        except Exception:
            pass

    # Date de modification postérieure à la création
    if result["creation_date"] and result["modification_date"]:
        try:
            creation = datetime.fromisoformat(result["creation_date"])
            modif    = datetime.fromisoformat(result["modification_date"])
            if modif > creation:
                delta_hours = (modif - creation).total_seconds() / 3600
                if delta_hours > 1:
                    result["signals"].append({
                        "type": "MODIFICATION_POSTERIEURE",
                        "detail": f"Document modifié {int(delta_hours)}h après sa création initiale",
                        "severity": "MEDIUM" if delta_hours < 24 else "HIGH",
                    })
        except Exception:
            pass

    # Document sans pages
    if result["num_pages"] is not None and result["num_pages"] == 0:
        result["signals"].append({
            "type": "DOCUMENT_VIDE",
            "detail": "PDF sans pages détectables",
            "severity": "HIGH",
        })

    # ── Score global métadonnées ─────────────────────────────────────────────
    high_count = sum(1 for s in result["signals"] if s["severity"] == "HIGH")
    med_count  = sum(1 for s in result["signals"] if s["severity"] == "MEDIUM")

    if high_count >= 1:
        result["metadata_risk"] = "HIGH"
    elif med_count >= 2:
        result["metadata_risk"] = "MEDIUM"
    elif med_count >= 1:
        result["metadata_risk"] = "LOW_MEDIUM"
    else:
        result["metadata_risk"] = "LOW"

    return result


# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────

def _clean_meta(value) -> Optional[str]:
    if not value:
        return None
    s = str(value).strip()
    return s if s and s not in ("None", "null", "") else None


def _parse_pdf_date(raw) -> Optional[str]:
    """
    Parse les dates PDF format D:YYYYMMDDHHmmSS ou ISO.
    Retourne une string ISO ou None.
    """
    if not raw:
        return None
    raw = str(raw).strip()
    # Format PDF standard : D:20231015143000+01'00'
    m = re.search(r"D:(\d{4})(\d{2})(\d{2})(\d{2})?(\d{2})?(\d{2})?", raw)
    if m:
        year, month, day = m.group(1), m.group(2), m.group(3)
        hour   = m.group(4) or "00"
        minute = m.group(5) or "00"
        try:
            return f"{year}-{month}-{day}T{hour}:{minute}"
        except Exception:
            pass
    # Format ISO direct
    m2 = re.search(r"(\d{4}-\d{2}-\d{2})", raw)
    if m2:
        return m2.group(1)
    return None
