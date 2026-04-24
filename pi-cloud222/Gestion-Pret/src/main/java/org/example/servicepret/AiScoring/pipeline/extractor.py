"""
pipeline/extractor.py — Extraction des valeurs clés depuis le texte OCR brut

CORRECTIONS v2 :
1. extract_releves_bancaires : extraction montants plus robuste + plage réaliste
2. extract_cin : tolérance accrue sur formats de dates tunisiennes
3. extract_domicile : détection récence corrigée (comparaison mois/année correcte)
4. extract_projet_agricole : détection gouvernorat étendue
5. Tous les extracteurs : retournent des valeurs neutres (pas de None pénalisants)
   quand le document est absent ou illisible
"""

import re
from datetime import datetime
from typing import Optional


# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────

def _clean(text: str) -> str:
    """Normalise les espaces, retire les caractères parasites, met en minuscule."""
    text = text or ''
    text = re.sub(r'[\r\n\t]+', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    # Normalise les tirets longs
    text = text.replace('–', '-').replace('—', '-')
    text = text.strip().lower()
    return text


def _find_first(patterns: list, text: str) -> Optional[str]:
    """Retourne la première capture trouvée parmi les patterns."""
    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE | re.MULTILINE)
        if m:
            return m.group(1).strip()
    return None


def _is_present(text: Optional[str]) -> int:
    """
    Retourne 1 si le texte OCR contient suffisamment de contenu.
    Seuil : 20 chars pour capturer les docs courts.
    """
    if text is None:
        return 0
    return 1 if len(text.strip()) > 20 else 0


def _parse_amount(raw: str) -> Optional[float]:
    """
    Parse un montant depuis une chaîne brute.
    Gère : 12.500,000 / 12,500.00 / 12 500 / 12500
    """
    if not raw:
        return None
    try:
        s = raw.strip().replace('\xa0', '').replace(' ', '')
        # Format européen : 12.500,00 → virgule = décimale
        if re.match(r'^\d{1,3}(\.\d{3})+(,\d+)?$', s):
            s = s.replace('.', '').replace(',', '.')
        # Format américain : 12,500.00
        elif re.match(r'^\d{1,3}(,\d{3})+(\.\d+)?$', s):
            s = s.replace(',', '')
        # Simple virgule décimale : 1500,50
        else:
            s = s.replace(',', '.')
        return float(s)
    except (ValueError, TypeError):
        return None


# ─────────────────────────────────────────────────────────────────────────────
# Extracteurs par type de document
# ─────────────────────────────────────────────────────────────────────────────

def extract_cin(text: str) -> dict:
    """
    CIN tunisien : 8 chiffres.
    Extrait : numéro CIN, nom, prénom, date de naissance, date d'expiration.
    """
    result = {
        'present':    _is_present(text),
        'cin_number': None,
        'nom':        None,
        'prenom':     None,
        'age':        None,
        'expire':     None,
        'cin_valide': 0,
    }

    if not result['present']:
        return result

    t = _clean(text)

    # Numéro CIN (8 chiffres isolés)
    m = re.search(r'(?<!\d)(\d{8})(?!\d)', t)
    if m:
        result['cin_number'] = m.group(1)

    # Date de naissance — plusieurs patterns tunisiens
    dob_raw = _find_first([
        r'(?:né[e]?|naissance|date\s*de\s*naissance|birth)[^\d]{0,20}(\d{1,2}[\/\-\.\s]\d{1,2}[\/\-\.\s]\d{4})',
        r'\b(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{4})\b',
        r'(?:né[e]?|naissance)[^\d]{0,15}(\d{4})',
    ], t)

    if dob_raw:
        dob_normalized = re.sub(r'[\s]', '/', dob_raw).replace('-', '/').replace('.', '/')
        for fmt in ('%d/%m/%Y', '%m/%d/%Y'):
            try:
                birth = datetime.strptime(dob_normalized, fmt)
                result['age'] = datetime.now().year - birth.year
                # Vérification anti-absurde : âge entre 16 et 90
                if result['age'] < 16 or result['age'] > 90:
                    result['age'] = None
                break
            except ValueError:
                pass
        # Année seule
        if result['age'] is None:
            m_year = re.match(r'^(\d{4})$', dob_raw.strip())
            if m_year:
                age_calc = datetime.now().year - int(m_year.group(1))
                if 16 <= age_calc <= 90:
                    result['age'] = age_calc

    # Date d'expiration
    exp_raw = _find_first([
        r'(?:expire[^\d]{0,10}|expir[^\d]{0,10}|validit[^\d]{0,15}|valid(?:\s+until)?[^\d]{0,10})(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{4})',
        r'(?:date\s+d.expiration)[^\d]{0,10}(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{4})',
    ], t)

    if exp_raw:
        result['expire'] = exp_raw
        try:
            normalized = exp_raw.replace('-', '/').replace('.', '/')
            exp_date = datetime.strptime(normalized, '%d/%m/%Y')
            result['cin_valide'] = 1 if exp_date > datetime.now() else 0
        except Exception:
            result['cin_valide'] = 1  # bénéfice du doute si format non parseable
    else:
        result['cin_valide'] = 1  # pas de date visible → valide par défaut

    return result


def extract_domicile(text: str) -> dict:
    """
    Justificatif de domicile (facture STEG/SONEDE ou certificat de résidence).
    """
    result = {
        'present':       _is_present(text),
        'type_domicile': None,
        'gouvernorat':   None,
        'recent':        0,
    }

    if not result['present']:
        return result

    t = _clean(text)

    # Type de document
    if re.search(r'steg|électricité|electricite|facture.*élect|facture.*electr', t):
        result['type_domicile'] = 'steg'
    elif re.search(r'sonede|eau\s+potable|facture.*eau', t):
        result['type_domicile'] = 'sonede'
    elif re.search(r'résidence|residence|certificat.*domicile|domicile.*certif', t):
        result['type_domicile'] = 'certificat'
    elif re.search(r'topnet|ooredoo|tunisie\s+telecom|téléphone|internet|mobilis', t):
        result['type_domicile'] = 'telecom'
    else:
        result['type_domicile'] = 'autre'

    # Gouvernorats tunisiens
    gouvernorats = [
        'tunis', 'ariana', 'ben arous', 'manouba', 'nabeul', 'zaghouan',
        'bizerte', 'beja', 'béja', 'jendouba', 'kef', 'le kef', 'siliana',
        'sousse', 'monastir', 'mahdia', 'sfax', 'kairouan', 'kasserine',
        'sidi bouzid', 'gabes', 'gabès', 'medenine', 'médenine', 'tataouine',
        'gafsa', 'tozeur', 'kebili', 'kébili',
    ]
    for gov in gouvernorats:
        if gov in t:
            result['gouvernorat'] = gov
            break

    # CORRECTION : Détection récence correcte (mois + année)
    now = datetime.now()
    months_map = {
        'janvier': 1, 'février': 2, 'fevrier': 2, 'mars': 3, 'avril': 4,
        'mai': 5, 'juin': 6, 'juillet': 7, 'août': 8, 'aout': 8,
        'septembre': 9, 'octobre': 10, 'novembre': 11,
        'décembre': 12, 'decembre': 12,
    }

    # Cherche d'abord une date explicite (la plus fiable)
    date_raw = _find_first([r'(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{4})'], t)
    if date_raw:
        try:
            d = datetime.strptime(date_raw.replace('-', '/').replace('.', '/'), '%d/%m/%Y')
            delta_days = (now - d).days
            result['recent'] = 1 if 0 <= delta_days <= 92 else 0  # ~3 mois
            return result
        except Exception:
            pass

    # Sinon cherche mois + année dans le texte
    # Pattern : "octobre 2024" ou "octobre 24"
    month_year_match = re.search(
        r'(' + '|'.join(months_map.keys()) + r')\s+(\d{4}|\d{2})\b',
        t, re.IGNORECASE
    )
    if month_year_match:
        month_name = month_year_match.group(1).lower()
        year_str   = month_year_match.group(2)
        month_num  = months_map.get(month_name, 0)
        year_num   = int(year_str)
        if year_num < 100:
            year_num += 2000
        if month_num > 0:
            try:
                doc_date = datetime(year_num, month_num, 1)
                delta_months = (now.year - doc_date.year) * 12 + (now.month - doc_date.month)
                result['recent'] = 1 if 0 <= delta_months <= 3 else 0
            except Exception:
                pass
        return result

    # Fallback : mois seul (sans année) — moins fiable
    for month_name, month_num in months_map.items():
        if month_name in t:
            diff = (now.month - month_num) % 12
            result['recent'] = 1 if diff <= 3 else 0
            break

    return result


def extract_releves_bancaires(text: str) -> dict:
    """
    CORRECTION MAJEURE : extraction des montants financiers.
    Stratégie en 3 passes pour maximiser la récupération de données.
    """
    result = {
        'present':        _is_present(text),
        'type_financier': None,
        'solde_moyen':    None,
        'revenu_estime':  None,
        'incidents':      0,
        'nb_releves':     0,
    }

    if not result['present']:
        return result

    t = _clean(text)

    # Type de document financier
    if re.search(r'relevé|extrait\s+de\s+compte|compte\s+courant|rib|bna|stb|amen|biat|attijari|zitouna|banque', t):
        result['type_financier'] = 'releve'
        nb = len(re.findall(r'relevé\s+(?:n°|numéro|\d)', t))
        result['nb_releves'] = max(nb, 1)
    elif re.search(r'déclaration\s+fiscale|declaration\s+fiscale|impôt|dgf|dgelf|revenu\s+imposable|bénéfice', t):
        result['type_financier'] = 'fiscal'
        result['nb_releves'] = 1

    amounts = []

    # PASSE 1 : Montants avec unité monétaire explicite (priorité maximale)
    # Exemples : "12.500 TND", "8 500 DT", "1500,50 dinars"
    patterns_with_unit = [
        r'(\d[\d\s]*(?:[.,]\d{1,3})?)\s*(?:tnd|dt|dinars?)\b',
        r'(?:tnd|dt|dinars?)\s*:?\s*(\d[\d\s]*(?:[.,]\d{1,3})?)',
    ]
    for pat in patterns_with_unit:
        for raw in re.findall(pat, t, re.IGNORECASE):
            val = _parse_amount(raw)
            if val is not None and 50 < val < 1_000_000:
                amounts.append(val)

    # PASSE 2 : Montants en contexte financier (avec label)
    # Exemples : "solde : 12.500,000", "revenu net 8500"
    contexte_patterns = [
        r'(?:solde|balance|total|montant|net|crédit|credit|débit|debit)[^\d]{0,25}(\d[\d\s]*[.,]?\d*)',
        r'(?:revenu|salaire|traitement)[^\d]{0,20}(\d[\d\s]*[.,]?\d*)',
    ]
    for pat in contexte_patterns:
        for m in re.finditer(pat, t, re.IGNORECASE):
            val = _parse_amount(m.group(1))
            if val is not None and 50 < val < 1_000_000:
                amounts.append(val)

    # PASSE 3 : Montants avec format européen typique (ex: 3.500,000)
    # Uniquement si les passes précédentes n'ont rien trouvé
    if not amounts:
        fallback = re.findall(r'\b(\d{1,3}(?:[.\s]\d{3})+(?:,\d+)?)\b', t)
        for raw in fallback:
            val = _parse_amount(raw)
            if val is not None and 50 < val < 1_000_000:
                amounts.append(val)

    # CORRECTION : dédupliquer et filtrer les doublons proches
    if amounts:
        amounts_unique = sorted(set(round(a, 0) for a in amounts))
        result['solde_moyen']    = round(sum(amounts_unique) / len(amounts_unique), 2)
        result['revenu_estime']  = max(amounts_unique)

    # Recherche explicite du revenu/salaire net (priorité maximale)
    revenu_raw = _find_first([
        r'(?:revenu\s+net|salaire\s+net|net\s+à\s+payer|net\s+mensuel|traitement\s+net)[^\d]{0,20}(\d[\d\s]*(?:[.,]\d+)?)',
        r'(?:solde\s+final|solde\s+disponible|solde\s+créditeur)[^\d]{0,20}(\d[\d\s]*(?:[.,]\d+)?)',
        r'(?:revenu\s+imposable|bénéfice\s+net)[^\d]{0,20}(\d[\d\s]*(?:[.,]\d+)?)',
    ], t)
    if revenu_raw:
        v = _parse_amount(revenu_raw)
        if v and 50 < v < 1_000_000:
            result['revenu_estime'] = max(result.get('revenu_estime') or 0, v)

    # Incidents de paiement
    result['incidents'] = len(re.findall(
        r'rejet|impayé|impaye|incident|débiteur|dépassement|opposition|contentieux|blocage|litige',
        t
    ))

    return result


def extract_projet_agricole(text: str) -> dict:
    """
    Description du projet agricole.
    Extrait : type de culture, surface, gouvernorat, type d'élevage, complétude.
    """
    result = {
        'present':       _is_present(text),
        'type_culture':  None,
        'surface_ha':    None,
        'gouvernorat':   None,
        'type_elevage':  None,
        'completude':    0,
    }

    if not result['present']:
        return result

    t = _clean(text)
    score = 0

    # Types de cultures
    cultures = {
        'cereales':      r'blé|ble|orge|triticale|céréale|cereale|avoine|seigle',
        'olivier':       r'olivier|olive|oliveraie|oléiculture|oleiculture',
        'maraichage':    r'tomate|poivron|oignon|maraich|légume|legume|piment|courgette|concombre|aubergine|laitue',
        'arboriculture': r'amandier|figuier|grenadier|arboricul|pommier|poirier|cerisier|abricotier',
        'fourrage':      r'fourrage|luzerne|sorgho|sulla|bersim',
        'vigne':         r'vigne|raisin|viticulture|vignoble',
        'dattier':       r'dattier|palmier|datte|palmeraie|deglet',
        'agrumes':       r'orange|citron|agrume|mandarine|pamplemousse|clémentine',
    }
    for culture, pat in cultures.items():
        if re.search(pat, t):
            result['type_culture'] = culture
            score += 1
            break

    # Surface
    surface_raw = _find_first([
        r'(\d+(?:[,\.]\d+)?)\s*(?:hectare|hectares|ha)\b',
        r'(?:surface|superficie)[^\d]{0,20}(\d+(?:[,\.]\d+)?)',
        r'(\d+(?:[,\.]\d+)?)\s*ha\b',
    ], t)
    if surface_raw:
        try:
            result['surface_ha'] = float(surface_raw.replace(',', '.'))
            score += 1
        except ValueError:
            pass

    # Élevage
    elevages = {
        'ovin':    r'ovin|ovins|mouton|moutons|brebis|agneau|bélier',
        'bovin':   r'bovin|bovins|vache|vaches|taureau|génisse|veau',
        'caprin':  r'caprin|caprins|chèvre|chèvres|bouc',
        'avicole': r'poulet|poulets|avicole|volaille|dinde|pintade|poule',
        'apicole': r'abeille|apicole|miel|ruche|ruches|apiculture',
        'camelin': r'chameau|dromadaire|camelin',
    }
    for elev, pat in elevages.items():
        if re.search(pat, t):
            result['type_elevage'] = elev
            score += 1
            break

    # Gouvernorat (liste complète)
    gouvernorats = [
        'tunis', 'ariana', 'nabeul', 'sfax', 'sousse', 'monastir',
        'kairouan', 'kasserine', 'beja', 'béja', 'jendouba', 'gabes', 'gabès',
        'medenine', 'médenine', 'gafsa', 'tozeur', 'kebili', 'kébili',
        'tataouine', 'sidi bouzid', 'siliana', 'zaghouan', 'bizerte',
        'mahdia', 'kef', 'le kef', 'ben arous', 'manouba',
    ]
    for gov in gouvernorats:
        if gov in t:
            result['gouvernorat'] = gov
            score += 1
            break

    # Description complète (présence de mots-clés projet)
    if re.search(r'objectif|coût|financement|investissement|rendement|production', t):
        score += 1

    result['completude'] = min(score, 4)
    return result


def extract_titre_foncier(text: str) -> dict:
    """Titre foncier ou contrat de location agricole."""
    result = {
        'present':        _is_present(text),
        'type_propriete': None,
        'surface_ha':     None,
        'numero_titre':   None,
    }

    if not result['present']:
        return result

    t = _clean(text)

    if re.search(r'titre\s+foncier|immatriculation\s+fonci|conservation\s+fonci|propriété\s+fonci', t):
        result['type_propriete'] = 'titre'
    elif re.search(r'contrat\s+de\s+location|bail\s+rural|bail\s+agricole|loyer|fermage', t):
        result['type_propriete'] = 'location'
    else:
        result['type_propriete'] = 'autre'

    # Numéro de titre
    num = _find_first([
        r'(?:titre|n°|numéro|numero|réf|ref)[^\d]{0,10}(\d{4,})',
        r'(?:parcelle)[^\d]{0,10}(\d{3,})',
        r'(?:lot\s+n°?)[^\d]{0,5}(\d{3,})',
    ], t)
    if num:
        result['numero_titre'] = num

    # Surface
    surface_raw = _find_first([
        r'(\d+(?:[,\.]\d+)?)\s*(?:ha|hectare|hectares)\b',
        r'(?:superficie|surface)[^\d]{0,20}(\d+(?:[,\.]\d+)?)',
    ], t)
    if surface_raw:
        try:
            result['surface_ha'] = float(surface_raw.replace(',', '.'))
        except ValueError:
            pass

    return result


def extract_carte_agriculteur(text: str) -> dict:
    """Carte d'agriculteur délivrée par le MDRT / ministère de l'agriculture."""
    result = {
        'present':      _is_present(text),
        'numero_carte': None,
        'valide':       0,
    }

    if not result['present']:
        return result

    t = _clean(text)

    if re.search(r'carte.*agricul|agricul.*carte|exploitant\s+agricole|ministère.*agriculture|mdrt|crda|carte\s+d.agriculteur', t):
        result['valide'] = 1

    num = _find_first([
        r'(?:carte|n°|numéro|numero|matricule)[^\d]{0,10}(\d{4,})',
    ], t)
    if num:
        result['numero_carte'] = num
        result['valide'] = 1

    return result


def extract_non_endettement(text: str) -> dict:
    """Attestation de non-endettement."""
    result = {
        'present':        _is_present(text),
        'valide':         0,
        'date_emission':  None,
    }

    if not result['present']:
        return result

    t = _clean(text)

    # Détection élargie
    if re.search(
            r'non.{0,20}endettem|aucune\s+dette|pas\s+de\s+dette|ne\s+doit\s+pas|'
            r'sans\s+dette|attestation.*endettem|à\s+jour.*obligations|'
            r'ne\s+présente\s+aucune|certifi[eo]\s+que|trésorerie|recette\s+des\s+finances',
            t
    ):
        result['valide'] = 1

    date_raw = _find_first([r'(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{4})'], t)
    if date_raw:
        result['date_emission'] = date_raw

    return result


def extract_assurance(text: str) -> dict:
    """Police d'assurance agricole."""
    result = {
        'present':           _is_present(text),
        'type_assurance':    None,
        'montant_couverture': None,
        'valide':            0,
    }

    if not result['present']:
        return result

    t = _clean(text)

    types_assur = {
        'multirisque': r'multirisque|tous\s+risques|all\s+risk',
        'recolte':     r'récolte|recolte|grêle|sécheresse|intempérie|gel',
        'materiel':    r'matériel|tracteur|équipement|engins|machine',
        'elevage':     r'élevage|bétail|mortalité\s+animale|cheptel',
    }
    for typ, pat in types_assur.items():
        if re.search(pat, t):
            result['type_assurance'] = typ
            result['valide'] = 1
            break

    if not result['valide'] and re.search(r'assurance|assuré|police|prime|contrat|couverture', t):
        result['valide'] = 1

    montant_raw = _find_first([
        r'(?:couverture|assuré|montant\s+assuré|valeur\s+assurée|capital\s+assuré)[^\d]{0,25}(\d[\d\s]*(?:[.,]\d+)?)',
        r'(\d[\d\s]*(?:[.,]\d+)?)\s*(?:tnd|dt)',
    ], t)
    if montant_raw:
        v = _parse_amount(montant_raw)
        if v and 100 < v < 10_000_000:
            result['montant_couverture'] = v

    return result


def extract_garantie_solidaire(text: str) -> dict:
    """Lettre de garantie solidaire."""
    result = {
        'present':          _is_present(text),
        'valide':           0,
        'garant_identifie': 0,
    }

    if not result['present']:
        return result

    t = _clean(text)

    if re.search(r'garantie|garant|caution|solidaire|cautionnement|co.débiteur', t):
        result['valide'] = 1

    # CIN du garant (8 chiffres isolés)
    if re.search(r'(?<!\d)\d{8}(?!\d)', t):
        result['garant_identifie'] = 1

    return result


# ─────────────────────────────────────────────────────────────────────────────
# Point d'entrée principal
# ─────────────────────────────────────────────────────────────────────────────

EXTRACTORS = {
    'cin':              extract_cin,
    'domicile':         extract_domicile,
    'releves':          extract_releves_bancaires,
    'projet':           extract_projet_agricole,
    'titre_foncier':    extract_titre_foncier,
    'carte_agri':       extract_carte_agriculteur,
    'non_endettement':  extract_non_endettement,
    'assurance':        extract_assurance,
    'garantie':         extract_garantie_solidaire,
}


def extract_all(ocr_texts: dict) -> dict:
    """
    Applique l'extracteur correspondant à chaque document OCR.

    Paramètre:
        ocr_texts: {nom_doc: texte_brut}  — "" ou None pour les absents

    Retourne:
        {nom_doc: {champs extraits}}
    """
    extracted = {}
    for doc_name, text in ocr_texts.items():
        extractor = EXTRACTORS.get(doc_name)
        if extractor:
            result = extractor(text or '')
            extracted[doc_name] = result
            if result.get('present'):
                print(f"[EXTRACT] {doc_name}: présent → {result}")
            else:
                print(f"[EXTRACT] {doc_name}: absent ou texte insuffisant")
        else:
            extracted[doc_name] = {'present': 0}
    return extracted


# ─────────────────────────────────────────────────────────────────────────────
# Test
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    test = {
        'cin':     "Carte d'Identité Nationale République Tunisienne - 12345678 - Né le 15/06/1980 - Expire 20/10/2028",
        'projet':  "Description projet agricole: Culture d'olivier sur 5.5 ha à Sfax, accompagné d'élevage ovin (200 têtes).",
        'releves': "BNA - Relevé compte courant N°123456 - Solde: 12.500,000 TND - Aucun incident de paiement",
        'domicile': "Facture STEG - Adresse: 12 rue des oliviers, Sfax - Octobre 2024 - Montant: 85 DT",
        'titre_foncier': "Titre Foncier N°987654 - Superficie: 10 ha - Conservation Foncière de Sfax",
        'carte_agri': "Carte Agriculteur N°11223 - CRDA Sfax - Ministère de l'Agriculture",
        'non_endettement': "Attestation de non endettement - Trésorerie Générale - certifie que le demandeur ne doit aucune somme",
        'assurance': "Police d'assurance récolte - Montant assuré: 30.000 TND - STAR",
        'garantie':  "Lettre de garantie solidaire - 12345678 - s'engage solidairement",
    }
    result = extract_all(test)
    import json
    print(json.dumps(result, indent=2, ensure_ascii=False))